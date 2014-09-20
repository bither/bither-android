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

package net.bither.qrcode;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.util.StringUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QRCodeTxTransport implements Serializable {
    private static final long serialVersionUID = 5979319690741716813L;
    private List<String> mHashList;
    private String mMyAddress;
    private String mToAddress;
    private long mTo;
    private long mFee;

    public List<String> getHashList() {
        return mHashList;
    }

    public void setHashList(List<String> mHashList) {
        this.mHashList = mHashList;
    }

    public String getMyAddress() {
        return mMyAddress;
    }

    public void setMyAddress(String mMyAddress) {
        this.mMyAddress = mMyAddress;
    }

    public String getToAddress() {
        return mToAddress;
    }

    public void setToAddress(String mOtherAddress) {
        this.mToAddress = mOtherAddress;
    }

    public long getTo() {
        return mTo;
    }

    public void setTo(long mTo) {
        this.mTo = mTo;
    }

    public long getFee() {
        return mFee;
    }

    public void setFee(long mFee) {
        this.mFee = mFee;
    }





}
