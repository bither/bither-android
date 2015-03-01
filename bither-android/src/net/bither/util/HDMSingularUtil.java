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

package net.bither.util;

import android.content.Context;

import net.bither.BitherSetting;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.EncryptedData;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.hd.DeterministicKey;
import net.bither.bitherj.crypto.hd.HDKeyDerivation;
import net.bither.bitherj.crypto.mnemonic.MnemonicCode;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Created by songchenwen on 15/2/12.
 */
public class HDMSingularUtil {
    public static interface HDMSingularUtilDelegate {
        public void setSingularModeAvailable(boolean available);

        public void onSingularModeBegin();

        public boolean shouldGoSingularMode();

        public void singularHotFinish();

        public void singularColdFinish();

        public void singularServerFinish(List<String> words, String qr);

        public void singularShowNetworkFailure();
    }

    private Context context;
    private HDMSingularUtilDelegate delegate;

    private boolean running;
    private boolean isSingularMode;

    private SecureCharSequence password;

    private byte[] hotMnemonicSeed;
    private byte[] coldMnemonicSeed;

    private String hotFirstAddress;
    private byte[] coldRoot;
    private DeterministicKey coldFirst;

    private HDMBId hdmBid;

    private List<String> coldWords;
    private String coldQr;

    public HDMSingularUtil(@Nonnull Context context, @Nonnull HDMSingularUtilDelegate delegate) {
        this.delegate = delegate;
        this.context = context;
        if (AddressManager.getInstance().getHdmKeychain() == null) {
            delegate.setSingularModeAvailable(true);
            running = false;
            isSingularMode = false;
        } else {
            delegate.setSingularModeAvailable(false);
            running = true;
            isSingularMode = false;
        }
    }

    public void runningWithoutSingularMode() {
        isSingularMode = false;
        running = true;
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                delegate.setSingularModeAvailable(false);
            }
        });
    }

    public boolean isInSingularMode() {
        return running && isSingularMode;
    }

    public boolean shouldGoSingularMode() {
        return delegate.shouldGoSingularMode();
    }

    public void setEntropy(byte[] entropy) {
        assert entropy.length == 64;
        delegate.onSingularModeBegin();
        running = true;
        isSingularMode = true;
        setEntropyInterval(entropy, true);
    }

    public void xrandomFinished() {
        delegate.singularHotFinish();
    }

    public void generateEntropy() {
        assert password != null;
        delegate.onSingularModeBegin();
        running = true;
        isSingularMode = true;
        new Thread() {
            @Override
            public void run() {
                byte[] entropy = new byte[64];
                new SecureRandom().nextBytes(entropy);
                setEntropyInterval(entropy, false);
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        delegate.singularHotFinish();
                    }
                });
            }
        }.start();
    }

    public void setPassword(SecureCharSequence password) {
        this.password = new SecureCharSequence(password);
    }

    private void setEntropyInterval(byte[] entropy, boolean xrandom) {
        hotMnemonicSeed = Arrays.copyOf(entropy, 32);
        coldMnemonicSeed = Arrays.copyOfRange(entropy, 32, 64);
        Utils.wipeBytes(entropy);
        initHotFirst();
        EncryptedData coldEncryptedMnemonicSeed = new EncryptedData(coldMnemonicSeed, password,
                xrandom);
        coldQr = QRCodeUtil.HDM_QR_CODE_FLAG + PrivateKeyUtil.getFullencryptHDMKeyChain(xrandom,
                coldEncryptedMnemonicSeed.toEncryptedString());
    }

    public void cold() {
        assert password != null;
        new Thread() {
            @Override
            public void run() {
                try {
                    coldWords = MnemonicCode.instance().toMnemonic(coldMnemonicSeed);
                } catch (MnemonicException.MnemonicLengthException e) {
                    throw new RuntimeException(e);
                }
                initColdFirst();
                hdmBid = new HDMBId(coldFirst.toAddress());
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        delegate.singularColdFinish();
                    }
                });
            }
        }.start();
    }

    public void server() {
        new ThreadNeedService(null, context) {
            @Override
            public void runWithService(BlockchainService service) {
                String preSign;
                try {
                    preSign = hdmBid.getPreSignString();
                } catch (Exception e) {
                    e.printStackTrace();
                    password.wipe();
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            delegate.singularShowNetworkFailure();
                        }
                    });
                    return;
                }
                byte[] sig = coldFirst.signHash(Utils.hexStringToByteArray(preSign), null);
                try {
                    hdmBid.setSignature(sig, password, hotFirstAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                    password.wipe();
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            delegate.singularShowNetworkFailure();
                        }
                    });
                    return;
                }
                try {
                    HDMKeychain keychain = new HDMKeychain(hotMnemonicSeed, password);
                    hdmBid.save();
                    KeyUtil.setHDKeyChain(keychain);
                    final int count = BitherjSettings.HDM_ADDRESS_PER_SEED_PREPARE_COUNT - keychain
                            .uncompletedAddressCount();
                    if (count > 0) {
                        keychain.prepareAddresses(count, password, Arrays.copyOf(coldRoot,
                                coldRoot.length));
                    }
                    if (service != null) {
                        service.stopAndUnregister();
                    }
                    keychain.completeAddresses(1, password,
                            new HDMKeychain.HDMFetchRemotePublicKeys() {
                                @Override
                                public void completeRemotePublicKeys(CharSequence password,
                                                                     List<HDMAddress.Pubs>
                                                                             partialPubs) {
                                    try {
                                        HDMKeychain.getRemotePublicKeys(hdmBid, password,
                                                partialPubs);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        ThreadUtil.runOnMainThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                delegate.singularShowNetworkFailure();
                                            }
                                        });
                                    }
                                }
                            });
                    if (service != null) {
                        service.startAndRegister();
                    }
                } catch (MnemonicException.MnemonicLengthException e) {
                    password.wipe();
                    throw new RuntimeException(e);
                }
                password.wipe();
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        delegate.singularServerFinish(coldWords, coldQr);
                    }
                });
            }
        }.start();
    }

    private void initHotFirst() {
        DeterministicKey hotEx = rootFromMnemonic(hotMnemonicSeed);
        DeterministicKey hotFirst = hotEx.deriveSoftened(0);
        hotFirstAddress = hotFirst.toAddress();
        hotEx.wipe();
        hotFirst.wipe();
    }

    private void initColdFirst() {
        DeterministicKey coldEx = rootFromMnemonic(coldMnemonicSeed);
        coldRoot = coldEx.getPubKeyExtended();
        coldFirst = coldEx.deriveSoftened(0);
        coldEx.wipe();
    }

    private DeterministicKey rootFromMnemonic(byte[] mnemonic) {
        try {
            byte[] hdSeed = HDMKeychain.seedFromMnemonic(mnemonic);
            DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
            DeterministicKey purpose = master.deriveHardened(44);
            DeterministicKey coinType = purpose.deriveHardened(0);
            DeterministicKey account = coinType.deriveHardened(0);
            DeterministicKey external = account.deriveSoftened(0);
            master.wipe();
            purpose.wipe();
            coinType.wipe();
            account.wipe();
            return external;
        } catch (MnemonicException.MnemonicLengthException e) {
            throw new RuntimeException(e);
        }
    }
}
