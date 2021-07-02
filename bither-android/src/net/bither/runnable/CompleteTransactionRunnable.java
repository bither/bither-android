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
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Coin;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.exception.PasswordException;
import net.bither.bitherj.exception.TxBuilderException;
import net.bither.bitherj.utils.Utils;

import java.util.List;


public class CompleteTransactionRunnable extends BaseRunnable {
    private Address wallet;

    private SecureCharSequence password;
    private long amount;
    private String toAddress;
    private String changeAddress;
    private boolean toSign = false;
    private HDMAddress.HDMFetchOtherSignatureDelegate sigFetcher1;
    private HDMAddress.HDMFetchOtherSignatureDelegate sigFetcher2;
    private boolean isBtc = true;
    private List<Out> outs = null;
    private Coin coin = Coin.BTC;
    private String blockHash;
    private Long dynamicFeeBase;

    static {
        registerTxBuilderExceptionMessages();
    }

    public CompleteTransactionRunnable(int addressPosition, long amount, String toAddress,
                                       String changeAddress, SecureCharSequence password, Long dynamicFeeBase) throws Exception {
        this(addressPosition, amount, toAddress, changeAddress, password, dynamicFeeBase, null);
    }

    public CompleteTransactionRunnable(Coin coin, int addressPosition, long amount, String toAddress,
                                       String changeAddress, SecureCharSequence password) throws Exception {
        this(coin, addressPosition, amount, toAddress, changeAddress, password, null, null);
    }

    public CompleteTransactionRunnable(int addressPosition, long amount, String toAddress,
                                       String changeAddress, SecureCharSequence password, boolean isBtc, List<Out> outs) throws Exception {
        this(addressPosition, amount, toAddress, changeAddress, password, null, null, isBtc, outs);
    }

    public CompleteTransactionRunnable(int addressPosition, long amount, String toAddress,
                                       String changeAddress, SecureCharSequence password, Long dynamicFeeBase,
                                       HDMAddress.HDMFetchOtherSignatureDelegate otherSigFetcher1) throws Exception {
        this(Coin.BTC, addressPosition, amount, toAddress, changeAddress, password, dynamicFeeBase, otherSigFetcher1, null);
    }

    public CompleteTransactionRunnable(Coin coin, int addressPosition, long amount, String toAddress,
                                       String changeAddress, SecureCharSequence password, Long dynamicFeeBase,
                                       HDMAddress.HDMFetchOtherSignatureDelegate
                                               otherSigFetcher1) throws Exception {
        this(coin, addressPosition, amount, toAddress, changeAddress, password, dynamicFeeBase, otherSigFetcher1, null);
    }

    public CompleteTransactionRunnable(int addressPosition, long amount, String toAddress,
                                       String changeAddress, SecureCharSequence password, Long dynamicFeeBase,
                                       HDMAddress.HDMFetchOtherSignatureDelegate otherSigFetcher1,
                                       HDMAddress.HDMFetchOtherSignatureDelegate otherSigFetcher2) throws Exception {
        this(Coin.BTC, addressPosition, amount, toAddress, changeAddress, password, dynamicFeeBase, otherSigFetcher1, otherSigFetcher2);
    }

