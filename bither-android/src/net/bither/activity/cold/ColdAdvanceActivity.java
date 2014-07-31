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

import com.google.bitcoin.core.ECKey;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.ScanActivity;
import net.bither.ScanQRCodeTransportActivity;
import net.bither.fragment.Refreshable;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DialogEditPassword;
import net.bither.ui.base.DialogImportPrivateKeyText;
import net.bither.ui.base.DialogPassword;
import net.bither.ui.base.DialogProgress;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SettingSelectorView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.BackClickListener;
import net.bither.util.PrivateKeyUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.WalletUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 14-7-23.
 */
public class ColdAdvanceActivity extends SwipeRightFragmentActivity {
    private Button btnEditPassword;
    private SettingSelectorView ssvImportPrivateKey;
    private DialogProgress dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cold_advance_options);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new BackClickListener());
        btnEditPassword = (Button) findViewById(R.id.btn_edit_password);
        ssvImportPrivateKey = (SettingSelectorView) findViewById(R.id.ssv_import_private_key);
        ssvImportPrivateKey.setSelector(importPrivateKeySelector);
        btnEditPassword.setOnClickListener(editPasswordClick);
        dp = new DialogProgress(this, R.string.please_wait);
    }

    private View.OnClickListener editPasswordClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hasAnyAction = true;
            DialogEditPassword dialog = new DialogEditPassword(ColdAdvanceActivity.this);
            dialog.show();
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
        public void onPasswordEntered(String password) {
            if (dp != null && !dp.isShowing()) {
                dp.setMessage(R.string.import_private_key_qr_code_importing);
                dp.show();
                ImportPrivateKeyThread importPrivateKeyThread = new ImportPrivateKeyThread
                        (content, password);
                importPrivateKeyThread.start();
            }
        }
    }

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
                }
        );
    }

    private class ImportPrivateKeyThread extends Thread {
        private String content;
        private String password;


        public ImportPrivateKeyThread(String content, String password) {
            this.content = content;
            this.password = password;
        }

        @Override
        public void run() {
            ECKey key = PrivateKeyUtil.getECKeyFromSingleString(content, password);
            if (key == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(ColdAdvanceActivity.this,
                                R.string.import_private_key_qr_code_failed);
                    }
                });
                return;
            }
            BitherAddressWithPrivateKey wallet = new BitherAddressWithPrivateKey(false);
            wallet.setKeyCrypter(key.getKeyCrypter());
            wallet.addKey(key);
            if (WalletUtils.getWatchOnlyAddressList().contains(wallet)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(ColdAdvanceActivity.this,
                                R.string.import_private_key_qr_code_failed_monitored);
                    }
                });
                return;
            } else if (WalletUtils.getPrivateAddressList().contains(wallet)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(ColdAdvanceActivity.this,
                                R.string.import_private_key_qr_code_failed_duplicate);
                    }
                });
                return;
            } else {
                PasswordSeed passwordSeed = AppSharedPreference.getInstance().getPasswordSeed();
                if (passwordSeed != null && !passwordSeed.checkPassword(password)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp != null && dp.isShowing()) {
                                dp.setThread(null);
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(ColdAdvanceActivity.this,
                                    R.string.import_private_key_qr_code_failed_different_password);
                        }
                    });
                    return;
                }
                List<BitherAddressWithPrivateKey> wallets = new
                        ArrayList<BitherAddressWithPrivateKey>();
                wallets.add(wallet);
                WalletUtils.addAddressWithPrivateKey(null, wallets);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    showImportSuccess();
                }
            });
        }
    }
}
