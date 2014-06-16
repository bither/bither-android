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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.bither.BitherSetting;
import net.bither.api.BitherMytransactionsApi;
import net.bither.model.BitherAddress;
import net.bither.service.BlockchainService;
import net.bither.util.BroadcastUtil;
import net.bither.util.LogUtil;
import net.bither.util.TransactionsUtil;
import net.bither.util.TransactionsUtil.ComparatorTx;
import net.bither.util.WalletUtils;

import org.json.JSONObject;

import android.util.Log;

import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;

public class GetMyTxFromBitherRunnable extends BaseRunnable {

    private static final String BLOCK_COUNT = "block_count";

    private List<BitherAddress> mAddressList;

    private BlockchainService mBlockchainService;
    private int mStoreBlockHeight;

    public GetMyTxFromBitherRunnable(BlockchainService blockchainService,
                                     List<BitherAddress> list) {
        this.mBlockchainService = blockchainService;
        this.mAddressList = list;

    }

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        try {
            for (BitherAddress address : mAddressList) {
                List<Transaction> transactions = new ArrayList<Transaction>();
                StoredBlock storedBlock = this.mBlockchainService
                        .getBlockStore();
                this.mStoreBlockHeight = storedBlock.getHeight();
                int apiBlockCount = 0;

//                try {
                BitherMytransactionsApi bitherMytransactionsApi = new BitherMytransactionsApi(
                        address.getAddress());
                bitherMytransactionsApi.handleHttpGet();
                String txResult = bitherMytransactionsApi.getResult();
                JSONObject jsonObject = new JSONObject(txResult);
                if (!jsonObject.isNull(BLOCK_COUNT)) {
                    apiBlockCount = jsonObject.getInt(BLOCK_COUNT);
                }
                List<Transaction> temp = new ArrayList<Transaction>();
                LogUtil.d("wallet", "apiBlockCount:" + apiBlockCount);
                temp = TransactionsUtil.getTransactionsFromBither(
                        jsonObject, this.mStoreBlockHeight);
                transactions.addAll(temp);

//                } catch (Exception e) {
//                    if (!address.hasPrivateKey()) {
//                        throw e;
//                    } else {
//                        e.printStackTrace();
//                    }
//                }

                LogUtil.d("progress", "transactions;" + transactions.size());
                int lastSeenHeight = Math.min(apiBlockCount, mStoreBlockHeight);
                if (lastSeenHeight <= 0) {
                    lastSeenHeight = apiBlockCount;
                }
                if (lastSeenHeight <= 0) {
                    lastSeenHeight = mStoreBlockHeight;
                }

                WalletUtils.addTxToWallet(address, transactions,
                        lastSeenHeight, storedBlock.getHeader().getHash());
                if (!address.isConsistent()) {
                    Log.e("wallet error", address.getAddress()
                            + ": isConsistent error");
                } else {
                    Log.d("wallet", address.getAddress() + " :sucess");
                }

                WalletUtils.saveBitherAddress(address);
                BroadcastUtil.sendBroadcastAddressState(address);
                BroadcastUtil
                        .sendBroadcastProgressState(BitherSetting.SYNC_PROGRESS_COMPLETE);
                WalletUtils.sendTotalBroadcast();
                Collections.sort(transactions, new ComparatorTx());
            }
            obtainMessage(HandlerMessage.MSG_SUCCESS);
        } catch (Exception e) {
            obtainMessage(HandlerMessage.MSG_FAILURE);
            e.printStackTrace();
        }

    }
}
