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
import net.bither.bitherj.db.imp.AbstractHDAccountAddressProvider;
import net.bither.bitherj.db.imp.base.ICursor;
import net.bither.bitherj.db.imp.base.IDb;
import net.bither.db.base.AndroidCursor;
import net.bither.db.base.AndroidDb;

public class HDAccountAddress2Provider extends AbstractHDAccountAddressProvider {
    private static HDAccountAddress2Provider hdAccountAddressProvider = new HDAccountAddress2Provider(BitherApplication.mTxDbHelper);

    public static HDAccountAddress2Provider getInstance() {
        return hdAccountAddressProvider;
    }

    private SQLiteOpenHelper helper;

    public HDAccountAddress2Provider(SQLiteOpenHelper helper) {
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
        this.getWriteDb().execUpdate(sql, params);
    }

    @Override
    public void execQueryOneRecord(String sql, String[] params, Function<ICursor, Void> func) {
        this.getReadDb().execQueryOneRecord(sql, params, func);
    }

    @Override
    public void execQueryLoop(String sql, String[] params, Function<ICursor, Void> func) {
        this.getReadDb().execQueryLoop(sql, params, func);
    }

    @Override
    public void execUpdate(IDb db, String sql, String[] params) {
        db.execUpdate(sql, params);
    }

    @Override
    public void execQueryOneRecord(IDb db, String sql, String[] params, Function<ICursor, Void> func) {
        db.execQueryOneRecord(sql, params, func);
    }

    @Override
    public void execQueryLoop(IDb db, String sql, String[] params, Function<ICursor, Void> func) {
        db.execQueryLoop(sql, params, func);
    }
}
