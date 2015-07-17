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

import net.bither.R;
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogXRandomInfo;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.ThreadUtil;
import net.bither.xrandom.EnterpriseHDMSeedUEntropyActivity;

import java.security.SecureRandom;

/**
 * Created by songchenwen on 15/6/9.
 */
public class AddEnterpriseHDMSeedActivity extends SwipeRightFragmentActivity {
    private CheckBox cbxXRandom;

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
}