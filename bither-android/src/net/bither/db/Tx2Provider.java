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
import android.database.sqlite.SQLiteOpenHelper;

import com.google.common.base.Function;

import net.bither.BitherApplication;
import net.bither.bitherj.core.In;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.imp.AbstractTxProvider;
import net.bither.bitherj.db.imp.base.ICursor;
import net.bither.bitherj.db.imp.base.IDb;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;
import net.bither.db.base.AndroidCursor;
import net.bither.db.base.AndroidDb;

public class Tx2Provider extends AbstractTxProvider {

    private static Tx2Provider txProvider = new Tx2Provider(BitherApplication.mTxDbHelper);

    public static Tx2Provider getInstance() {
        return txProvider;
    }

    private SQLiteOpenHelper helper;

    public Tx2Provider(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    @Override
    public IDb getReadDb() {
        return new AndroidDb(this.helper.getReadableDatabase());
    }

    @Override
    public IDb getWriteDb() {
        return new AndroidDb(this.helper.getWritableDatabase());
    }

    @Override
    protected void insertTxToDb(IDb db, Tx tx) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        if (tx.getBlockNo() != Tx.TX_UNCONFIRMED) {
            cv.put(AbstractDb.TxsColumns.BLOCK_NO, tx.getBlockNo());
        } else {
            cv.putNull(AbstractDb.TxsColumns.BLOCK_NO);
        }
        cv.put(AbstractDb.TxsColumns.TX_HASH, Base58.encode(tx.getTxHash()));
        cv.put(AbstractDb.TxsColumns.SOURCE, tx.getSource());
        cv.put(AbstractDb.TxsColumns.TX_TIME, tx.getTxTime());
        cv.put(AbstractDb.TxsColumns.TX_VER, tx.getTxVer());
        cv.put(AbstractDb.TxsColumns.TX_LOCKTIME, tx.getTxLockTime());
        mdb.getSQLiteDatabase().insert(AbstractDb.Tables.TXS, null, cv);
    }

    @Override
    protected void insertInToDb(IDb db, In in) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.InsColumns.TX_HASH, Base58.encode(in.getTxHash()));
        cv.put(AbstractDb.InsColumns.IN_SN, in.getInSn());
        cv.put(AbstractDb.InsColumns.PREV_TX_HASH, Base58.encode(in.getPrevTxHash()));
        cv.put(AbstractDb.InsColumns.PREV_OUT_SN, in.getPrevOutSn());
        if (in.getInSignature() != null) {
            cv.put(AbstractDb.InsColumns.IN_SIGNATURE, Base58.encode(in.getInSignature()));
        } else {
            cv.putNull(AbstractDb.InsColumns.IN_SIGNATURE);
        }
        cv.put(AbstractDb.InsColumns.IN_SEQUENCE, in.getInSequence());
        mdb.getSQLiteDatabase().insert(AbstractDb.Tables.INS, null, cv);
    }

    @Override
    protected void insertOutToDb(IDb db, Out out) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.OutsColumns.TX_HASH, Base58.encode(out.getTxHash()));
        cv.put(AbstractDb.OutsColumns.OUT_SN, out.getOutSn());
        cv.put(AbstractDb.OutsColumns.OUT_SCRIPT, Base58.encode(out.getOutScript()));
        cv.put(AbstractDb.OutsColumns.OUT_VALUE, out.getOutValue());
        cv.put(AbstractDb.OutsColumns.OUT_STATUS, out.getOutStatus().getValue());
        if (!Utils.isEmpty(out.getOutAddress())) {
            cv.put(AbstractDb.OutsColumns.OUT_ADDRESS, out.getOutAddress());
        } else {
            cv.putNull(AbstractDb.OutsColumns.OUT_ADDRESS);
        }
        //support hd
        if (out.getHDAccountId() != -1) {
            cv.put(AbstractDb.OutsColumns.HD_ACCOUNT_ID,
                    out.getHDAccountId());
        } else {
            cv.putNull(AbstractDb.OutsColumns.HD_ACCOUNT_ID);
        }
        mdb.getSQLiteDatabase().insert(AbstractDb.Tables.OUTS, null, cv);
    }
}
