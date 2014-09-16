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

package net.bither.fragment.cold;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.QrCodeActivity;
import net.bither.R;
import net.bither.ScanActivity;
import net.bither.ScanQRCodeTransportActivity;
import net.bither.activity.cold.ColdActivity;
import net.bither.activity.cold.ColdAdvanceActivity;
import net.bither.activity.cold.SignTxActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogPassword;

import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.AnimationUtil;
import net.bither.util.BackupUtil;
import net.bither.util.BackupUtil.BackupListener;
import net.bither.util.DateTimeUtil;
import net.bither.util.FileUtil;
import net.bither.util.KeyUtil;
import net.bither.util.SecureCharSequence;
import net.bither.util.StringUtil;

import java.util.Date;
import java.util.List;

public class OptionColdFragment extends Fragment implements Selectable {
    private int ONE_HOUR = 1 * 60 * 60 * 1000;
    private final int duration = 1000;

    private Button btnGetSign;
    private Button btnCloneTo;
    private Button btnCloneFrom;
    private Button btnBackupTime;
    private Button btnAdvance;
    private FrameLayout flBackTime;
    private ProgressBar pbBackTime;
    private TextView tvVersion;
    private LinearLayout llQrForAll;
    private DialogProgress dp;


    private OnClickListener toSignActivityClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (AddressManager.getInstance().getPrivKeyAddresses() == null
                    || AddressManager.getInstance().getPrivKeyAddresses().size() == 0) {
                DropdownMessage.showDropdownMessage(getActivity(), R.string.private_key_is_empty);
                return;
            }
            Intent intent = new Intent(getActivity(), SignTxActivity.class);
            startActivity(intent);
        }
    };
    private OnClickListener cloneToClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            new DialogPassword(getActivity(), new IDialogPasswordListener() {
                @Override
                public void onPasswordEntered(SecureCharSequence password) {
                    password.wipe();
                    String content = PrivateKeyUtil.getPrivateKeyStringFromAllPrivateAddresses();
                    Intent intent = new Intent(getActivity(), QrCodeActivity.class);
                    intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                            getString(R.string.clone_to_title));
                    intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, content);
                    startActivity(intent);
                }
            }).show();
        }
    };
    private OnClickListener cloneFromClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), ScanQRCodeTransportActivity.class);
            intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                    getString(R.string.clone_from_title));
            startActivityForResult(intent, BitherSetting.INTENT_REF.CLONE_FROM_REQUEST_CODE);
        }

        ;
    };
    private OnClickListener qrForAllClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            String content = "";
            List<Address> addresses = AddressManager.getInstance().getPrivKeyAddresses();
            for (int i = 0;
                 i < addresses.size();
                 i++) {
                String pubStr = Utils.bytesToHexString(addresses.get(i).getPubKey());
                content += pubStr;
                if (i < addresses.size() - 1) {
                    content += StringUtil.QR_CODE_SPLIT;
                }
            }
            Intent intent = new Intent(getActivity(), QrCodeActivity.class);
            intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, content);
            intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                    getString(R.string.qr_code_for_all_addresses_title));
            startActivity(intent);
        }
    };

    private OnClickListener advanceClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), ColdAdvanceActivity.class);
            startActivity(intent);
        }
    };

    private OnClickListener backupTimeListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (FileUtil.existSdCardMounted()) {
                long backupTime = AppSharedPreference.getInstance().getLastBackupkeyTime().getTime();
                if (backupTime + ONE_HOUR < System.currentTimeMillis()) {
                    backupPrivateKey();
                } else {
                    DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(getActivity(),
                            getString(R.string.backup_again), new Runnable() {
                        public void run() {
                            backupPrivateKey();
                        }
                    }
                    );
                    dialogConfirmTask.show();
                }
            }
        }
    };

    @Override
    public void onSelected() {
        configureCloneButton();
        configureQrForAll();
    }

    private void configureCloneButton() {
        if (AddressManager.getInstance().getPrivKeyAddresses() != null && AddressManager.getInstance().getPrivKeyAddresses()
                .size() > 0) {
            btnCloneFrom.setVisibility(View.GONE);
            btnCloneTo.setVisibility(View.VISIBLE);
        } else {
            btnCloneFrom.setVisibility(View.VISIBLE);
            btnCloneTo.setVisibility(View.GONE);
        }
    }

    private void configureQrForAll() {
        if (AddressManager.getInstance().getPrivKeyAddresses() != null && AddressManager.getInstance().getPrivKeyAddresses()
                .size() > 0) {
            llQrForAll.setVisibility(View.VISIBLE);
        } else {
            llQrForAll.setVisibility(View.GONE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String content;
            DialogPassword dialogPassword;
            switch (requestCode) {
                case BitherSetting.INTENT_REF.CLONE_FROM_REQUEST_CODE:
                    content = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                    dialogPassword = new DialogPassword(getActivity(),
                            new CloneFromPasswordListenerI(content));
                    dialogPassword.setCheckPre(false);
                    dialogPassword.setTitle(R.string.clone_from_password);
                    dialogPassword.show();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cold_option, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        configureCloneButton();
        configureQrForAll();
        showBackupTime();
    }

    private void initView(View view) {
        btnGetSign = (Button) view.findViewById(R.id.btn_get_sign);
        btnCloneTo = (Button) view.findViewById(R.id.btn_clone_to);
        btnCloneFrom = (Button) view.findViewById(R.id.btn_clone_from);
        btnAdvance = (Button) view.findViewById(R.id.btn_advance);
        llQrForAll = (LinearLayout) view.findViewById(R.id.ll_qr_all_keys);
        tvVersion = (TextView) view.findViewById(R.id.tv_version);
        flBackTime = (FrameLayout) view.findViewById(R.id.ll_back_up);
        pbBackTime = (ProgressBar) view.findViewById(R.id.pb_back_up);
        setPbBackTimeSize();
        String version = null;
        try {
            version = getActivity().getPackageManager().getPackageInfo(getActivity()
                    .getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        if (version != null) {
            tvVersion.setText(version);
            tvVersion.setVisibility(View.VISIBLE);
        } else {
            tvVersion.setVisibility(View.GONE);
        }
        dp = new DialogProgress(getActivity(), R.string.please_wait);
        btnGetSign.setOnClickListener(toSignActivityClickListener);
        btnCloneTo.setOnClickListener(cloneToClick);
        btnCloneFrom.setOnClickListener(cloneFromClick);
        llQrForAll.setOnClickListener(qrForAllClick);
        btnAdvance.setOnClickListener(advanceClick);
        btnBackupTime = (Button) view.findViewById(R.id.btn_backup_time);
        btnBackupTime.setOnClickListener(backupTimeListener);
        showBackupTime();

    }

    private void showBackupTime() {

        if (FileUtil.existSdCardMounted()) {
            Date date = AppSharedPreference.getInstance().getLastBackupkeyTime();
            if (date == null) {
                flBackTime.setVisibility(View.GONE);
            } else {
                flBackTime.setVisibility(View.VISIBLE);
                String relativeDate = DateTimeUtil.getRelativeDate(getActivity(), date).toString();
                btnBackupTime.setText(StringUtil.format(getString(R.string.last_time_of_back_up)
                        + " ", relativeDate));
            }
        } else {
            flBackTime.setVisibility(View.VISIBLE);
            btnBackupTime.setText(R.string.no_sd_card_of_back_up);
        }

    }

    private void setPbBackTimeSize() {
        Drawable drawable = btnGetSign.getCompoundDrawables()[2];
        int w = drawable.getIntrinsicWidth();
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) pbBackTime
                .getLayoutParams();
        layoutParams.width = w;
        layoutParams.height = w;
        pbBackTime.setLayoutParams(layoutParams);
    }

    private void backupFinish() {
        pbBackTime.setVisibility(View.INVISIBLE);
        btnBackupTime.setText(R.string.backup_finish);
        AnimationUtil.fadeOut(btnBackupTime, new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fadeinBackupTime();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        }, duration);

    }

    private void fadeinBackupTime() {
        AnimationUtil.fadeIn(btnBackupTime, new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                btnBackupTime.setVisibility(View.INVISIBLE);
                showBackupTime();
                AnimationUtil.fadeOut(btnBackupTime);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        }, duration);


    }

    private void backupPrivateKey() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbBackTime.setVisibility(View.VISIBLE);
            }
        });
        BackupUtil.backupColdKey(false, new BackupListener() {

            @Override
            public void backupSuccess() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        backupFinish();

                    }
                }, 1000);
            }

            @Override
            public void backupError() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pbBackTime.setVisibility(View.INVISIBLE);
                        showBackupTime();
                    }
                }, 1000);

            }
        });
    }

    private class CloneFromPasswordListenerI implements IDialogPasswordListener {
        private String content;

        public CloneFromPasswordListenerI(String content) {
            this.content = content;
        }

        @Override
        public void onPasswordEntered(SecureCharSequence password) {
            if (dp != null && !dp.isShowing()) {
                dp.setMessage(R.string.clone_from_waiting);
                CloneThread cloneThread = new CloneThread(content, password);
                dp.setThread(cloneThread);
                dp.show();
                cloneThread.start();
            }
        }
    }

    private class CloneThread extends Thread {
        private String content;
        private SecureCharSequence password;

        public CloneThread(String content, SecureCharSequence password) {
            this.content = content;
            this.password = password;
        }

        public void run() {
            List<Address> addressList = PrivateKeyUtil.getECKeysFromString(content, password);
            password.wipe();
            if (addressList == null || addressList.size() == 0) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(getActivity(),
                                R.string.clone_from_failed);
                    }
                });
                return;
            }
            KeyUtil.addAddressList(null, addressList);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    configureCloneButton();
                    configureQrForAll();
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(getActivity(), R.string.clone_from_success);
                    if (getActivity() instanceof ColdActivity) {
                        ColdActivity activity = (ColdActivity) getActivity();
                        Fragment f = activity.getFragmentAtIndex(1);
                        if (f != null && f instanceof Refreshable) {
                            Refreshable r = (Refreshable) f;
                            r.doRefresh();
                        }
                        activity.scrollToFragmentAt(1);
                    }
                }
            });
        }
    }
}
