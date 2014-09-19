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


import net.bither.bitherj.core.Address;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.util.QRCodeUtil;
import net.bither.util.StringUtil;


public class PasswordSeed {
    private String address;
    private String keyStr;
    private ECKey ecKey;

    public PasswordSeed(String str) {
        int indexOfSplit = QRCodeUtil.indexOfOfSplitChar(str);
        this.address = str.substring(0, indexOfSplit);
        this.keyStr = str.substring(indexOfSplit + 1);
    }

    public PasswordSeed(Address address) {
        this.address = address.getAddress();
        this.keyStr = address.getEncryptPrivKey();
    }

    public boolean checkPassword(CharSequence password) {
        this.ecKey = PrivateKeyUtil.getECKeyFromSingleString(keyStr, password);
        if (this.ecKey == null) {
            return false;
        }
        return StringUtil.compareString(address,
                this.ecKey.toAddress());

    }

    public ECKey getECKey() {
        return this.ecKey;
    }

    public String getAddress() {
        return this.address;
    }

    @Override
    public String toString() {
        return this.address + QRCodeUtil.QR_CODE_SPLIT + this.keyStr;
    }

}
