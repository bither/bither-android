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

import net.bither.bitherj.core.In;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class TxHelper {
    private TxHelper() {

    }

    public static void insertTx(SQLiteDatabase db, Tx txItem) {
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

    public static void applyContentValues(Tx txItem, ContentValues cv) {
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

    public static void applyContentValues(In inItem, ContentValues cv) {
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

    public static void applyContentValues(Out outItem, ContentValues cv) {
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
        //support hd
        if (outItem.getOutType() != Out.OutType.NORMAL) {
            cv.put(AbstractDb.OutsColumns.BELONG_ACCOUNT,
                    outItem.getOutType() == Out.OutType.BELONG_HD_ACCOUNT ? 1 : 0);
        }
    }

    public static List<AddressTx> insertOut(SQLiteDatabase db, Tx txItem) {
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

    public static List<AddressTx> insertIn(SQLiteDatabase db, Tx txItem) {
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


    public static Tx applyCursor(Cursor c) throws AddressFormatException {
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

    public static In applyCursorIn(Cursor c) throws AddressFormatException {
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

    public static Out applyCursorOut(Cursor c) throws AddressFormatException {
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
        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.BELONG_ACCOUNT);
        if (idColumn == -1) {
            outItem.setOutType(Out.OutType.NORMAL);
        } else {
            int outType = c.getInt(idColumn);
            if (outType == 1) {
                outItem.setOutType(Out.OutType.BELONG_HD_ACCOUNT);
            } else {
                outItem.setOutType(Out.OutType.NO_BELONG_HD_ACCOUNT);
            }
        }
        return outItem;
    }

    public static void addInsAndOuts(SQLiteDatabase db, Tx txItem) throws AddressFormatException {
        String txHashStr = Base58.encode(txItem.getTxHash());
        txItem.setOuts(new ArrayList<Out>());
        txItem.setIns(new ArrayList<In>());
        String sql = "select * from ins where tx_hash=? order by in_sn";
        Cursor c = db.rawQuery(sql, new String[]{txHashStr});
        while (c.moveToNext()) {
            In inItem = TxHelper.applyCursorIn(c);
            inItem.setTx(txItem);
            txItem.getIns().add(inItem);
        }
        c.close();

        sql = "select * from outs where tx_hash=? order by out_sn";
        c = db.rawQuery(sql, new String[]{txHashStr});
        while (c.moveToNext()) {
            Out outItem = TxHelper.applyCursorOut(c);
            outItem.setTx(txItem);
            txItem.getOuts().add(outItem);
        }
        c.close();
    }

    public static class AddressTx {
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
