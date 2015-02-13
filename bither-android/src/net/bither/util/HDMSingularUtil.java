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
import net.bither.bitherj.api.http.HttpException;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.hd.DeterministicKey;
import net.bither.bitherj.crypto.hd.HDKeyDerivation;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
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

        public void singularServerFinish();

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
        delegate.setSingularModeAvailable(false);
        isSingularMode = false;
        running = true;
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
        setEntropyInterval(entropy);
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
                setEntropyInterval(entropy);
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

    private void setEntropyInterval(byte[] entropy) {
        hotMnemonicSeed = Arrays.copyOf(entropy, 32);
        coldMnemonicSeed = Arrays.copyOfRange(entropy, 33, 64);
        Utils.wipeBytes(entropy);
        initHotFirst();
    }

    public void cold() {
        assert password != null;
        new Thread() {
            @Override
            public void run() {
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

    public void server() throws HttpException {
        new ThreadNeedService(null, context){
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
                    final int count = BitherSetting.HDM_ADDRESS_PER_SEED_PREPARE_COUNT - keychain
                            .uncompletedAddressCount();
                    if (count > 0) {
                        keychain.prepareAddresses(count, password, Arrays.copyOf(coldRoot,
                                coldRoot.length));
                    }
                    if(service != null){
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
                    if(service != null){
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
                        delegate.singularServerFinish();
                    }
                });
            }
        }.start();
    }

    public byte[] getColdMnemonicSeed(){
        return coldMnemonicSeed;
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
