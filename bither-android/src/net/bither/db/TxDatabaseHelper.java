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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.db.AbstractDb;

public class TxDatabaseHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 3;
    private static final String DB_NAME = "bitherj.db";

    public TxDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createBlocksTable(db);
        createTxsTable(db);
        createAddressTxsTable(db);
        createInsTable(db);
        createOutsTable(db);
        createPeersTable(db);
        createHDAccountAddress(db);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_ACCOUNT_ID_AND_PATH_TYPE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                v1Tov2(db);
            case 2:
                v2Tov3(db);

        }
    }


    private void createBlocksTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_BLOCKS_SQL);
        db.execSQL(AbstractDb.CREATE_BLOCK_NO_INDEX);
        db.execSQL(AbstractDb.CREATE_BLOCK_PREV_INDEX);
    }

    private void createTxsTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_TXS_SQL);
        db.execSQL(AbstractDb.CREATE_TX_BLOCK_NO_INDEX);
    }

    private void createAddressTxsTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_ADDRESSTXS_SQL);
    }

    private void createInsTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_INS_SQL);
        db.execSQL(AbstractDb.CREATE_IN_PREV_TX_HASH_INDEX);
    }

    private void createOutsTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_OUTS_SQL);
        db.execSQL(AbstractDb.CREATE_OUT_OUT_ADDRESS_INDEX);
        db.execSQL(AbstractDb.CREATE_OUT_HD_ACCOUNT_ID_INDEX);
    }

    private void createPeersTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_PEER_SQL);
    }

    private void createHDAccountAddress(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_ADDRESSES);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_ADDRESS_INDEX);
    }

//    private void createColdHDAccountAddress(SQLiteDatabase db) {
//        db.execSQL(AbstractDb.CREATE_COLD_HD_ACCOUNT_ADDRESSES);
//        db.execSQL(AbstractDb.CREATE_COLD_HD_ACCOUNT_ADDRESS_INDEX);
//    }


    private void v1Tov2(SQLiteDatabase db) {
        //v1.34
        db.execSQL(AbstractDb.ADD_HD_ACCOUNT_ID_FOR_OUTS);
        createHDAccountAddress(db);
    }

    private void v2Tov3(SQLiteDatabase db) {
        //v1.37
        // add hd_account_id to hd_account_addresses
        Cursor c = db.rawQuery("select count(0) from hd_account_addresses", null);
        int cnt = 0;
        if (c.moveToNext()) {
            cnt = c.getInt(0);
        }
        c.close();

        db.execSQL("create table if not exists " +
                "hd_account_addresses2 " +
                "(hd_account_id integer not null" +
                ", path_type integer not null" +
                ", address_index integer not null" +
                ", is_issued integer not null" +
                ", address text not null" +
                ", pub text not null" +
                ", is_synced integer not null" +
                ", primary key (address));");
        if (cnt > 0) {
            db.execSQL("ALTER TABLE hd_account_addresses ADD COLUMN hd_account_id integer");

            int hd_account_id = -1;
            c = BitherApplication.mAddressDbHelper.getReadableDatabase().rawQuery("select hd_account_id from hd_account", null);
            if (c.moveToNext()) {
                hd_account_id = c.getInt(0);
                if (c.moveToNext()) {
                    c.close();
                    throw new RuntimeException("tx db upgrade from 2 to 3 failed. more than one record in hd_account");
                } else {
                    c.close();
                }
            } else {
                c.close();
                throw new RuntimeException("tx db upgrade from 2 to 3 failed. no record in hd_account");
            }

            db.execSQL("update hd_account_addresses set hd_account_id=?", new String[] {Integer.toString(hd_account_id)});
            db.execSQL("INSERT INTO hd_account_addresses2(hd_account_id,path_type,address_index,is_issued,address,pub,is_synced) " +
                    "SELECT hd_account_id,path_type,address_index,is_issued,address,pub,is_synced FROM hd_account_addresses;");
        }
        int oldCnt = 0;
        int newCnt = 0;
        c = db.rawQuery("select count(0) cnt from hd_account_addresses", null);
        if (c.moveToNext()) {
            oldCnt = c.getInt(0);
        }
        c.close();
        c = db.rawQuery("select count(0) cnt from hd_account_addresses2", null);
        if (c.moveToNext()) {
            newCnt = c.getInt(0);
        }
        c.close();
        if (oldCnt != newCnt) {
            throw new RuntimeException("tx db upgrade from 2 to 3 failed. new hd_account_addresses table record count not the same as old one");
        } else {
            db.execSQL("DROP TABLE hd_account_addresses;");
            db.execSQL("ALTER TABLE hd_account_addresses2 RENAME TO hd_account_addresses;");
        }

        db.execSQL(AbstractDb.CREATE_OUT_HD_ACCOUNT_ID_INDEX);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_ACCOUNT_ID_AND_PATH_TYPE_INDEX);
    }


}
