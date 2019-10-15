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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.Qr;
import net.bither.ui.base.DropdownMessage;
import net.bither.util.FileUtil;
import net.bither.util.ImageFileUtil;
import net.bither.util.ImageManageUtil;
import net.bither.util.LogUtil;
import net.bither.util.PermissionUtil;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

import java.util.List;

public class DialogFragmentFancyQrCodePager extends DialogFragment implements View.OnClickListener {
    public static interface QrCodeThemeChangeListener {
        public void qrCodeThemeChangeTo(Qr.QrCodeTheme theme);
    }

    public static final String FragmentTag = "DialogFragmentFancyQrCodePager";
    public static final String ContentKey = "Content";
    public static final String VanityLengthKey = "VanityLength";

    private static final float VanityShareGapRate = 0.1f;
    private static final float VanityShareMarginRate = 0.1f;
    private static final float VanityShareQrSizeRate = 0.9f;
    private static final float VanityShareWaterMarkHeightRate = 0.1f;

    private View vContainer;
    private TextView tvAddress;
    private String content;
    private int vanityLength;
    private ViewPager pager;
    private ImageButton tbtnShowAvatar;
    private View ivShowAvatarSeparator;
    private PagerAdapter adapter;
    private QrCodeThemeChangeListener listener;
    private Activity activity;

    public static DialogFragmentFancyQrCodePager newInstance(String content, int vanityLength) {
        DialogFragmentFancyQrCodePager dialog = new DialogFragmentFancyQrCodePager();
        Bundle bundle = new Bundle();
        bundle.putString(ContentKey, content);
        bundle.putInt(VanityLengthKey, vanityLength);
        dialog.setArguments(bundle);
        return dialog;
    }

