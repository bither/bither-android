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

public class HDAccountDatabaseHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    private static final String DB_NAME = "hd.db";

    public HDAccountDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createHDAccount(db);
        createAccountAddress(db);
        createAccountTxs(db);
        createAccountIns(db);
        createAccountOuts(db);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void createHDAccount(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT);

    }

    private void createAccountAddress(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_ADDRESSES);
    }

    private void createAccountTxs(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_TX);
    }

    private void createAccountOuts(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_OUT);
    }

    private void createAccountIns(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT_IN);
    }
}
