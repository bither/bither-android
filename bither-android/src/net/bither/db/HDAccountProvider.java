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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class HDAccountProvider implements IHDAccountProvider {

    private final static String queryTxHashOfHDAccount = " select  distinct txs.tx_hash from addresses_txs txs ,hd_account_addresses hd where txs.address=hd.address";
    private final static String inQueryTxHashOfHDAccount = " (" + queryTxHashOfHDAccount + ")";

    private static HDAccountProvider hdAccountProvider = new HDAccountProvider(BitherApplication.mDbHelper);

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
        int issuedIndex = -1;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select ifnull(max(address_index),-1) address_index from " + AbstractDb.Tables.HD_ACCOUNT_ADDRESS + " where path_type=? and is_issued=?  ",
                new String[]{Integer.toString(pathType.getValue()), "1"});
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDAccountAddressesColumns.ADDRESS_INDEX);
            if (idColumn != -1) {
                issuedIndex = cursor.getInt(idColumn);
            }
        }
        cursor.close();
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
        cursor.close();
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
        cursor.close();
        return address;
    }

    @Override
    public HashSet<String> getBelongAccountAddresses(List<String> addressList) {
        HashSet<String> addressSet = new HashSet<String>();

        List<String> temp = new ArrayList<String>();
        if (addressList != null) {
            for (String str : addressList) {
                temp.add(Utils.format("'%s'", str));
            }
        }
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = Utils.format("select address from hd_account_addresses where address in (%s) "
                , Utils.joinString(temp, ","));
        Cursor cursor = db.rawQuery(sql,
                null);
        while (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDAccountAddressesColumns.ADDRESS);
            if (idColumn != -1) {
                addressSet.add(cursor.getString(idColumn));
            }
        }
        cursor.close();
        return addressSet;
    }

    @Override
    public List<byte[]> getPubs(AbstractHD.PathType pathType) {
        List<byte[]> adressPubList = new ArrayList<byte[]>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select pub from hd_account_addresses where path_type=? ",
                new String[]{Integer.toString(pathType.getValue())});
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
        cursor.close();
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
        cursor.close();
        return adressPubList;
    }


    @Override
    public List<Out> getUnspendOutByHDAccount(int hdAccountId) {
        List<Out> outItems = new ArrayList<Out>();
        String unspendOutSql = "select a.* from outs a,txs b where a.tx_hash=b.tx_hash " +
                " and a.out_status=? and a.hd_account_id=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql,
                new String[]{Integer.toString(Out.OutStatus.unspent.getValue()), Integer.toString(hdAccountId)});
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
        db.update(AbstractDb.Tables.HD_ACCOUNT_ADDRESS, cv, " path_type=? and address_index<=? ", new String[]{
                Integer.toString(pathType.getValue()), Integer.toString(index)
        });
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
    public long getHDAccountConfirmedBanlance(int hdAccountId) {
        long sum = 0;
        String unspendOutSql = "select ifnull(sum(a.out_value),0) sum from outs a,txs b where a.tx_hash=b.tx_hash " +
                "  and a.out_status=? and a.hd_account_id=? and b.block_no is not null";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql,
                new String[]{Integer.toString(Out.OutStatus.unspent.getValue()), Integer.toString(hdAccountId)});

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
    public List<Tx> getHDAccountUnconfirmedTx() {
        List<Tx> txList = new ArrayList<Tx>();

        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select * from txs where tx_hash in" +
                    inQueryTxHashOfHDAccount +
                    " and  block_no is null " +
                    " order by block_no desc";
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
                    " from ins b, txs c " +
                    " where c.tx_hash in " +
                    inQueryTxHashOfHDAccount +
                    " and b.tx_hash=c.tx_hash and c.block_no is null  " +
                    " order by b.tx_hash ,b.in_sn";
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
                    " from  outs b, txs c " +
                    " where c.tx_hash in" +
                    inQueryTxHashOfHDAccount +
                    " and b.tx_hash=c.tx_hash and c.block_no is null  " +
                    " order by b.tx_hash,b.out_sn";
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
    public void updateSyncdComplete(HDAccount.HDAccountAddress address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountAddressesColumns.IS_SYNCED, address.isSyncedComplete() ? 1 : 0);
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
        cursor.close();
        return cnt;
    }

    @Override
    public List<Tx> getRecentlyTxsByAccount(int greateThanBlockNo, int limit) {
        List<Tx> txItemList = new ArrayList<Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from txs  where  tx_hash in " +
                inQueryTxHashOfHDAccount +
                " and ((block_no is null) or (block_no is not null and block_no>?)) " +
                " order by ifnull(block_no,4294967295) desc, tx_time desc " +
                " limit ? ";
        Cursor c = db.rawQuery(sql, new String[]{Integer.toString(greateThanBlockNo), Integer.toString(limit)});
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
    public long sentFromAccount(int hdAccountId, byte[] txHash) {
        String sql = "select  sum(o.out_value) out_value from ins i,outs o where" +
                " i.tx_hash=? and o.tx_hash=i.prev_tx_hash and i.prev_out_sn=o.out_sn and o.hd_account_id=?";
        long sum = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor;
        cursor = db.rawQuery(sql, new String[]{Base58.encode(txHash),
                Integer.toString(hdAccountId)});
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
    public List<Tx> getTxAndDetailByHDAccount() {
        List<Tx> txItemList = new ArrayList<Tx>();

        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select * from txs where tx_hash in " +
                    inQueryTxHashOfHDAccount +
                    " order by" +
                    " ifnull(block_no,4294967295) desc  ";
            Cursor c = db.rawQuery(sql, null);
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

    @Override
    public List<Tx> getTxAndDetailByHDAccount(int page) {
        List<Tx> txItemList = new ArrayList<Tx>();

        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select * from txs where tx_hash in " +
                    inQueryTxHashOfHDAccount +
                    " order by" +
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


    @Override
    public int hdAccountTxCount() {
        int result = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count( distinct a.tx_hash) cnt from addresses_txs a ,hd_account_addresses b where a.address=b.address  ";
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
        cv.put(AbstractDb.HDAccountAddressesColumns.IS_SYNCED, hdAccountAddress.isSyncedComplete() ? 1 : 0);
        return cv;
    }
}
