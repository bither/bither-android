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

package net.bither.runnable;

import java.math.BigInteger;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.model.BitherAddress;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.model.UnSignTransaction;
import net.bither.util.GenericUtils;
import net.bither.util.StringUtil;
import net.bither.util.TransactionsUtil;
import net.bither.util.WalletUtils;
import android.content.Context;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.InsufficientMoneyException.CouldNotAdjustDownwards;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.core.Wallet.SendRequest;

public class CompleteTransactionRunnable extends BaseRunnable {
	private BitherAddress wallet;
	private SendRequest request;
	private String password;
	private boolean isPreSign = false;

	public CompleteTransactionRunnable(int addressPosition,
			SendRequest request, String password) throws Exception {
		if (StringUtil.isEmpty(password)) {
			BitherAddress a = WalletUtils.getWatchOnlyAddressList().get(
					addressPosition);
			wallet = a;
			isPreSign = true;
		} else {
			BitherAddress a = WalletUtils.getPrivateAddressList().get(
					addressPosition);
			if (a instanceof BitherAddressWithPrivateKey) {
				wallet = (BitherAddressWithPrivateKey) a;
			} else {
				throw new Exception("address not with private key");
			}
		}
		request.changeAddress = new Address(wallet.getNetworkParameters(),
				wallet.getAddress());
		this.password = password;
		this.request = request;
	}

	@Override
	public void run() {
		obtainMessage(HandlerMessage.MSG_PREPARE);
		try {
			if (!StringUtil.isEmpty(password)) {
				request.aesKey = wallet.getKeyCrypter().deriveKey(password);
				if (!wallet.checkAESKey(request.aesKey)) {
					obtainMessage(HandlerMessage.MSG_PASSWORD_WRONG);
					return;
				}
			}
			if (isPreSign) {
				wallet.completeTx(request);
				TransactionsUtil.addUnSignTxToCache(new UnSignTransaction(
						request.tx, wallet.getAddress()));
			} else {
				wallet.completeTx(request);
			}
			// bitcoinj forgot to calculate output value minus fee value
			if (request.emptyWallet && request.ensureMinRequiredFee) {
				request.fee = request.tx.getValueSentFromMe(wallet).subtract(
						request.tx.getOutput(0).getValue());
			}
			obtainMessage(HandlerMessage.MSG_SUCCESS, request);
		} catch (Exception e) {
			e.printStackTrace();
			String msg = getMessageFromException(e);
			obtainMessage(HandlerMessage.MSG_FAILURE, msg);
		}
	}

	public String getMessageFromException(Exception e) {
		Context context = BitherApplication.mContext;
		String msg = null;
		if (e instanceof IllegalArgumentException) {
			msg = context.getString(R.string.send_failed_dust_out_put);
		} else {
			BigInteger pendding = wallet.getBalance(BalanceType.ESTIMATED)
					.subtract(wallet.getBalance(BalanceType.AVAILABLE));
			String penddingStr = null;
			if (pendding.signum() > 0) {
				penddingStr = "\n"
						+ String.format(context
								.getString(R.string.send_failed_pendding),
								GenericUtils.formatValueWithBold(pendding));
			}
			if (e instanceof InsufficientMoneyException) {
				if (e instanceof CouldNotAdjustDownwards) {
					msg = context
							.getString(R.string.send_failed_not_enough_for_fee);
					if (!StringUtil.isEmpty(penddingStr)) {
						msg += penddingStr;
					}
				} else {
					msg = context
							.getString(R.string.send_failed_not_enough_available);
					InsufficientMoneyException ime = (InsufficientMoneyException) e;
					if (ime.missing != null && ime.missing.signum() > 0) {
						msg = String.format(context
								.getString(R.string.send_failed_missing_btc),
								GenericUtils.formatValueWithBold(ime.missing));

					}
					if (!StringUtil.isEmpty(penddingStr)) {
						msg += penddingStr;
					}
					if (ime.missing != null && ime.missing.signum() > 0
							&& pendding.subtract(ime.missing).signum() >= 0) {
						msg += "\n"
								+ context
										.getString(R.string.send_failed_waiting_for_another_confirmation);
					}
				}
			}
		}
		if (StringUtil.isEmpty(msg)) {
			msg = context.getString(R.string.send_failed);
		}
		return msg;
	}
}
