/*
 * Copyright 2014 http://Bither.net
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

package net.bither.model;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.EncryptedPrivateKey;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterException;

import net.bither.util.PrivateKeyUtil;
import net.bither.util.StringUtil;

import org.spongycastle.crypto.params.KeyParameter;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class BitherAddressWithPrivateKey extends BitherAddress {
    private static final long serialVersionUID = 8947862254125936326L;

    public BitherAddressWithPrivateKey() {
        this(true);
    }

    public BitherAddressWithPrivateKey(boolean generatePrivateKey) {
        super();
        if (generatePrivateKey) {
            super.addKey(new ECKey());
        }
    }

    @Override
    public boolean hasPrivateKey() {
        return true;
    }

    public boolean checkPrivateKeyDecryption(CharSequence password) {
        if (password == null || password.length() == 0) {
            return false;
        }
        KeyCrypter keyCrypter = getKeyCrypter();
        ECKey key = null;
        List<ECKey> keys = getKeys();
        if (keys.size() >= 0) {
            key = keys.get(0);
        }
        if (keyCrypter == null || key == null) {
            return false;
        }
        EncryptedPrivateKey encryptedPrivateKey = key.getEncryptedPrivateKey();
        KeyParameter aesKey = keyCrypter.deriveKey(password);
        if (encryptedPrivateKey == null || aesKey == null) {
            return false;
        }
        byte[] decrypted = null;
        try {
            decrypted = keyCrypter.decrypt(encryptedPrivateKey, aesKey);
        } catch (KeyCrypterException e) {
            e.printStackTrace();
            return false;
        }
        if (decrypted == null) {
            return false;
        }
        byte[] pubKey = ECKey.publicKeyFromPrivate(new BigInteger(1, decrypted),
                key.isCompressed());
        PrivateKeyUtil.wipeDecryptedPrivateKey(decrypted);
        return Arrays.equals(key.getPubKey(), pubKey);
    }
}
