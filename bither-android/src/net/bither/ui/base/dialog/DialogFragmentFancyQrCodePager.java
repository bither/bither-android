/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.ui.base.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import net.bither.R;
import net.bither.bitherj.utils.LogUtil;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;
import net.bither.util.FileUtil;
import net.bither.util.ImageFileUtil;
import net.bither.util.Qr;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

import java.util.List;

/**
 * Created by songchenwen on 14-6-6.
 */
public class DialogFragmentFancyQrCodePager extends DialogFragment implements View.OnClickListener {
    public static interface QrCodeThemeChangeListener {
        public void qrCodeThemeChangeTo(Qr.QrCodeTheme theme);
    }

    public static final String FragmentTag = "DialogFragmentFancyQrCodePager";
    public static final String ContentKey = "Content";

    private View vContainer;
    private String content;
    private ViewPager pager;
    private CheckBox tbtnShowAvatar;
    private View ivShowAvatarSeparator;
    private PagerAdapter adapter;
    private QrCodeThemeChangeListener listener;
    private Activity activity;

    public static DialogFragmentFancyQrCodePager newInstance(String content) {
        DialogFragmentFancyQrCodePager dialog = new DialogFragmentFancyQrCodePager();
        Bundle bundle = new Bundle();
        bundle.putString(ContentKey, content);
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.QrCodePager);
        content = getArguments().getString(ContentKey);
        adapter = new PagerAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        vContainer = inflater.inflate(R.layout.dialog_fancy_qr_code_pager, container, false);
        pager = (ViewPager) vContainer.findViewById(R.id.pager);
        tbtnShowAvatar = (CheckBox) vContainer.findViewById(R.id.cbx_show_avatar);
        ivShowAvatarSeparator = vContainer.findViewById(R.id.iv_show_avatar_separator);
        vContainer.setOnClickListener(this);
        vContainer.findViewById(R.id.ibtn_share).setOnClickListener(this);
        vContainer.findViewById(R.id.ibtn_save).setOnClickListener(this);
        pager.setOffscreenPageLimit(1);
        int size = Math.min(UIUtil.getScreenWidth(), UIUtil.getScreenHeight());
        pager.getLayoutParams().width = pager.getLayoutParams().height = size;
        pager.setAdapter(adapter);
        pager.setCurrentItem(AppSharedPreference.getInstance().getFancyQrCodeTheme().ordinal());
        tbtnShowAvatar.setOnCheckedChangeListener(showAvatarCheckedChange);
        return vContainer;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onStart() {
        activity = getActivity();
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        if (AppSharedPreference.getInstance().hasUserAvatar()) {
            ivShowAvatarSeparator.setVisibility(View.VISIBLE);
            tbtnShowAvatar.setVisibility(View.VISIBLE);
            tbtnShowAvatar.setChecked(true);
        } else {
            ivShowAvatarSeparator.setVisibility(View.GONE);
            tbtnShowAvatar.setVisibility(View.GONE);
            tbtnShowAvatar.setChecked(false);
        }
        super.onStart();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Qr.QrCodeTheme theme = getActiveTheme();
        if (theme != null && theme.ordinal() != AppSharedPreference.getInstance()
                .getFancyQrCodeTheme().ordinal()) {
            AppSharedPreference.getInstance().setFancyQrCodeTheme(theme);
            if (listener != null) {
                listener.qrCodeThemeChangeTo(theme);
            }
        }
        super.onDismiss(dialog);
    }

    private void share() {
        new ShareThread().start();
    }

    private void save() {
        new SaveThread().start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibtn_share:
                share();
                break;
            case R.id.ibtn_save:
                save();
                break;
            default:
                dismiss();
                break;
        }
    }

    private CompoundButton.OnCheckedChangeListener showAvatarCheckedChange = new CompoundButton
            .OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            FragmentManager manager = getChildFragmentManager();
            List<Fragment> fragments = manager.getFragments();
            if (fragments == null) {
                return;
            }
            for (Fragment f : fragments) {
                if (f instanceof DialogFragmentFancyQrCodeSinglePage) {
                    ((DialogFragmentFancyQrCodeSinglePage) f).setShowAvatar(isChecked);
                }
            }
        }
    };

    private class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter() {
            super(getChildFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            Qr.QrCodeTheme theme = null;
            if (position >= 0 && position < Qr.QrCodeTheme.values().length) {
                theme = Qr.QrCodeTheme.values()[position];
            }
            return DialogFragmentFancyQrCodeSinglePage.newInstance(content,
                    theme).setShowAvatar(tbtnShowAvatar.isChecked()).setOnClickListener
                    (DialogFragmentFancyQrCodePager.this);
        }

        @Override
        public int getCount() {
            return Qr.QrCodeTheme.values().length;
        }
    }

    private void dismissInAnyThread() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                dismissAllowingStateLoss();
            }
        });
    }

    private class SaveThread extends Thread {
        @Override
        public void run() {
            Bitmap bmp = getQrCode();
            dismissInAnyThread();
            if (bmp == null) {
                return;
            }
            long time = System.currentTimeMillis();
            ImageFileUtil.saveImageToDcim(bmp, 0, time);
            DropdownMessage.showDropdownMessage(activity, R.string.fancy_qr_code_save_success);
        }
    }

    private class ShareThread extends Thread {
        @Override
        public void run() {
            Bitmap bmp = getQrCode();
            dismissInAnyThread();
            if (bmp == null) {
                LogUtil.w("QR", "share qr code null");
                DropdownMessage.showDropdownMessage(activity, R.string.market_share_failed);
                return;
            }
            final Uri uri = FileUtil.saveShareImage(bmp);
            if (uri == null) {
                DropdownMessage.showDropdownMessage(activity, R.string.market_share_failed);
                return;
            }
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setType("image/jpg");
                    try {
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        DropdownMessage.showDropdownMessage(activity, R.string.market_share_failed);
                    }
                }
            });
        }
    }

    public Bitmap getQrCode() {
        Fragment f = getActiveFragment();
        if (f == null) {
            LogUtil.w("QR", "share qr active fragment null");
        }
        if (f != null && f instanceof DialogFragmentFancyQrCodeSinglePage) {
            DialogFragmentFancyQrCodeSinglePage page = (DialogFragmentFancyQrCodeSinglePage) f;
            return page.getQrCode();
        }
        return null;
    }

    public Qr.QrCodeTheme getActiveTheme() {
        Fragment f = getActiveFragment();
        if (f != null && f instanceof DialogFragmentFancyQrCodeSinglePage) {
            DialogFragmentFancyQrCodeSinglePage page = (DialogFragmentFancyQrCodeSinglePage) f;
            return page.getTheme();
        }
        return null;
    }

    public Fragment getActiveFragment() {
        Fragment localFragment = null;
        if (this.pager == null) {
            return localFragment;
        }
        localFragment = getFragmentAtIndex(pager.getCurrentItem());
        return localFragment;
    }

    public Fragment getFragmentAtIndex(int i) {
        String str = StringUtil.makeFragmentName(this.pager.getId(), i);
        return getChildFragmentManager().findFragmentByTag(str);
    }

    public DialogFragmentFancyQrCodePager setQrCodeThemeChangeListener(QrCodeThemeChangeListener
                                                                               listener) {
        this.listener = listener;
        return this;
    }

}
