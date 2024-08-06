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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.TrashCanActivity;
import net.bither.VerifyMessageSignatureActivity;
import net.bither.activity.cold.HdmImportWordListActivity;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.core.Version;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.EncryptedData;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.bip38.Bip38;
import net.bither.bitherj.crypto.mnemonic.MnemonicCode;
import net.bither.bitherj.crypto.mnemonic.MnemonicWordList;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.factory.ImportHDSeed;
import net.bither.bitherj.factory.ImportPrivateKey;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.TransactionsUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.enums.TotalBalanceHide;
import net.bither.factory.ImportHDSeedAndroid;
import net.bither.factory.ImportPrivateKeyAndroid;
import net.bither.fragment.Refreshable;
import net.bither.mnemonic.MnemonicCodeAndroid;
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
import net.bither.ui.base.dialog.DialogAddressQrCopy;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogEditPassword;
import net.bither.ui.base.dialog.DialogEnterpriseHDMEnable;
import net.bither.ui.base.dialog.DialogImportBip38KeyText;
import net.bither.ui.base.dialog.DialogImportHdAccountSeedQrCodeSelectLanguage;
import net.bither.ui.base.dialog.DialogImportPrivateKeyText;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogPasswordWithOther;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSignMessageSelectType;
import net.bither.ui.base.dialog.DialogWithActions;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.ICheckPasswordListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.BroadcastUtil;
import net.bither.util.FileUtil;
import net.bither.util.HDMKeychainRecoveryUtil;
import net.bither.util.HDMResetServerPasswordUtil;
import net.bither.util.LogUtil;
import net.bither.util.NetworkUtil;
import net.bither.util.ThreadUtil;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HotAdvanceActivity extends SwipeRightFragmentActivity {
    private SettingSelectorView ssvWifi;
    private Button btnEditPassword;
    private Button btnExportAddress;
    private SettingSelectorView ssvImportPrivateKey;
    private SettingSelectorView ssvImprotBip38Key;
    private SettingSelectorView ssvSyncInterval;
    private SettingSelectorView ssvPinCode;
    private SettingSelectorView ssvQrCodeQuality;
    private SettingSelectorView ssvPasswordStrengthCheck;
    private SettingSelectorView ssvTotalBalanceHide;
    // TODO: api config
    private SettingSelectorView ssvApiConfig;
    private Button btnExportLog;
    private Button btnResetTx;
    private Button btnTrashCan;
    private LinearLayout btnHDMRecovery;
    private LinearLayout btnHDMServerPasswordReset;
    private LinearLayout llDetectBcc;
    private LinearLayout llForkCoins, signHash;
    private DialogProgress dp;
    private HDMKeychainRecoveryUtil hdmRecoveryUtil;
    private HDMResetServerPasswordUtil hdmResetServerPasswordUtil;
    private TextView tvVserion;
    public static final String SplitCoinKey = "SplitCoin";
    private final AddressTxLoadingReceiver addressIsLoadingReceiver = new AddressTxLoadingReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_hot_advance_options);
        initView();
        registerReceiver();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        tvVserion = (TextView) findViewById(R.id.tv_version);
        ssvWifi = (SettingSelectorView) findViewById(R.id.ssv_wifi);
        ssvPinCode = (SettingSelectorView) findViewById(R.id.ssv_pin_code);
        btnEditPassword = (Button) findViewById(R.id.btn_edit_password);
        btnTrashCan = (Button) findViewById(R.id.btn_trash_can);
        btnHDMRecovery = (LinearLayout) findViewById(R.id.ll_hdm_recover);
        llDetectBcc = (LinearLayout) findViewById(R.id.ll_detect_bcc);
        llForkCoins = (LinearLayout) findViewById(R.id.ll_fork_coins);
        signHash = (LinearLayout) findViewById(R.id.ll_sign_hash);
        btnHDMServerPasswordReset = (LinearLayout) findViewById(R.id.ll_hdm_server_auth_reset);
        ssvImportPrivateKey = (SettingSelectorView) findViewById(R.id.ssv_import_private_key);
        ssvImprotBip38Key = (SettingSelectorView) findViewById(R.id.ssv_import_bip38_key);
        ssvSyncInterval = (SettingSelectorView) findViewById(R.id.ssv_sync_interval);
        ssvQrCodeQuality = (SettingSelectorView) findViewById(R.id.ssv_qr_code_quality);
        ssvPasswordStrengthCheck = (SettingSelectorView) findViewById(R.id.ssv_password_strength_check);
        ssvTotalBalanceHide = (SettingSelectorView) findViewById(R.id.ssv_total_balance_hide);
        ssvApiConfig = (SettingSelectorView) findViewById(R.id.ssv_api_config);
        ssvApiConfig.setSelector(apiConfigSelector);
        ssvWifi.setSelector(wifiSelector);
        ssvImportPrivateKey.setSelector(importPrivateKeySelector);
        ssvImprotBip38Key.setSelector(importBip38KeySelector);
        ssvSyncInterval.setSelector(syncIntervalSelector);
        ssvPinCode.setSelector(pinCodeSelector);
        ssvQrCodeQuality.setSelector(qrCodeQualitySelector);
        btnEditPassword.setOnClickListener(editPasswordClick);
        ssvPasswordStrengthCheck.setSelector(passwordStrengthCheckSelector);
        ssvTotalBalanceHide.setSelector(totalBalanceHideSelector);
        btnTrashCan.setOnClickListener(trashCanClick);
        btnHDMRecovery.setOnClickListener(hdmRecoverClick);
        llDetectBcc.setOnClickListener(detectBccClick);
        llForkCoins.setOnClickListener(splitClick);
        btnHDMServerPasswordReset.setOnClickListener(hdmServerPasswordResetClick);
        ((SettingSelectorView) findViewById(R.id.ssv_message_signing)).setSelector
                (messageSigningSelector);
        dp = new DialogProgress(this, R.string.please_wait);
        btnExportLog = (Button) findViewById(R.id.btn_export_log);
        btnExportLog.setOnClickListener(exportLogClick);
        btnResetTx = (Button) findViewById(R.id.btn_reset_tx);
        btnResetTx.setOnClickListener(resetTxListener);
        // btnResetTx.setOnClickListener(selDialog);
        btnExportAddress = (Button) findViewById(R.id.btn_export_address);
        btnExportAddress.setOnClickListener(exportAddressClick);
        findViewById(R.id.btn_network_custom_peer).setOnClickListener(networkCustomPeerClick);
        findViewById(R.id.btn_network_monitor).setOnClickListener(networkMonitorClick);
        findViewById(R.id.ll_bither_address).setOnClickListener(bitherAddressQrClick);
        findViewById(R.id.ibtn_bither_address_qr).setOnClickListener(bitherAddressQrClick);
