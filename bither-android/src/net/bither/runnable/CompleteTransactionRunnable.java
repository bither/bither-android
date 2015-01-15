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
import net.bither.bitherj.api.SignatureHDMApi;
import net.bither.bitherj.api.http.Http400Exception;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.TransactionSignature;
import net.bither.bitherj.exception.PasswordException;
import net.bither.bitherj.exception.TxBuilderException;
import net.bither.bitherj.utils.Utils;
import net.bither.util.LogUtil;

import java.util.ArrayList;
import java.util.List;


public class CompleteTransactionRunnable extends BaseRunnable {
    private Address wallet;

    private SecureCharSequence password;
    private long amount;
    private String toAddress;
    private String changeAddress;
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
                case TxMaxSize:
                    format = R.string.send_failed_max_tx_size;
                    break;
            }
            type.registerFormatString(BitherApplication.mContext.getString(format));
        }
    }

    public CompleteTransactionRunnable(int addressPosition, long amount, String toAddress, boolean isHDM, SecureCharSequence password) throws Exception {
        this(addressPosition, amount, toAddress, toAddress, isHDM, password);
    }

    public CompleteTransactionRunnable(int addressPosition, long amount, String toAddress,
                                       String changeAddress, boolean isHDM,
                                       SecureCharSequence password) throws Exception {
        this.amount = amount;
        this.toAddress = toAddress;
        this.password = password;
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

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        try {
            Tx tx = wallet.buildTx(amount, toAddress, changeAddress);
            if (tx == null) {
                obtainMessage(HandlerMessage.MSG_FAILURE, BitherApplication.mContext.getString(R
                        .string.send_failed));
                return;
            }
            if (toSign) {
                if (wallet.isHDM()) {
                    ((HDMAddress) wallet).signTx(tx, password,
                            new HDMAddress.HDMFetchOtherSignatureDelegate() {

                                @Override
                                public List<TransactionSignature> getOtherSignature(int addressIndex, CharSequence password, List<byte[]> unsignHash, Tx tx) {
                                    List<TransactionSignature> transactionSignatureList = new ArrayList<TransactionSignature>();
                                    try {

                                        HDMBId hdmbId = HDMBId.getHDMBidFromDb();
                                        byte[] decryptedPassword = hdmbId.decryptHDMBIdPassword(password);
                                        SignatureHDMApi signatureHDMApi = new SignatureHDMApi(HDMBId.getHDMBidFromDb().getAddress(), addressIndex, decryptedPassword, unsignHash);
                                        signatureHDMApi.handleHttpPost();
                                        List<byte[]> bytesList = signatureHDMApi.getResult();
                                        for (byte[] bytes : bytesList) {
                                            TransactionSignature transactionSignature = new TransactionSignature(ECKey.ECDSASignature.decodeFromDER(bytes), TransactionSignature.SigHash.ALL, false);
                                            transactionSignatureList.add(transactionSignature);
                                        }
                                    } catch (Exception e) {
                                        if (e instanceof Http400Exception) {
                                            throw new HDMServerSignException(R.string
                                                    .hdm_address_sign_tx_server_error);
                                        } else {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                    return transactionSignatureList;
                                }
                            });
                } else {
                    wallet.signTx(tx, password);
                }
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
        } else if (e != null && e instanceof HDMServerSignException) {
            return e.getMessage();
        } else {
            return BitherApplication.mContext.getString(R.string.send_failed);
        }
    }

    private static final class HDMServerSignException extends RuntimeException {
        HDMServerSignException(int msg) {
            this(BitherApplication.mContext.getString(msg));
        }

        HDMServerSignException(String msg) {
            super(msg);
        }
    }
}
