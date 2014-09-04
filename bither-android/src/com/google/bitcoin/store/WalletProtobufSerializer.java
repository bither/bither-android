/**
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.bitcoin.store;

import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.EncryptedPrivateKey;
import net.bither.bitherj.crypto.KeyCrypter;
import net.bither.bitherj.crypto.KeyCrypterScrypt;

import org.bitcoinj.wallet.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/*
Use only the upgrade bitcoinj->bitherj (bither less than v0.0.8)
 */

public class WalletProtobufSerializer {
    private static final Logger log = LoggerFactory.getLogger(WalletProtobufSerializer.class);


    public ECKey readWallet(InputStream input) throws Exception {

        Protos.Wallet walletProto = parseToProto(input);
        return readWallet(walletProto);


    }


    public ECKey readWallet(Protos.Wallet walletProto) throws Exception {
        // Read the scrypt parameters that specify how encryption and decryption is performed.
        ECKey ecKey = null;
        KeyCrypterScrypt keyCrypterScrypt = null;
        if (walletProto.hasEncryptionParameters()) {
            Protos.ScryptParameters encryptionParameters = walletProto.getEncryptionParameters();
            keyCrypterScrypt = new KeyCrypterScrypt(encryptionParameters.getSalt().toByteArray());
        }


        // Read all keys
        for (Protos.Key keyProto : walletProto.getKeyList()) {
            if (!(keyProto.getType() == Protos.Key.Type.ORIGINAL || keyProto.getType() == Protos.Key.Type.ENCRYPTED_SCRYPT_AES)) {
                throw new Exception("Unknown key type in wallet, type = " + keyProto.getType());
            }

            byte[] privKey = keyProto.hasPrivateKey() ? keyProto.getPrivateKey().toByteArray() : null;
            EncryptedPrivateKey encryptedPrivateKey = null;
            if (keyProto.hasEncryptedPrivateKey()) {
                Protos.EncryptedPrivateKey encryptedPrivateKeyProto = keyProto.getEncryptedPrivateKey();
                encryptedPrivateKey = new EncryptedPrivateKey(encryptedPrivateKeyProto.getInitialisationVector().toByteArray(),
                        encryptedPrivateKeyProto.getEncryptedPrivateKey().toByteArray());
            }
            byte[] pubKey = keyProto.hasPublicKey() ? keyProto.getPublicKey().toByteArray() : null;
            final KeyCrypter keyCrypter = keyCrypterScrypt;
            if (keyCrypter != null) {
                // If the key is encrypted construct an ECKey using the encrypted private key bytes.
                ecKey = new ECKey(encryptedPrivateKey, pubKey, keyCrypter);
            } else {
                // Construct an unencrypted private key.
                ecKey = new ECKey(privKey, pubKey);
            }
            //ecKey.setCreationTimeSeconds((keyProto.getCreationTimestamp() + 500) / 1000);

        }
        return ecKey;


    }

    private static Protos.Wallet parseToProto(InputStream input) throws IOException {
        return Protos.Wallet.parseFrom(input);
    }


}

