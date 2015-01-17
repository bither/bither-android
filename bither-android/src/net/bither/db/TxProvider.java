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

package net.bither.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.In;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.ITxProvider;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Sha256Hash;
import net.bither.bitherj.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TxProvider implements ITxProvider {

    private static TxProvider txProvider = new TxProvider(BitherApplication.mDbHelper);

    public static TxProvider getInstance() {
        return txProvider;
    }

    private SQLiteOpenHelper mDb;

    public TxProvider(SQLiteOpenHelper db) {
        this.mDb = db;
    }

    public List<Tx> getTxAndDetailByAddress(String address) {
        List<Tx> txItemList = new ArrayList<Tx>();
        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select b.* from addresses_txs a, txs b where a.tx_hash=b.tx_hash and a.address=? order by b.block_no desc";
            Cursor c = db.rawQuery(sql, new String[]{address});
            while (c.moveToNext()) {
                Tx txItem = applyCursor(c);
                txItem.setIns(new ArrayList<In>());
                txItem.setOuts(new ArrayList<Out>());
                txItemList.add(txItem);
                txDict.put(new Sha256Hash(txItem.getTxHash()), txItem);
            }
            c.close();
            addInForTxDetail(db, address, txDict);
            addOutForTxDetail(db, address, txDict);

        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return txItemList;
    }

    private void addInForTxDetail(SQLiteDatabase db, String address, HashMap<Sha256Hash, Tx> txDict) throws AddressFormatException {
        String sql = "select b.* from addresses_txs a, ins b where a.tx_hash=b.tx_hash and a.address=? "
                + "order by b.tx_hash ,b.in_sn";
        Cursor c = db.rawQuery(sql, new String[]{address});
        while (c.moveToNext()) {
            In inItem = applyCursorIn(c);
            Tx tx = txDict.get(new Sha256Hash(inItem.getTxHash()));
            if (tx != null) {
                tx.getIns().add(inItem);
            }
        }
        c.close();
    }

    private void addOutForTxDetail(SQLiteDatabase db, String address, HashMap<Sha256Hash, Tx> txDict) throws AddressFormatException {
        String sql = "select b.* from addresses_txs a, outs b where a.tx_hash=b.tx_hash and a.address=? "
                + "order by b.tx_hash,b.out_sn";
        Cursor c = db.rawQuery(sql, new String[]{address});
        while (c.moveToNext()) {
            Out out = applyCursorOut(c);
            Tx tx = txDict.get(new Sha256Hash(out.getTxHash()));
            if (tx != null) {
                tx.getOuts().add(out);
            }
        }
        c.close();
    }

    @Override
    public List<Tx> getTxAndDetailByAddress(String address, int page) {
        List<Tx> txItemList = new ArrayList<Tx>();

        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select b.* from addresses_txs a, txs b where a.tx_hash=b.tx_hash and a.address=? order by b.block_no desc limit ?,? ";
            Cursor c = db.rawQuery(sql, new String[]{
                    address, Integer.toString((page - 1) * BitherjSettings.TX_PAGE_SIZE), Integer.toString(BitherjSettings.TX_PAGE_SIZE)
            });
            while (c.moveToNext()) {
                Tx txItem = applyCursor(c);
                txItem.setIns(new ArrayList<In>());
                txItem.setOuts(new ArrayList<Out>());
                txItemList.add(txItem);
                txDict.put(new Sha256Hash(txItem.getTxHash()), txItem);
            }
            c.close();
            addInForTxDetail(db, address, txDict);
            addOutForTxDetail(db, address, txDict);

        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return txItemList;
    }

    public List<Tx> getPublishedTxs() {
        List<Tx> txItemList = new ArrayList<Tx>();
        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from txs where block_no is null";
        try {
            Cursor c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                Tx txItem = applyCursor(c);
                txItem.setIns(new ArrayList<In>());
                txItem.setOuts(new ArrayList<Out>());
                txItemList.add(txItem);
                txDict.put(new Sha256Hash(txItem.getTxHash()), txItem);
            }
            c.close();

            sql = "select b.* from txs a, ins b  where a.tx_hash=b.tx_hash  and a.block_no is null "
                    + "order by b.tx_hash ,b.in_sn";
            c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                In inItem = applyCursorIn(c);
                Tx tx = txDict.get(new Sha256Hash(inItem.getTxHash()));
                tx.getIns().add(inItem);
            }
            c.close();

            sql = "select b.* from txs a, outs b where a.tx_hash=b.tx_hash and a.block_no is null "
                    + "order by b.tx_hash,b.out_sn";
            c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                Out out = applyCursorOut(c);
                Tx tx = txDict.get(new Sha256Hash(out.getTxHash()));
                tx.getOuts().add(out);
            }
            c.close();

        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {

        }
        return txItemList;
    }

    public Tx getTxDetailByTxHash(byte[] txHash) {
        Tx txItem = null;
        String txHashStr = Base58.encode(txHash);
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from txs where tx_hash=?";
        Cursor c = db.rawQuery(sql, new String[]{txHashStr});
        try {
            if (c.moveToNext()) {
                txItem = applyCursor(c);
            }

            if (txItem != null) {
                addInsAndOuts(db, txItem);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return txItem;
    }

    private void addInsAndOuts(SQLiteDatabase db, Tx txItem) throws AddressFormatException {
        String txHashStr = Base58.encode(txItem.getTxHash());
        txItem.setOuts(new ArrayList<Out>());
        txItem.setIns(new ArrayList<In>());
        String sql = "select * from ins where tx_hash=? order by in_sn";
        Cursor c = db.rawQuery(sql, new String[]{txHashStr});
        while (c.moveToNext()) {
            In inItem = applyCursorIn(c);
            inItem.setTx(txItem);
            txItem.getIns().add(inItem);
        }
        c.close();

        sql = "select * from outs where tx_hash=? order by out_sn";
        c = db.rawQuery(sql, new String[]{txHashStr});
        while (c.moveToNext()) {
            Out outItem = applyCursorOut(c);
            outItem.setTx(txItem);
            txItem.getOuts().add(outItem);
        }
        c.close();
    }

    public boolean isExist(byte[] txHash) {
        boolean result = false;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(0) from txs where tx_hash=?";
        Cursor c = db.rawQuery(sql, new String[]{Base58.encode(txHash)});
        if (c.moveToNext()) {
            result = c.getInt(0) > 0;
        }
        c.close();
        return result;
    }

    public void add(Tx txItem) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        addTxToDb(db, txItem);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void addTxs(List<Tx> txItems) {
        if (txItems.size() > 0) {
            SQLiteDatabase db = this.mDb.getWritableDatabase();
            db.beginTransaction();
            for (Tx txItem : txItems) {

                addTxToDb(db, txItem);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    private void insertTx(SQLiteDatabase db, Tx txItem) {
        String existSql = "select count(0) cnt from txs where tx_hash=?";
        Cursor c = db.rawQuery(existSql, new String[]{Base58.encode(txItem.getTxHash())});
        int cnt = 0;
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            if (idColumn != -1) {
                cnt = c.getInt(idColumn);
            }
        }
        c.close();
        if (cnt == 0) {
            ContentValues cv = new ContentValues();
            applyContentValues(txItem, cv);
            db.insert(AbstractDb.Tables.TXS, null, cv);
        }

    }

    private List<AddressTx> insertIn(SQLiteDatabase db, Tx txItem) {
        Cursor c;
        String sql;
        ContentValues cv;
        List<AddressTx> addressTxes = new ArrayList<AddressTx>();
        for (In inItem : txItem.getIns()) {
            String existSql = "select count(0) cnt from ins where tx_hash=? and in_sn=?";
            c = db.rawQuery(existSql, new String[]{Base58.encode(inItem.getTxHash()), Integer.toString(inItem.getInSn())});
            int cnt = 0;
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex("cnt");
                if (idColumn != -1) {
                    cnt = c.getInt(idColumn);
                }
            }
            c.close();
            if (cnt == 0) {
                cv = new ContentValues();
                applyContentValues(inItem, cv);
                db.insert(AbstractDb.Tables.INS, null, cv);
            }

            sql = "select out_address from outs where tx_hash=? and out_sn=?";
            c = db.rawQuery(sql, new String[]{
                    Base58.encode(inItem.getPrevTxHash()), Integer.toString(inItem.getPrevOutSn())
            });
            while (c.moveToNext()) {
                int idColumn = c.getColumnIndex("out_address");
                if (idColumn != -1) {
                    addressTxes.add(new AddressTx(c.getString(idColumn), Base58.encode(txItem.getTxHash())));
                }
            }
            c.close();
            sql = "update outs set out_status=? where tx_hash=? and out_sn=?";
            db.execSQL(sql, new String[]{Integer.toString(Out.OutStatus.spent.getValue()), Base58.encode(inItem.getPrevTxHash()), Integer.toString(inItem.getPrevOutSn())});
        }
        return addressTxes;

    }

    private List<AddressTx> insertOut(SQLiteDatabase db, Tx txItem) {
        Cursor c;
        String sql;
        ContentValues cv;
        List<AddressTx> addressTxes = new ArrayList<AddressTx>();
        for (Out outItem : txItem.getOuts()) {
            String existSql = "select count(0) cnt from outs where tx_hash=? and out_sn=?";
            c = db.rawQuery(existSql, new String[]{Base58.encode(outItem.getTxHash()), Integer.toString(outItem.getOutSn())});
            int cnt = 0;
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex("cnt");
                if (idColumn != -1) {
                    cnt = c.getInt(idColumn);
                }
            }
            c.close();
            if (cnt == 0) {
                cv = new ContentValues();
                applyContentValues(outItem, cv);
                db.insert(AbstractDb.Tables.OUTS, null, cv);
            }
            if (!Utils.isEmpty(outItem.getOutAddress())) {
                addressTxes.add(new AddressTx(outItem.getOutAddress(), Base58.encode(txItem.getTxHash())));
            }
            sql = "select tx_hash from ins where prev_tx_hash=? and prev_out_sn=?";
            c = db.rawQuery(sql, new String[]{Base58.encode(txItem.getTxHash()), Integer.toString(outItem.getOutSn())});
            boolean isSpentByExistTx = false;
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex("tx_hash");
                if (idColumn != -1) {
                    addressTxes.add(new AddressTx(outItem.getOutAddress(), c.getString(idColumn)));
                }
                isSpentByExistTx = true;
            }
            c.close();
            if (isSpentByExistTx) {
                sql = "update outs set out_status=? where tx_hash=? and out_sn=?";
                db.execSQL(sql, new String[]{
                        Integer.toString(Out.OutStatus.spent.getValue()), Base58.encode(txItem.getTxHash()), Integer.toString(outItem.getOutSn())
                });
            }

        }
        return addressTxes;
    }

    private void addTxToDb(SQLiteDatabase db, Tx txItem) {
        insertTx(db, txItem);
        List<AddressTx> addressesTxsRels = new ArrayList<AddressTx>();
        List<AddressTx> temp = insertIn(db, txItem);
        if (temp != null && temp.size() > 0) {
            addressesTxsRels.addAll(temp);
        }
        temp = insertOut(db, txItem);
        if (temp != null && temp.size() > 0) {
            addressesTxsRels.addAll(temp);
        }
        for (AddressTx addressTx : addressesTxsRels) {
            String sql = "insert or ignore into addresses_txs(address, tx_hash) values(?,?)";
            db.execSQL(sql, new String[]{addressTx.getAddress(), addressTx.getTxHash()});
        }

    }


    public void remove(byte[] txHash) {
        String txHashStr = Base58.encode(txHash);
        List<String> txHashes = new ArrayList<String>();
        List<String> needRemoveTxHashes = new ArrayList<String>();
        txHashes.add(txHashStr);
        while (txHashes.size() > 0) {
            String thisHash = txHashes.get(0);
            txHashes.remove(0);
            needRemoveTxHashes.add(thisHash);
            List<String> temp = getRelayTx(thisHash);
            txHashes.addAll(temp);
        }
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        for (String str : needRemoveTxHashes) {
            removeSingleTx(db, str);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void removeSingleTx(SQLiteDatabase db, String tx) {
        String deleteTx = "delete from txs where tx_hash='" + tx + "'";
        String deleteIn = "delete from ins where tx_hash='" + tx + "'";
        String deleteOut = "delete from outs where tx_hash='" + tx + "'";
        String deleteAddressesTx = "delete from addresses_txs where tx_hash='" + tx + "'";
        String inSql = "select prev_tx_hash,prev_out_sn from ins where tx_hash='" + tx + "'";
        String existOtherIn = "select count(0) cnt from ins where prev_tx_hash=? and prev_out_sn=?";
        String updatePrevOut = "update outs set out_status=%d where tx_hash=%s and out_sn=%d";
        Cursor c = db.rawQuery(inSql, new String[]{tx});
        List<Object[]> needUpdateOuts = new ArrayList<Object[]>();
        while (c.moveToNext()) {
            int idColumn = c.getColumnIndex(AbstractDb.InsColumns.PREV_TX_HASH);
            String prevTxHash = null;
            int prevOutSn = 0;
            if (idColumn != -1) {
                prevTxHash = c.getString(idColumn);
            }
            idColumn = c.getColumnIndex(AbstractDb.InsColumns.PREV_OUT_SN);
            if (idColumn != -1) {
                prevOutSn = c.getInt(idColumn);
            }
            needUpdateOuts.add(new Object[]{prevTxHash, prevOutSn});

        }
        c.close();
        db.execSQL(deleteAddressesTx);
        db.execSQL(deleteOut);
        db.execSQL(deleteIn);
        db.execSQL(deleteTx);
        for (Object[] array : needUpdateOuts) {
            c = db.rawQuery(existOtherIn, new String[]{array[0].toString(), array[1].toString()});
            while (c.moveToNext()) {
                if (c.getInt(0) == 0) {
                    String updateSql = Utils.format(updatePrevOut,
                            Out.OutStatus.unspent.getValue(), array[0].toString(), Integer.valueOf(array[1].toString()));
                    db.execSQL(updateSql);
                }

            }
            c.close();

        }
    }

    private List<String> getRelayTx(String txHash) {
        List<String> relayTxHashes = new ArrayList<String>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String relayTx = "select distinct tx_hash from ins where prev_tx_hash=?";
        Cursor c = db.rawQuery(relayTx, new String[]{txHash});
        while (c.moveToNext()) {
            relayTxHashes.add(c.getString(0));
        }
        c.close();
        return relayTxHashes;
    }

    public boolean isAddressContainsTx(String address, Tx txItem) {
        boolean result = false;
        String sql = "select count(0) from ins a, txs b where a.tx_hash=b.tx_hash and" +
                " b.block_no is not null and a.prev_tx_hash=? and a.prev_out_sn=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c;
        for (In inItem : txItem.getIns()) {
            c = db.rawQuery(sql, new String[]{Base58.encode(inItem.getPrevTxHash()), Integer.toString(inItem.getPrevOutSn())});
            if (c.moveToNext()) {
                if (c.getInt(0) > 0) {
                    c.close();
                    return false;
                }
            }
            c.close();

        }
        sql = "select count(0) from addresses_txs where tx_hash=? and address=?";
        c = db.rawQuery(sql, new String[]{
                Base58.encode(txItem.getTxHash()), address
        });
        int count = 0;
        if (c.moveToNext()) {
            count = c.getInt(0);
        }
        c.close();
        if (count > 0) {
            return true;
        }
        sql = "select count(0) from outs where tx_hash=? and out_sn=? and out_address=?";
        for (In inItem : txItem.getIns()) {
            c = db.rawQuery(sql, new String[]{Base58.encode(inItem.getPrevTxHash())
                    , Integer.toString(inItem.getPrevOutSn()), address});
            count = 0;
            if (c.moveToNext()) {
                count = c.getInt(0);
            }
            c.close();
            if (count > 0) {
                return true;
            }
        }
        return result;
    }

    public boolean isTxDoubleSpendWithConfirmedTx(Tx tx) {
        String sql = "select count(0) from ins a, txs b where a.tx_hash=b.tx_hash and" +
                " b.block_no is not null and a.prev_tx_hash=? and a.prev_out_sn=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c;
        for (In inItem : tx.getIns()) {
            c = db.rawQuery(sql, new String[]{Base58.encode(inItem.getPrevTxHash()), Integer.toString(inItem.getPrevOutSn())});
            if (c.moveToNext()) {
                if (c.getInt(0) > 0) {
                    c.close();
                    return true;
                }
            }
            c.close();

        }
        return false;
    }

    public List<String> getInAddresses(Tx tx) {
        List<String> result = new ArrayList<String>();
        String sql = "select out_address from outs where tx_hash=? and out_sn=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c;
        for (In inItem : tx.getIns()) {
            c = db.rawQuery(sql, new String[]{Base58.encode(inItem.getPrevTxHash())
                    , Integer.toString(inItem.getPrevOutSn())});
            if (c.moveToNext()) {
                if (!c.isNull(0)) {
                    result.add(c.getString(0));
                }
            }
            c.close();
        }
        return result;
    }

    public void confirmTx(int blockNo, List<byte[]> txHashes) {
        if (blockNo == Tx.TX_UNCONFIRMED || txHashes == null) {
            return;
        }
        String sql = "update txs set block_no=%d where tx_hash='%s'";
        String existSql = "select count(0) from txs where block_no=? and tx_hash=?";
        String doubleSpendSql = "select a.tx_hash from ins a, ins b where a.prev_tx_hash=b.prev_tx_hash " +
                "and a.prev_out_sn=b.prev_out_sn and a.tx_hash<>b.tx_hash and b.tx_hash=?";
        String blockTimeSql = "select block_time from blocks where block_no=?";
        String updateTxTimeThatMoreThanBlockTime = "update txs set tx_time=%d where block_no=%d and tx_time>%d";
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        Cursor c;
        for (byte[] txHash : txHashes) {
            c = db.rawQuery(existSql, new String[]{Integer.toString(blockNo), Base58.encode(txHash)});
            if (c.moveToNext()) {
                int cnt = c.getInt(0);
                c.close();
                if (cnt > 0) {
                    continue;
                }
            } else {
                c.close();
            }
            String updateSql = Utils.format(sql, blockNo, Base58.encode(txHash));
            db.execSQL(updateSql);
            c = db.rawQuery(doubleSpendSql, new String[]{Base58.encode(txHash)});
            List<String> txHashes1 = new ArrayList<String>();
            while (c.moveToNext()) {
                int idColumn = c.getColumnIndex("tx_hash");
                if (idColumn != -1) {
                    txHashes1.add(c.getString(idColumn));
                }
            }
            c.close();
            List<String> needRemoveTxHashes = new ArrayList<String>();
            while (txHashes1.size() > 0) {
                String thisHash = txHashes1.get(0);
                txHashes1.remove(0);
                needRemoveTxHashes.add(thisHash);
                List<String> temp = getRelayTx(thisHash);
                txHashes1.addAll(temp);
            }
            for (String each : needRemoveTxHashes) {
                removeSingleTx(db, each);
            }

        }
        c = db.rawQuery(blockTimeSql, new String[]{Integer.toString(blockNo)});
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("block_time");
            if (idColumn != -1) {
                int blockTime = c.getInt(idColumn);
                c.close();
                String sqlTemp = Utils.format(updateTxTimeThatMoreThanBlockTime, blockTime, blockNo, blockTime);
                db.execSQL(sqlTemp);
            }
        } else {
            c.close();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void unConfirmTxByBlockNo(int blockNo) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        String sql = "update txs set block_no=null where block_no>=" + blockNo;
        db.execSQL(sql);
    }

    public List<Tx> getUnspendTxWithAddress(String address) {
        String unspendOutSql = "select a.*,b.tx_ver,b.tx_locktime,b.tx_time,b.block_no,b.source,ifnull(b.block_no,0)*a.out_value coin_depth " +
                "from outs a,txs b where a.tx_hash=b.tx_hash" +
                " and a.out_address=? and a.out_status=?";
        List<Tx> txItemList = new ArrayList<Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql, new String[]{address, Integer.toString(Out.OutStatus.unspent.getValue())});
        try {
            while (c.moveToNext()) {
                int idColumn = c.getColumnIndex("coin_depth");

                Tx txItem = applyCursor(c);
                Out outItem = applyCursorOut(c);
                if (idColumn != -1) {
                    outItem.setCoinDepth(c.getLong(idColumn));
                }
                outItem.setTx(txItem);
                txItem.setOuts(new ArrayList<Out>());
                txItem.getOuts().add(outItem);
                txItemList.add(txItem);

            }
            c.close();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return txItemList;
    }

    public List<Out> getUnspendOutWithAddress(String address) {
        List<Out> outItems = new ArrayList<Out>();
        String unspendOutSql = "select a.* from outs a,txs b where a.tx_hash=b.tx_hash " +
                " and a.out_address=? and a.out_status=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql,
                new String[]{address, Integer.toString(Out.OutStatus.unspent.getValue())});
        try {
            while (c.moveToNext()) {
                outItems.add(applyCursorOut(c));
            }
            c.close();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return outItems;
    }

    public long getConfirmedBalanceWithAddress(String address) {
        long sum = 0;
        String unspendOutSql = "select ifnull(sum(a.out_value),0) sum from outs a,txs b where a.tx_hash=b.tx_hash " +
                " and a.out_address=? and a.out_status=? and b.block_no is not null";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql,
                new String[]{address, Integer.toString(Out.OutStatus.unspent.getValue())});

        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("sum");
            if (idColumn != -1) {
                sum = c.getLong(idColumn);
            }
        }
        c.close();
        return sum;
    }

    public List<Tx> getUnconfirmedTxWithAddress(String address) {
        List<Tx> txList = new ArrayList<Tx>();

        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select b.* from addresses_txs a, txs b " +
                    "where a.tx_hash=b.tx_hash and a.address=? and b.block_no is null " +
                    "order by b.block_no desc";
            Cursor c = db.rawQuery(sql, new String[]{address});
            while (c.moveToNext()) {
                Tx txItem = applyCursor(c);
                txItem.setIns(new ArrayList<In>());
                txItem.setOuts(new ArrayList<Out>());
                txList.add(txItem);
                txDict.put(new Sha256Hash(txItem.getTxHash()), txItem);
            }
            c.close();
            sql = "select b.tx_hash,b.in_sn,b.prev_tx_hash,b.prev_out_sn " +
                    "from addresses_txs a, ins b, txs c " +
                    "where a.tx_hash=b.tx_hash and b.tx_hash=c.tx_hash and c.block_no is null and a.address=? "
                    + "order by b.tx_hash ,b.in_sn";
            c = db.rawQuery(sql, new String[]{address});
            while (c.moveToNext()) {
                In inItem = applyCursorIn(c);
                Tx tx = txDict.get(new Sha256Hash(inItem.getTxHash()));
                if (tx != null) {
                    tx.getIns().add(inItem);
                }
            }
            c.close();

            sql = "select b.tx_hash,b.out_sn,b.out_value,b.out_address " +
                    "from addresses_txs a, outs b, txs c " +
                    "where a.tx_hash=b.tx_hash and b.tx_hash=c.tx_hash and c.block_no is null and a.address=? "
                    + "order by b.tx_hash,b.out_sn";
            c = db.rawQuery(sql, new String[]{address});
            while (c.moveToNext()) {
                Out out = applyCursorOut(c);
                Tx tx = txDict.get(new Sha256Hash(out.getTxHash()));
                if (tx != null) {
                    tx.getOuts().add(out);
                }
            }
            c.close();

        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return txList;
    }

//    public List<Out> getUnSpendOutCanSpendWithAddress(String address) {
//        List<Out> outItems = new ArrayList<Out>();
//        String confirmedOutSql = "select a.*,b.block_no*a.out_value coin_depth from outs a,txs b" +
//                " where a.tx_hash=b.tx_hash and b.block_no is not null and a.out_address=? and a.out_status=?";
//        String selfOutSql = "select a.* from outs a,txs b where a.tx_hash=b.tx_hash and b.block_no" +
//                " is null and a.out_address=? and a.out_status=? and b.source>=?";
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        Cursor c = db.rawQuery(confirmedOutSql,
//                new String[]{address, Integer.toString(Out.OutStatus.unspent.getValue())});
//        try {
//            while (c.moveToNext()) {
//                Out outItem = applyCursorOut(c);
//                int idColumn = c.getColumnIndex("coin_depth");
//                if (idColumn != -1) {
//                    outItem.setCoinDepth(c.getLong(idColumn));
//                }
//                outItems.add(outItem);
//            }
//            c.close();
//            c = db.rawQuery(selfOutSql, new String[]{address,
//                    Integer.toString(Out.OutStatus.unspent.getValue()), "1"});
//            while (c.moveToNext()) {
//                outItems.add(applyCursorOut(c));
//            }
//            c.close();
//        } catch (AddressFormatException e) {
//            e.printStackTrace();
//        }
//        return outItems;
//    }
//
//    public List<Out> getUnSpendOutButNotConfirmWithAddress(String address) {
//        List<Out> outItems = new ArrayList<Out>();
//        String selfOutSql = "select a.* from outs a,txs b where a.tx_hash=b.tx_hash and b.block_no" +
//                " is null and a.out_address=? and a.out_status=? and b.source=?";
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        Cursor c = db.rawQuery(selfOutSql, new String[]{address,
//                Integer.toString(Out.OutStatus.unspent.getValue()), "0"});
//        try {
//            while (c.moveToNext()) {
//                outItems.add(applyCursorOut(c));
//
//            }
//        } catch (AddressFormatException e) {
//            e.printStackTrace();
//        } finally {
//            c.close();
//        }
//
//        return outItems;
//    }

    public int txCount(String address) {
        int result = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(0) cnt from addresses_txs  where address=?";
        Cursor c = db.rawQuery(sql, new String[]{address});
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            if (idColumn != -1) {
                result = c.getInt(idColumn);
            }
        }
        c.close();

        return result;
    }

    public long totalReceive(String address) {
        long result = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select ifnull(sum(out_value),0) total from outs where out_address=?";
        Cursor c = db.rawQuery(sql, new String[]{address});
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("total");
            if (idColumn != -1) {
                result = c.getLong(idColumn);
            }
        }
        c.close();
        return result;
    }

    public long totalSend(String address) {
        long result = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select ifnull(sum(b.out_value),0) total " +
                " from ins a, outs b " +
                " where a.prev_tx_hash=b.tx_hash and a.prev_out_sn=b.out_sn and b.out_address=?";
        Cursor c = db.rawQuery(sql, new String[]{address});
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("total");
            if (idColumn != -1) {
                result = c.getLong(idColumn);
            }
        }
        c.close();
        return result;
    }

    public void txSentBySelfHasSaw(byte[] txHash) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        String sql = "update txs set source=source+1 where tx_hash=? and source>=1";
        db.execSQL(sql, new String[]{Base58.encode(txHash)});
    }

    public List<Out> getOuts() {
        List<Out> outItemList = new ArrayList<Out>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from outs ";
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                outItemList.add(applyCursorOut(c));
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return outItemList;
    }

