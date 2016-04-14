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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import net.bither.R;
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogHDMSeedWordList;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSimpleQr;
import net.bither.ui.base.listener.IBackClickListener;

import java.util.List;

/**
 * Created by songchenwen on 15/6/9.
 */
public class EnterpriseHDMSeedActivity extends SwipeRightFragmentActivity {
    private static final int AddCode = 1504;
    private EnterpriseHDMSeed seed;
    private DialogProgress dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise_hdm_seed);
        initView();
        if (EnterpriseHDMSeed.hasSeed()) {
            seed = EnterpriseHDMSeed.seed();
        } else {
            startActivityForResult(new Intent(this, AddEnterpriseHDMSeedActivity.class), AddCode);
        }
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        findViewById(R.id.btn_seed_qr).setOnClickListener(qrClick);
        findViewById(R.id.btn_cosign).setOnClickListener(cosignClick);
        findViewById(R.id.btn_backup_qr).setOnClickListener(backupQrClick);
        findViewById(R.id.btn_backup_phrase).setOnClickListener(backupPhraseClick);
        dp = new DialogProgress(this, R.string.please_wait);
    }

    private View.OnClickListener qrClick = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            final DialogPassword.PasswordGetter passwordGetter = new DialogPassword
                    .PasswordGetter(v.getContext());
            new Thread() {
                @Override
                public void run() {
                    if (seed == null) {
                        return;
                    }
                    SecureCharSequence password = passwordGetter.getPassword();
                    if (password == null) {
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.show();
                        }
                    });
                    try {
                        final byte[] extPub = seed.getExternalRootPubExtended(password);
                        password.wipe();
                        passwordGetter.wipe();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                new DialogSimpleQr(v.getContext(), EnterpriseHDMSeed.XPubPrefix
                                        + Utils.bytesToHexString(extPub).toUpperCase(),
                                        R.string.enterprise_hdm_seed_backup_qr_code_promote).show();
                            }
                        });
                    } catch (MnemonicException.MnemonicLengthException e) {
                        e.printStackTrace();
                        password.wipe();
                        passwordGetter.wipe();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                            }
                        });
                    }
                }
            }.start();
        }
    };

    private View.OnClickListener cosignClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(), SignTxActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener backupQrClick = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            final DialogPassword.PasswordGetter passwordGetter = new DialogPassword
                    .PasswordGetter(v.getContext());
            new Thread() {
                @Override
                public void run() {
                    SecureCharSequence password = passwordGetter.getPassword();
                    if (password == null) {
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.show();
                        }
                    });
                    final String content = QRCodeUtil.Enterprise_HDM_QR_CODE_FLAG + seed
                            .getEncryptedMnemonicSeed();
                    password.wipe();
                    passwordGetter.wipe();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            if (content != null) {
                                new DialogSimpleQr(v.getContext(), content, R.string
                                        .enterprise_hdm_seed_backup_qr_code_promote).show();
                            }
                        }
                    });
                }
            }.start();
        }
    };

    private View.OnClickListener backupPhraseClick = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            final DialogPassword.PasswordGetter passwordGetter = new DialogPassword
                    .PasswordGetter(v.getContext());
            new Thread() {
                @Override
                public void run() {
                    SecureCharSequence password = passwordGetter.getPassword();
                    if (password == null) {
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.show();
                        }
                    });
                    try {
                        final List<String> words = seed.getSeedWords(password);
                        password.wipe();
                        passwordGetter.wipe();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                if (words != null) {
                                    new DialogHDMSeedWordList(v.getContext(), words).show();
                                }
                            }
                        });
                    } catch (MnemonicException.MnemonicLengthException e) {
                        password.wipe();
                        passwordGetter.wipe();
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                            }
                        });
                    }
                }
            }.start();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AddCode) {
            if (resultCode != RESULT_OK) {
                finish();
            } else {
                seed = EnterpriseHDMSeed.seed();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
