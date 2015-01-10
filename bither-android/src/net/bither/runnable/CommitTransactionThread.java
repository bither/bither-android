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

import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.core.UnSignTransaction;
import net.bither.service.BlockchainService;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.util.ThreadUtil;
import net.bither.bitherj.utils.TransactionsUtil;

public class CommitTransactionThread extends ThreadNeedService {
    public static interface CommitTransactionListener {
        public void onCommitTransactionSuccess(Tx tx);

        public void onCommitTransactionFailed();
    }

    private int addressPosition;
    private Address wallet;
    private Tx tx;
    private CommitTransactionListener listener;

    public CommitTransactionThread(DialogProgress dp, int addressPosition, Tx tx,
                                   boolean withPrivateKey, CommitTransactionListener listener)
            throws Exception {
        super(dp, dp.getContext());
        this.addressPosition = addressPosition;
        this.listener = listener;
        if (withPrivateKey) {
            Address a = AddressManager.getInstance().getPrivKeyAddresses().get(addressPosition);
            if (a.hasPrivKey()) {
                wallet = a;
            } else {
                throw new Exception("address not with private key");
            }
        } else {
            wallet = AddressManager.getInstance().getWatchOnlyAddresses().get(addressPosition);
        }
        this.tx = tx;
    }

    @Override
    public void runWithService(BlockchainService service) {
        boolean success = false;
        try {
            PeerManager.instance().publishTransaction(tx);
            TransactionsUtil.removeSignTx(new UnSignTransaction(tx, wallet.getAddress()));
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        } finally {
            final boolean s = success;
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if(dp.isShowing()) {
                        dp.dismiss();
                    }
                    if (listener != null) {
                        if (s) {
                            listener.onCommitTransactionSuccess(tx);
                        } else {
                            listener.onCommitTransactionFailed();
                        }
                    }
                }
            });
        }
    }
}
