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

import android.database.sqlite.SQLiteOpenHelper;

import com.google.common.base.Function;

import net.bither.BitherApplication;
import net.bither.bitherj.db.imp.AbstractBlockProvider;
import net.bither.bitherj.db.imp.base.ICursor;
import net.bither.bitherj.db.imp.base.IDb;
import net.bither.db.base.AndroidCursor;
import net.bither.db.base.AndroidDb;

public class Block2Provider extends AbstractBlockProvider {
    private static Block2Provider blockProvider = new Block2Provider(BitherApplication.mTxDbHelper);

    public static Block2Provider getInstance() {
        return blockProvider;
    }

    private SQLiteOpenHelper helper;

    public Block2Provider(SQLiteOpenHelper helper) {
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
    public void execUpdate(String sql, String[] params) {
        AndroidDb mdb = (AndroidDb)this.getWriteDb();
        if (params == null) {
            params = new String[] {};
        }
        mdb.getSQLiteDatabase().execSQL(sql, params);
    }

    @Override
    public void execQueryOneRecord(String sql, String[] params, Function<ICursor, Void> func) {
        AndroidDb mdb = (AndroidDb)this.getReadDb();
        ICursor c = new AndroidCursor(mdb.getSQLiteDatabase().rawQuery(sql, params));
        if (c.moveToNext()) {
            func.apply(c);
        }
        c.close();
    }

    @Override
    public void execQueryLoop(String sql, String[] params, Function<ICursor, Void> func) {
        AndroidDb mdb = (AndroidDb)this.getReadDb();
        ICursor c = new AndroidCursor(mdb.getSQLiteDatabase().rawQuery(sql, params));
        while (c.moveToNext()) {
            func.apply(c);
        }
        c.close();
    }

    @Override
    public void execUpdate(IDb db, String sql, String[] params) {
        AndroidDb mdb = (AndroidDb)db;
        if (params == null) {
            params = new String[] {};
        }
        mdb.getSQLiteDatabase().execSQL(sql, params);
    }

    @Override
    public void execQueryOneRecord(IDb db, String sql, String[] params, Function<ICursor, Void> func) {
        AndroidDb mdb = (AndroidDb)db;
        ICursor c = new AndroidCursor(mdb.getSQLiteDatabase().rawQuery(sql, params));
        if (c.moveToNext()) {
            func.apply(c);
        }
        c.close();
    }

    @Override
    public void execQueryLoop(IDb db, String sql, String[] params, Function<ICursor, Void> func) {
        AndroidDb mdb = (AndroidDb)db;
        ICursor c = new AndroidCursor(mdb.getSQLiteDatabase().rawQuery(sql, params));
        while (c.moveToNext()) {
            func.apply(c);
        }
        c.close();
    }
}
