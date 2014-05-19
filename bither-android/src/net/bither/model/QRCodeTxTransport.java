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

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.util.GenericUtils;
import net.bither.util.StringUtil;

import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.params.MainNetParams;

public class QRCodeTxTransport implements Serializable {
	private static final long serialVersionUID = 5979319690741716813L;
	private List<String> mHashList;
	private String mMyAddress;
	private String mToAddress;

	private BigInteger mTo;
	private BigInteger mFee;

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

	public BigInteger getTo() {
		return mTo;
	}

	public void setTo(BigInteger mTo) {
		this.mTo = mTo;
	}

	public BigInteger getFee() {
		return mFee;
	}

	public void setFee(BigInteger mFee) {
		this.mFee = mFee;
	}

	public void signTransaction(Transaction tx) {

	}

	public static QRCodeTxTransport fromSendRequestWithUnsignedTransaction(
			SendRequest request, String fromAddress) {
		QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
		qrCodeTransport.setMyAddress(fromAddress);
		TransactionOutput out = request.tx.getOutput(0);
		String toAddress = BitherApplication.mContext
				.getString(R.string.address_cannot_be_parsed);
		try {
			toAddress = GenericUtils.addressFromScriptPubKey(
					out.getScriptPubKey(), MainNetParams.get());
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		qrCodeTransport.setToAddress(toAddress);
		qrCodeTransport.setTo(out.getValue());
		qrCodeTransport.setFee(request.fee);
		List<String> hashList = new ArrayList<String>();
		for (int i = 0; i < request.tx.getInputs().size(); i++) {
			TransactionInput tInput = request.tx.getInputs().get(i);
			boolean anyoneCanPay = false;
			Sha256Hash hash = request.tx.hashForSignature(i, tInput
					.getOutpoint().getConnectedOutput().getScriptBytes(),
					Transaction.SigHash.ALL, anyoneCanPay);
			hashList.add(hash.toString());

		}
		qrCodeTransport.setHashList(hashList);
		return qrCodeTransport;
	}

	public static QRCodeTxTransport formatQRCodeTransport(String str) {
		try {
			String[] strArray = str.split(StringUtil.QR_CODE_SPLIT);
			QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
			String address = strArray[0];
			if (!StringUtil.validBicoinAddress(address)) {
				return null;
			}
			qrCodeTransport.setMyAddress(address);

			qrCodeTransport.setFee(BigInteger.valueOf(Long.parseLong(
					strArray[1], 16)));
			qrCodeTransport.setToAddress(strArray[2]);
			qrCodeTransport.setTo(BigInteger.valueOf(Long.parseLong(
					strArray[3], 16)));
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

	public static String getPreSignString(QRCodeTxTransport qrCodeTransport) {
		String preSignString = qrCodeTransport.getMyAddress()
				+ StringUtil.QR_CODE_SPLIT
				+ Long.toHexString(qrCodeTransport.getFee().longValue())
						.toLowerCase(Locale.US)
				+ StringUtil.QR_CODE_SPLIT
				+ qrCodeTransport.getToAddress()
				+ StringUtil.QR_CODE_SPLIT
				+ Long.toHexString(qrCodeTransport.getTo().longValue())
						.toLowerCase(Locale.US) + StringUtil.QR_CODE_SPLIT;
		for (int i = 0; i < qrCodeTransport.getHashList().size(); i++) {
			String hash = qrCodeTransport.getHashList().get(i);
			if (i < qrCodeTransport.getHashList().size() - 1) {
				preSignString = preSignString + hash + StringUtil.QR_CODE_SPLIT;
			} else {
				preSignString = preSignString + hash;
			}
		}

		return preSignString;
	}
}
