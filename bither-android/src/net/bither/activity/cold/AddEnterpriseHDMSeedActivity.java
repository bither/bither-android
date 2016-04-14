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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.crypto.EncryptedData;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.ScanActivity;
import net.bither.qrcode.ScanQRCodeTransportActivity;
import net.bither.qrcode.ScanQRCodeWithOtherActivity;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogWithActions;
import net.bither.ui.base.dialog.DialogXRandomInfo;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.ICheckPasswordListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;
import net.bither.xrandom.EnterpriseHDMSeedUEntropyActivity;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/6/9.
 */
public class AddEnterpriseHDMSeedActivity extends SwipeRightFragmentActivity {
    private static final int ImportFromQRTag = 1341;

    private CheckBox cbxXRandom;
    private DialogProgress dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_enterprise_hdm_seed);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        cbxXRandom = (CheckBox) findViewById(R.id.cbx_xrandom);
        cbxXRandom.setOnCheckedChangeListener(xrandomCheckedChange);
        findViewById(R.id.ibtn_xrandom_info).setOnClickListener(DialogXRandomInfo.GuideClick);
        findViewById(R.id.btn_add).setOnClickListener(addClick);
        findViewById(R.id.ibtn_option).setOnClickListener(optionsClick);
        dp = new DialogProgress(this, R.string.please_wait);
    }

    private CompoundButton.OnCheckedChangeListener xrandomCheckedChange = new CompoundButton
            .OnCheckedChangeListener() {
        private boolean ignoreListener = false;
        private DialogConfirmTask dialog;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked && !ignoreListener) {
                cbxXRandom.setChecked(true);
                getDialog().show();
            }
        }

        private DialogConfirmTask getDialog() {
            if (dialog == null) {
                dialog = new DialogConfirmTask(AddEnterpriseHDMSeedActivity.this, getResources()
                        .getString(R.string.xrandom_uncheck_warn), new Runnable() {
                    @Override
                    public void run() {
                        cbxXRandom.post(new Runnable() {
                            @Override
                            public void run() {
                                ignoreListener = true;
                                cbxXRandom.setChecked(false);
                                ignoreListener = false;
                            }
                        });
                    }
                });
            }
            return dialog;
        }
    };

    private View.OnClickListener addClick = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            if (cbxXRandom.isChecked()) {
                final Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(AddEnterpriseHDMSeedActivity.this,
                                EnterpriseHDMSeedUEntropyActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                        startActivity(intent);
                        finish();
                    }
                };
                if (AppSharedPreference.getInstance().shouldAutoShowXRandomInstruction()) {
                    DialogXRandomInfo dialog = new DialogXRandomInfo(AddEnterpriseHDMSeedActivity
                            .this, true, true);
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            run.run();
                        }
                    });
                    dialog.show();
                } else {
                    run.run();
                }
            } else {
                final DialogPassword.PasswordGetter passwordGetter = new DialogPassword
                        .PasswordGetter(AddEnterpriseHDMSeedActivity.this);
                final DialogProgress dp = new DialogProgress(v.getContext(), R.string.please_wait);
                new Thread() {
                    @Override
                    public void run() {
                        SecureCharSequence password = passwordGetter.getPassword();
                        if (password == null) {
                            return;
                        }
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.show();
                                v.setKeepScreenOn(true);
                            }
                        });
                        final EnterpriseHDMSeed seed = new EnterpriseHDMSeed(new SecureRandom(),
                                password);
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                v.setKeepScreenOn(false);
                                dp.dismiss();
                                if (seed != null) {
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    DropdownMessage.showDropdownMessage
                                            (AddEnterpriseHDMSeedActivity.this, R.string
                                                    .enterprise_hdm_seed_add_fail);
                                }
                            }
                        });
                    }
                }.start();

            }
        }
    };

    private View.OnClickListener optionsClick = new DialogWithActions
            .DialogWithActionsClickListener() {

        @Override
        protected List<DialogWithActions.Action> getActions() {
            ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
            actions.add(new DialogWithActions.Action(R.string
                    .enterprise_hdm_seed_import_from_qr_code, new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(AddEnterpriseHDMSeedActivity.this,
                            ScanQRCodeWithOtherActivity.class);
                    intent.putExtra(BitherSetting.INTENT_REF.QRCODE_TYPE, BitherSetting.QRCodeType.Bither);
                    intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, getString(R.string
                            .enterprise_hdm_seed_import_from_qr_code));
                    startActivityForResult(intent, ImportFromQRTag);
                }
            }));
            actions.add(new DialogWithActions.Action(R.string
                    .enterprise_hdm_seed_import_from_phrase, new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(AddEnterpriseHDMSeedActivity.this,
                            EnterpriseHdmImportPhraseActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                    startActivity(intent);
                    finish();
                }
            }));
            return actions;
        }
    };

    private class ImportSeedPasswordListener implements IDialogPasswordListener {
        private String content;

        public ImportSeedPasswordListener(String content) {
            this.content = content;
        }

        @Override
        public void onPasswordEntered(final SecureCharSequence password) {
            if (dp != null && !dp.isShowing()) {
                dp.setMessage(R.string.import_private_key_qr_code_importing);
            }
            new ThreadNeedService(dp, AddEnterpriseHDMSeedActivity.this) {

                @Override
                public void runWithService(BlockchainService service) {
                    EncryptedData data = new EncryptedData(content.substring(1));
                    try {
                        EnterpriseHDMSeed seed = new EnterpriseHDMSeed(data.decrypt(new
                                SecureCharSequence(password)), data.isXRandom(), password);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    } catch (MnemonicException.MnemonicLengthException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                DropdownMessage.showDropdownMessage(AddEnterpriseHDMSeedActivity
                                        .this, R.string.enterprise_hdm_seed_add_fail);
                            }
                        });
                    }
                }
            }.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ImportFromQRTag) {
            final String seedStr = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            if (seedStr.indexOf(QRCodeUtil.Enterprise_HDM_QR_CODE_FLAG) == 0) {
                DialogPassword dialogPassword = new DialogPassword(this, new
                        ImportSeedPasswordListener(seedStr));
                dialogPassword.setCheckPre(false);
                dialogPassword.setCheckPasswordListener(new ICheckPasswordListener() {
                    @Override
                    public boolean checkPassword(SecureCharSequence password) {
                        String keyString = seedStr.substring(1);
                        String[] passwordSeeds = QRCodeUtil.splitOfPasswordSeed(keyString);
                        String encreyptString = Utils.joinString(new String[]{passwordSeeds[0],
                                passwordSeeds[1], passwordSeeds[2]}, QRCodeUtil.QR_CODE_SPLIT);
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
            } else {
                DropdownMessage.showDropdownMessage(AddEnterpriseHDMSeedActivity.this, R.string
                        .enterprise_hdm_seed_import_format_error);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}