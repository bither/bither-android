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

package net.bither.db.base;

import android.database.sqlite.SQLiteDatabase;

import net.bither.bitherj.db.imp.base.IDb;

public class Db implements IDb {

    private SQLiteDatabase sqliteDatabase;

    public Db(SQLiteDatabase sqliteDatabase) {
        this.sqliteDatabase = sqliteDatabase;
    }

    @Override
    public void beginTransaction() {
        this.sqliteDatabase.beginTransaction();
    }

    @Override
    public void endTransaction() {
        this.sqliteDatabase.setTransactionSuccessful();
        this.sqliteDatabase.endTransaction();
    }

    @Override
    public void close() {
        this.sqliteDatabase.close();
    }

    public SQLiteDatabase getSQLiteDatabase() {
        return this.sqliteDatabase;
    }
}
