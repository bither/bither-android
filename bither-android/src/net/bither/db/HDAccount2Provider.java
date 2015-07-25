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
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.imp.AbstractHDAccountProvider;
import net.bither.bitherj.db.imp.base.ICursor;
import net.bither.bitherj.db.imp.base.IDb;
import net.bither.bitherj.utils.Base58;
import net.bither.db.base.AndroidCursor;
import net.bither.db.base.AndroidDb;

public class HDAccount2Provider extends AbstractHDAccountProvider {
    private static HDAccount2Provider hdAccountProvider = new HDAccount2Provider(BitherApplication.mAddressDbHelper);

    public static HDAccount2Provider getInstance() {
        return hdAccountProvider;
    }

    private SQLiteOpenHelper helper;

    public HDAccount2Provider(SQLiteOpenHelper helper) {
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
    protected int insertHDAccountToDb(IDb db, String encryptedMnemonicSeed, String encryptSeed, String firstAddress, boolean isXrandom, byte[] externalPub, byte[] internalPub) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountColumns.ENCRYPT_SEED, encryptSeed);
        cv.put(AbstractDb.HDAccountColumns.ENCRYPT_MNMONIC_SEED, encryptedMnemonicSeed);
        cv.put(AbstractDb.HDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.HDAccountColumns.HD_ADDRESS, firstAddress);
        cv.put(AbstractDb.HDAccountColumns.EXTERNAL_PUB, Base58.encode(externalPub));
        cv.put(AbstractDb.HDAccountColumns.INTERNAL_PUB, Base58.encode(internalPub));
        return  (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.HD_ACCOUNT, null, cv);
    }

    @Override
    protected int insertMonitorHDAccountToDb(IDb db, String firstAddress, boolean isXrandom, byte[] externalPub, byte[] internalPub) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountColumns.HD_ADDRESS, firstAddress);
        cv.put(AbstractDb.HDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.HDAccountColumns.EXTERNAL_PUB, Base58.encode(externalPub));
        cv.put(AbstractDb.HDAccountColumns.INTERNAL_PUB, Base58.encode(internalPub));
        return (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.HD_ACCOUNT, null, cv);
    }

    @Override
    protected boolean hasPasswordSeed(IDb db) {
        return Address2Provider.getInstance().hasPasswordSeed(db);
    }

    @Override
    protected void addPasswordSeed(IDb db, PasswordSeed passwordSeed) {
        Address2Provider.getInstance().addPasswordSeed(db, passwordSeed);
    }
}
