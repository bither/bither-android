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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.dialog.DialogGenerateAddressFinalConfirm;
import net.bither.util.KeyUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/1/7.
 */
public class PrivateKeyUEntropyActivity extends UEntropyActivity {
    public static final String PrivateKeyCountKey = UEntropyActivity.class.getName() + ".private_key_count_key";
    private static final int MinGeneratingTime = 5000;
    private int targetCount;
    private GenerateThread generateThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        targetCount = getIntent().getExtras().getInt(PrivateKeyCountKey, 0);
        if (targetCount <= 0) {
            super.finish();
            return;
        }
    }

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
        if (obj instanceof ArrayList && ((ArrayList) obj).get(0) instanceof String) {
            final ArrayList<String> addresses = (ArrayList<String>) obj;
            DialogGenerateAddressFinalConfirm dialog = new DialogGenerateAddressFinalConfirm
                    (PrivateKeyUEntropyActivity.this, addresses.size(), true);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Intent intent = new Intent();
                    intent.putExtra(BitherSetting.INTENT_REF.ADD_PRIVATE_KEY_SUGGEST_CHECK_TAG,
                            !PasswordSeed.hasPasswordSeed());
                    intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG,
                            addresses);
                    setResult(RESULT_OK, intent);
                    finish();
                    overridePendingTransition(0, R.anim.slide_out_bottom);
                }
            });
            dialog.show();
        } else {
            setResult(RESULT_OK);
            finish();
            overridePendingTransition(0, R.anim.slide_out_bottom);
        }
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
            super(null, PrivateKeyUEntropyActivity.this);
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
            final ArrayList<String> addressStrs = new ArrayList<String>();
            double progress = startProgress;
            double itemProgress = (1.0 - startProgress - saveProgress) / (double) targetCount;

            try {
                entropyCollector.start();
                if (service != null) {
                    service.stopAndUnregister();
                }

                XRandom xRandom = new XRandom(entropyCollector);

                List<Address> addressList = new ArrayList<Address>();
                for (int i = 0;
                     i < targetCount;
                     i++) {
                    if (cancelRunnable != null) {
                        finishGenerate(service);
                        runOnUiThread(cancelRunnable);
                        return;
                    }

                    ECKey ecKey = ECKey.generateECKey(xRandom);
                    ecKey.setFromXRandom(true);

                    progress += itemProgress * progressKeyRate;
                    onProgress(progress);
                    if (cancelRunnable != null) {
                        finishGenerate(service);
                        runOnUiThread(cancelRunnable);
                        return;
                    }


                    // start encrypt
                    ecKey = PrivateKeyUtil.encrypt(ecKey, password);
                    Address address = new Address(ecKey.toAddress(), ecKey.getPubKey(),
                            PrivateKeyUtil.getEncryptedString(ecKey), ecKey.isFromXRandom());
                    ecKey.clearPrivateKey();
                    addressList.add(address);
                    addressStrs.add(address.getAddress());

                    progress += itemProgress * progressEntryptRate;
                    onProgress(progress);
                }

                entropyCollector.stop();
                password.wipe();
                password = null;

                if (cancelRunnable != null) {
                    finishGenerate(service);
                    runOnUiThread(cancelRunnable);
                    return;
                }

                KeyUtil.addAddressListByDesc(service, addressList);
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            finishGenerate(service);
            if (success) {
                while (System.currentTimeMillis() - startGeneratingTime < MinGeneratingTime) {

                }
                onProgress(1);
                onSuccess(addressStrs);
            } else {
                onFailed();
            }
        }
    }

}
