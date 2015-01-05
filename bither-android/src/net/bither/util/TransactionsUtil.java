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

import android.util.Base64;

import net.bither.BitherSetting;
import net.bither.api.BitherMytransactionsApi;
import net.bither.api.GetInSignaturesApi;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.core.Block;
import net.bither.bitherj.core.BlockChain;
import net.bither.bitherj.core.In;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.exception.ScriptException;
import net.bither.bitherj.exception.VerificationException;
import net.bither.bitherj.script.Script;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Sha256Hash;
import net.bither.bitherj.utils.Utils;
import net.bither.http.HttpSetting;
import net.bither.bitherj.core.UnSignTransaction;
import net.bither.preference.AppSharedPreference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class TransactionsUtil {


    private static final String TX = "tx";
    private static final String BLOCK_COUNT = "block_count";
    private static final String TX_CNT = "tx_cnt";


    private static List<UnSignTransaction> unsignTxs = new ArrayList<UnSignTransaction>();

    private static List<Tx> getTransactionsFromBither(
            JSONObject jsonObject, int storeBlockHeight) throws JSONException {
        List<Tx> transactions = new ArrayList<Tx>();
        if (!jsonObject.isNull(TX)) {
            JSONArray txsArray = jsonObject.getJSONArray(TX);
            for (int i = 0; i < txsArray.length(); i++) {
                JSONArray txArray = txsArray.getJSONArray(i);
                if (txArray.length() < 2) {
                    continue;
                }
                int height = txArray.getInt(0);
                if (height > storeBlockHeight && storeBlockHeight > 0) {
                    continue;
                }
                String txString = txArray.getString(1);
                byte[] txBytes = Base64.decode(txString, Base64.URL_SAFE);
                // LogUtil.d("height", "h:" + height);
                Tx tx = new Tx(txBytes);

                tx.setBlockNo(height);
                transactions.add(tx);
            }
        }
        return transactions;

    }

    public static List<In> getInSignatureFromBither(String str) {
        List<In> result = new ArrayList<In>();
        if (str.length() > 0) {
            String[] txs = str.split(";");
            for (String tx : txs) {
                String[] ins = tx.split(":");
                byte[] txHash = Utils.reverseBytes(Base64.decode(ins[0], Base64.URL_SAFE));
                for (int i = 1; i < ins.length; i++) {
                    String[] array = ins[i].split(",");
                    int inSn = Integer.decode(array[0]);
                    byte[] inSignature = Base64.decode(array[1], Base64.URL_SAFE);
                    In in = new In();
                    in.setTxHash(txHash);
                    in.setInSn(inSn);
                    in.setInSignature(inSignature);
                    result.add(in);
                }
            }
        }
        return result;
    }

    public static class ComparatorTx implements Comparator<Tx> {

        @Override
        public int compare(Tx lhs, Tx rhs) {
            if (lhs.getBlockNo() != rhs.getBlockNo()) {
                return Integer.valueOf(lhs.getBlockNo()).compareTo(Integer.valueOf(rhs.getBlockNo()));
            } else {
                return Integer.valueOf(lhs.getTxTime()).compareTo(Integer.valueOf(rhs.getTxTime()));
            }

        }

    }

    // TODO display unSignTx

    public static UnSignTransaction getUnsignTxFromCache(String address) {
        synchronized (unsignTxs) {
            for (UnSignTransaction unSignTransaction : unsignTxs) {
                if (Utils.compareString(address,
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

    public static boolean signTransaction(Tx tx, String qrCodeContent)
            throws ScriptException {
        String[] stringArray = QRCodeUtil.splitString(qrCodeContent);
        List<byte[]> hashList = new ArrayList<byte[]>();
        for (String str : stringArray) {
            if (!Utils.isEmpty(str)) {
                hashList.add(Utils.hexStringToByteArray(str));
            }
        }
        tx.signWithSignatures(hashList);
        return tx.verifySignatures();
    }


    public static BitherSetting.AddressType checkAddress(List<String> addressList) throws Exception {
        for (String address : addressList) {
            BitherMytransactionsApi bitherMytransactionsApi = new BitherMytransactionsApi(address.toString());
            bitherMytransactionsApi.handleHttpGet();
            String result = bitherMytransactionsApi.getResult();
            JSONObject json = new JSONObject(result);
            if (!json.isNull(HttpSetting.SPECIAL_TYPE)) {
                int specialType = json.getInt(HttpSetting.SPECIAL_TYPE);
                if (specialType == 0) {
                    return BitherSetting.AddressType.SpecialAddress;
                } else {
                    return BitherSetting.AddressType.TxTooMuch;
                }
            }
        }
        return BitherSetting.AddressType.Normal;
    }

    public static void getMyTxFromBither() throws Exception {
        if (AppSharedPreference.getInstance().getAppMode() != BitherjSettings.AppMode.HOT) {
            return;
        }

        Block storedBlock = BlockChain.getInstance().getLastBlock();
        int storeBlockHeight = storedBlock.getBlockNo();

        for (Address address : AddressManager.getInstance().getAllAddresses()) {
            if (!address.isSyncComplete()) {
                List<Tx> transactions = new ArrayList<Tx>();
                int apiBlockCount = 0;
                int txSum = 0;
                boolean needGetTxs = true;
                int page = 1;
                while (needGetTxs) {
                    BitherMytransactionsApi bitherMytransactionsApi = new BitherMytransactionsApi(
                            address.getAddress(), page);
                    bitherMytransactionsApi.handleHttpGet();
                    String txResult = bitherMytransactionsApi.getResult();
                    JSONObject jsonObject = new JSONObject(txResult);
                    if (!jsonObject.isNull(BLOCK_COUNT)) {
                        apiBlockCount = jsonObject.getInt(BLOCK_COUNT);
                    }
                    int txCnt = jsonObject.getInt(TX_CNT);
                    List<Tx> temp = TransactionsUtil.getTransactionsFromBither(
                            jsonObject, storeBlockHeight);
                    transactions.addAll(temp);
                    txSum = txSum + transactions.size();
                    needGetTxs = txSum < txCnt;
                    page++;
                }
                if (apiBlockCount < storeBlockHeight && storeBlockHeight - apiBlockCount < 100) {
                    BlockChain.getInstance().rollbackBlock(apiBlockCount);
                }
                Collections.sort(transactions, new ComparatorTx());
                address.initTxs(transactions);
                address.setSyncComplete(true);
                address.updatePubkey();
            }
        }
    }

    public static Thread completeInputsForAddressInBackground(final Address address) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                completeInputsForAddress(address);
            }
        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
        return thread;
    }

    public static void completeInputsForAddress(Address address) {
        try {
            int fromBlock = address.needCompleteInSignature();
            while (fromBlock > 0) {
                GetInSignaturesApi api = new GetInSignaturesApi(address.getAddress(), fromBlock);
                api.handleHttpGet();
                address.completeInSignature(getInSignatureFromBither(api.getResult()));
                fromBlock = address.needCompleteInSignature();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