    public static DialogFragmentFancyQrCodePager newInstance(String content) {
        return newInstance(content, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.QrCodePager);
        content = getArguments().getString(ContentKey);
        vanityLength = getArguments().getInt(VanityLengthKey, 0);
        adapter = new PagerAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        vContainer = inflater.inflate(R.layout.dialog_fancy_qr_code_pager, container, false);
        tvAddress = (TextView) vContainer.findViewById(R.id.tv_address);
        pager = (ViewPager) vContainer.findViewById(R.id.pager);
        tbtnShowAvatar = (ImageButton) vContainer.findViewById(R.id.cbx_show_avatar);
        ivShowAvatarSeparator = vContainer.findViewById(R.id.iv_show_avatar_separator);
        vContainer.setOnClickListener(this);
        vContainer.findViewById(R.id.ibtn_share).setOnClickListener(this);
        vContainer.findViewById(R.id.ibtn_save).setOnClickListener(this);
        pager.setOffscreenPageLimit(1);
        int size = Math.min(UIUtil.getScreenWidth(), UIUtil.getScreenHeight());
        pager.getLayoutParams().width = size;
        pager.getLayoutParams().height = (int) (size * (vanityLength > 0 ?
                DialogFragmentFancyQrCodeSinglePage.VanitySizeRate : 1));
        pager.setAdapter(adapter);
        pager.setCurrentItem(AppSharedPreference.getInstance().getFancyQrCodeTheme().ordinal());
        tbtnShowAvatar.setOnClickListener(showAvatarCheckedChange);
        configureVanityAddress();
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
            tbtnShowAvatar.setSelected(true);
        } else {
            ivShowAvatarSeparator.setVisibility(View.GONE);
            tbtnShowAvatar.setVisibility(View.GONE);
            tbtnShowAvatar.setSelected(false);
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
        if (PermissionUtil.isWriteExternalStoragePermission(activity, BitherSetting.REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE)) {
            new SaveThread().start();
        }
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

    private View.OnClickListener showAvatarCheckedChange = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            tbtnShowAvatar.setSelected(!tbtnShowAvatar.isSelected());
            FragmentManager manager = getChildFragmentManager();
            List<Fragment> fragments = manager.getFragments();
            if (fragments == null) {
                return;
            }
            for (Fragment f : fragments) {
                if (f instanceof DialogFragmentFancyQrCodeSinglePage) {
                    ((DialogFragmentFancyQrCodeSinglePage) f).setShowAvatar(tbtnShowAvatar
                            .isSelected());
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
            return DialogFragmentFancyQrCodeSinglePage.newInstance(content, theme, vanityLength >
                    0).setShowAvatar
                    (tbtnShowAvatar.isSelected()).setOnClickListener(DialogFragmentFancyQrCodePager.this);
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
            Bitmap qr = page.getQrCode();
            if (vanityLength > 0) {
                Bitmap bmpWaterMark = BitmapFactory.decodeResource(getResources(), R.drawable
                        .pin_code_water_mark);
                int qrSize = (int) (qr.getHeight() * VanityShareQrSizeRate);
                int tvWidth = tvAddress.getWidth();
                int tvHeight = tvAddress.getHeight();
                int width = Math.max(tvWidth, qrSize);
                int margin = (int) (width * VanityShareMarginRate);
                int waterMarkHeight = (int) (qrSize * VanityShareWaterMarkHeightRate);
                int waterMarkWidth = waterMarkHeight * bmpWaterMark.getWidth() / bmpWaterMark
                        .getHeight();
                Bitmap bmp = Bitmap.createBitmap(width + margin * 2, qrSize + tvHeight +
                        waterMarkHeight + (int) (VanityShareGapRate * qrSize) +
                        margin * 2, Bitmap.Config.ARGB_8888);
                Bitmap tvBmp = ImageManageUtil.getBitmapFromView(tvAddress);
                Canvas c = new Canvas(bmp);
                c.drawColor(getResources().getColor(R.color.vanity_address_qr_bg));
                c.drawBitmap(tvBmp, (bmp.getWidth() - tvBmp.getWidth()) / 2, margin, null);
                c.drawBitmap(qr, null, new Rect((bmp.getWidth() - qrSize) / 2, bmp.getHeight() -
                        margin - waterMarkHeight - qrSize, (bmp.getWidth() - qrSize) / 2 +
                        qrSize, bmp.getHeight() - margin - waterMarkHeight), null);
                c.drawBitmap(bmpWaterMark, null, new Rect((bmp.getWidth() - waterMarkWidth) / 2,
                        bmp.getHeight() - margin - waterMarkHeight, (bmp.getWidth() -
                        waterMarkWidth) / 2 + waterMarkWidth, bmp.getHeight() - margin), null);
                return bmp;
            } else {
                return qr;
            }
        }
        return null;
    }

    private void configureVanityAddress() {
        if (vanityLength > 0) {
            float radiusRate = 0.36f;
            float dxRate = 0f;
            float dyRate = 0f;
            float size = tvAddress.getTextSize();
            SpannableStringBuilder spannable = new SpannableStringBuilder(content);

            ShadowSpan shadow = new ShadowSpan(size * radiusRate, size * dxRate, size * dyRate,
                    getResources().getColor(R.color.vanity_address_glow));
            RelativeSizeSpan bigger = new RelativeSizeSpan(1.3f);
            ForegroundColorSpan color = new ForegroundColorSpan(getResources().getColor(R.color
                    .vanity_address_text));
            spannable.setSpan(shadow, 0, vanityLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(bigger, 0, vanityLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(color, 0, vanityLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            spannable.insert(vanityLength, " ");
            RelativeSizeSpan smaller = new RelativeSizeSpan(0.6f);
            spannable.setSpan(smaller, vanityLength, vanityLength + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvAddress.setText(spannable);
        } else {
            tvAddress.setText(content);
        }
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

    private class ShadowSpan extends CharacterStyle {
        private float dx;
        private float dy;
        private float radius;
        private int color;

        public ShadowSpan(float radius, float dx, float dy, int color) {
            this.radius = radius;
            this.dx = dx;
            this.dy = dy;
            this.color = color;
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.setShadowLayer(radius, dx, dy, color);
        }
    }
}
