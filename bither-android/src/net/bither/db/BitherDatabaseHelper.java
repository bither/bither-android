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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.bitherj.db.AbstractDb;

public class BitherDatabaseHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    private static final String DB_NAME = "bitherj.db";

    public BitherDatabaseHelper(Context context) {
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

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    private void createBlocksTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_BLOCKS_SQL);
        db.execSQL(AbstractDb.CREATE_BLOCK_NO_INDEX);
        db.execSQL(AbstractDb.CREATE_BLOCK_PREV_INDEX);
    }

    private void createTxsTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_TXS_SQL);
    }

    private void createAddressTxsTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_ADDRESSTXS_SQL);

    }

    private void createInsTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_INS_SQL);
    }

    private void createOutsTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_OUTS_SQL);

    }

    private void createPeersTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_PEER_SQL);
    }
}
