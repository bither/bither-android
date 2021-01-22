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
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitpieHDAccountCold;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicCode;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DialogFragmentHDMSingularColdSeed;
import net.bither.util.BackupUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yiwenlong on 19/1/2.
 */
public class BitpieHDAccountColdUEntropyActivity extends UEntropyActivity {
    private static final int MinGeneratingTime = 5000;
    private GenerateThread generateThread;
    private List<String> words;

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
        final Intent intent = new Intent();
        ArrayList<String> addresses = new ArrayList<String>();
        addresses.add(BitpieHDAccountCold.BitpieHDAccountPlaceHolder);
        intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG, addresses);
        DialogFragmentHDMSingularColdSeed.newInstance(
                words,
                R.string.bitpie_add_hd_account_show_seed_label,
                R.string.bitpie_add_hd_account_show_seed_button,
                new DialogFragmentHDMSingularColdSeed.DialogFragmentHDMSingularColdSeedListener() {
                    @Override
                    public void HDMSingularColdSeedRemembered() {
                        setResult(RESULT_OK, intent);
                        finish();
                        overridePendingTransition(0, R.anim.slide_out_bottom);
                    }
                })
                .show(getSupportFragmentManager(), DialogFragmentHDMSingularColdSeed.FragmentTag);
    }

    private class GenerateThread extends ThreadNeedService {
        final private double saveProgress = 0.02;
        final private double startProgress = 0.02;

        private long startGeneratingTime;

        private SecureCharSequence password;
        private Runnable cancelRunnable;
        private UEntropyCollector entropyCollector;

        public GenerateThread(UEntropyCollector collector, SecureCharSequence password) {
            super(null, BitpieHDAccountColdUEntropyActivity.this);
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
        public void runWithService(final BlockchainService service) {
            boolean success = false;
            onProgress(startProgress);
            BitpieHDAccountCold hdAccount = null;
            Integer hdSeedId = null;
            try {
                if (service != null) {
                    service.stopAndUnregister();
                }
                entropyCollector.start();

                XRandom xRandom = new XRandom(entropyCollector);

                if (cancelRunnable != null) {
                    finishGenerate(service);
                    runOnUiThread(cancelRunnable);
                    return;
                }

                hdAccount = new BitpieHDAccountCold(MnemonicCode.instance(), xRandom, password);
                hdSeedId = hdAccount.getHdSeedId();
                if (cancelRunnable != null) {
                    finishGenerate(service);
                    runOnUiThread(cancelRunnable);
                    return;
                }

                final List<String> validWords = hdAccount.getSeedWords(password);
                String firstAddress = HDAccount.getFirstAddress(words);
                String dbFirstAddress = hdAccount.getFirstAddressFromDb();
                if (!firstAddress.equals(dbFirstAddress)) {
                    onFailed(hdSeedId, password, new Runnable() {
                        @Override
                        public void run() {
                            finishGenerate(service);
                        }
                    });
                    return;
                }
                words = validWords;

                BackupUtil.backupColdKey(false);

                onProgress(1);

                entropyCollector.stop();
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (success) {
                finishGenerate(service);
                while (System.currentTimeMillis() - startGeneratingTime < MinGeneratingTime) { }
                onProgress(1);
                onSuccess(BitpieHDAccountCold.BitpieHDAccountPlaceHolder);
            } else {
                onFailed(hdSeedId, password, new Runnable() {
                    @Override
                    public void run() {
                        finishGenerate(service);
                    }
                });
            }
        }
    }
}
