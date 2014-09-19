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

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.Utils;
import net.bither.util.OldQRCodeUtil;
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

    public static QRCodeTxTransport fromSendRequestWithUnsignedTransaction(Tx tx) {
        QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
        qrCodeTransport.setMyAddress(tx.getFromAddress());
        String toAddress = tx.getFirstOutAddress();
        if (StringUtil.isEmpty(toAddress)) {
            toAddress = BitherApplication.mContext.getString(R.string.address_cannot_be_parsed);
        }
        qrCodeTransport.setToAddress(toAddress);
        qrCodeTransport.setTo(tx.amountSentToAddress(toAddress));
        qrCodeTransport.setFee(tx.getFee());
        List<String> hashList = new ArrayList<String>();
        for (byte[] h : tx.getUnsignedInHashes()) {
            hashList.add(Utils.bytesToHexString(h));
        }
        qrCodeTransport.setHashList(hashList);
        return qrCodeTransport;
    }

    public static QRCodeTxTransport formatQRCodeTransport(String str) {
        try {
            String[] strArray = OldQRCodeUtil.splitOldString(str);
            QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
            String address = strArray[0];
            if (!StringUtil.validBicoinAddress(address)) {
                return null;
            }
            qrCodeTransport.setMyAddress(address);

            qrCodeTransport.setFee(Long.parseLong(
                    strArray[1], 16));
            qrCodeTransport.setToAddress(strArray[2]);
            qrCodeTransport.setTo(Long.parseLong(
                    strArray[3], 16));
            List<String> hashList = new ArrayList<String>();
            for (int i = 4; i < strArray.length; i++) {
                String text = strArray[i];
                if (!StringUtil.isEmpty(text)) {
                    hashList.add(text);
                }
            }
            qrCodeTransport.setHashList(hashList);

            return qrCodeTransport;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
