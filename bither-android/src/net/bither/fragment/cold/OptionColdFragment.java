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
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
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
import net.bither.R;
import net.bither.activity.cold.BitpieColdSignChangeCoinActivity;
import net.bither.activity.cold.ColdActivity;
import net.bither.activity.cold.ColdAdvanceActivity;
import net.bither.activity.cold.SignTxActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitpieHDAccountCold;
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicCode;
import net.bither.bitherj.crypto.mnemonic.MnemonicWordList;
import net.bither.bitherj.qrcode.QRCodeEnodeUtil;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.mnemonic.MnemonicCodeAndroid;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.BitherQRCodeActivity;
import net.bither.qrcode.ScanActivity;
import net.bither.qrcode.ScanQRCodeTransportActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SettingSelectorView;
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
import net.bither.util.PermissionUtil;
import net.bither.util.UnitUtilWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class OptionColdFragment extends Fragment implements Selectable {
    private int ONE_HOUR = 1 * 60 * 60 * 1000;
    private final int duration = 1000;

    private Button btnGetSign;
    private Button btnCloneTo;
    private Button btnCloneFrom;
    private TextView tvBackupTime;
    private TextView tvBackupPath;
    private Button btnAdvance;
    private SettingSelectorView ssvBitcoinUnit;
    private FrameLayout flBackTime;
    private ProgressBar pbBackTime;
    private TextView tvVersion;
    private LinearLayout llQrForAll;
    private DialogProgress dp;
    private TextView tvPrivacyPolicy;
    private Button btnGetSignChangeCoin;

    private SettingSelectorView.SettingSelector bitcoinUnitSelector = new SettingSelectorView
            .SettingSelector() {
        @Override
        public int getOptionCount() {
            return UnitUtilWrapper.BitcoinUnitWrapper.values().length;
        }

        @Override
        public CharSequence getOptionName(int index) {
            UnitUtilWrapper.BitcoinUnitWrapper unit = UnitUtilWrapper.BitcoinUnitWrapper.values()
                    [index];
            SpannableString s = new SpannableString("  " + unit.name());
            Bitmap bmp = UnitUtilWrapper.getBtcSlimSymbol(getResources().getColor(R.color
                    .text_field_text_color), getResources().getDisplayMetrics().scaledDensity *
                    15.6f, unit);
            s.setSpan(new ImageSpan(getActivity(), bmp, ImageSpan.ALIGN_BASELINE), 0, 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return s;
        }

        @Override
        public String getOptionNote(int index) {
            return null;
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }

        @Override
        public String getSettingName() {
            return getString(R.string.setting_name_bitcoin_unit);
        }

        @Override
        public int getCurrentOptionIndex() {
            return AppSharedPreference.getInstance().getBitcoinUnit().ordinal();
        }

        @Override
        public void onOptionIndexSelected(int index) {
            AppSharedPreference.getInstance().setBitcoinUnit(UnitUtilWrapper.BitcoinUnitWrapper
                    .values()[index]);
        }
    };

    private OnClickListener toSignActivityClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if ((AddressManager.getInstance().getPrivKeyAddresses() == null
                        || AddressManager.getInstance().getPrivKeyAddresses().size() == 0)
                    && !AddressManager.getInstance().hasHDMKeychain()
                    && !EnterpriseHDMSeed.hasSeed()
                    && !AddressManager.getInstance().hasHDAccountCold()
                    && !AddressManager.getInstance().hasBitpieHDAccountCold()) {
                DropdownMessage.showDropdownMessage(getActivity(), R.string.private_key_is_empty);
                return;
            }
            Intent intent = new Intent(getActivity(), SignTxActivity.class);
            startActivity(intent);
        }
    };

    private OnClickListener toSignChangeCoinActivityClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!AddressManager.getInstance().hasBitpieHDAccountCold()) {
                DropdownMessage.showDropdownMessage(getActivity(), R.string.bitpie_connector_add_account_label);
                return;
            }
            Intent intent = new Intent(getActivity(), BitpieColdSignChangeCoinActivity.class);
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
                    String content = PrivateKeyUtil.getEncryptPrivateKeyStringFromAllAddresses();
                    Intent intent = new Intent(getActivity(), BitherQRCodeActivity.class);
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
            String content = QRCodeEnodeUtil.getPublicKeyStrOfPrivateKey();
            Intent intent = new Intent(getActivity(), BitherQRCodeActivity.class);
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
                if (!PermissionUtil.isWriteExternalStoragePermission(getActivity(), BitherSetting.REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE)) {
                    return;
                }
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

    private OnClickListener privacyPolicyClick = new OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bither/bither-android/wiki/PrivacyPolicy"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                DropdownMessage.showDropdownMessage(getActivity(), R.string.find_browser_error);
            }
        }
    };

    @Override
    public void onSelected() {
        configureCloneButton();
        configureQrForAll();
    }

    private void configureCloneButton() {
        if ((AddressManager.getInstance().getPrivKeyAddresses() != null && AddressManager.getInstance().getPrivKeyAddresses().size() > 0)
                || AddressManager.getInstance().hasHDMKeychain()
                || AddressManager.getInstance().hasHDAccountCold()
                || AddressManager.getInstance().hasBitpieHDAccountCold()) {
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
        btnGetSignChangeCoin = view.findViewById(R.id.btn_get_sign_change_coin);
        btnCloneTo = (Button) view.findViewById(R.id.btn_clone_to);
        btnCloneFrom = (Button) view.findViewById(R.id.btn_clone_from);
        btnAdvance = (Button) view.findViewById(R.id.btn_advance);
        ssvBitcoinUnit = (SettingSelectorView) view.findViewById(R.id.ssv_bitcoin_unit);
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
        tvPrivacyPolicy = (TextView) view.findViewById(R.id.tv_privacy_policy);
        tvPrivacyPolicy.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        dp = new DialogProgress(getActivity(), R.string.please_wait);
        btnGetSign.setOnClickListener(toSignActivityClickListener);
        btnGetSignChangeCoin.setOnClickListener(toSignChangeCoinActivityClickListener);
        btnCloneTo.setOnClickListener(cloneToClick);
        btnCloneFrom.setOnClickListener(cloneFromClick);
        llQrForAll.setOnClickListener(qrForAllClick);
        btnAdvance.setOnClickListener(advanceClick);
        ssvBitcoinUnit.setSelector(bitcoinUnitSelector);
        tvBackupTime = (TextView) view.findViewById(R.id.tv_backup_time);
        tvBackupPath = (TextView) view.findViewById(R.id.tv_backup_path);
        flBackTime.setOnClickListener(backupTimeListener);
        showBackupTime();
        tvPrivacyPolicy.setOnClickListener(privacyPolicyClick);
    }

    private void showBackupTime() {
        if (FileUtil.existSdCardMounted()) {
            Date date = AppSharedPreference.getInstance().getLastBackupkeyTime();
            if (date == null) {
                flBackTime.setVisibility(View.GONE);
            } else {
                flBackTime.setVisibility(View.VISIBLE);
                final List<File> files = FileUtil.getBackupFileListOfCold();
                if (files != null && files.size() > 0) {
                    String relativeDate = DateTimeUtil.getRelativeDate(getActivity(), date).toString();
                    tvBackupTime.setText(Utils.format(getString(R.string.last_time_of_back_up)
                            + " ", relativeDate));
                } else {
                    tvBackupTime.setText(R.string.no_backup);
                }
                tvBackupPath.setText(FileUtil.getBackupSdCardDir().getAbsolutePath());
            }
        } else {
            flBackTime.setVisibility(View.VISIBLE);
            tvBackupTime.setText(R.string.no_sd_card_of_back_up);
            tvBackupPath.setVisibility(View.GONE);
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
        final List<File> files = FileUtil.getBackupFileListOfCold();
        if (files != null && files.size() > 0) {
            tvBackupTime.setText(R.string.backup_finish);
        } else {
            tvBackupTime.setText(R.string.backup_failed);
        }
        AnimationUtil.fadeOut(tvBackupTime, new Animation.AnimationListener() {
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
        AnimationUtil.fadeIn(tvBackupTime, new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                tvBackupTime.setVisibility(View.INVISIBLE);
                showBackupTime();
                AnimationUtil.fadeOut(tvBackupTime);

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
            List<Address> addressList = PrivateKeyUtil.getECKeysFromBackupString(content, password);

            HDMKeychain hdmKeychain = PrivateKeyUtil.getHDMKeychain(PrivateKeyUtil.getCloneContent(), password);

            BitpieHDAccountCold bitpieHDAccountCold = null;
            MnemonicWordList bitpieColdMnemonicWordList = MnemonicWordList.getMnemonicWordListForHdSeed(PrivateKeyUtil.getCloneContent());
            MnemonicCode mnemonicCode = null;
            if (bitpieColdMnemonicWordList != null) {
                try {
                    mnemonicCode = new MnemonicCodeAndroid();
                    mnemonicCode.setMnemonicWordList(bitpieColdMnemonicWordList);
                    bitpieHDAccountCold = PrivateKeyUtil.getBitpieHDAccountCold(mnemonicCode, PrivateKeyUtil.getCloneContent(), password);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            HDAccountCold hdAccountCold = null;
            MnemonicWordList mnemonicWordList = MnemonicWordList.getMnemonicWordListForHdSeed(PrivateKeyUtil.getCloneContent());
            if (mnemonicWordList != null) {
                try {
                    mnemonicCode = new MnemonicCodeAndroid();
                    mnemonicCode.setMnemonicWordList(mnemonicWordList);
                    hdAccountCold = PrivateKeyUtil.getHDAccountCold(mnemonicCode, PrivateKeyUtil.getCloneContent(), password);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if ((addressList == null || addressList.size() == 0) && (hdmKeychain == null) &&
                    hdAccountCold == null && bitpieHDAccountCold == null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(getActivity(),
                                R.string.clone_from_failed_content_empty);
                    }
                });
                return;
            }
            if (addressList != null) {
                KeyUtil.addAddressListByDesc(null, addressList);
            }
            if (hdmKeychain != null) {
                KeyUtil.setHDKeyChain(hdmKeychain);
            }
            password.wipe();
            if (mnemonicCode != null && mnemonicWordList != null) {
                AppSharedPreference.getInstance().setMnemonicWordList(mnemonicWordList);
                MnemonicCode.setInstance(mnemonicCode);
            }

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
