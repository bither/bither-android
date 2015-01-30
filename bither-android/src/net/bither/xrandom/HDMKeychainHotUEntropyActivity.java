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

package net.bither.xrandom;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.fragment.cold.AddAddressColdHDMFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.dialog.CenterDialog;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.util.KeyUtil;

/**
 * Created by songchenwen on 15/1/9.
 */
public class HDMKeychainHotUEntropyActivity extends UEntropyActivity {
    private static final int MinGeneratingTime = 5000;
    private GenerateThread generateThread;
    public static DialogPassword.PasswordGetter passwordGetter;

    @Override
    Thread getGeneratingThreadWithXRandom(UEntropyCollector collector,
                                          SecureCharSequence password) {
        generateThread = new GenerateThread(collector, password);
        return generateThread;
    }

    @Override
    void cancelGenerating(Runnable cancelRunnable) {
        generateThread.cancel(cancelRunnable);
    }

    @Override
    void didSuccess(Object obj) {
        setResult(RESULT_OK);
        finish();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

    private class GenerateThread extends ThreadNeedService {
        private double saveProgress = 0.1;
        private double startProgress = 0.01;
        private double progressKeyRate = 0.5;
        private double progressEntryptRate = 0.5;

        private long startGeneratingTime;

        private SecureCharSequence password;
        private Runnable cancelRunnable;
        private UEntropyCollector entropyCollector;

        public GenerateThread(UEntropyCollector collector, SecureCharSequence password) {
            super(null, HDMKeychainHotUEntropyActivity.this);
            this.password = password;
            entropyCollector = collector;
        }

        @Override
        public synchronized void start() {
            if (password == null) {
                throw new IllegalStateException("GenerateThread does not have password");
            }
            startGeneratingTime = System.currentTimeMillis();
            super.start();
            onProgress(startProgress);
        }

        public void cancel(Runnable cancelRunnable) {
            this.cancelRunnable = cancelRunnable;
        }

        private void finishGenerate(BlockchainService service) {
            entropyCollector.stop();
        }


        @Override
        public void runWithService(BlockchainService service) {
            boolean success = false;
            double progress = startProgress;
            double itemProgress = (1.0 - startProgress - saveProgress);

            try {
                entropyCollector.start();

                XRandom xRandom = new XRandom(entropyCollector);

                if (cancelRunnable != null) {
                    finishGenerate(service);
                    runOnUiThread(cancelRunnable);
                    return;
                }

                HDMKeychain chain = new HDMKeychain(xRandom, password);

                progress += itemProgress * progressKeyRate;
                onProgress(progress);
                if (cancelRunnable != null) {
                    finishGenerate(service);
                    runOnUiThread(cancelRunnable);
                    return;
                }
                KeyUtil.setHDKeyChain(chain);
                progress += itemProgress * progressEntryptRate;
                onProgress(progress);

                entropyCollector.stop();
                passwordGetter.setPassword(password);
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            finishGenerate(service);
            if (success) {
                while (System.currentTimeMillis() - startGeneratingTime < MinGeneratingTime) {

                }
                onProgress(1);
                onSuccess(AddAddressColdHDMFragment.HDMSeedAddressPlaceHolder);
            } else {
                onFailed();
            }
        }
    }

    private static class DialogGenerateHDMKeychainFinalConfirm extends CenterDialog implements
            View.OnClickListener {
        private TextView tv;

        public DialogGenerateHDMKeychainFinalConfirm(Context context) {
            super(context);
            setContentView(R.layout.dialog_xrandom_final_confirm);
            tv = (TextView) findViewById(R.id.tv);
            tv.setText(context.getString(R.string.hdm_keychain_xrandom_final_confirm));
            findViewById(R.id.btn_ok).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            dismiss();
        }
    }

    @Override
    protected void backToFromActivity() {
        overridePendingTransition(R.anim.uentropy_activity_back_enter, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        passwordGetter = null;
    }
}
