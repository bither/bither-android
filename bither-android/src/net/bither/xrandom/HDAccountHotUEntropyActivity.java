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

import android.content.Intent;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.fragment.hot.AddAddressHotHDAccountFragment;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.util.KeyUtil;

import java.util.ArrayList;

/**
 * Created by songchenwen on 15/4/16.
 */
public class HDAccountHotUEntropyActivity extends UEntropyActivity {
    private static final int MinGeneratingTime = 5000;
    private GenerateThread generateThread;

    @Override
    Thread getGeneratingThreadWithXRandom(UEntropyCollector collector, SecureCharSequence
            password) {
        generateThread = new GenerateThread(collector, password);
        return generateThread;
    }

    @Override
    void cancelGenerating(Runnable cancelRunnable) {
        generateThread.cancel(cancelRunnable);
    }

    @Override
    void didSuccess(Object obj) {
        Intent intent = new Intent();
        ArrayList<String> addresses = new ArrayList<String>();
        addresses.add(AddAddressHotHDAccountFragment.HDAccountPlaceHolder);
        intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG, addresses);
        setResult(RESULT_OK, intent);
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
            super(null, HDAccountHotUEntropyActivity.this);
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
            if (password != null) {
                password.wipe();
                password = null;
            }
            if (service != null) {
                service.startAndRegister();
            }
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

                byte[] entropy = new byte[16];
                xRandom.nextBytes(entropy);
                progress += itemProgress * progressKeyRate;
                onProgress(progress);
                if (cancelRunnable != null) {
                    finishGenerate(service);
                    runOnUiThread(cancelRunnable);
                    return;
                }

                HDAccount hdAccount = new HDAccount(entropy, password);
                KeyUtil.setHDAccount(service, hdAccount);

                progress += itemProgress * progressEntryptRate;
                onProgress(progress);

                entropyCollector.stop();
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            finishGenerate(service);
            if (success) {
                while (System.currentTimeMillis() - startGeneratingTime < MinGeneratingTime) {

                }
                onProgress(1);
                onSuccess(AddAddressHotHDAccountFragment.HDAccountPlaceHolder);
            } else {
                onFailed();
            }
        }
    }
}
