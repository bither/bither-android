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
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodeEnodeUtil {

    private static final String QR_CODE_LETTER = "*";

    public static String getPublicKeyStrOfPrivateKey() {
        String content = "";
        List<Address> addresses = AddressManager.getInstance().getPrivKeyAddresses();
        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            String pubStr = "";
            if (address.isFromXRandom()) {
                pubStr = QRCodeUtil.XRANDOM_FLAG;
            }
            pubStr = pubStr + Utils.bytesToHexString(address.getPubKey());
            content += pubStr;
            if (i < addresses.size() - 1) {
                content += QRCodeUtil.QR_CODE_SPLIT;
            }
        }
        content.toUpperCase(Locale.US);
        return content;
    }

    public static List<Address> formatPublicString(String content) {
        String[] strs = QRCodeUtil.splitString(content);
        ArrayList<Address> wallets = new ArrayList<Address>();
        for (String str : strs) {
            boolean isXRandom = false;
            if (str.indexOf(QRCodeUtil.XRANDOM_FLAG) == 0) {
                isXRandom = true;
                str = str.substring(1);
            }
            byte[] pub = StringUtil.hexStringToByteArray(str);
            String addString = Utils.toAddress(Utils.sha256hash160(pub));
            Address address = new Address(addString, pub, null, false);
            address.setFromXRandom(isXRandom);
            wallets.add(address);
        }
        return wallets;

    }

    private static QRCodeTxTransport fromSendRequestWithUnsignedTransaction(Tx tx) {
        QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
        qrCodeTransport.setMyAddress(tx.getFromAddress());
        String toAddress = tx.getFirstOutAddress();
        if (Utils.isEmpty(toAddress)) {
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

    public static String getPresignTxString(Tx tx) {
        QRCodeTxTransport qrCodeTransport = fromSendRequestWithUnsignedTransaction(tx);
        String preSignString = "";
        try {
            preSignString = Base58.bas58ToHex(qrCodeTransport.getMyAddress())
                    + QRCodeUtil.QR_CODE_SPLIT
                    + Long.toHexString(qrCodeTransport.getFee())
                    .toLowerCase(Locale.US)
                    + QRCodeUtil.QR_CODE_SPLIT
                    + Base58.bas58ToHex(qrCodeTransport.getToAddress())
                    + QRCodeUtil.QR_CODE_SPLIT
                    + Long.toHexString(qrCodeTransport.getTo())
                    .toLowerCase(Locale.US) + QRCodeUtil.QR_CODE_SPLIT;
            for (int i = 0; i < qrCodeTransport.getHashList().size(); i++) {
                String hash = qrCodeTransport.getHashList().get(i);
                if (i < qrCodeTransport.getHashList().size() - 1) {
                    preSignString = preSignString + hash + QRCodeUtil.QR_CODE_SPLIT;
                } else {
                    preSignString = preSignString + hash;
                }
            }
            preSignString.toUpperCase(Locale.US);
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }

        return preSignString;
    }

    public static QRCodeTxTransport formatQRCodeTransport(String str) {
        try {
            String[] strArray = QRCodeUtil.splitString(str);
            QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
            String address = strArray[0];
            if (!StringUtil.validBicoinAddress(address)) {
                return null;
            }
            qrCodeTransport.setMyAddress(Base58.bas58ToHex(address));
            qrCodeTransport.setFee(Long.parseLong(
                    strArray[1], 16));
            qrCodeTransport.setToAddress(Base58.bas58ToHex(strArray[2]));
            qrCodeTransport.setTo(Long.parseLong(
                    strArray[3], 16));
            List<String> hashList = new ArrayList<String>();
            for (int i = 4; i < strArray.length; i++) {
                String text = strArray[i];
                if (!Utils.isEmpty(text)) {
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

    private static QRCodeTxTransport oldFromSendRequestWithUnsignedTransaction(Tx tx) {
        QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
        qrCodeTransport.setMyAddress(tx.getFromAddress());
        String toAddress = tx.getFirstOutAddress();
        if (Utils.isEmpty(toAddress)) {
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

    public static String oldGetPreSignString(Tx tx) {
        QRCodeTxTransport qrCodeTransport = oldFromSendRequestWithUnsignedTransaction(tx);
        String preSignString = qrCodeTransport.getMyAddress()
                + QRCodeUtil.OLD_QR_CODE_SPLIT
                + Long.toHexString(qrCodeTransport.getFee())
                .toLowerCase(Locale.US)
                + QRCodeUtil.OLD_QR_CODE_SPLIT
                + qrCodeTransport.getToAddress()
                + QRCodeUtil.OLD_QR_CODE_SPLIT
                + Long.toHexString(qrCodeTransport.getTo())
                .toLowerCase(Locale.US) + QRCodeUtil.OLD_QR_CODE_SPLIT;
        for (int i = 0; i < qrCodeTransport.getHashList().size(); i++) {
            String hash = qrCodeTransport.getHashList().get(i);
            if (i < qrCodeTransport.getHashList().size() - 1) {
                preSignString = preSignString + hash + QRCodeUtil.OLD_QR_CODE_SPLIT;
            } else {
                preSignString = preSignString + hash;
            }
        }

        return preSignString;
    }

    public static String oldEncodeQrCodeString(String text) {
        Pattern pattern = Pattern.compile("[A-Z]");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String letter = matcher.group(0);
            matcher.appendReplacement(sb, QR_CODE_LETTER + letter);
        }
        matcher.appendTail(sb);

        return sb.toString().toUpperCase(Locale.US);
    }

}
