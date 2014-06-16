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

package net.bither.util;

import android.util.Log;

import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.EncryptedPrivateKey;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterScrypt;
import com.google.bitcoin.params.MainNetParams;
import com.google.protobuf.ByteString;

import net.bither.model.BitherAddressWithPrivateKey;

import org.bitcoinj.wallet.Protos.ScryptParameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class PrivateKeyUtil {
    public static String getPrivateKeyString(EncryptedPrivateKey key, KeyCrypter crypter) {
        String salt = "1";
        if (crypter instanceof KeyCrypterScrypt) {
            KeyCrypterScrypt scrypt = (KeyCrypterScrypt) crypter;
            salt = Utils.bytesToHexString(scrypt.getScryptParameters().getSalt().toByteArray());
        }
        return Utils.bytesToHexString(key.getEncryptedBytes()) + StringUtil.QR_CODE_SPLIT + Utils
                .bytesToHexString(key.getInitialisationVector()) + StringUtil.QR_CODE_SPLIT + salt;
    }

    public static String getPrivateKeyStringFromAllPrivateAddresses() {
        String content = "";
        List<BitherAddressWithPrivateKey> privates = WalletUtils.getPrivateAddressList();
        for (int i = 0;
             i < privates.size();
             i++) {
            BitherAddressWithPrivateKey wallet = privates.get(i);
            ECKey key = wallet.getKeys().get(0);
            content += getPrivateKeyString(key.getEncryptedPrivateKey(), key.getKeyCrypter());
            if (i < privates.size() - 1) {
                content += StringUtil.QR_CODE_SPLIT;
            }
        }
        return content;
    }

    public static ECKey getECKeyFromSingleString(String str, String password) {
        String[] strs = str.split(StringUtil.QR_CODE_SPLIT);
        if (strs.length != 3) {
            Log.e("Backup", "PrivateKeyFromString format error");
            return null;
        }
        EncryptedPrivateKey epk = new EncryptedPrivateKey(StringUtil.hexStringToByteArray
                (strs[1]), StringUtil.hexStringToByteArray(strs[0]));
        byte[] salt = StringUtil.hexStringToByteArray(strs[2]);
        KeyCrypterScrypt crypter = new KeyCrypterScrypt(ScryptParameters.newBuilder().setSalt
                (ByteString.copyFrom(salt)).build());
        try {
            byte[] pub = ECKey.publicKeyFromPrivate(new BigInteger(1, crypter.decrypt(epk,
                            crypter.deriveKey(password))), true
            );
            return new ECKey(epk, pub, crypter);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<ECKey> getECKeysFromString(String str, String password) {
        String[] strs = str.split(StringUtil.QR_CODE_SPLIT);
        if (strs.length % 3 != 0) {
            Log.e("Backup", "PrivateKeyFromString format error");
            return null;
        }
        ArrayList<ECKey> list = new ArrayList<ECKey>();
        for (int i = 0;
             i < strs.length;
             i += 3) {
            ECKey key = getECKeyFromSingleString(strs[i] + StringUtil.QR_CODE_SPLIT + strs[i + 1]
                    + StringUtil.QR_CODE_SPLIT + strs[i + 2], password);
            if (key == null) {
                return null;
            } else {
                list.add(key);
            }
        }
        return list;
    }

    public static ECKey getEncryptedECKey(String decrypted, String password) {
        try {
            ECKey key = new DumpedPrivateKey(MainNetParams.get(), decrypted).getKey();
            KeyCrypterScrypt crypter = new KeyCrypterScrypt();
            return key.encrypt(crypter, crypter.deriveKey(password));
        } catch (Exception e) {
            return null;
        }
    }
}
