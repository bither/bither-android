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

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import net.bither.BitherSetting;
import net.bither.model.UnSignTransaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.VerificationException;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.script.Script;

public class TransactionsUtil {

	private static final String EXPLORER_VERSION = "ver";
	private static final String EXPLORER_IN = "in";
	private static final String EXPLORER_OUT = "out";

	private static final String EXPLORER_OUT_ADDRESS = "address";
	private static final String EXPLORER_COINBASE = "coinbase";
	private static final String EXPLORER_SEQUENCE = "sequence";
	private static final String EXPLORER_TIME = "time";

	private static final String TXS = "txs";
	private static final String BITHER_BLOCK_HASH = "block_hash";
	private static final String TX_HASH = "tx_hash";
	private static final String BITHER_BLOCK_NO = "block_no";
	private static final String BITHER_VALUE = "val";
	private static final String PREV_TX_HASH = "prev";
	private static final String PREV_OUTPUT_SN = "n";
	private static final String SCRIPT_PUB_KEY = "script";

	private static final byte[] EMPTY_BYTES = new byte[32];

	private static List<UnSignTransaction> unsignTxs = new ArrayList<UnSignTransaction>();

	public static enum TransactionFeeMode {
		Normal(10000), Low(1000);

		private int satoshi;

		TransactionFeeMode(int satoshi) {
			this.satoshi = satoshi;
		}

		public int getMinFeeSatoshi() {
			return satoshi;
		}
	}

	public static List<Transaction> getTransactionsFromBither(
			JSONObject jsonObject, int storeBlockHeight) throws JSONException,
			WrongNetworkException, AddressFormatException,
			VerificationException, ParseException, NoSuchFieldException,
			IllegalAccessException, IllegalArgumentException {
		List<Transaction> transactions = new ArrayList<Transaction>();

		if (!jsonObject.isNull(TXS)) {
			JSONArray txArray = jsonObject.getJSONArray(TXS);
			double count = 0;
			double size = txArray.length();

			for (int j = 0; j < txArray.length(); j++) {
				JSONObject tranJsonObject = txArray.getJSONObject(j);
				String blockHash = tranJsonObject.getString(BITHER_BLOCK_HASH);
				String txHash = tranJsonObject.getString(TX_HASH);
				int height = tranJsonObject.getInt(BITHER_BLOCK_NO);
				if (height > storeBlockHeight && storeBlockHeight > 0) {
					continue;
				}
				int version = 1;
				Date updateTime = new Date();
				if (!tranJsonObject.isNull(EXPLORER_TIME)) {
					updateTime = DateTimeUtil
							.getDateTimeForTimeZone(tranJsonObject
									.getString(EXPLORER_TIME));
				}
				if (!tranJsonObject.isNull(EXPLORER_VERSION)) {
					version = tranJsonObject.getInt(EXPLORER_VERSION);

				}
				Transaction transaction = new Transaction(
						BitherSetting.NETWORK_PARAMETERS, version,
						new Sha256Hash(txHash));
				transaction.addBlockAppearance(new Sha256Hash(blockHash),
						height);
				if (!tranJsonObject.isNull(EXPLORER_OUT)) {
					JSONArray tranOutArray = tranJsonObject
							.getJSONArray(EXPLORER_OUT);
					for (int i = 0; i < tranOutArray.length(); i++) {
						JSONObject tranOutJson = tranOutArray.getJSONObject(i);
						BigInteger value = BigInteger.valueOf(tranOutJson
								.getLong(BITHER_VALUE));
						if (!tranOutJson.isNull(EXPLORER_OUT_ADDRESS)) {
							Address address = new Address(
									BitherSetting.NETWORK_PARAMETERS,
									tranOutJson.getString(EXPLORER_OUT_ADDRESS));
							String str = tranOutJson.getString(SCRIPT_PUB_KEY);
							// Script script = new Script(
							// );
							// byte[] bytes1 = ScriptBuilder.createOutputScript(
							// address).getProgram();
							// byte[] bytes2 = StringUtil
							// .hexStringToByteArray(str);
							// LogUtil.d("tx", Arrays.equals(bytes1, bytes2) +
							// ";");
							TransactionOutput transactionOutput = new TransactionOutput(
									BitherSetting.NETWORK_PARAMETERS,
									transaction, value,
									StringUtil.hexStringToByteArray(str));
							transaction.addOutput(transactionOutput);
						}

					}

				}

				if (!tranJsonObject.isNull(EXPLORER_IN)) {
					JSONArray tranInArray = tranJsonObject
							.getJSONArray(EXPLORER_IN);
					for (int i = 0; i < tranInArray.length(); i++) {
						JSONObject tranInJson = tranInArray.getJSONObject(i);
						TransactionOutPoint transactionOutPoint = null;
						if (!tranInJson.isNull(EXPLORER_COINBASE)) {
							long index = 0;
							if (!tranInJson.isNull(EXPLORER_SEQUENCE)) {
								index = tranInJson.getLong(EXPLORER_SEQUENCE);
							}
							transactionOutPoint = new TransactionOutPoint(
									BitherSetting.NETWORK_PARAMETERS, index,
									Sha256Hash.ZERO_HASH);

						} else {

							String prevOutHash = tranInJson
									.getString(PREV_TX_HASH);
							long n = 0;
							if (!tranInJson.isNull(PREV_OUTPUT_SN)) {
								n = tranInJson.getLong(PREV_OUTPUT_SN);
							}
							transactionOutPoint = new TransactionOutPoint(
									BitherSetting.NETWORK_PARAMETERS, n,
									new Sha256Hash(prevOutHash));

						}
						// Log.d("transaction", transaction.toString());
						if (transactionOutPoint != null) {
							TransactionInput transactionInput = new TransactionInput(
									BitherSetting.NETWORK_PARAMETERS,
									transaction, Script.createInputScript(
											EMPTY_BYTES, EMPTY_BYTES),
									transactionOutPoint);

							transaction.addInput(transactionInput);
						}

					}
				}
				transaction.getConfidence().setAppearedAtChainHeight(height);
				transaction.getConfidence().setConfidenceType(
						ConfidenceType.BUILDING);
				transaction.getConfidence().setDepthInBlocks(
						storeBlockHeight - height + 1);
				transaction.setUpdateTime(updateTime);
				// Log.d("transaction", "transaction.num:" + transaction);
				Field txField = Transaction.class.getDeclaredField("hash");
				txField.setAccessible(true);
				txField.set(transaction, new Sha256Hash(txHash));
				transactions.add(transaction);
				count++;
				double progress = BitherSetting.SYNC_TX_PROGRESS_BLOCK_HEIGHT
						+ BitherSetting.SYNC_TX_PROGRESS_STEP1
						+ BitherSetting.SYNC_TX_PROGRESS_STEP2 * (count / size);
				BroadcastUtil.sendBroadcastProgressState(progress);

			}

		}

		LogUtil.d("transaction", "transactions.num:" + transactions.size());
		return transactions;

	}