    public CompleteTransactionRunnable(Coin coin, int addressPosition, long amount, String toAddress,
                                       String changeAddress, SecureCharSequence password,
                                       Long dynamicFeeBase, HDMAddress.HDMFetchOtherSignatureDelegate
                                               otherSigFetcher1,
                                       HDMAddress.HDMFetchOtherSignatureDelegate
                                               otherSigFetcher2) throws Exception {
        boolean isHDM = otherSigFetcher1 != null || otherSigFetcher2 != null;
        this.amount = amount;
        this.toAddress = toAddress;
        this.password = password;
        this.dynamicFeeBase = dynamicFeeBase;
        sigFetcher1 = otherSigFetcher1;
        sigFetcher2 = otherSigFetcher2;
        this.coin = coin;
        if (isHDM) {
            Address a = AddressManager.getInstance().getHdmKeychain().getAddresses().get
                    (addressPosition);
            wallet = a;
            toSign = true;
        } else if (password == null || password.length() == 0) {
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
        if (!Utils.isEmpty(changeAddress)) {
            this.changeAddress = changeAddress;
        } else {
            this.changeAddress = wallet.getAddress();
        }
    }

    public CompleteTransactionRunnable(int addressPosition, long amount, String toAddress,
                                       String changeAddress, SecureCharSequence password,
                                       HDMAddress.HDMFetchOtherSignatureDelegate
                                               otherSigFetcher1,
                                       HDMAddress.HDMFetchOtherSignatureDelegate
                                               otherSigFetcher2, boolean isBtc,List<Out> outs) throws Exception {
        boolean isHDM = otherSigFetcher1 != null || otherSigFetcher2 != null;
        this.amount = amount;
        this.toAddress = toAddress;
        this.password = password;
        sigFetcher1 = otherSigFetcher1;
        sigFetcher2 = otherSigFetcher2;
        this.outs = outs;
        this.isBtc = isBtc;
        if (isHDM) {
            Address a = AddressManager.getInstance().getHdmKeychain().getAddresses().get
                    (addressPosition);
            wallet = a;
            toSign = true;
        } else if (password == null || password.length() == 0) {
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
        if (!Utils.isEmpty(changeAddress)) {
            this.changeAddress = changeAddress;
        } else {
            this.changeAddress = wallet.getAddress();
        }
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        if (coin == Coin.BTC) {
            signTx();
        } else {
            if (outs == null) {
                signSplitCoinTxs(coin.getSplitCoin());
            } else {
                signBccTxs(outs);
            }
        }
    }

    private void signSplitCoinTxs(SplitCoin splitCoin) {
        try {
            List<Tx> txs = wallet.buildSplitCoinTx(amount, toAddress, changeAddress, splitCoin, !toSign);
            if (txs == null) {
                obtainMessage(HandlerMessage.MSG_FAILURE, BitherApplication.mContext.getString(R
                        .string.send_failed));
                return;
            }
            if (toSign) {
                for (Tx tx: txs) {
                    if(coin == Coin.BCD && blockHash != null && !blockHash.isEmpty()) {
                        tx.setBlockHash(Utils.hexStringToByteArray(blockHash));
                    }
                    wallet.signTx(tx, password, coin);
                    if (!tx.verifySignatures()) {
                        obtainMessage(HandlerMessage.MSG_FAILURE, getMessageFromException(null));
                        return;
                    }
                }
                if (password != null) {
                    password.wipe();
                }
            }else if(coin == Coin.BCD){
                for (Tx tx: txs) {
                    if (coin == Coin.BCD && blockHash != null && !blockHash.isEmpty()) {
                        tx.setBlockHash(Utils.hexStringToByteArray(blockHash));
                    }
                }
            }
            obtainMessage(HandlerMessage.MSG_SUCCESS, txs);
        } catch (Exception e) {
            if (password != null) {
                password.wipe();
            }

            if (e instanceof HDMSignUserCancelExcetion) {
                obtainMessage(HandlerMessage.MSG_FAILURE);
                return;
            }

            e.printStackTrace();
            String msg = getMessageFromException(e);
            obtainMessage(HandlerMessage.MSG_FAILURE, msg);
        }
    }

    private void signBccTxs(List<Out> outs) {
        try {
            List<Tx> txs = wallet.buildBccTx(amount, toAddress, changeAddress,outs);
            if (txs == null) {
                obtainMessage(HandlerMessage.MSG_FAILURE, BitherApplication.mContext.getString(R
                        .string.send_failed));
                return;
            }
            if (toSign) {
                for (Tx tx: txs) {
                    tx.setDetectBcc(true);
                    wallet.signTx(tx, password, isBtc,outs);
                    if (!tx.verifySignatures()) {
                        obtainMessage(HandlerMessage.MSG_FAILURE, getMessageFromException(null));
                        return;
                    }
                }
                if (password != null) {
                    password.wipe();
                }
            }
            obtainMessage(HandlerMessage.MSG_SUCCESS, txs);
        } catch (Exception e) {
            if (password != null) {
                password.wipe();
            }

            if (e instanceof HDMSignUserCancelExcetion) {
                obtainMessage(HandlerMessage.MSG_FAILURE);
                return;
            }

            e.printStackTrace();
            String msg = getMessageFromException(e);
            obtainMessage(HandlerMessage.MSG_FAILURE, msg);
        }
    }

    private void signTx() {
        try {
            Tx tx = wallet.buildTx(amount, toAddress, changeAddress, coin, dynamicFeeBase, !toSign);
            if (tx == null) {
                obtainMessage(HandlerMessage.MSG_FAILURE, BitherApplication.mContext.getString(R
                        .string.send_failed));
                return;
            }
            if (toSign) {
                if (wallet.isHDM()) {
                    if (sigFetcher1 != null && sigFetcher2 != null) {
                        ((HDMAddress) wallet).signTx(tx, password, sigFetcher1, sigFetcher2);
                    } else if (sigFetcher1 != null || sigFetcher2 != null) {
                        ((HDMAddress) wallet).signTx(tx, password,
                                sigFetcher1 != null ? sigFetcher1 : sigFetcher2);
                    } else {
                        throw new RuntimeException("need sig fetcher to sign hdm tx");
                    }
                } else {
                    wallet.signTx(tx, password, coin);
                }
                if (password != null) {
                    password.wipe();
                }
                if (!tx.verifySignatures()) {
                    obtainMessage(HandlerMessage.MSG_FAILURE, getMessageFromException(null));
                    return;
                }
            }
            obtainMessage(HandlerMessage.MSG_SUCCESS, tx);
        } catch (Exception e) {
            if (password != null) {
                password.wipe();
            }
            if (e instanceof HDMSignUserCancelExcetion) {
                obtainMessage(HandlerMessage.MSG_FAILURE);
                return;
            }
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
        } else if (e != null && e instanceof HDMServerSignException) {
            return e.getMessage();
        } else {
            return BitherApplication.mContext.getString(R.string.send_failed);
        }
    }

    public static void registerTxBuilderExceptionMessages() {
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
                case TxMaxSize:
                    format = R.string.send_failed_max_tx_size;
                    break;
            }
            type.registerFormatString(BitherApplication.mContext.getString(format));
        }
    }

    public static final class HDMServerSignException extends RuntimeException {
        public HDMServerSignException(int msg) {
            this(BitherApplication.mContext.getString(msg));
        }

        HDMServerSignException(String msg) {
            super(msg);
        }
    }

    public static final class HDMSignUserCancelExcetion extends RuntimeException {

    }
}
