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
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.core.EnterpriseHDMAddress;
import net.bither.bitherj.core.EnterpriseHDMKeychain;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.IEnterpriseHDMProvider;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;

import java.util.ArrayList;
import java.util.List;

public class EnterpriseHDMProvider implements IEnterpriseHDMProvider {
    private SQLiteOpenHelper mDb;

    private static EnterpriseHDMProvider enterpriseHDMProvider = new EnterpriseHDMProvider(
            BitherApplication.mDbHelper);

    public static EnterpriseHDMProvider getInstance() {
        return enterpriseHDMProvider;
    }

    private EnterpriseHDMProvider(SQLiteOpenHelper hdAccountDB) {
        this.mDb = hdAccountDB;
    }


    @Override
    public void addEnterpriseHDMAddress(List<EnterpriseHDMAddress> enterpriseHDMAddressList) {

        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        for (EnterpriseHDMAddress enterpriseHDMAddress : enterpriseHDMAddressList) {
            ContentValues cv = applyContentValues(enterpriseHDMAddress);
            db.insert(AbstractDb.Tables.ENTERPRISE_HDM_ADDRESS, null, cv);
        }
        db.setTransactionSuccessful();
        db.endTransaction();

    }

    private ContentValues applyContentValues(EnterpriseHDMAddress enterpriseHDMAddress) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(AbstractDb.EnterpriseHDMAddressColumns.HDM_INDEX, enterpriseHDMAddress.getIndex());
        contentValues.put(AbstractDb.EnterpriseHDMAddressColumns.ADDRESS, enterpriseHDMAddress.getAddress());
        contentValues.put(AbstractDb.EnterpriseHDMAddressColumns.IS_SYNCED, enterpriseHDMAddress.isSyncComplete() ? 1 : 0);

        return contentValues;

    }

    @Override
    public void updateSyncComplete(EnterpriseHDMAddress enterpriseHDMAddress) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.EnterpriseHDMAddressColumns.IS_SYNCED, enterpriseHDMAddress.isSyncComplete() ? 1 : 0);
        db.update(AbstractDb.Tables.ENTERPRISE_HDM_ADDRESS, cv, AbstractDb.EnterpriseHDMAddressColumns.ADDRESS + "=?"
                , new String[]{enterpriseHDMAddress.getAddress()});

    }

    @Override
    public void addMultiSignSet(int n, int m) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.EnterpriseMultiSignSetColumns.MultiSignN, n);
        cv.put(AbstractDb.EnterpriseMultiSignSetColumns.MultiSignM, m);
        db.insert(AbstractDb.Tables.ENTERPRISE_MULTI_SIGN_SET, null, cv);

    }

    @Override
    public List<EnterpriseHDMAddress> getEnterpriseHDMAddress(EnterpriseHDMKeychain keychain) {
        List<EnterpriseHDMAddress> enterpriseHDMAddressList = new ArrayList<EnterpriseHDMAddress>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        int threshold = 0;

        Cursor cursor = db.rawQuery("select multi_sign_n from enterprise_multi_sign_set", null);
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.EnterpriseMultiSignSetColumns.MultiSignN);
            if (idColumn != -1) {
                threshold = cursor.getInt(idColumn);
            }
        }
        cursor.close();
        cursor = db.rawQuery("select * from " + AbstractDb.Tables.ENTERPRISE_HDM_ADDRESS + " order by hdm_index asc ",
                null);
        try {
            while (cursor.moveToNext()) {
                enterpriseHDMAddressList.add(format(cursor, threshold, keychain));
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        cursor.close();

        return enterpriseHDMAddressList;


    }


    private EnterpriseHDMAddress format(Cursor c, int threshold, EnterpriseHDMKeychain keychain)
            throws AddressFormatException {
        String address = null;
        boolean isSync = false;
        int index = 0;
        List<byte[]> bytes = new ArrayList<byte[]>();

        int idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.HDM_INDEX);
        if (idColumn != -1) {
            index = c.getInt(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.ADDRESS);
        if (idColumn != -1) {
            address = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.IS_SYNCED);
        if (idColumn != -1) {
            isSync = c.getInt(idColumn) == 1;
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_0);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_1);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_2);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_3);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_4);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_5);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_6);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_7);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_8);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        idColumn = c.getColumnIndex(AbstractDb.EnterpriseHDMAddressColumns.PUB_KEY_9);
        if (idColumn != -1) {
            String pub = c.getString(idColumn);
            bytes.add(Base58.decode(pub));
        }
        EnterpriseHDMAddress.Pubs pubs = new EnterpriseHDMAddress.Pubs(index, threshold, bytes);
        EnterpriseHDMAddress enterpriseHDMAddress = new EnterpriseHDMAddress
                (pubs, address, keychain, isSync);
        return enterpriseHDMAddress;


    }
}
