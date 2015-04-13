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

package net.bither.activity.hot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.TrashCanActivity;
import net.bither.VerifyMessageSignatureActivity;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.core.Version;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.bip38.Bip38;
import net.bither.bitherj.factory.ImportPrivateKey;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.TransactionsUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.db.TxProvider;
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
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SettingSelectorView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogEditPassword;
import net.bither.ui.base.dialog.DialogImportBip38KeyText;
import net.bither.ui.base.dialog.DialogImportPrivateKeyText;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogPasswordWithOther;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSignMessageSelectAddress;
import net.bither.ui.base.dialog.DialogSimpleQr;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.ICheckPasswordListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.BroadcastUtil;
import net.bither.util.FileUtil;
import net.bither.util.HDMKeychainRecoveryUtil;
import net.bither.util.HDMResetServerPasswordUtil;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;

import java.io.File;
import java.io.IOException;

public class HotAdvanceActivity extends SwipeRightFragmentActivity {
    private SettingSelectorView ssvWifi;
    private Button btnEditPassword;
    private Button btnRCheck;
    private Button btnExportAddress;
    private SettingSelectorView ssvImportPrivateKey;
    private SettingSelectorView ssvImprotBip38Key;
    private SettingSelectorView ssvSyncInterval;
    private SettingSelectorView ssvPinCode;
    private SettingSelectorView ssvQrCodeQuality;
    private Button btnExportLog;
    private Button btnResetTx;
    private Button btnTrashCan;
    private LinearLayout btnHDMRecovery;
    private LinearLayout btnHDMServerPasswordReset;
    private DialogProgress dp;
    private HDMKeychainRecoveryUtil hdmRecoveryUtil;
    private HDMResetServerPasswordUtil hdmResetServerPasswordUtil;
    private TextView tvVserion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_hot_advance_options);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        tvVserion = (TextView) findViewById(R.id.tv_version);
        ssvWifi = (SettingSelectorView) findViewById(R.id.ssv_wifi);
        ssvPinCode = (SettingSelectorView) findViewById(R.id.ssv_pin_code);
        btnEditPassword = (Button) findViewById(R.id.btn_edit_password);
        btnRCheck = (Button) findViewById(R.id.btn_r_check);
        btnTrashCan = (Button) findViewById(R.id.btn_trash_can);
        btnHDMRecovery = (LinearLayout) findViewById(R.id.ll_hdm_recover);
        btnHDMServerPasswordReset = (LinearLayout) findViewById(R.id.ll_hdm_server_auth_reset);
        ssvImportPrivateKey = (SettingSelectorView) findViewById(R.id.ssv_import_private_key);
        ssvImprotBip38Key = (SettingSelectorView) findViewById(R.id.ssv_import_bip38_key);
        ssvSyncInterval = (SettingSelectorView) findViewById(R.id.ssv_sync_interval);
        ssvQrCodeQuality = (SettingSelectorView) findViewById(R.id.ssv_qr_code_quality);
        ssvWifi.setSelector(wifiSelector);
        ssvImportPrivateKey.setSelector(importPrivateKeySelector);
        ssvImprotBip38Key.setSelector(importBip38KeySelector);
        ssvSyncInterval.setSelector(syncIntervalSelector);
        ssvPinCode.setSelector(pinCodeSelector);
        ssvQrCodeQuality.setSelector(qrCodeQualitySelector);
        btnEditPassword.setOnClickListener(editPasswordClick);
        btnRCheck.setOnClickListener(rCheckClick);
        btnTrashCan.setOnClickListener(trashCanClick);
        btnHDMRecovery.setOnClickListener(hdmRecoverClick);
        btnHDMServerPasswordReset.setOnClickListener(hdmServerPasswordResetClick);
        ((SettingSelectorView) findViewById(R.id.ssv_message_signing)).setSelector
                (messageSigningSelector);
        dp = new DialogProgress(this, R.string.please_wait);
        btnExportLog = (Button) findViewById(R.id.btn_export_log);
        btnExportLog.setOnClickListener(exportLogClick);
        btnResetTx = (Button) findViewById(R.id.btn_reset_tx);
        btnResetTx.setOnClickListener(resetTxListener);
        btnExportAddress = (Button) findViewById(R.id.btn_export_address);
        btnExportAddress.setOnClickListener(exportAddressClick);
        findViewById(R.id.ll_bither_address).setOnClickListener(bitherAddressClick);
        findViewById(R.id.ibtn_bither_address_qr).setOnClickListener(bitherAddressQrClick);
        findViewById(R.id.iv_logo).setOnClickListener(rawPrivateKeyClick);
        tvVserion.setText(Version.name + " " + Version.version);
        hdmRecoveryUtil = new HDMKeychainRecoveryUtil(this, dp);
        configureHDMServerPasswordReset();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ssvPinCode.loadData();
        configureHDMRecovery();
        configureHDMServerPasswordReset();
    }

    private View.OnClickListener rawPrivateKeyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(HotAdvanceActivity.this, RawPrivateKeyActivity.class));
        }
    };

    private View.OnClickListener rCheckClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (AddressManager.getInstance().getAllAddresses() == null || AddressManager
                    .getInstance().getAllAddresses().size() == 0) {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                        R.string.rcheck_no_address);
                return;
            }
            Intent intent = new Intent(HotAdvanceActivity.this, RCheckActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener editPasswordClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hasAnyAction = true;
            DialogEditPassword dialog = new DialogEditPassword(HotAdvanceActivity.this);
            dialog.show();
        }
    };
    private View.OnClickListener exportAddressClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (FileUtil.existSdCardMounted()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final File addressFile = new File(FileUtil.getDiskDir("", true), "address.txt");

                            String addressListString = "";
                            for (Address address : AddressManager.getInstance().getAllAddresses()) {
                                addressListString = addressListString + address.getAddress() + "\n";
                            }
                            Utils.writeFile(addressListString, addressFile);
                            HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                                            getString(R.string.export_address_success) + "\n" + addressFile
                                                    .getAbsolutePath());
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            } else {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.no_sd_card);
            }

        }
    };

    private View.OnClickListener exportLogClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (FileUtil.existSdCardMounted()) {
                Runnable confirmRunnable = new Runnable() {

                    @Override
                    public void run() {
                        final File logTagDir = FileUtil.getDiskDir("log", true);
                        try {
                            File logDir = BitherApplication.getLogDir();
                            FileUtil.copyFile(logDir, logTagDir);
//                            if (BitherjSettings.DEV_DEBUG) {
//                                SQLiteDatabase addressDB = BitherApplication.mAddressDbHelper.getReadableDatabase();
//                                FileUtil.copyFile(new File(addressDB.getPath()), new File(logTagDir, "address.db"));
//
//                                SQLiteDatabase txDb = BitherApplication.mDbHelper.getReadableDatabase();
//                                FileUtil.copyFile(new File(txDb.getPath()), new File(logTagDir, "tx.db"));
//                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                                        getString(R.string.export_success) + "\n" + logTagDir
                                                .getAbsolutePath());
                            }
                        });
                    }
                };
                DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(HotAdvanceActivity
                        .this, getString(R.string.export_log_prompt), confirmRunnable);
                dialogConfirmTask.show();
            } else {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.no_sd_card);
            }

        }
    };

    private View.OnClickListener trashCanClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            startActivity(new Intent(HotAdvanceActivity.this, TrashCanActivity.class));
        }
    };

    private View.OnClickListener hdmRecoverClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!hdmRecoveryUtil.canRecover()) {
                return;
            }
            new ThreadNeedService(null, HotAdvanceActivity.this) {

                @Override
                public void runWithService(BlockchainService service) {
                    if (service != null) {
                        service.stopAndUnregister();
                    }
                    try {
                        final int result = hdmRecoveryUtil.recovery();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                configureHDMRecovery();
                                if (result > 0) {
                                    DropdownMessage.showDropdownMessage(HotAdvanceActivity
                                            .this, result);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (service != null) {
                        service.startAndRegister();
                    }
                }
            }.start();
        }
    };

    private View.OnClickListener hdmServerPasswordResetClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            new DialogConfirmTask(v.getContext(),
                    getString(R.string.hdm_reset_server_password_confirm), new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!dp.isShowing()) {
                                dp.show();
                            }
                        }
                    });
                    hdmResetServerPasswordUtil = new HDMResetServerPasswordUtil(HotAdvanceActivity
                            .this, dp);
                    final boolean result = hdmResetServerPasswordUtil.changePassword();
                    hdmResetServerPasswordUtil = null;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            if (result) {
                                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                                        R.string.hdm_reset_server_password_success);
                            }
                        }
                    });
                }
            }).show();
        }
    };

    private View.OnClickListener resetTxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (BitherApplication.canReloadTx()) {
                Runnable confirmRunnable = new Runnable() {
                    @Override
                    public void run() {
                        BitherApplication.reloadTxTime = System.currentTimeMillis();
                        PasswordSeed passwordSeed = PasswordSeed.getPasswordSeed();
                        if (passwordSeed == null) {
                            resetTx();
                        } else {
                            callPassword();
                        }
                    }
                };
                DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(HotAdvanceActivity
                        .this, getString(R.string.reload_tx_need_too_much_time), confirmRunnable);
                dialogConfirmTask.show();

            } else {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                        R.string.tx_cannot_reloding);
            }


        }


    };

    private void callPassword() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (PasswordSeed.hasPasswordSeed()) {
                    DialogPassword dialogPassword = new DialogPassword(HotAdvanceActivity.this,
                            new IDialogPasswordListener() {
                                @Override
                                public void onPasswordEntered(SecureCharSequence password) {
                                    resetTx();

                                }
                            });
                    dialogPassword.show();
                } else {
                    resetTx();
                }
            }
        });
    }

    private void resetTx() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dp == null) {
                    dp = new DialogProgress(HotAdvanceActivity.this, R.string.please_wait);
                }
                dp.show();
            }
        });
        ThreadNeedService threadNeedService = new ThreadNeedService(dp, HotAdvanceActivity.this) {
            @Override
            public void runWithService(BlockchainService service) {
                service.stopAndUnregister();
                for (Address address : AddressManager.getInstance().getAllAddresses()) {
                    address.setSyncComplete(false);
                    address.updateSyncComplete();

                }
                TxProvider.getInstance().clearAllTx();
                for (Address address : AddressManager.getInstance().getAllAddresses()) {
                    address.notificatTx(null, Tx.TxNotificationType.txFromApi);
                }
                try {
                    if (!AddressManager.getInstance().addressIsSyncComplete()) {
                        TransactionsUtil.getMyTxFromBither();
                    }
                    service.startAndRegister();
                    HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                                    R.string.reload_tx_success);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                                    R.string.network_or_connection_error);
                        }
                    });

                }
            }
        };
        threadNeedService.start();
    }

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
                    new DialogSignMessageSelectAddress(HotAdvanceActivity.this).show();
                    break;
                case 1:
                default:
                    startActivity(new Intent(HotAdvanceActivity.this,
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
                        startActivity(new Intent(HotAdvanceActivity.this,
                                PinCodeDisableActivity.class));
                        return;
                    case 1:
                        startActivity(new Intent(HotAdvanceActivity.this,
                                PinCodeChangeActivity.class));
                        return;
                }
            } else {
                startActivity(new Intent(HotAdvanceActivity.this, PinCodeEnableActivity.class));
            }
        }
    };


    private SettingSelectorView.SettingSelector importPrivateKeySelector = new
            SettingSelectorView.SettingSelector() {
                @Override
                public int getOptionCount() {
                    hasAnyAction = true;
                    return 2;
                }

                @Override
                public String getOptionName(int index) {
                    switch (index) {
                        case 0:
                            return getString(R.string.import_private_key_qr_code);
                        case 1:
                            return getString(R.string.import_private_key_text);
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
                            importPrivateKeyFromQrCode(false);
                            return;
                        case 1:
                            new DialogImportPrivateKeyText(HotAdvanceActivity.this).show();
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
                    new DialogImportBip38KeyText(HotAdvanceActivity.this).show();
                    return;
                default:
                    return;
            }
        }
    };

    private SettingSelectorView.SettingSelector wifiSelector = new SettingSelectorView
            .SettingSelector() {

        @Override
        public void onOptionIndexSelected(int index) {
            boolean orginSyncBlockOnluWifi = AppSharedPreference.getInstance()
                    .getSyncBlockOnlyWifi();
            hasAnyAction = true;
            final boolean isOnlyWifi = index == 1;
            AppSharedPreference.getInstance().setSyncBlockOnlyWifi(isOnlyWifi);
            if (orginSyncBlockOnluWifi != isOnlyWifi) {
                BroadcastUtil.sendBroadcastStartPeer();
            }

        }

        @Override
        public String getSettingName() {
            return getString(R.string.setting_name_wifi);
        }

        @Override
        public String getOptionName(int index) {
            if (index == 1) {
                return getString(R.string.setting_name_wifi_yes);
            } else {
                return getString(R.string.setting_name_wifi_no);
            }
        }

        @Override
        public int getOptionCount() {
            hasAnyAction = true;
            return 2;
        }

        @Override
        public int getCurrentOptionIndex() {
            boolean onlyUseWifi = AppSharedPreference.getInstance().getSyncBlockOnlyWifi();
            if (onlyUseWifi) {
                return 1;
            } else {
                return 0;
            }

        }

        @Override
        public String getOptionNote(int index) {
            return null;
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }
    };

    private SettingSelectorView.SettingSelector syncIntervalSelector = new SettingSelectorView
            .SettingSelector() {
        @Override
        public int getOptionCount() {
            return 2;
        }

        @Override
        public String getOptionName(int index) {
            if (index == 0) {
                return getString(BitherSetting.SyncInterval.Normal.getStringId());
            } else {
                return getString(BitherSetting.SyncInterval.OnlyOpenApp.getStringId());
            }

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
            return getString(R.string.synchronous_interval);
        }

        @Override
        public int getCurrentOptionIndex() {
            BitherSetting.SyncInterval syncInterval = AppSharedPreference.getInstance()
                    .getSyncInterval();
            if (syncInterval == BitherSetting.SyncInterval.OnlyOpenApp) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public void onOptionIndexSelected(int index) {
            switch (index) {
                case 0:
                    AppSharedPreference.getInstance().setSyncInterval(BitherSetting.SyncInterval
                            .Normal);
                    break;
                case 1:
                    AppSharedPreference.getInstance().setSyncInterval(BitherSetting.SyncInterval
                            .OnlyOpenApp);
                    break;
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

    private String bip38DecodeString;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (hdmRecoveryUtil.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (hdmResetServerPasswordUtil != null && hdmResetServerPasswordUtil.onActivityResult
                (requestCode, resultCode, data)) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case BitherSetting.INTENT_REF.IMPORT_PRIVATE_KEY_REQUEST_CODE:
                final String content = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                if (content.indexOf(QRCodeUtil.HDM_QR_CODE_FLAG) == 0) {
                    DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.can_not_import_hdm_cold_seed);
                    return;
                }
                DialogPassword dialogPassword = new DialogPassword(this,
                        new ImportPrivateKeyPasswordListenerI(content, false));
                dialogPassword.setCheckPre(false);
                dialogPassword.setTitle(R.string.import_private_key_qr_code_password);
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
                                (HotAdvanceActivity.this, ImportPrivateKey.ImportPrivateKeyType
                                        .Text, dp, priv, password);
                        importPrivateKey.importPrivateKey();
                    }
                }).show();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

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
                    DialogPassword dialogPassword = new DialogPassword(HotAdvanceActivity.this,
                            walletIDialogPasswordListener);
                    dialogPassword.show();

                } else {
                    ImportPrivateKeyAndroid importPrivateKey = new ImportPrivateKeyAndroid(HotAdvanceActivity
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
            ImportPrivateKeyAndroid importPrivateKey = new ImportPrivateKeyAndroid(HotAdvanceActivity.this,
                    ImportPrivateKey.ImportPrivateKeyType.Bip38, dp, bip38DecodeString, password);
            importPrivateKey.importPrivateKey();
        }
    };

    private void configureHDMRecovery() {
        if (hdmRecoveryUtil.canRecover()) {
            btnHDMRecovery.setVisibility(View.VISIBLE);
        } else {
            btnHDMRecovery.setVisibility(View.GONE);
        }
    }

    private void configureHDMServerPasswordReset() {
        if (HDMBId.getHDMBidFromDb() == null) {
            btnHDMServerPasswordReset.setVisibility(View.GONE);
        } else {
            btnHDMServerPasswordReset.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasAnyAction = false;

    public void showImportSuccess() {
        hasAnyAction = false;
        DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                R.string.import_private_key_qr_code_success, new Runnable() {
                    @Override
                    public void run() {
                        if (BitherApplication.hotActivity != null) {
                            Fragment f = BitherApplication.hotActivity.getFragmentAtIndex(1);
                            if (f != null && f instanceof Refreshable) {
                                Refreshable r = (Refreshable) f;
                                r.doRefresh();
                            }
                        }
                        if (hasAnyAction) {
                            return;
                        }
                        finish();
                        if (BitherApplication.hotActivity != null) {
                            ThreadUtil.getMainThreadHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    BitherApplication.hotActivity.scrollToFragmentAt(1);
                                }
                            }, getFinishAnimationDuration());
                        }
                    }
                });
    }

    private View.OnClickListener bitherAddressClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            StringUtil.copyString(BitherjSettings.DONATE_ADDRESS);
            DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                    R.string.bither_team_address_copied);
        }
    };

    private View.OnClickListener bitherAddressQrClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            new DialogSimpleQr(v.getContext(), BitherjSettings.DONATE_ADDRESS,
                    R.string.bither_team_address).show();
        }
    };

}
