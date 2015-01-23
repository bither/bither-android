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

package net.bither.factory;

import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.BitherjSettings.AddressType;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.crypto.DumpedPrivateKey;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.TransactionsUtil;
import net.bither.preference.AppSharedPreference;

import java.util.ArrayList;
import java.util.List;

public abstract class ImportPrivateKey {


    public enum ImportPrivateKeyType {
        Text, BitherQrcode, Bip38, HDMColdSeedQRCode, HDMColdPhrase
    }

    public static final int IMPORT_FAILED = 0;
    public static final int PASSWORD_WRONG = 1;
    public static final int NETWORK_FAILED = 2;
    public static final int CAN_NOT_IMPORT_BITHER_COLD_PRIVATE_KEY = 3;
    public static final int PRIVATE_KEY_ALREADY_EXISTS = 4;
    public static final int PASSWORD_IS_DIFFEREND_LOCAL = 5;
    public static final int CONTAIN_SPECIAL_ADDRESS = 6;
    public static final int TX_TOO_MUCH = 7;

    private String content;
    private SecureCharSequence password;

    private ImportPrivateKeyType importPrivateKeyType;


    public ImportPrivateKey(ImportPrivateKeyType importPrivateKeyType
            , String content, SecureCharSequence password) {
        this.content = content;
        this.password = password;
        this.importPrivateKeyType = importPrivateKeyType;
    }


    public abstract void importError(int errorCode);

    public Address initPrivateKey() {
        ECKey ecKey = getEckey();
        try {
            if (ecKey == null) {
                if (importPrivateKeyType == ImportPrivateKeyType.BitherQrcode) {
                    importError(PASSWORD_WRONG);
                } else {
                    importError(IMPORT_FAILED);
                }
                return null;
            } else {
                List<String> addressList = new ArrayList<String>();
                addressList.add(ecKey.toAddress());
                if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT) {
                    return checkAddress(ecKey, addressList);
                } else {
                    return addECKey(ecKey);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            importError(IMPORT_FAILED);
            return null;
        } finally {
            password.wipe();
            if (ecKey != null) {
                ecKey.clearPrivateKey();
            }
        }

    }

    private Address checkAddress(ECKey ecKey, List<String> addressList) {
        try {
            AddressType addressType = TransactionsUtil.checkAddress(addressList);
            return handlerResult(ecKey, addressType);
        } catch (Exception e) {
            importError(NETWORK_FAILED);
            return null;
        }

    }

    private Address addECKey(ECKey ecKey) {
        String encryptedPrivateString;
        if (importPrivateKeyType == ImportPrivateKeyType.BitherQrcode) {
            encryptedPrivateString = QRCodeUtil.getNewVersionEncryptPrivKey(content);
        } else {
            ecKey = PrivateKeyUtil.encrypt(ecKey, password);
            encryptedPrivateString = PrivateKeyUtil.getEncryptedString(ecKey);
        }
        Address address = new Address(ecKey.toAddress(), ecKey.getPubKey(), encryptedPrivateString
                , ecKey.isFromXRandom());
        if (AddressManager.getInstance().getWatchOnlyAddresses().contains(address)) {
            password.wipe();
            importError(CAN_NOT_IMPORT_BITHER_COLD_PRIVATE_KEY);
            return null;
        } else if (AddressManager.getInstance().getPrivKeyAddresses().contains(address)) {
            password.wipe();
            importError(PRIVATE_KEY_ALREADY_EXISTS);
            return null;

        } else {
            if (importPrivateKeyType == ImportPrivateKeyType.BitherQrcode) {
                PasswordSeed passwordSeed = AppSharedPreference.getInstance().getPasswordSeed();
                if (passwordSeed != null && !passwordSeed.checkPassword(password)) {
                    password.wipe();
                    importError(PASSWORD_IS_DIFFEREND_LOCAL);
                    return null;
                }
            } else {
                password.wipe();
            }
            return address;


        }

    }

    private Address handlerResult(ECKey ecKey
            , AddressType addressType) {
        Address address = null;
        switch (addressType) {
            case Normal:
                address = addECKey(ecKey);
                break;
            case SpecialAddress:
                importError(CONTAIN_SPECIAL_ADDRESS);
                break;
            case TxTooMuch:
                importError(TX_TOO_MUCH);
                break;
        }
        return address;
    }


    private ECKey getEckey() {
        ECKey ecKey = null;
        DumpedPrivateKey dumpedPrivateKey = null;
        try {
            switch (this.importPrivateKeyType) {
                case Text:
                    dumpedPrivateKey = new DumpedPrivateKey(this.content);
                    ecKey = dumpedPrivateKey.getKey();
                    break;
                case BitherQrcode:
                    ecKey = PrivateKeyUtil.getECKeyFromSingleString(content, password);
                    break;
                case Bip38:
                    dumpedPrivateKey = new DumpedPrivateKey(this.content);
                    ecKey = dumpedPrivateKey.getKey();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dumpedPrivateKey != null) {
                dumpedPrivateKey.clearPrivateKey();
            }
        }
        return ecKey;
    }

}
