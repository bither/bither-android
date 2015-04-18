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
import net.bither.bitherj.core.AbstractHD;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.In;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.OutPoint;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.IHDAccountProvider;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Sha256Hash;
import net.bither.bitherj.utils.Utils;
import net.bither.image.glcrop.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class HDAccountProvider implements IHDAccountProvider {
    private static HDAccountProvider hdAccountProvider = new HDAccountProvider(BitherApplication.mHDDbHelper);

    public static HDAccountProvider getInstance() {
        return hdAccountProvider;
    }


    private SQLiteOpenHelper mDb;


    private HDAccountProvider(SQLiteOpenHelper hdAccountDB) {
        this.mDb = hdAccountDB;
    }


    @Override
    public void addAddress(List<HDAccount.HDAccountAddress> hdAccountAddresses) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        for (HDAccount.HDAccountAddress hdAccountAddress : hdAccountAddresses) {
            ContentValues cv = getHDMAddressCV(hdAccountAddress);
            db.insert(AbstractDb.Tables.HD_ACCOUNT_ADDRESS, null, cv);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }


    @Override
    public int issuedIndex(AbstractHD.PathType pathType) {
        int issuedIndex = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select ifnull(max(address_index),0) address_index from " + AbstractDb.Tables.HD_ACCOUNT_ADDRESS + " where path_type=? and is_issued=?  ",
                new String[]{Integer.toString(pathType.getValue()), "1"});
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDAccountAddressesColumns.ADDRESS_INDEX);
            if (idColumn != -1) {
                issuedIndex = cursor.getInt(idColumn);
            }
        }
        return issuedIndex;
    }


    @Override
    public int allGeneratedAddressCount(AbstractHD.PathType pathType) {
        int count = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select ifnull(count(address),0) count from "
                        + AbstractDb.Tables.HD_ACCOUNT_ADDRESS + " where path_type=? ",
                new String[]{Integer.toString(pathType.getValue())});
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex("count");
            if (idColumn != -1) {
                count = cursor.getInt(idColumn);
            }
        }
        return count;
    }

    @Override
    public String externalAddress() {
        String address = null;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select address from " + AbstractDb.Tables.HD_ACCOUNT_ADDRESS
                        + " where path_type=? and is_issued=? order by address_index asc limit 1 ",
                new String[]{Integer.toString(AbstractHD.PathType.EXTERNAL_ROOT_PATH.getValue()), "0"});
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDAccountAddressesColumns.ADDRESS);
            if (idColumn != -1) {
                address = cursor.getString(idColumn);
            }
        }
        return address;
    }

    @Override
    public HashSet<String> getAllAddress() {
        HashSet<String> addressSet = new HashSet<String>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select address from  " + AbstractDb.Tables.HD_ACCOUNT_ADDRESS,
                null);
        while (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDAccountAddressesColumns.ADDRESS);
            if (idColumn != -1) {
                addressSet.add(cursor.getString(idColumn));
            }
        }
        return addressSet;
    }

    @Override
    public List<byte[]> getPubs(AbstractHD.PathType pathType) {
        List<byte[]> adressPubList = new ArrayList<byte[]>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select pub from  " + AbstractDb.Tables.HD_ACCOUNT_ADDRESS,
                null);
        while (cursor.moveToNext()) {
            try {
                int idColumn = cursor.getColumnIndex(AbstractDb.HDAccountAddressesColumns.PUB);
                if (idColumn != -1) {
                    adressPubList.add(Base58.decode(cursor.getString(idColumn)));
                }
            } catch (AddressFormatException e) {
                e.printStackTrace();
            }
        }
        return adressPubList;
    }

    public List<HDAccount.HDAccountAddress> getAllHDAddress() {
        List<HDAccount.HDAccountAddress> adressPubList = new ArrayList<HDAccount.HDAccountAddress>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select address,pub,path_type,address_index,is_issued,is_synced from  " + AbstractDb.Tables.HD_ACCOUNT_ADDRESS,
                null);
        while (cursor.moveToNext()) {
            HDAccount.HDAccountAddress hdAccountAddress = formatAddress(cursor);
            if (hdAccountAddress != null) {
                adressPubList.add(hdAccountAddress);
            }
        }
        return adressPubList;
    }


    @Override
    public List<Tx> getUnspentTxs() {
        String unspendOutSql =
                "select a.*,b.tx_ver,b.tx_locktime,b.tx_time,b.block_no,b.source,ifnull(b.block_no,0)*a.out_value coin_depth " +
                        "from outs a,txs b where a.tx_hash=b.tx_hash" +
                        " and  a.out_status=? and a.belong_account=?";
        List<Tx> txItemList = new ArrayList<Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql, new String[]{Integer.toString(Out.OutStatus.unspent.getValue()), "1"});
        try {
            while (c.moveToNext()) {
                int idColumn = c.getColumnIndex("coin_depth");

                Tx txItem = TxHelper.applyCursor(c);
                Out outItem = TxHelper.applyCursorOut(c);
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

    @Override
    public List<Out> getUnspendOut() {
        List<Out> outItems = new ArrayList<Out>();
        String unspendOutSql = "select a.* from outs a,txs b where a.tx_hash=b.tx_hash " +
                " and a.out_status=? and a.belong_account=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql,
                new String[]{Integer.toString(Out.OutStatus.unspent.getValue()), "1"});
        try {
            while (c.moveToNext()) {
                outItems.add(TxHelper.applyCursorOut(c));
            }
            c.close();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return outItems;
    }

    @Override
    public List<HDAccount.HDAccountAddress> addTx(Tx tx) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        List<TxHelper.AddressTx> addressTxes = addTxToDb(db, tx);
        db.setTransactionSuccessful();
        db.endTransaction();
        List<HDAccount.HDAccountAddress> hdAccountAddressList = new ArrayList<HDAccount.HDAccountAddress>();
        List<HDAccount.HDAccountAddress> hdAccountAddressAllList = getAllHDAddress();
        for (HDAccount.HDAccountAddress hdAccountAddress : hdAccountAddressAllList) {
            for (TxHelper.AddressTx addressTx : addressTxes) {
                if (Utils.compareString(hdAccountAddress.getAddress(), addressTx.getAddress())) {
                    hdAccountAddressList.add(hdAccountAddress);
                }
            }
        }
        return hdAccountAddressList;

    }

    @Override
    public void addTxs(List<Tx> txList) {
        if (txList.size() > 0) {
            SQLiteDatabase db = this.mDb.getWritableDatabase();
            db.beginTransaction();
            for (Tx txItem : txList) {
                addTxToDb(db, txItem);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }


    @Override
    public HDAccount.HDAccountAddress addressForPath(AbstractHD.PathType type, int index) {

        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select address,pub,path_type,address_index,is_issued,is_synced from " +
                        AbstractDb.Tables.HD_ACCOUNT_ADDRESS + " where path_type=? and address_index=? ",
                new String[]{Integer.toString(type.getValue()), Integer.toString(index)});
        HDAccount.HDAccountAddress accountAddress = null;
        if (cursor.moveToNext()) {
            accountAddress = formatAddress(cursor);
        }
        cursor.close();
        return accountAddress;
    }

    @Override
    public void updateIssuedIndex(AbstractHD.PathType pathType, int index) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountAddressesColumns.IS_ISSUED, 1);
        db.update(AbstractDb.Tables.HD_ACCOUNT_ADDRESS, cv, " path_type=? and address_index=? ", new String[]{
                Integer.toString(pathType.getValue()), Integer.toString(index)
        });

    }

    @Override
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

    @Override
    public List<HDAccount.HDAccountAddress> belongAccount(List<String> addresses) {
        List<HDAccount.HDAccountAddress> hdAccountAddressList = new ArrayList<HDAccount.HDAccountAddress>();
        List<String> temp = new ArrayList<String>();
        for (String str : addresses) {
            temp.add(Utils.format("'%s'", str));
        }
        String sql = "select address,pub,path_type,address_index,is_issued,is_synced from " + AbstractDb.Tables.HD_ACCOUNT_ADDRESS
                + " where address in (" + Utils.joinString(temp, ",") + ")";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, null);

        while (cursor.moveToNext()) {
            hdAccountAddressList.add(formatAddress(cursor));

        }
        cursor.close();
        return hdAccountAddressList;
    }

    @Override
    public int txCount() {
        int result = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(0) cnt from txs ";
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            if (idColumn != -1) {
                result = c.getInt(idColumn);
            }
        }
        c.close();
        return result;
    }

    @Override
    public long getConfirmedBanlance() {
        long sum = 0;
        String unspendOutSql = "select ifnull(sum(a.out_value),0) sum from outs a,txs b where a.tx_hash=b.tx_hash " +
                "  and a.out_status=? and a.belong_account=? and b.block_no is not null";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql,
                new String[]{Integer.toString(Out.OutStatus.unspent.getValue()), "1"});

        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("sum");
            if (idColumn != -1) {
                sum = c.getLong(idColumn);
            }
        }
        c.close();
        return sum;
    }

    @Override
    public List<Tx> getUnconfirmedTx() {
        List<Tx> txList = new ArrayList<Tx>();

        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select * from txs where  block_no is null " +
                    "order by block_no desc";
            Cursor c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                Tx txItem = TxHelper.applyCursor(c);
                txItem.setIns(new ArrayList<In>());
                txItem.setOuts(new ArrayList<Out>());
                txList.add(txItem);
                txDict.put(new Sha256Hash(txItem.getTxHash()), txItem);
            }
            c.close();
            sql = "select b.tx_hash,b.in_sn,b.prev_tx_hash,b.prev_out_sn " +
                    "from ins b, txs c " +
                    "where  b.tx_hash=c.tx_hash and c.block_no is null  "
                    + "order by b.tx_hash ,b.in_sn";
            c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                In inItem = TxHelper.applyCursorIn(c);
                Tx tx = txDict.get(new Sha256Hash(inItem.getTxHash()));
                if (tx != null) {
                    tx.getIns().add(inItem);
                }
            }
            c.close();

            sql = "select b.tx_hash,b.out_sn,b.out_value,b.out_address " +
                    "from  outs b, txs c " +
                    "where  b.tx_hash=c.tx_hash and c.block_no is null  "
                    + "order by b.tx_hash,b.out_sn";
            c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                Out out = TxHelper.applyCursorOut(c);
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

    @Override
    public Tx getTxDetailByTxHash(byte[] txHash) {
        Tx txItem = null;
        String txHashStr = Base58.encode(txHash);
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from txs where tx_hash=?";
        Cursor c = db.rawQuery(sql, new String[]{txHashStr});
        try {
            if (c.moveToNext()) {
                txItem = TxHelper.applyCursor(c);
            }

            if (txItem != null) {
                TxHelper.addInsAndOuts(db, txItem);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return txItem;
    }

    @Override
    public List<HDAccount.HDAccountAddress> getSigningAddressesForInputs(List<In> inList) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        List<HDAccount.HDAccountAddress> hdAccountAddressList =
                new ArrayList<HDAccount.HDAccountAddress>();
        Cursor c;
        for (In in : inList) {
            String sql = "select a.address,a.path_type,a.address_index,a.is_synced" +
                    " from hd_account_addresses a ,outs b" +
                    " where a.address=b.out_address" +
                    " and b.tx_hash=? and b.out_sn=?  ";
            OutPoint outPoint = in.getOutpoint();
            c = db.rawQuery(sql, new String[]{Base58.encode(in.getPrevTxHash()), Integer.toString(outPoint.getOutSn())});
            if (c.moveToNext()) {
                hdAccountAddressList.add(formatAddress(c));
            }
            c.close();
        }
        return hdAccountAddressList;
    }

    @Override
    public List<Tx> getPublishedTxs() {
        List<Tx> txItemList = new ArrayList<Tx>();
        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from txs where block_no is null";
        try {
            Cursor c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                Tx txItem = TxHelper.applyCursor(c);
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
                In inItem = TxHelper.applyCursorIn(c);
                Tx tx = txDict.get(new Sha256Hash(inItem.getTxHash()));
                tx.getIns().add(inItem);
            }
            c.close();

            sql = "select b.* from txs a, outs b where a.tx_hash=b.tx_hash and a.block_no is null "
                    + "order by b.tx_hash,b.out_sn";
            c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                Out out = TxHelper.applyCursorOut(c);
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

    @Override
    public void updateSyncdComplete(HDAccount.HDAccountAddress address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountAddressesColumns.IS_SYNCED, address.isSynced() ? 1 : 0);
        db.update(AbstractDb.Tables.HD_ACCOUNT_ADDRESS, cv, "address=?"
                , new String[]{address.getAddress()});


    }

    @Override
    public void updateSyncdForIndex(AbstractHD.PathType pathType, int index) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountAddressesColumns.IS_SYNCED, 1);
        db.update(AbstractDb.Tables.HD_ACCOUNT_ADDRESS, cv, "path_type=? and address_index>?"
                , new String[]{Integer.toString(pathType.getValue()), Integer.toString(index)});

    }

    @Override
    public void setSyncdNotComplete() {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountAddressesColumns.IS_SYNCED, 0);
        db.update(AbstractDb.Tables.HD_ACCOUNT_ADDRESS, cv, null
                , null);


    }

    @Override
    public void clearAllTx() {
        SQLiteDatabase db = mDb.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("drop table " + AbstractDb.Tables.TXS + ";");
        db.execSQL("drop table " + AbstractDb.Tables.OUTS + ";");
        db.execSQL("drop table " + AbstractDb.Tables.INS + ";");

        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_TX);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_TX_BLOCK_NO_INDEX);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_OUT);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_OUT_OUT_ADDRESS_INDEX);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_IN);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_IN_PREV_TX_HASH_INDEX);


        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public int unSyncedAddressCount() {
        String sql = "select count(address) cnt from hd_account_addresses where is_synced=? ";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, new String[]{"0"});
        int cnt = 0;
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex("cnt");
            if (idColumn != -1) {
                cnt = cursor.getInt(idColumn);
            }
        }
        return cnt;
    }

    @Override
    public List<Tx> getRecentlyTxsByAddress(int greateThanBlockNo, int limit) {
        List<Tx> txItemList = new ArrayList<Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from txs  where  " +
                "((block_no is null) or (block_no is not null and block_no>%d)) " +
                "order by ifnull(block_no,4294967295) desc, tx_time desc " +
                "limit %d ";
        sql = Utils.format(sql, greateThanBlockNo, limit);
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                Tx txItem = TxHelper.applyCursor(c);
                txItemList.add(txItem);
            }

            for (Tx item : txItemList) {
                TxHelper.addInsAndOuts(db, item);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return txItemList;
    }

    @Override
    public long sentFromAddress(byte[] txHash) {
        String sql = "select  sum(o.out_value) out_value from ins i,outs o where" +
                " i.tx_hash=? and o.tx_hash=i.prev_tx_hash and i.prev_out_sn=o.out_sn and o.belong_account=?";
        long sum = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor;
        cursor = db.rawQuery(sql, new String[]{Base58.encode(txHash),
                "1"});
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.OutsColumns.OUT_VALUE);
            if (idColumn != -1) {
                sum = cursor.getLong(idColumn);
            }
        }
        cursor.close();

        return sum;
    }

    @Override
    public List<Tx> getTxAndDetailByAddress(int page) {
        List<Tx> txItemList = new ArrayList<Tx>();

        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select * from txs order by" +
                    " ifnull(block_no,4294967295) desc limit ?,? ";
            Cursor c = db.rawQuery(sql, new String[]{
                    Integer.toString((page - 1) * BitherjSettings.TX_PAGE_SIZE), Integer.toString(BitherjSettings.TX_PAGE_SIZE)
            });
            StringBuilder txsStrBuilder = new StringBuilder();
            while (c.moveToNext()) {
                Tx txItem = TxHelper.applyCursor(c);
                txItem.setIns(new ArrayList<In>());
                txItem.setOuts(new ArrayList<Out>());
                txItemList.add(txItem);
                txDict.put(new Sha256Hash(txItem.getTxHash()), txItem);
                txsStrBuilder.append("'").append(Base58.encode(txItem.getTxHash())).append("'").append(",");
            }
            c.close();

            if (txsStrBuilder.length() > 1) {
                String txs = txsStrBuilder.substring(0, txsStrBuilder.length() - 1);
                sql = Utils.format("select b.* from ins b where b.tx_hash in (%s)" +
                        " order by b.tx_hash ,b.in_sn", txs);
                c = db.rawQuery(sql, null);
                while (c.moveToNext()) {
                    In inItem = TxHelper.applyCursorIn(c);
                    Tx tx = txDict.get(new Sha256Hash(inItem.getTxHash()));
                    if (tx != null) {
                        tx.getIns().add(inItem);
                    }
                }
                c.close();
                sql = Utils.format("select b.* from outs b where b.tx_hash in (%s)" +
                        " order by b.tx_hash,b.out_sn", txs);
                c = db.rawQuery(sql, null);
                while (c.moveToNext()) {
                    Out out = TxHelper.applyCursorOut(c);
                    Tx tx = txDict.get(new Sha256Hash(out.getTxHash()));
                    if (tx != null) {
                        tx.getOuts().add(out);
                    }
                }
                c.close();
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return txItemList;
    }

    private List<TxHelper.AddressTx> addTxToDb(SQLiteDatabase db, Tx txItem) {
        HashSet<String> addressSet = getAllAddress();
        for (Out out : txItem.getOuts()) {
            if (addressSet.contains(out.getOutAddress())) {
                out.setOutType(Out.OutType.BELONG_HD_ACCOUNT);
            } else {
                out.setOutType(Out.OutType.NO_BELONG_HD_ACCOUNT);
            }
        }
        TxHelper.insertTx(db, txItem);
        List<TxHelper.AddressTx> addressesTxsRels = new ArrayList<TxHelper.AddressTx>();
        List<TxHelper.AddressTx> temp = TxHelper.insertIn(db, txItem);

        if (temp != null && temp.size() > 0) {
            addressesTxsRels.addAll(temp);
        }

        temp = TxHelper.insertOut(db, txItem);
        if (temp != null && temp.size() > 0) {
            addressesTxsRels.addAll(temp);
        }
        return temp;

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

    private void removeSingleTx(SQLiteDatabase db, String tx) {
        String deleteTx = "delete from txs where tx_hash='" + tx + "'";
        String deleteIn = "delete from ins where tx_hash='" + tx + "'";
        String deleteOut = "delete from outs where tx_hash='" + tx + "'";

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


    private HDAccount.HDAccountAddress formatAddress(Cursor c) {
        String address = null;
        byte[] pubs = null;
        AbstractHD.PathType ternalRootType = AbstractHD.PathType.EXTERNAL_ROOT_PATH;
        int index = 0;
        boolean isIssued = false;
        boolean isSynced = true;
        HDAccount.HDAccountAddress hdAccountAddress = null;
        try {
            int idColumn = c.getColumnIndex(AbstractDb.HDAccountAddressesColumns.ADDRESS);
            if (idColumn != -1) {
                address = c.getString(idColumn);
            }
            idColumn = c.getColumnIndex(AbstractDb.HDAccountAddressesColumns.PUB);
            if (idColumn != -1) {
                pubs = Base58.decode(c.getString(idColumn));
            }
            idColumn = c.getColumnIndex(AbstractDb.HDAccountAddressesColumns.PATH_TYPE);
            if (idColumn != -1) {
                ternalRootType = AbstractHD.getTernalRootType(c.getInt(idColumn));

            }
            idColumn = c.getColumnIndex(AbstractDb.HDAccountAddressesColumns.ADDRESS_INDEX);
            if (idColumn != -1) {
                index = c.getInt(idColumn);
            }
            idColumn = c.getColumnIndex(AbstractDb.HDAccountAddressesColumns.IS_ISSUED);
            if (idColumn != -1) {
                isIssued = c.getInt(idColumn) == 1;
            }
            idColumn = c.getColumnIndex(AbstractDb.HDAccountAddressesColumns.IS_SYNCED);
            if (idColumn != -1) {
                isSynced = c.getInt(idColumn) == 1;
            }
            hdAccountAddress = new HDAccount.HDAccountAddress(address, pubs,
                    ternalRootType, index, isIssued, isSynced);
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return hdAccountAddress;
    }


    private ContentValues getHDMAddressCV(HDAccount.HDAccountAddress hdAccountAddress) {
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountAddressesColumns.PUB, Base58.encode(hdAccountAddress.getPub()));
        cv.put(AbstractDb.HDAccountAddressesColumns.ADDRESS, hdAccountAddress.getAddress());
        cv.put(AbstractDb.HDAccountAddressesColumns.PATH_TYPE, hdAccountAddress.getPathType().getValue());
        cv.put(AbstractDb.HDAccountAddressesColumns.ADDRESS_INDEX, hdAccountAddress.getIndex());
        cv.put(AbstractDb.HDAccountAddressesColumns.IS_ISSUED, hdAccountAddress.isIssued() ? 1 : 0);
        cv.put(AbstractDb.HDAccountAddressesColumns.IS_SYNCED, hdAccountAddress.isSynced() ? 1 : 0);
        return cv;
    }
}
