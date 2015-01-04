package net.bither.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.IAddressProvider;

import java.util.ArrayList;
import java.util.List;

public class AddressProvider implements IAddressProvider {

    private static AddressProvider addressProvider = new AddressProvider(BitherApplication.mAddressDbHelper);

    public static AddressProvider getInstance() {
        return addressProvider;
    }

    private SQLiteOpenHelper mDb;


    private AddressProvider(SQLiteOpenHelper db) {
        this.mDb = db;
    }
    @Override
    public List<HDMKeychain> getKeychains() {
        ArrayList<HDMKeychain> result = new ArrayList<HDMKeychain>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from hd_keys";
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            result.add(applyCursor(c));
        }
        c.close();
        return result;
    }

    @Override
    public HDMKeychain getKeychain(int hdKeyId) {
        HDMKeychain result = null;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from hd_keys";
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToNext()) {
            result = applyCursor(c);
        }
        c.close();
        return result;
    }

    @Override
    public List<HDMAddress> getHDMAddress() {
        return null;
    }

    @Override
    public List<Address> getPrivKeyAddresses() {
        return null;
    }

    @Override
    public List<Address> getWatchOnlyAddresses() {
        return null;
    }

    @Override
    public List<Address> getTrashAddresses() {
        return null;
    }

    @Override
    public void addHDMAddress(List<HDMAddress> addresses) {

    }

    @Override
    public int addHDKey(String encryptSeed, String bitherId, String encryptBitherPassword) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContentValues(encryptSeed, bitherId, encryptBitherPassword, cv);
        return (int)db.insert(AbstractDb.Tables.HDKeys, null, cv);
    }

    private void applyContentValues(String encryptSeed, String bitherId, String encryptBitherPassword, ContentValues cv) {
        cv.put(AbstractDb.HDKeysColumns.ENCRYPT_SEED, encryptSeed);
        cv.put(AbstractDb.HDKeysColumns.BITHER_ID, bitherId);
        cv.put(AbstractDb.HDKeysColumns.ENCRYPT_BITHER_PASSWORD, encryptBitherPassword);
    }

    private HDMKeychain applyCursor(Cursor c) {
        int hdKeyId = 1;
        String encryptSeed = "";
        String bitherId = "";
        String encryptBitherPassword = "";
        int idColumn = c.getColumnIndex(AbstractDb.HDKeysColumns.HD_KEY_ID);
        if (idColumn != -1) {
            hdKeyId = c.getInt(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDKeysColumns.ENCRYPT_SEED);
        if (idColumn != -1) {
            encryptSeed = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDKeysColumns.BITHER_ID);
        if (idColumn != -1) {
            bitherId = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDKeysColumns.ENCRYPT_BITHER_PASSWORD);
        if (idColumn != -1) {
            encryptBitherPassword = c.getString(idColumn);
        }
        return new HDMKeychain(hdKeyId, encryptSeed, bitherId, encryptBitherPassword);

    }
}
