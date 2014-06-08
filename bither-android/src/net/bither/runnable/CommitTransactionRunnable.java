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

import java.util.List;

import net.bither.model.BitherAddress;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.model.UnSignTransaction;
import net.bither.util.BroadcastUtil;
import net.bither.util.TransactionsUtil;
import net.bither.util.WalletUtils;

import com.google.bitcoin.core.Transaction;

public class CommitTransactionRunnable extends BaseRunnable {
    private int addressPosition;
    private BitherAddress wallet;
    private Transaction tx;

    public CommitTransactionRunnable(int addressPosition, Transaction tx)
            throws Exception {
        this(addressPosition, tx, true);
    }

    public CommitTransactionRunnable(int addressPosition, Transaction tx,
                                     boolean withPrivateKey) throws Exception {
        this.addressPosition = addressPosition;
        if (withPrivateKey) {
            BitherAddress a = WalletUtils.getPrivateAddressList().get(
                    addressPosition);
            if (a instanceof BitherAddressWithPrivateKey) {
                wallet = (BitherAddressWithPrivateKey) a;
            } else {
                throw new Exception("address not with private key");
            }
        } else {
            wallet = WalletUtils.getWatchOnlyAddressList().get(addressPosition);
        }
        this.tx = tx;
    }

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        try {
            wallet.commitTx(tx);
            WalletUtils.notifyAddressInfo(wallet);
            List<BitherAddress> watchonlys = WalletUtils
                    .getWatchOnlyAddressList();
            for (BitherAddress a : watchonlys) {
                if (a != wallet) {
                    if (a.isPendingTransactionRelevant(tx)) {
                        if (a.getTransaction(tx.getHash()) == null) {
                            a.receivePending(tx, null);
                            WalletUtils.notifyAddressInfo(a);
                        }
                    }
                }
            }

            List<BitherAddressWithPrivateKey> privates = WalletUtils
                    .getPrivateAddressList();
            for (BitherAddressWithPrivateKey a : privates) {
                if (a != wallet) {
                    if (a.isPendingTransactionRelevant(tx)) {
                        if (a.getTransaction(tx.getHash()) == null) {
                            a.receivePending(tx, null);
                            WalletUtils.notifyAddressInfo(a);
                        }
                    }
                }
            }

            BroadcastUtil.sendBroadcastTx(tx, addressPosition,
                    wallet.hasPrivateKey());
            WalletUtils.sendTotalBroadcast();
            TransactionsUtil.removeSignTx(new UnSignTransaction(tx, wallet
                    .getAddress()));
            obtainMessage(HandlerMessage.MSG_SUCCESS, tx);
        } catch (Exception e) {
            e.printStackTrace();
            obtainMessage(HandlerMessage.MSG_FAILURE);
        }
    }
}