	public static class ComparatorTx implements Comparator<Transaction> {

		@Override
		public int compare(Transaction lhs, Transaction rhs) {
			return lhs.getUpdateTime().compareTo(rhs.getUpdateTime());
		}

	}

	// TODO display unSignTx
	public static UnSignTransaction getUnsignTxFromCache(String address) {
		synchronized (unsignTxs) {
			for (UnSignTransaction unSignTransaction : unsignTxs) {
				if (StringUtil.compareString(address,
						unSignTransaction.getAddress())) {
					return unSignTransaction;
				}
			}
			return null;
		}

	}

	public static void removeSignTx(UnSignTransaction unSignTransaction) {
		synchronized (unsignTxs) {
			if (unsignTxs.contains(unSignTransaction)) {
				unsignTxs.remove(unSignTransaction);
			}
		}
	}

	public static void addUnSignTxToCache(UnSignTransaction unSignTransaction) {
		synchronized (unsignTxs) {
			if (unsignTxs.contains(unSignTransaction)) {
				unsignTxs.remove(unSignTransaction);
			}
			unsignTxs.add(unSignTransaction);
		}
	}

	public static void signTransaction(Transaction tx, String qrCodeContent)
			throws ScriptException {
		String[] stringArray = qrCodeContent.split(StringUtil.QR_CODE_SPLIT);
		List<String> hashList = new ArrayList<String>();
		for (String str : stringArray) {
			if (!StringUtil.isEmpty(str)) {
				hashList.add(str);
				LogUtil.d("sign", str);
			}

		}
		for (int i = 0; i < tx.getInputs().size(); i++) {
			TransactionInput input = tx.getInputs().get(i);
			String str = hashList.get(i);
			input.setScriptSig(new Script(StringUtil.hexStringToByteArray(str)));
			input.getScriptSig().correctlySpends(tx, i,
					input.getOutpoint().getConnectedOutput().getScriptPubKey(),
					true);

		}
	}

	public static void configureMinFee(long satoshi) {
		try {
			Field field = Transaction.class
					.getField("REFERENCE_DEFAULT_MIN_TX_FEE");
			field.setAccessible(true);
			field.set(null, BigInteger.valueOf(satoshi));
			SendRequest.DEFAULT_FEE_PER_KB = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