//        findViewById(R.id.iv_logo).setOnClickListener(rawPrivateKeyClick);
        tvVserion.setText(Version.name + " " + Version.version);
        hdmRecoveryUtil = new HDMKeychainRecoveryUtil(this, dp);
        configureHDMServerPasswordReset();

        signHash.setOnClickListener(signHashClick);
    }

    private void registerReceiver() {
        registerReceiver(addressIsLoadingReceiver, new IntentFilter(NotificationAndroidImpl.ACTION_ADDRESS_TX_LOADING_STATE));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(addressIsLoadingReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ssvPinCode.loadData();
        configureHDMRecovery();
        configureHDMServerPasswordReset();
    }

    private final class AddressTxLoadingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Utils.compareString(intent.getAction(), NotificationAndroidImpl.ACTION_ADDRESS_TX_LOADING_STATE)) {
                return;
            }
            if (!intent.hasExtra(NotificationAndroidImpl.ACTION_ADDRESS_TX_LOADING_INFO)) {
                return;
            }
            String address = intent.getStringExtra(NotificationAndroidImpl.ACTION_ADDRESS_TX_LOADING_INFO);
            if (Utils.isEmpty(address)) {
                return;
            }
            if (dp != null) {
                dp.setMessage(getString(R.string.tip_sync_address_tx, address));
            }
        }
    }

    private View.OnClickListener signHashClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new DialogSignMessageSelectType(HotAdvanceActivity.this, true, true).show();
        }
    };

    private View.OnClickListener networkMonitorClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(HotAdvanceActivity.this, NetworkMonitorActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener networkCustomPeerClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(HotAdvanceActivity.this, NetworkCustomPeerActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener rawPrivateKeyClick = new View.OnClickListener() {
        private int clickedTime;

        @Override
        public void onClick(View v) {
            v.removeCallbacks(delay);
            clickedTime++;
            if (clickedTime >= 7) {
                new DialogEnterpriseHDMEnable(HotAdvanceActivity.this).show();
                clickedTime = 0;
                return;
            }
            v.postDelayed(delay, 400);
        }

        private Runnable delay = new Runnable() {
            @Override
            public void run() {
                if (clickedTime < 7) {
                    startActivity(new Intent(HotAdvanceActivity.this, RawPrivateKeyActivity.class));
                }
                clickedTime = 0;
            }
        };
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
                            String addressListString = "";
                            for (Address address : AddressManager.getInstance().getAllAddresses()) {
                                addressListString = addressListString + address.getAddress() + "\n";
                            }
                            if (Utils.isEmpty(addressListString)) {
                                HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.export_address_empty);
                                    }
                                });
                            } else {
                                final File addressFile = new File(FileUtil.getDiskDir("", true), "address.txt");
                                Utils.writeFile(addressListString, addressFile);
                                HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                                                getString(R.string.export_address_success) + "\n" + addressFile
                                                        .getAbsolutePath());
                                    }
                                });
                            }
                        } catch (Exception e) {
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
                            if (BitherjSettings.DEV_DEBUG) {
                                SQLiteDatabase addressDB = BitherApplication.mAddressDbHelper.getReadableDatabase();
                                FileUtil.copyFile(new File(addressDB.getPath()), new File(logTagDir, "address.db"));

                                SQLiteDatabase txDb = BitherApplication.mTxDbHelper.getReadableDatabase();
                                FileUtil.copyFile(new File(txDb.getPath()), new File(logTagDir, "tx.db"));

                            }
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

    private View.OnClickListener splitClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(HotAdvanceActivity.this, SplitForkCoinsActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener detectBccClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(HotAdvanceActivity.this, SplitBccSelectAddressActivity.class);
            intent.putExtra(SplitCoinKey, SplitCoin.BCC);
            intent.putExtra(SplitBccSelectAddressActivity.DETECT_BCC_ASSETS, true);
            startActivity(intent);
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

    /**
     * Improve the method
     */

    private View.OnClickListener resetTxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (AddressManager.getInstance().noAddress()) {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.no_private_key);
                return;
            }
            if (BitherApplication.canReloadTx()) {
                if (NetworkUtil.isConnected()) {
                    if (!TransactionsUtil.isReloading) {
                        final Runnable confirmRunnable = new Runnable() {
                            @Override
                            public void run() {
                                PasswordSeed passwordSeed = PasswordSeed.getPasswordSeed();
                                if (passwordSeed == null) {
                                    // TODO: the dialog determine the web type
                                    // showSelectedDialog();
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
                        DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.no_sync_complete);
                    }
                } else {
                    DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.tip_network_error);
                }
            } else {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.tx_cannot_reloding);
            }
        }


    };

    // TODO: select dialog
    private DialogWithActions.DialogWithActionsClickListener selDialog = new DialogWithActions.DialogWithActionsClickListener() {
        @Override
        protected List<DialogWithActions.Action> getActions() {

            ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
            /*
            final Runnable bitherRunnable = new Runnable() {
                @Override
                public void run() {
                    callPassword(0);
                }
            };
            final Runnable blockChainRunnable = new Runnable() {
                @Override
                public void run() {
                    callPassword(1);
                }
            };
            DialogConfirmTask dialogBitherTask = new DialogConfirmTask(HotAdvanceActivity.this, getString(R.string.reload_tx_need_too_much_time), bitherRunnable);
            dialogBitherTask.show();
            DialogConfirmTask dialogBlockCainTask = new DialogConfirmTask(HotAdvanceActivity.this, getString(R.string.reload_tx_need_too_much_time), blockChainRunnable);
            dialogBlockCainTask.show();

            actions.add(new DialogWithActions.Action("bither.net", bitherRunnable));
            actions.add(new DialogWithActions.Action("blockchain.info", blockChainRunnable));

            return actions;
            */
            if (BitherApplication.canReloadTx()) {
                // ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
                final Runnable bitherRunnable = new Runnable() {
                    @Override
                    public void run() {
                        callPassword();
                    }
                };
                final Runnable blockChainRunnable = new Runnable() {
                    @Override
                    public void run() {
                        callPassword();
                    }
                };
                /*
                DialogConfirmTask dialogBitherTask = new DialogConfirmTask(HotAdvanceActivity.this, getString(R.string.reload_tx_need_too_much_time), bitherRunnable);
                dialogBitherTask.show();
                DialogConfirmTask dialogBlockCainTask = new DialogConfirmTask(HotAdvanceActivity.this, getString(R.string.reload_tx_need_too_much_time), blockChainRunnable);
                dialogBlockCainTask.show();
                */

                actions.add(new DialogWithActions.Action("bither.net", bitherRunnable));
                actions.add(new DialogWithActions.Action("blockchain.info", blockChainRunnable));

                return actions;
            } else {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                        R.string.tx_cannot_reloding);
            }
            /*
            actions.add(new DialogWithActions.Action("bither.net",
                    new Runnable() {
                        @Override
                        public void run() {
                            //
                            callPassword(0);

                        }
                    }));
            actions.add(new DialogWithActions.Action("blockChain.info",
                    new Runnable() {
                        @Override
                        public void run() {
                            //
                            callPassword(1);
                        }
                    }));
            */

            return actions;
        }
    };


    /**
     * end
     */

    private void callPassword() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (PasswordSeed.hasPasswordSeed()) {
                    DialogPassword dialogPassword = new DialogPassword(HotAdvanceActivity.this,
                            new IDialogPasswordListener() {
                                @Override
                                public void onPasswordEntered(SecureCharSequence password) {
                                    AddressManager addressManager = AddressManager.getInstance();
                                    if (addressManager.hasHDAccountHot()) {
                                        addressManager.getHDAccountHot().addSegwitPub(password);
                                    }
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
                BitherApplication.reloadTxTime = System.currentTimeMillis();
                if (dp == null) {
                    dp = new DialogProgress(HotAdvanceActivity.this, R.string.reload_tx_please_wait);
                } else {
                    dp.setMessage(R.string.reload_tx_please_wait);
                }
                dp.show();
                resetTx(false);
            }
        });
    }

    private void resetTx(final boolean isRetry) {
        ThreadNeedService threadNeedService = new ThreadNeedService(dp, HotAdvanceActivity.this) {
            @Override
            public void runWithService(BlockchainService service) {
                service.stopAndUnregister();
                if (!isRetry) {
                    for (Address address : AddressManager.getInstance().getAllAddresses()) {
                        address.setSyncComplete(false);
                        address.updateSyncComplete();
                    }
                    AbstractDb.hdAccountAddressProvider.setSyncedNotComplete();
                    AbstractDb.txProvider.clearAllTx();
                    for (Address address : AddressManager.getInstance().getAllAddresses()) {
                        address.notificatTx(null, Tx.TxNotificationType.txFromApi);
                    }
                }
                try {
                    if (!AddressManager.getInstance().addressIsSyncComplete()) {
                        TransactionsUtil.getMyTxFromBither();
                    }
                    AbstractDb.peerProvider.recreate();
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
                    TransactionsUtil.isReloading = false;
                    HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(HotAdvanceActivity.this, getString(R.string.reload_tx_failed), getString(R.string.choose_mode_warm_retry), new Runnable() {
                                @Override
                                public void run() {
                                   resetTx(true);
                                }
                            }, false);
                            dialogConfirmTask.setCanceledOnTouchOutside(false);
                            dialogConfirmTask.show();
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
                    new DialogSignMessageSelectType(HotAdvanceActivity.this, true, false).show();
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
                    if (AddressManager.getInstance().getHDAccountHot() != null) {
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
                            return getString(R.string.import_hd_account_seed_qr_code);
                        case 3:
                            return getString(R.string.import_hd_account_seed_phrase);
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
                            importPrivateKeyFromQrCode(false);
                            break;
                        case 1:
                            new DialogImportPrivateKeyText(HotAdvanceActivity.this).show();
                            break;
                        case 2:
                            importHDFromQRCode();
                            break;
                        case 3:
                            importHDFromPhrase();
                            break;
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

    private SettingSelectorView.SettingSelector passwordStrengthCheckSelector = new
            SettingSelectorView.SettingSelector() {

                @Override
                public int getOptionCount() {
                    return 2;
                }

                @Override
                public CharSequence getOptionName(int index) {
                    if (index == 0) {
                        return getString(R.string.password_strength_check_on);
                    }
                    return getString(R.string.password_strength_check_off);
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
                    return getString(R.string.password_strength_check);
                }

                @Override
                public int getCurrentOptionIndex() {
                    return AppSharedPreference.getInstance().getPasswordStrengthCheck() ? 0 : 1;
                }

                @Override
                public void onOptionIndexSelected(int index) {
                    boolean check = index == 0;
                    if (check) {
                        AppSharedPreference.getInstance().setPasswordStrengthCheck(check);
                    } else {
                        new DialogConfirmTask(HotAdvanceActivity.this, getString(R.string
                                .password_strength_check_off_warn), new Runnable() {
                            @Override
                            public void run() {
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AppSharedPreference.getInstance().setPasswordStrengthCheck(false);
                                        ssvPasswordStrengthCheck.loadData();
                                    }
                                });
                            }
                        }).show();
                    }
                }
            };

    private SettingSelectorView.SettingSelector totalBalanceHideSelector = new
            SettingSelectorView.SettingSelector() {


                @Override
                public int getOptionCount() {
                    return TotalBalanceHide.values().length;
                }

                @Override
                public CharSequence getOptionName(int index) {
                    return TotalBalanceHide.values()[index].displayName();
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
                    return getString(R.string.total_balance_hide_setting_name);
                }

                @Override
                public int getCurrentOptionIndex() {
                    return AppSharedPreference.getInstance().getTotalBalanceHide().ordinal();
                }

                @Override
                public void onOptionIndexSelected(int index) {
                    AppSharedPreference.getInstance().setTotalBalanceHide(TotalBalanceHide.values()[index]);
                }
            };

    /**
     * set Api Config
     */

    private SettingSelectorView.SettingSelector apiConfigSelector = new
            SettingSelectorView.SettingSelector() {
                int length = BitherjSettings.ApiConfig.values().length;

                @Override
                public int getOptionCount() {
                    return length;
                }

                @Override
                public CharSequence getOptionName(int index) {
                    BitherjSettings.ApiConfig apiConfig = getModeByIndex(index);
                    switch (apiConfig) {
                        case BITHER_NET:
                            return getString(R.string.setting_name_api_config_bither);
                        case BLOCKCHAIN_INFO:
                            return getString(R.string.setting_name_api_config_blockchain);
                    }
                    return getString(R.string.setting_name_api_config_bither);
                }

                @Override
                public CharSequence getOptionNote(int index) {
                    switch (getModeByIndex(index)) {
                        case BITHER_NET:
                            return getString(R.string.setting_api_config_bither_net);
                        case BLOCKCHAIN_INFO:
                            return getString(R.string.setting_api_config_blockchain);
                        default:
                            return getString(R.string.setting_api_config_bither_net);
                    }
                }

                @Override
                public Drawable getOptionDrawable(int index) {
                    return null;
                }

                @Override
                public CharSequence getSettingName() {
                    return getString(R.string.setting_api_config);
                }

                @Override
                public int getCurrentOptionIndex() {
                    BitherjSettings.ApiConfig apiConfig = AppSharedPreference.getInstance().getApiConfig();
                    switch (apiConfig) {
                        case BITHER_NET:
                            return 0;
                        case BLOCKCHAIN_INFO:
                            return 1;
                        default:
                            return 0;
                    }
                }

                @Override
                public void onOptionIndexSelected(int index) {
                    AppSharedPreference.getInstance().setApiConfig(getModeByIndex(index));
                }

                private BitherjSettings.ApiConfig getModeByIndex(int index) {
                    if (index >= 0 && index < length) {
                        switch (index) {
                            case 0:
                                return BitherjSettings.ApiConfig.BITHER_NET;
                            case 1:
                                return BitherjSettings.ApiConfig.BLOCKCHAIN_INFO;
                        }
                    }
                    return BitherjSettings.ApiConfig.BITHER_NET;
                }
            };

    private void importHDFromQRCode() {
        Intent intent = new Intent(this, ScanQRCodeTransportActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                getString(R.string.import_hd_account_seed_qr_code));
        startActivityForResult(intent, BitherSetting.INTENT_REF
                .IMPORT_HD_ACCOUNT_SEED_REQUEST_CODE);

    }

    private void importHDFromPhrase() {
        Intent intent = new Intent(this, HdmImportWordListActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.IMPORT_HD_SEED_TYPE, ImportHDSeed.ImportHDSeedType.HDSeedPhrase);
        startActivityForResult(intent, BitherSetting.INTENT_REF.IMPORT_ACCOUNT_SEED_FROM_PHRASE_REQUEST_CODE);
    }

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
            case BitherSetting.INTENT_REF.IMPORT_HD_ACCOUNT_SEED_REQUEST_CODE:
                final String hdAccountSeed = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                final MnemonicWordList mnemonicWordList = MnemonicWordList.getMnemonicWordListForHdSeed(hdAccountSeed);
                if (mnemonicWordList != null) {
                    if (mnemonicWordList == MnemonicWordList.English) {
                        new DialogImportHdAccountSeedQrCodeSelectLanguage(this, new DialogImportHdAccountSeedQrCodeSelectLanguage.Listener() {
                            @Override
                            public void onClicked(MnemonicWordList wordList) {
                                importHdAccountSeedQrCode(mnemonicWordList, wordList, hdAccountSeed);
                            }

                            @Override
                            public void cancel() {

                            }
                        }).show();
                    } else {
                        importHdAccountSeedQrCode(mnemonicWordList, mnemonicWordList, hdAccountSeed);
                    }
                } else {
                    DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.import_hd_account_seed_format_error);
                }
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
            case BitherSetting.INTENT_REF.IMPORT_ACCOUNT_SEED_FROM_PHRASE_REQUEST_CODE:
                ssvImportPrivateKey.loadData();
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void importHdAccountSeedQrCode(final MnemonicWordList mnemonicWordList, final MnemonicWordList selectMnemonicWordList, final String hdAccountSeed) {
        try {
            MnemonicCode mnemonicCode = new MnemonicCodeAndroid();
            mnemonicCode.setMnemonicWordList(mnemonicWordList);
            MnemonicCode selectMnemonicCode = new MnemonicCodeAndroid();
            selectMnemonicCode.setMnemonicWordList(selectMnemonicWordList);
            DialogPassword dialogPassword = new DialogPassword(this,
                    new ImportHDAccountPasswordListener(hdAccountSeed, mnemonicCode, selectMnemonicCode));
            dialogPassword.setCheckPre(false);
            dialogPassword.setCheckPasswordListener(new ICheckPasswordListener() {
                @Override
                public boolean checkPassword(SecureCharSequence password) {
                    String keyString = hdAccountSeed.substring(mnemonicWordList.getHdQrCodeFlag().length());
                    String[] passwordSeeds = QRCodeUtil.splitOfPasswordSeed(keyString);
                    String encreyptString = Utils.joinString(new String[]{passwordSeeds[0], passwordSeeds[1], passwordSeeds[2]}, QRCodeUtil.QR_CODE_SPLIT);
                    EncryptedData encryptedData = new EncryptedData(encreyptString);
                    byte[] result = null;
                    try {
                        result = encryptedData.decrypt(password);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return result != null;
                }
            });
            dialogPassword.setTitle(R.string.import_private_key_qr_code_password);
            dialogPassword.show();
        } catch (IOException e) {
            e.printStackTrace();
            DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.import_hd_account_seed_format_error);
        }
    }

    private class ImportHDAccountPasswordListener implements IDialogPasswordListener {
        private String content;
        private MnemonicCode mnemonicCode;
        private MnemonicCode qrSelectMnemonicCode;

        public ImportHDAccountPasswordListener(String content, MnemonicCode mnemonicCode, MnemonicCode qrSelectMnemonicCode) {
            this.content = content;
            this.mnemonicCode = mnemonicCode;
            this.qrSelectMnemonicCode = qrSelectMnemonicCode;
        }

        @Override
        public void onPasswordEntered(SecureCharSequence password) {
            if (dp != null && !dp.isShowing()) {
                dp.setMessage(R.string.import_private_key_qr_code_importing);
                LogUtil.d("importhdseed", "onPasswordEntered");
                ImportHDSeedAndroid importHDSeedAndroid = new ImportHDSeedAndroid
                        (HotAdvanceActivity.this, ImportHDSeed.ImportHDSeedType.HDSeedQRCode, dp, content, null, password, mnemonicCode, qrSelectMnemonicCode);
                importHDSeedAndroid.importHDSeed();
            }

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
        ssvImportPrivateKey.loadData();
        ssvImprotBip38Key.loadData();
        if (BitherApplication.hotActivity != null) {
            Fragment f = BitherApplication.hotActivity.getFragmentAtIndex(1);
            if (f != null && f instanceof Refreshable) {
                Refreshable r = (Refreshable) f;
                r.doRefresh();
            }
        }
        DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                R.string.import_private_key_qr_code_success, new Runnable() {
                    @Override
                    public void run() {
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

    private View.OnClickListener bitherAddressQrClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            new DialogAddressQrCopy(v.getContext(), BitherjSettings.DONATE_ADDRESS,
                    R.string.bither_team_address).show();
        }
    };

}
