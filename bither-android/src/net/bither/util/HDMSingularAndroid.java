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
import net.bither.bitherj.delegate.AbstratHDMHotAdd;
import net.bither.bitherj.delegate.HDMSingular;
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
public class HDMSingularAndroid extends HDMSingular {


    private Context context;


    public HDMSingularAndroid(@Nonnull Context context, @Nonnull HDMSingularUtilDelegate delegate) {
        super(delegate);
        this.context = context;

    }

    @Override
    protected void runOnUIThread(Runnable runnable) {
        ThreadUtil.runOnMainThread(runnable);
    }

    public void server() {
        new ThreadNeedService(null, context) {
            @Override
            public void runWithService(final BlockchainService service) {
                callInServer(new AbstratHDMHotAdd.IGenerateHDMKeyChain() {
                    @Override
                    public void generateHDMKeyChain(HDMKeychain hdmKeychain) {
                        KeyUtil.setHDKeyChain(hdmKeychain);

                    }

                    @Override
                    public void beginCompleteAddress() {
                        if (service != null) {
                            service.stopAndUnregister();
                        }
                    }

                    @Override
                    public void completeAddrees(List<HDMAddress> hdmAddresses) {
                        if (service != null) {
                            service.startAndRegister();
                        }
                    }

                   
                });
            }
        }.start();
    }


}
