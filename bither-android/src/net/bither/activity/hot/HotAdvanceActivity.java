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

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.ScanActivity;
import net.bither.ScanQRCodeTransportActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.db.TxProvider;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ImportPrivateKeyWithHotThread;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SettingSelectorView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogEditPassword;
import net.bither.ui.base.dialog.DialogImportPrivateKeyText;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.BackClickListener;
import net.bither.util.FileUtil;
import net.bither.util.SecureCharSequence;
import net.bither.util.ThreadUtil;
import net.bither.util.TransactionsUtil;

import java.io.File;
import java.io.IOException;

public class HotAdvanceActivity extends SwipeRightFragmentActivity {
    private SettingSelectorView ssvWifi;
    private Button btnEditPassword;
    private SettingSelectorView ssvImportPrivateKey;
    private Button btnExportLog;
    private Button btnResetTx;
    private DialogProgress dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot_advance_options);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new BackClickListener());
        ssvWifi = (SettingSelectorView) findViewById(R.id.ssv_wifi);
        btnEditPassword = (Button) findViewById(R.id.btn_edit_password);
        ssvImportPrivateKey = (SettingSelectorView) findViewById(R.id.ssv_import_private_key);
        ssvWifi.setSelector(wifiSelector);
        ssvImportPrivateKey.setSelector(importPrivateKeySelector);
        btnEditPassword.setOnClickListener(editPasswordClick);
        dp = new DialogProgress(this, R.string.please_wait);
        btnExportLog = (Button) findViewById(R.id.btn_export_log);
        btnExportLog.setOnClickListener(exportLogClick);
        btnResetTx = (Button) findViewById(R.id.btn_reset_tx);
        btnResetTx.setOnClickListener(resetTxListener);
    }

    private SettingSelectorView.SettingSelector wifiSelector = new SettingSelectorView
            .SettingSelector() {

        @Override
        public void onOptionIndexSelected(int index) {
            hasAnyAction = true;
            final boolean isOnlyWifi = index == 1;
            AppSharedPreference.getInstance().setSyncBlockOnlyWifi(isOnlyWifi);
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

    private View.OnClickListener editPasswordClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hasAnyAction = true;
            DialogEditPassword dialog = new DialogEditPassword(HotAdvanceActivity.this);
            dialog.show();
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
                            File logDir = Utils.getLogDir();

                            FileUtil.copyFile(logDir, logTagDir);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this,
                                        getString(R.string.export_success) + "\n" + logTagDir.getAbsolutePath());
                            }
                        });
                    }
                };
                DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(HotAdvanceActivity.this
                        , getString(R.string.export_log_prompt), confirmRunnable);
                dialogConfirmTask.show();
            } else {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.no_sd_card);
            }

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
                        PasswordSeed passwordSeed = AppSharedPreference.getInstance().getPasswordSeed();
                        if (passwordSeed == null) {
                            resetTx();
                        } else {
                            callPassword();
                        }
                    }
                };
                DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(HotAdvanceActivity.this,
                        getString(R.string.reload_tx_need_too_much_time), confirmRunnable
                );
                dialogConfirmTask.show();

            } else {
                DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.tx_cannot_reloding);
            }


        }


    };

    private void callPassword() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                DialogPassword dialogPassword = new DialogPassword(HotAdvanceActivity.this, new DialogPassword.DialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        resetTx();

                    }
                });
                dialogPassword.show();

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
                try {
                    service.stopAndUnregister();
                    for (Address address : AddressManager.getInstance().getAllAddresses()) {
                        address.setSyncComplete(false);
                        address.savePubKey();

                    }
                    TxProvider.getInstance().clearAllTx();
                    for (Address address : AddressManager.getInstance().getAllAddresses()) {
                        address.notificatTx(null, Tx.TxNotificationType.txFromApi);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.reload_tx_failed);
                        }
                    });
                    return;
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
                            DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.reload_tx_success);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    HotAdvanceActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            DropdownMessage.showDropdownMessage(HotAdvanceActivity.this, R.string.network_or_connection_error);
                        }
                    });

                }
            }
        };
        threadNeedService.start();


    }

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
                            importPrivateKeyFromQrCode();
                            return;
                        case 1:
                            importPrivateKeyFromText();
                            return;
                        default:
                            return;
                    }
                }
            };

    private void importPrivateKeyFromQrCode() {
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
                String content = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                DialogPassword dialogPassword = new DialogPassword(this,
                        new ImportPrivateKeyPasswordListener(content));
                dialogPassword.setCheckPre(false);
                dialogPassword.setTitle(R.string.import_private_key_qr_code_password);
                dialogPassword.show();
                break;
        }
    }

    private class ImportPrivateKeyPasswordListener implements DialogPassword
            .DialogPasswordListener {
        private String content;

        public ImportPrivateKeyPasswordListener(String content) {
            this.content = content;
        }

        @Override
        public void onPasswordEntered(SecureCharSequence password) {
            if (dp != null && !dp.isShowing()) {
                dp.setMessage(R.string.import_private_key_qr_code_importing);
                Runnable impoprtSuccessRunnable = new Runnable() {
                    @Override
                    public void run() {
                        showImportSuccess();
                    }
                };
                ImportPrivateKeyWithHotThread importPrivateKeyThread = new ImportPrivateKeyWithHotThread(HotAdvanceActivity.this, dp,
                        content, password, impoprtSuccessRunnable);
                importPrivateKeyThread.start();
            }
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
                }
        );
    }


}