//    public List<Out> getUnSpentOuts() {
//        List<Out> outItemList = new ArrayList<Out>();
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        String sql = "select * from outs where out_status=?";
//        Cursor c = db.rawQuery(sql, new String[]{"0"});
//        try {
//            while (c.moveToNext()) {
//                outItemList.add(applyCursorOut(c));
//            }
//        } catch (AddressFormatException e) {
//            e.printStackTrace();
//        } finally {
//            c.close();
//        }
//
//        return outItemList;
//    }

    public List<In> getRelatedIn(String address) {
        List<In> list = new ArrayList<In>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select ins.* from ins,addresses_txs " +
                "where ins.tx_hash=addresses_txs.tx_hash and addresses_txs.address=? ";
        Cursor c = db.rawQuery(sql, new String[]{address});
        try {
            while (c.moveToNext()) {
                list.add(applyCursorIn(c));
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return list;
    }

    public List<Tx> getRecentlyTxsByAddress(String address, int greateThanBlockNo, int limit) {
        List<Tx> txItemList = new ArrayList<Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select b.* from addresses_txs a, txs b where a.tx_hash=b.tx_hash and a.address='%s' " +
                "and ((b.block_no is null) or (b.block_no is not null and b.block_no>%d)) " +
                "order by ifnull(b.block_no,4294967295) desc, b.tx_time desc " +
                "limit %d ";
        sql = Utils.format(sql, address, greateThanBlockNo, limit);
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                Tx txItem = applyCursor(c);
                txItemList.add(txItem);
            }

            for (Tx item : txItemList) {
                addInsAndOuts(db, item);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return txItemList;
    }

//    public List<Long> txInValues(byte[] txHash) {
//        List<Long> inValues = new ArrayList<Long>();
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        String sql = "select b.out_value " +
//                "from ins a left outer join outs b on a.prev_tx_hash=b.tx_hash and a.prev_out_sn=b.out_sn " +
//                "where a.tx_hash=?";
//        Cursor c = db.rawQuery(sql, new String[]{Base58.encode(txHash)});
//        while (c.moveToNext()) {
//            int idColumn = c.getColumnIndex("out_value");
//            if (idColumn != -1) {
//                inValues.add(c.getLong(idColumn));
//            } else {
//                inValues.add(null);
//            }
//        }
//        c.close();
//        return inValues;
//    }

    public HashMap<Sha256Hash, Tx> getTxDependencies(Tx txItem) {
        HashMap<Sha256Hash, Tx> result = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {


            for (In inItem : txItem.getIns()) {
                Tx tx;
                String txHashStr = Base58.encode(inItem.getTxHash());
                String sql = "select * from txs where tx_hash=?";
                Cursor c = db.rawQuery(sql, new String[]{txHashStr});
                if (c.moveToNext()) {
                    tx = applyCursor(c);
                    c.close();
                } else {
                    c.close();
                    continue;
                }
                addInsAndOuts(db, tx);
                result.put(new Sha256Hash(tx.getTxHash()), tx);

            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void clearAllTx() {
        SQLiteDatabase db = mDb.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("drop table " + AbstractDb.Tables.TXS + ";");
        db.execSQL("drop table " + AbstractDb.Tables.OUTS + ";");
        db.execSQL("drop table " + AbstractDb.Tables.INS + ";");
        db.execSQL("drop table " + AbstractDb.Tables.ADDRESSES_TXS + ";");
        db.execSQL("drop table " + AbstractDb.Tables.PEERS + ";");
        db.execSQL(AbstractDb.CREATE_TXS_SQL);
        db.execSQL(AbstractDb.CREATE_TX_BLOCK_NO_INDEX);
        db.execSQL(AbstractDb.CREATE_OUTS_SQL);
        db.execSQL(AbstractDb.CREATE_OUT_OUT_ADDRESS_INDEX);
        db.execSQL(AbstractDb.CREATE_INS_SQL);
        db.execSQL(AbstractDb.CREATE_IN_PREV_TX_HASH_INDEX);
        db.execSQL(AbstractDb.CREATE_ADDRESSTXS_SQL);
        db.execSQL(AbstractDb.CREATE_PEER_SQL);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void completeInSignature(List<In> ins) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        String sql = "update ins set in_signature=? where tx_hash=? and in_sn=? and ifnull(in_signature,'')=''";
        for (In in : ins) {
            db.execSQL(sql, new String[]{Base58.encode(in.getInSignature())
                    , Base58.encode(in.getTxHash()), Integer.toString(in.getInSn())});
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public int needCompleteInSignature(String address) {
        int result = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select max(txs.block_no) from outs,ins,txs where outs.out_address=? " +
                "and ins.prev_tx_hash=outs.tx_hash and ins.prev_out_sn=outs.out_sn " +
                "and ifnull(ins.in_signature,'')='' and txs.tx_hash=ins.tx_hash";
        Cursor c = db.rawQuery(sql, new String[]{address});
        if (c.moveToNext()) {
            result = c.getInt(0);
        }
        c.close();
        return result;
    }

    private void applyContentValues(Tx txItem, ContentValues cv) {
        if (txItem.getBlockNo() != Tx.TX_UNCONFIRMED) {
            cv.put(AbstractDb.TxsColumns.BLOCK_NO, txItem.getBlockNo());
        } else {
            cv.putNull(AbstractDb.TxsColumns.BLOCK_NO);
        }
        cv.put(AbstractDb.TxsColumns.TX_HASH, Base58.encode(txItem.getTxHash()));
        cv.put(AbstractDb.TxsColumns.SOURCE, txItem.getSource());
        cv.put(AbstractDb.TxsColumns.TX_TIME, txItem.getTxTime());
        cv.put(AbstractDb.TxsColumns.TX_VER, txItem.getTxVer());
        cv.put(AbstractDb.TxsColumns.TX_LOCKTIME, txItem.getTxLockTime());
    }

    private void applyContentValues(In inItem, ContentValues cv) {
        cv.put(AbstractDb.InsColumns.TX_HASH, Base58.encode(inItem.getTxHash()));
        cv.put(AbstractDb.InsColumns.IN_SN, inItem.getInSn());
        cv.put(AbstractDb.InsColumns.PREV_TX_HASH, Base58.encode(inItem.getPrevTxHash()));
        cv.put(AbstractDb.InsColumns.PREV_OUT_SN, inItem.getPrevOutSn());
        if (inItem.getInSignature() != null) {
            cv.put(AbstractDb.InsColumns.IN_SIGNATURE, Base58.encode(inItem.getInSignature()));
        } else {
            cv.putNull(AbstractDb.InsColumns.IN_SIGNATURE);
        }
        cv.put(AbstractDb.InsColumns.IN_SEQUENCE, inItem.getInSequence());
    }

    private void applyContentValues(Out outItem, ContentValues cv) {
        cv.put(AbstractDb.OutsColumns.TX_HASH, Base58.encode(outItem.getTxHash()));
        cv.put(AbstractDb.OutsColumns.OUT_SN, outItem.getOutSn());
        cv.put(AbstractDb.OutsColumns.OUT_SCRIPT, Base58.encode(outItem.getOutScript()));
        cv.put(AbstractDb.OutsColumns.OUT_VALUE, outItem.getOutValue());
        cv.put(AbstractDb.OutsColumns.OUT_STATUS, outItem.getOutStatus().getValue());
        if (!Utils.isEmpty(outItem.getOutAddress())) {
            cv.put(AbstractDb.OutsColumns.OUT_ADDRESS, outItem.getOutAddress());
        } else {
            cv.putNull(AbstractDb.OutsColumns.OUT_ADDRESS);
        }
    }

    private Tx applyCursor(Cursor c) throws AddressFormatException {
        Tx txItem = new Tx();
        int idColumn = c.getColumnIndex(AbstractDb.TxsColumns.BLOCK_NO);
        if (!c.isNull(idColumn)) {
            txItem.setBlockNo(c.getInt(idColumn));
        } else {
            txItem.setBlockNo(Tx.TX_UNCONFIRMED);
        }
        idColumn = c.getColumnIndex(AbstractDb.TxsColumns.TX_HASH);
        if (idColumn != -1) {
            txItem.setTxHash(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(AbstractDb.TxsColumns.SOURCE);
        if (idColumn != -1) {
            txItem.setSource(c.getInt(idColumn));
        }
        if (txItem.getSource() >= 1) {
            txItem.setSawByPeerCnt(txItem.getSource() - 1);
            txItem.setSource(1);
        } else {
            txItem.setSawByPeerCnt(0);
            txItem.setSource(0);
        }
        idColumn = c.getColumnIndex(AbstractDb.TxsColumns.TX_TIME);
        if (idColumn != -1) {
            txItem.setTxTime(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.TxsColumns.TX_VER);
        if (idColumn != -1) {
            txItem.setTxVer(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.TxsColumns.TX_LOCKTIME);
        if (idColumn != -1) {
            txItem.setTxLockTime(c.getInt(idColumn));
        }
        return txItem;

    }

    private In applyCursorIn(Cursor c) throws AddressFormatException {
        In inItem = new In();
        int idColumn = c.getColumnIndex(AbstractDb.InsColumns.TX_HASH);
        if (idColumn != -1) {
            inItem.setTxHash(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(AbstractDb.InsColumns.IN_SN);
        if (idColumn != -1) {
            inItem.setInSn(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.InsColumns.PREV_TX_HASH);
        if (idColumn != -1) {
            inItem.setPrevTxHash(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(AbstractDb.InsColumns.PREV_OUT_SN);
        if (idColumn != -1) {
            inItem.setPrevOutSn(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.InsColumns.IN_SIGNATURE);
        if (idColumn != -1) {
            String inSignature = c.getString(idColumn);
            if (!Utils.isEmpty(inSignature)) {
                inItem.setInSignature(Base58.decode(c.getString(idColumn)));
            }
        }
        idColumn = c.getColumnIndex(AbstractDb.InsColumns.IN_SEQUENCE);
        if (idColumn != -1) {
            inItem.setInSequence(c.getInt(idColumn));
        }
        return inItem;
    }

    private Out applyCursorOut(Cursor c) throws AddressFormatException {
        Out outItem = new Out();
        int idColumn = c.getColumnIndex(AbstractDb.OutsColumns.TX_HASH);
        if (idColumn != -1) {
            outItem.setTxHash(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_SN);
        if (idColumn != -1) {
            outItem.setOutSn(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_SCRIPT);
        if (idColumn != -1) {
            outItem.setOutScript(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_VALUE);
        if (idColumn != -1) {
            outItem.setOutValue(c.getLong(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_STATUS);
        if (idColumn != -1) {
            outItem.setOutStatus(Out.getOutStatus(c.getInt(idColumn)));
        }
        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_ADDRESS);
        if (idColumn != -1) {
            outItem.setOutAddress(c.getString(idColumn));
        }
        return outItem;
    }

    private static class AddressTx {
        private String address;
        private String txHash;

        public AddressTx(String address, String txHash) {
            this.address = address;
            this.txHash = txHash;

        }

        public String getTxHash() {
            return txHash;
        }

        public void setTxHash(String txHash) {
            this.txHash = txHash;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }


    }
}


































