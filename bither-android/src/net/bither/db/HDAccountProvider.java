package net.bither.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.IHDAccountProvider;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class HDAccountProvider implements IHDAccountProvider {

    private static HDAccountProvider hdAccountProvider = new HDAccountProvider(BitherApplication.mAddressDbHelper);

    public static HDAccountProvider getInstance() {
        return hdAccountProvider;
    }

    private SQLiteOpenHelper mDb;


    private HDAccountProvider(SQLiteOpenHelper db) {
        this.mDb = db;
    }

    @Override
    public String getHDFirstAddress(int hdSeedId) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select hd_address from hd_account where hd_account_id=?"
                , new String[]{Integer.toString(hdSeedId)});
        String address = null;
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDAccountColumns.HD_ADDRESS);
            if (idColumn != -1) {
                address = cursor.getString(idColumn);
            }
        }
        cursor.close();
        return address;
    }

    @Override
    public int addHDAccount(String encryptedMnemonicSeed, String encryptSeed, String firstAddress
            , boolean isXrandom, String addressOfPS, byte[] externalPub
            , byte[] internalPub) {
        if (this.isPubExist(externalPub, internalPub)) {
            return -1;
        }
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountColumns.ENCRYPT_SEED, encryptSeed);
        cv.put(AbstractDb.HDAccountColumns.ENCRYPT_MNMONIC_SEED, encryptedMnemonicSeed);
        cv.put(AbstractDb.HDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.HDAccountColumns.HD_ADDRESS, firstAddress);
        cv.put(AbstractDb.HDAccountColumns.EXTERNAL_PUB, Base58.encode(externalPub));
        cv.put(AbstractDb.HDAccountColumns.INTERNAL_PUB, Base58.encode(internalPub));
        int seedId = (int) db.insert(AbstractDb.Tables.HD_ACCOUNT, null, cv);
        if (!AddressProvider.hasPasswordSeed(db) && !Utils.isEmpty(addressOfPS)) {
            AddressProvider.addPasswordSeed(db, new PasswordSeed(addressOfPS, encryptedMnemonicSeed));
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return seedId;

    }

    @Override
    public int addMonitoredHDAccount(String firstAddress, boolean isXrandom, byte[] externalPub, byte[] internalPub) {
        if (this.isPubExist(externalPub, internalPub)) {
            return -1;
        }
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountColumns.HD_ADDRESS, firstAddress);
        cv.put(AbstractDb.HDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.HDAccountColumns.EXTERNAL_PUB, Base58.encode(externalPub));
        cv.put(AbstractDb.HDAccountColumns.INTERNAL_PUB, Base58.encode(internalPub));
        int seedId = (int) db.insert(AbstractDb.Tables.HD_ACCOUNT, null, cv);
        db.setTransactionSuccessful();
        db.endTransaction();
        return seedId;
    }

//    @Override
//    public boolean hasHDAccountCold() {
//        boolean result = false;
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        String sql = "select count(hd_address) cnt from hd_account where encrypt_seed is not " +
//                "null and encrypt_mnemonic_seed is not null";
//        Cursor cursor = db.rawQuery(sql, null);
//        if (cursor.moveToNext()) {
//            int idColumn = cursor.getColumnIndex("cnt");
//            if (idColumn != -1) {
//                result = cursor.getInt(idColumn) > 0;
//            }
//        }
//        cursor.close();
//        return result;
//    }

    @Override
    public boolean hasMnemonicSeed(int hdAccountId) {
        boolean result = false;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(0) cnt from hd_account where encrypt_mnemonic_seed is not null and hd_account_id=?";
        Cursor cursor = db.rawQuery(sql, new String[] {Integer.toString(hdAccountId)});
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex("cnt");
            if (idColumn != -1) {
                result = cursor.getInt(idColumn) > 0;
            }
        }
        cursor.close();
        return result;
    }

    @Override
    public byte[] getExternalPub(int hdSeedId) {
        byte[] pub = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            Cursor c = db.rawQuery("select external_pub from hd_account where hd_account_id=? "
                    , new String[]{Integer.toString(hdSeedId)});
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex(AbstractDb.HDAccountColumns.EXTERNAL_PUB);
                if (idColumn != -1) {
                    String pubStr = c.getString(idColumn);
                    pub = Base58.decode(pubStr);
                }
            }
            c.close();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }

        return pub;
    }

    @Override
    public byte[] getInternalPub(int hdSeedId) {
        byte[] pub = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            Cursor c = db.rawQuery("select internal_pub from hd_account where hd_account_id=? "
                    , new String[]{Integer.toString(hdSeedId)});
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex(AbstractDb.HDAccountColumns.INTERNAL_PUB);
                if (idColumn != -1) {
                    String pubStr = c.getString(idColumn);
                    pub = Base58.decode(pubStr);
                }
            }
            c.close();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }


        return pub;
    }


    @Override
    public String getHDAccountEncryptSeed(int hdSeedId) {
        String hdAccountEncryptSeed = null;

        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery("select " + AbstractDb.HDAccountColumns.ENCRYPT_SEED + " from hd_account where hd_account_id=? "
                , new String[]{Integer.toString(hdSeedId)});
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex(AbstractDb.HDAccountColumns.ENCRYPT_SEED);
            if (idColumn != -1) {
                hdAccountEncryptSeed = c.getString(idColumn);
            }
        }
        c.close();
        return hdAccountEncryptSeed;
    }

    @Override
    public String getHDAccountEncryptMnemonicSeed(int hdSeedId) {
        String hdAccountMnmonicEncryptSeed = null;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery("select " + AbstractDb.HDAccountColumns.ENCRYPT_MNMONIC_SEED + " from hd_account where hd_account_id=? "
                , new String[]{Integer.toString(hdSeedId)});
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex(AbstractDb.HDAccountColumns.ENCRYPT_MNMONIC_SEED);
            if (idColumn != -1) {
                hdAccountMnmonicEncryptSeed = c.getString(idColumn);
            }
        }
        c.close();
        return hdAccountMnmonicEncryptSeed;
    }

    @Override
    public boolean hdAccountIsXRandom(int seedId) {
        boolean result = false;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select is_xrandom from hd_account where hd_account_id=?";
        Cursor cursor = db.rawQuery(sql, new String[]{Integer.toString(seedId)});
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDAccountColumns.IS_XRANDOM);
            if (idColumn != -1) {
                result = cursor.getInt(idColumn) == 1;
            }
        }
        cursor.close();
        return result;
    }

    @Override
    public List<Integer> getHDAccountSeeds() {
        List<Integer> hdSeedIds = new ArrayList<Integer>();
        Cursor c = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            String sql = "select " + AbstractDb.HDAccountColumns.HD_ACCOUNT_ID + " from " + AbstractDb.Tables.HD_ACCOUNT;
            c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                hdSeedIds.add(c.getInt(0));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return hdSeedIds;
    }

    @Override
    public boolean isPubExist(byte[] externalPub, byte[] internalPub) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(0) cnt from hd_account where external_pub=? or internal_pub=?";
        Cursor c = db.rawQuery(sql, new String[] {Base58.encode(externalPub), Base58.encode(internalPub)});
        boolean isExist = false;
        if (c.moveToNext()) {
            isExist = c.getInt(0) > 0;
        }
        c.close();
        return isExist;
    }
}
