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

import com.google.common.base.Function;

import net.bither.bitherj.db.imp.base.ICursor;
import net.bither.bitherj.db.imp.base.IDb;

public class AndroidDb implements IDb {

    private SQLiteDatabase sqliteDatabase;

    public AndroidDb(SQLiteDatabase sqliteDatabase) {
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

    @Override
    public void execUpdate(String sql, String[] params) {
        if (params == null) {
            params = new String[] {};
        }
        this.getSQLiteDatabase().execSQL(sql, params);
    }

    @Override
    public void execQueryOneRecord(String sql, String[] params, Function<ICursor, Void> func) {
        ICursor c = new AndroidCursor(this.getSQLiteDatabase().rawQuery(sql, params));
        if (c.moveToNext()) {
            func.apply(c);
        }
        c.close();
    }

    @Override
    public void execQueryLoop(String sql, String[] params, Function<ICursor, Void> func) {
        ICursor c = new AndroidCursor(this.getSQLiteDatabase().rawQuery(sql, params));
        while (c.moveToNext()) {
            func.apply(c);
        }
        c.close();
    }

    public SQLiteDatabase getSQLiteDatabase() {
        return this.sqliteDatabase;
    }
}
