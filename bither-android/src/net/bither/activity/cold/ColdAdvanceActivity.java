/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.activity.cold;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.TrashCanActivity;
import net.bither.VerifyMessageSignatureActivity;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Version;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.bip38.Bip38;
import net.bither.bitherj.factory.ImportHDSeed;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.bitherj.factory.ImportPrivateKey;
import net.bither.factory.ImportHDSeedAndroid;
import net.bither.factory.ImportPrivateKeyAndroid;
import net.bither.fragment.Refreshable;
import net.bither.pin.PinCodeChangeActivity;
import net.bither.pin.PinCodeDisableActivity;
import net.bither.pin.PinCodeEnableActivity;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.ScanActivity;
import net.bither.qrcode.ScanQRCodeTransportActivity;
import net.bither.qrcode.ScanQRCodeWithOtherActivity;
import net.bither.rawprivatekey.RawPrivateKeyActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SettingSelectorView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogEditPassword;
import net.bither.ui.base.dialog.DialogImportBip38KeyText;
import net.bither.ui.base.dialog.DialogImportPrivateKeyText;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogPasswordWithOther;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSignMessageSelectAddress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.ICheckPasswordListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;


public class ColdAdvanceActivity extends SwipeRightFragmentActivity {
    private SettingSelectorView ssvPinCode;
    private Button btnEditPassword;
    private SettingSelectorView ssvImportPrivateKey;
    private SettingSelectorView ssvImprotBip38Key;
    private SettingSelectorView ssvQrCodeQuality;
    private Button btnTrashCan;
    private DialogProgress dp;
    private TextView tvVserion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_cold_advance_options);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        tvVserion = (TextView) findViewById(R.id.tv_version);
        ssvPinCode = (SettingSelectorView) findViewById(R.id.ssv_pin_code);
        ssvPinCode.setSelector(pinCodeSelector);
        btnEditPassword = (Button) findViewById(R.id.btn_edit_password);
        btnTrashCan = (Button) findViewById(R.id.btn_trash_can);
        ssvImportPrivateKey = (SettingSelectorView) findViewById(R.id.ssv_import_private_key);
        ssvImportPrivateKey.setSelector(importPrivateKeySelector);
        ssvImprotBip38Key = (SettingSelectorView) findViewById(R.id.ssv_import_bip38_key);
        ssvImprotBip38Key.setSelector(importBip38KeySelector);
        ssvQrCodeQuality = (SettingSelectorView) findViewById(R.id.ssv_qr_code_quality);
        ssvQrCodeQuality.setSelector(qrCodeQualitySelector);
        btnEditPassword.setOnClickListener(editPasswordClick);
        btnTrashCan.setOnClickListener(trashCanClick);
        ((SettingSelectorView) findViewById(R.id.ssv_message_signing)).setSelector
                (messageSigningSelector);
        findViewById(R.id.iv_logo).setOnClickListener(rawPrivateKeyClick);
        tvVserion.setText(Version.name + " " + Version.version);
        dp = new DialogProgress(this, R.string.please_wait);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ssvPinCode.loadData();
    }

    private View.OnClickListener rawPrivateKeyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(ColdAdvanceActivity.this, RawPrivateKeyActivity.class));
        }
    };

    private View.OnClickListener editPasswordClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hasAnyAction = true;
            DialogEditPassword dialog = new DialogEditPassword(ColdAdvanceActivity.this);
            dialog.show();
        }
    };


    private View.OnClickListener trashCanClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            startActivity(new Intent(ColdAdvanceActivity.this, TrashCanActivity.class));
        }
    };

    private SettingSelectorView.SettingSelector messageSigningSelector = new SettingSelectorView
            .SettingSelector() {


        @Override
        public int getOptionCount() {
            return 2;
        }

        @Override
        public CharSequence getOptionName(int index) {
            switch (index) {
                case 0:
                    return getString(R.string.sign_message_activity_name);
                case 1:
                default:
                    return getString(R.string.verify_message_signature_activity_name);
            }
        }

        @Override
        public CharSequence getOptionNote(int index) {
            return null;
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }

        @Override
        public CharSequence getSettingName() {
            return getString(R.string.sign_message_setting_name);
        }

        @Override
        public int getCurrentOptionIndex() {
            return -1;
        }

        @Override
        public void onOptionIndexSelected(int index) {
            switch (index) {
                case 0:
                    new DialogSignMessageSelectAddress(ColdAdvanceActivity.this).show();
                    break;
                case 1:
                default:
                    startActivity(new Intent(ColdAdvanceActivity.this,
                            VerifyMessageSignatureActivity.class));
                    break;
            }
        }
    };

    private SettingSelectorView.SettingSelector qrCodeQualitySelector = new SettingSelectorView
            .SettingSelector() {

        @Override
        public int getOptionCount() {
            return QRCodeUtil.QRQuality.values().length;
        }

        @Override
        public CharSequence getSettingName() {
            return getString(R.string.qr_code_quality_setting_name);
        }

        @Override
        public int getCurrentOptionIndex() {
            return AppSharedPreference.getInstance().getQRQuality().ordinal();
        }

        @Override
        public CharSequence getOptionName(int index) {
            switch (index) {
                case 1:
                    return getString(R.string.qr_code_quality_setting_low);
                case 0:
                default:
                    return getString(R.string.qr_code_quality_setting_normal);
            }
        }

        @Override
        public void onOptionIndexSelected(int index) {
            if (index >= 0 && index < getOptionCount()) {
                AppSharedPreference.getInstance().setQRQuality(QRCodeUtil.QRQuality.values()
                        [index]);
            }
        }

        @Override
        public CharSequence getOptionNote(int index) {
            return null;
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }
    };

    private SettingSelectorView.SettingSelector pinCodeSelector = new SettingSelectorView
            .SettingSelector() {
        private AppSharedPreference p = AppSharedPreference.getInstance();
        private boolean hasPinCode;

        @Override
        public int getOptionCount() {
            hasPinCode = p.hasPinCode();
            return hasPinCode ? 2 : 1;
        }

        @Override
        public String getOptionName(int index) {
            if (hasPinCode) {
                switch (index) {
                    case 0:
                        return getString(R.string.pin_code_setting_close);
                    case 1:
                        return getString(R.string.pin_code_setting_change);
                }
            }
            return getString(R.string.pin_code_setting_open);
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
            return getString(R.string.pin_code_setting_name);
        }

        @Override
        public int getCurrentOptionIndex() {
            return -1;
        }

        @Override
        public void onOptionIndexSelected(int index) {
            if (hasPinCode) {
                switch (index) {
                    case 0:
                        startActivity(new Intent(ColdAdvanceActivity.this,
                                PinCodeDisableActivity.class));
                        return;
                    case 1:
                        startActivity(new Intent(ColdAdvanceActivity.this,
                                PinCodeChangeActivity.class));
                        return;
                }
            } else {
                startActivity(new Intent(ColdAdvanceActivity.this, PinCodeEnableActivity.class));
            }
        }
    };
    private SettingSelectorView.SettingSelector importPrivateKeySelector = new
            SettingSelectorView.SettingSelector() {
                @Override
                public int getOptionCount() {
                    hasAnyAction = true;
                    if (AddressManager.getInstance().hasHDMKeychain()) {
                        return 2;
                    } else {
                        return 4;
                    }
                }

                @Override
                public String getOptionName(int index) {
                    switch (index) {
                        case 0:
                            return getString(R.string.import_private_key_qr_code);
                        case 1:
                            return getString(R.string.import_private_key_text);
                        case 2:
                            return getString(R.string.import_hdm_cold_seed_qr_code);
                        case 3:
                            return getString(R.string.import_hdm_cold_seed_phrase);
                        default:
                            return "";
                    }
                }

                @Override
                public String getOptionNote(int index) {
                    return null;
                }

                @Override
                public Drawable getOptionDrawable(int index) {
                    switch (index) {
                        case 0:
                        case 2:
                            return getResources().getDrawable(R.drawable.scan_button_icon);
                        case 1:
                        case 3:
                            return getResources().getDrawable(R.drawable.import_private_key_text_icon);
                        default:
                            return null;
                    }
                }

                @Override
                public String getSettingName() {
                    return getString(R.string.setting_name_import_private_key);
                }

                @Override
                public int getCurrentOptionIndex() {
                    return -1;
                }

                @Override
                public void onOptionIndexSelected(int index) {
                    hasAnyAction = true;
                    switch (index) {
                        case 0:
                            importPrivateKeyFromQRCode();
                            return;
                        case 1:
                            importPrivateKeyFromText();
                            return;
                        case 2:
                            importHDMColdFromQRCode();
                            return;
                        case 3:
                            importHDMColdFromPhrase();
                            return;
                        default:
                            return;
                    }
                }
            };

    private SettingSelectorView.SettingSelector importBip38KeySelector = new SettingSelectorView
            .SettingSelector() {
        @Override
        public int getOptionCount() {

            return 2;
        }

        @Override
        public String getOptionName(int index) {
            switch (index) {
                case 0:
                    return getString(R.string.import_bip38_key_qr_code);
                case 1:
                    return getString(R.string.import_bip38_key_text);
                default:
                    return "";
            }
        }

        @Override
        public String getOptionNote(int index) {
            return null;
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            switch (index) {
                case 0:
                    return getResources().getDrawable(R.drawable.scan_button_icon);
                case 1:
                    return getResources().getDrawable(R.drawable.import_private_key_text_icon);
                default:
                    return null;
            }
        }

        @Override
        public String getSettingName() {
            return getString(R.string.setting_name_import_bip38_key);
        }

        @Override
        public int getCurrentOptionIndex() {
            return -1;
        }

        @Override
        public void onOptionIndexSelected(int index) {
            switch (index) {
                case 0:
                    importPrivateKeyFromQrCode(true);
                    return;
                case 1:
                    new DialogImportBip38KeyText(ColdAdvanceActivity.this).show();
                    return;
                default:
                    return;
            }
        }
    };

    private void importPrivateKeyFromQrCode(boolean isFromBip38) {
        if (isFromBip38) {
            Intent intent = new Intent(this, ScanQRCodeWithOtherActivity.class);
            intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                    getString(R.string.import_bip38private_key_qr_code_scan_title));
            intent.putExtra(BitherSetting.INTENT_REF.QRCODE_TYPE, BitherSetting.QRCodeType.Bip38);
            startActivityForResult(intent, BitherSetting.INTENT_REF
                    .IMPORT_BIP38PRIVATE_KEY_REQUEST_CODE);
        } else {
            Intent intent = new Intent(this, ScanQRCodeTransportActivity.class);
            intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                    getString(R.string.import_private_key_qr_code_scan_title));
            startActivityForResult(intent, BitherSetting.INTENT_REF
                    .IMPORT_PRIVATE_KEY_REQUEST_CODE);
        }
    }

    private void importHDMColdFromQRCode() {
        Intent intent = new Intent(this, ScanQRCodeTransportActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                getString(R.string.import_hdm_cold_seed_qr_code));
        startActivityForResult(intent, BitherSetting.INTENT_REF
                .IMPORT_HDM_COLD_SEED_REQUEST_CODE);

    }

    private void importHDMColdFromPhrase() {

    }

    private void importPrivateKeyFromQRCode() {
        Intent intent = new Intent(this, ScanQRCodeTransportActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                getString(R.string.import_private_key_qr_code_scan_title));
        startActivityForResult(intent, BitherSetting.INTENT_REF.IMPORT_PRIVATE_KEY_REQUEST_CODE);
    }

    private void importPrivateKeyFromText() {
        new DialogImportPrivateKeyText(this).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case BitherSetting.INTENT_REF.IMPORT_PRIVATE_KEY_REQUEST_CODE:
                final String content = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                DialogPassword dialogPassword = new DialogPassword(this,
                        new ImportPrivateKeyPasswordListenerI(content, false));
                dialogPassword.setCheckPre(false);
                dialogPassword.setCheckPasswordListener(new ICheckPasswordListener() {
                    @Override
                    public boolean checkPassword(SecureCharSequence password) {
                        ECKey ecKey = PrivateKeyUtil.getECKeyFromSingleString(content, password);
                        boolean result = ecKey != null;
                        if (ecKey != null) {
                            ecKey.clearPrivateKey();
                        }
                        return result;
                    }
                });
                dialogPassword.setTitle(R.string.import_private_key_qr_code_password);
                dialogPassword.show();
                break;
            case BitherSetting.INTENT_REF.IMPORT_BIP38PRIVATE_KEY_REQUEST_CODE:
                final String bip38Content = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                DialogPasswordWithOther dialogPasswordWithOther = new DialogPasswordWithOther
                        (this, new ImportPrivateKeyPasswordListenerI(null, true));
                dialogPasswordWithOther.setCheckPre(false);
                dialogPasswordWithOther.setCheckPasswordListener(new ICheckPasswordListener() {
                    @Override
                    public boolean checkPassword(SecureCharSequence password) {
                        try {
                            bip38DecodeString = Bip38.decrypt(bip38Content, password).toString();
                            return bip38DecodeString != null;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                });
                dialogPasswordWithOther.setTitle(R.string.enter_bip38_key_password);
                dialogPasswordWithOther.show();
                break;
            case DialogImportPrivateKeyText.ScanPrivateKeyQRCodeRequestCode:
                final String priv = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                if (!Utils.validBitcoinPrivateKey(priv)) {
                    DropdownMessage.showDropdownMessage(this,
                            R.string.import_private_key_text_format_error);
                    break;
                }
                new DialogPassword(this, new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        ImportPrivateKeyAndroid importPrivateKey = new ImportPrivateKeyAndroid
                                (ColdAdvanceActivity.this, ImportPrivateKey.ImportPrivateKeyType
                                        .Text, dp, priv, password);
                        importPrivateKey.importPrivateKey();
                    }
                }).show();
                break;
            case BitherSetting.INTENT_REF.IMPORT_HDM_COLD_SEED_REQUEST_CODE:
                final String hdmSeed = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);

                new DialogPassword(this, new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        ImportHDSeedAndroid importHDSeedAndroid = new ImportHDSeedAndroid
                                (ColdAdvanceActivity.this, dp, hdmSeed, password);
                        importHDSeedAndroid.importColdSeed();
                    }
                }).show();
                break;
        }
    }

    private String bip38DecodeString;

    private class ImportPrivateKeyPasswordListenerI implements IDialogPasswordListener {
        private String content;
        private boolean isFromBip38;

        public ImportPrivateKeyPasswordListenerI(String content, boolean isFromBip38) {
            this.content = content;
            this.isFromBip38 = isFromBip38;
        }

        @Override
        public void onPasswordEntered(SecureCharSequence password) {
            if (dp != null && !dp.isShowing()) {
                dp.setMessage(R.string.import_private_key_qr_code_importing);
                if (isFromBip38) {
                    DialogPassword dialogPassword = new DialogPassword(ColdAdvanceActivity.this,
                            walletIDialogPasswordListener);
                    dialogPassword.show();

                } else {
                    ImportPrivateKeyAndroid importPrivateKey = new ImportPrivateKeyAndroid(ColdAdvanceActivity
                            .this, ImportPrivateKey.ImportPrivateKeyType.BitherQrcode, dp,
                            content, password);
                    importPrivateKey.importPrivateKey();

                }

            }
        }
    }

    private IDialogPasswordListener walletIDialogPasswordListener = new IDialogPasswordListener() {
        @Override
        public void onPasswordEntered(SecureCharSequence password) {
            ImportPrivateKeyAndroid importPrivateKey = new ImportPrivateKeyAndroid(ColdAdvanceActivity.this,
                    ImportPrivateKey.ImportPrivateKeyType.Bip38, dp, bip38DecodeString, password);
            importPrivateKey.importPrivateKey();
        }
    };

    private boolean hasAnyAction = false;

    public void showImportSuccess() {
        hasAnyAction = false;
        DropdownMessage.showDropdownMessage(ColdAdvanceActivity.this,
                R.string.import_private_key_qr_code_success, new Runnable() {
                    @Override
                    public void run() {
                        if (BitherApplication.coldActivity != null) {
                            Fragment f = BitherApplication.coldActivity.getFragmentAtIndex(1);
                            if (f != null && f instanceof Refreshable) {
                                Refreshable r = (Refreshable) f;
                                r.doRefresh();
                            }
                        }
                        if (hasAnyAction) {
                            return;
                        }
                        finish();
                        if (BitherApplication.coldActivity != null) {
                            ThreadUtil.getMainThreadHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    BitherApplication.coldActivity.scrollToFragmentAt(1);
                                }
                            }, getFinishAnimationDuration());
                        }
                    }
                });
    }


}
