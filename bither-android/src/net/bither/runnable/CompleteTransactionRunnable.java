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

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.BitherjApplication;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.exception.PasswordException;
import net.bither.bitherj.exception.TxBuilderException;
import net.bither.bitherj.utils.LogUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.util.SecureCharSequence;


public class CompleteTransactionRunnable extends BaseRunnable {
    private Address wallet;

    private SecureCharSequence password;
    private long amount;
    private String toAddress;
    private boolean toSign = false;

    static {
        for (TxBuilderException.TxBuilderErrorType type : TxBuilderException.TxBuilderErrorType
                .values()) {
            int format = R.string.send_failed;
            switch (type) {
                case TxNotEnoughMoney:
                    format = R.string.send_failed_missing_btc;
                    break;
                case TxDustOut:
                    format = R.string.send_failed_dust_out_put;
                    break;
                case TxWaitConfirm:
                    format = R.string.send_failed_pendding;
                    break;
            }
            type.registerFormatString(BitherjApplication.mContext.getString(format));
        }
    }

    public CompleteTransactionRunnable(int addressPosition, long amount, String toAddress,
                                       SecureCharSequence password) throws Exception {
        this.amount = amount;
        this.toAddress = toAddress;
        this.password = password;
        if (password == null || password.length() == 0) {
            Address a = AddressManager.getInstance().getWatchOnlyAddresses().get(addressPosition);
            wallet = a;
            toSign = false;
        } else {
            Address a = AddressManager.getInstance().getPrivKeyAddresses().get(addressPosition);
            if (a.hasPrivKey()) {
                wallet = a;
            } else {
                throw new Exception("address not with private key");
            }
            toSign = true;
        }
    }

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        try {
            Tx tx = wallet.buildTx(amount, toAddress);
            if (tx == null) {
                obtainMessage(HandlerMessage.MSG_FAILURE, BitherApplication.mContext.getString(R
                        .string.send_failed));
                return;
            }
            if (toSign) {
                wallet.signTx(tx, password);
                password.wipe();
                LogUtil.i("SignTransaction", "sign transaction hash: " + Utils.hashToString(tx
                        .getTxHash()) + " , " +
                        "content: " + Utils.bytesToHexString(tx.bitcoinSerialize()));
                if (!tx.verifySignatures()) {
                    LogUtil.w("SignTransaction", "sign transaction failed");
                    obtainMessage(HandlerMessage.MSG_FAILURE, getMessageFromException(null));
                    return;
                }
                LogUtil.i("SignTransaction", "sign transaction success");
            }
            obtainMessage(HandlerMessage.MSG_SUCCESS, tx);
        } catch (Exception e) {
            e.printStackTrace();
            String msg = getMessageFromException(e);
            obtainMessage(HandlerMessage.MSG_FAILURE, msg);
        }
    }

    public String getMessageFromException(Exception e) {
        if (e != null && e instanceof TxBuilderException) {
            return e.getMessage();
        } else if (e != null && e instanceof PasswordException) {
            return BitherApplication.mContext.getString(R.string.password_wrong);
        } else {
            return BitherApplication.mContext.getString(R.string.send_failed);
        }
    }
}
