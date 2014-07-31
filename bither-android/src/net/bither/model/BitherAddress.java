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

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.VerificationException;
import com.google.bitcoin.core.Wallet;
import com.google.common.collect.Lists;

import net.bither.BitherSetting;
import net.bither.service.BlockchainService;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BitherAddress extends Wallet {

    private static final long serialVersionUID = 2L;
    private AddressInfo addressInfo;
    private ECKey key;
    private String mAddress;
    // TODO: remove support for error address
    private boolean isError;

    public BitherAddress() {
        super(BitherSetting.NETWORK_PARAMETERS);
    }

    public BitherAddress(byte[] pubKey) {
        super(BitherSetting.NETWORK_PARAMETERS);
        addKey(new ECKey(null, pubKey, true));
    }

    private BitherAddress(NetworkParameters params) {
        super(params);
    }

    public String getAddress() {
        return mAddress;
    }

    public String getShortAddress() {
        return StringUtil.shortenAddress(getAddress());
    }

    @Override
    public int addKeys(List<ECKey> keys) {
        if (keys.size() > 0 && getKeys().isEmpty()) {
            this.key = keys.get(0);
            mAddress = new Address(getNetworkParameters(), key.getPubKeyHash())
                    .toString();
            super.addKeys(Lists.newArrayList(key));
            return 1;
        }
        return 0;
    }

    @Override
    public boolean removeKey(ECKey key) {
        return false;
    }

    public boolean replaceEckey(ECKey oldKey, ECKey newKey) {
        if (newKey == null) {
            return false;
        }
        boolean isRemoved = super.removeKey(oldKey);
        if (oldKey == null || isRemoved) {
            return addKey(newKey);
        } else {
            return false;
        }

    }

    public boolean isReadyToShow() {

        return getLastBlockSeenHeight() > 0;
    }

    public AddressInfo getAddressInfo() {
        return addressInfo;
    }

    public void setAddressInfo(AddressInfo addressInfo) {
        this.addressInfo = addressInfo;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean isError) {
        this.isError = isError;
    }

    public boolean hasPrivateKey() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BitherAddress) {
            BitherAddress bit = (BitherAddress) o;
            return StringUtil.compareString(getAddress(), bit.getAddress());
        }
        return false;
    }

    @Override
    public void receiveFromBlock(Transaction tx, StoredBlock block,
                                 BlockChain.NewBlockType blockType, int relativityOffset)
            throws VerificationException {
        if (block.getHeight() > getLastBlockSeenHeight()) {
            super.receiveFromBlock(tx, block, blockType, relativityOffset);
        }
    }

    @Override
    public void notifyNewBestBlock(StoredBlock block)
            throws VerificationException {
        if (block.getHeight() > getLastBlockSeenHeight()) {
            super.notifyNewBestBlock(block);
        }
    }

    @Override
    public boolean isConsistent() {
        try {
            boolean isConistent = super.isConsistent()
                    && isConsistentOfUnspent();
            if (!isConistent) {
                WalletUtils.fixWalletTransactions(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            WalletUtils.fixWalletTransactions(this);
        }
        return true;
    }

    public Object disableAutoSave() {
        try {
            Field fileManagerField = Wallet.class
                    .getDeclaredField("vFileManager");
            fileManagerField.setAccessible(true);
            Object files = fileManagerField.get(this);
            fileManagerField.set(this, null);
            return files;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void enableAutoSave(Object files) {
        try {
            Field fileManagerField = Wallet.class
                    .getDeclaredField("vFileManager");
            fileManagerField.setAccessible(true);
            fileManagerField.set(this, files);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearTransactionsWithoutSave() {
        try {
            Object files = disableAutoSave();
            clearTransactions(0);
            enableAutoSave(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isConsistentOfUnspent() {
        try {
            Field unspentField = Wallet.class.getDeclaredField("unspent");
            unspentField.setAccessible(true);
            Map<Sha256Hash, Transaction> unspent = (HashMap<Sha256Hash, Transaction>) unspentField
                    .get(this);
            HashMap<String, List<TransactionOutPoint>> txHashOutPoints = getTranscationPutPoint();
            Iterator iter = unspent.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Sha256Hash, Transaction> entry = (Map.Entry<Sha256Hash, Transaction>) iter
                        .next();
                Transaction entryTx = entry.getValue();
                for (int i = 0;
                     i < entryTx.getOutputs().size();
                     i++) {
                    TransactionOutput entryTxOutput = entryTx.getOutputs().get(
                            i);
                    if (entryTxOutput.isMine(this)
                            && (entryTxOutput.getValue().compareTo(
                            BigInteger.ZERO) > 0)) {
                        String hashString = entryTx.getHashAsString();
                        if (txHashOutPoints.containsKey(hashString)) {
                            List<TransactionOutPoint> transactionOutPoints = txHashOutPoints
                                    .get(hashString);
                            for (TransactionOutPoint tPoint : transactionOutPoints) {
                                if (StringUtil.compareString(hashString, tPoint
                                        .getHash().toString())
                                        && i == tPoint.getIndex()) {
                                    return false;
                                }
                            }
                        }

                    }
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return true;

    }

    private HashMap<String, List<TransactionOutPoint>> getTranscationPutPoint() {
        List<Transaction> txList = getTransactionsByTime();
        HashMap<String, List<TransactionOutPoint>> result = new HashMap<String,
                List<TransactionOutPoint>>();
        for (Transaction tx : txList) {
            for (TransactionInput transactionInput : tx.getInputs()) {
                String inputHash = transactionInput.getOutpoint().getHash()
                        .toString();
                if (result.containsKey(inputHash)) {
                    result.get(inputHash).add(transactionInput.getOutpoint());
                } else {
                    List<TransactionOutPoint> transactionOutPoints = new
                            ArrayList<TransactionOutPoint>();
                    transactionOutPoints.add(transactionInput.getOutpoint());
                    result.put(inputHash, transactionOutPoints);
                }

            }
        }
        return result;

    }

    public void reset(BlockchainService service) {
        if (service != null) {
            service.stopPeerGroup();
        }
        setLastBlockSeenHash(Sha256Hash.ZERO_HASH);
        setLastBlockSeenHeight(0);
        clearTransactions(0);
    }
    // try to save sync
    // @Override
    // public WalletFiles autosaveToFile(File f, long delayTime,
    // TimeUnit timeUnit, @Nullable Listener eventListener) {
    // WalletFiles files = super.autosaveToFile(f, delayTime, timeUnit,
    // eventListener);
    // FakeScheduledThreadPoolExecutor fakeExecutor = new
    // FakeScheduledThreadPoolExecutor(
    // getAddress());
    // try {
    // Field executorField = WalletFiles.class
    // .getDeclaredField("executor");
    // executorField.setAccessible(true);
    // executorField.set(files, fakeExecutor);
    // } catch (Exception e) {
    // log.warn("fake save later failed for address: " + getAddress(), e);
    // }
    // return files;
    // }
}
