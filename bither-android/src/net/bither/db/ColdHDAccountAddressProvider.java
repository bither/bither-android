//package net.bither.db;
//
//import android.content.ContentValues;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//
//import net.bither.BitherApplication;
//import net.bither.bitherj.crypto.PasswordSeed;
//import net.bither.bitherj.db.AbstractDb;
//import net.bither.bitherj.db.IColdHDAccountAddressProvider;
//import net.bither.bitherj.exception.AddressFormatException;
//import net.bither.bitherj.utils.Base58;
//import net.bither.bitherj.utils.Utils;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ColdHDAccountAddressProvider implements IColdHDAccountAddressProvider {
//
//    private static ColdHDAccountAddressProvider coldHDAccountAddressProvider = new
//            ColdHDAccountAddressProvider(BitherApplication.mAddressDbHelper);
//
//    public static ColdHDAccountAddressProvider getInstance() {
//        return coldHDAccountAddressProvider;
//    }
//
//    private SQLiteOpenHelper mDb;
//
//
//    private ColdHDAccountAddressProvider(SQLiteOpenHelper db) {
//        this.mDb = db;
//    }
//
//    @Override
//    public int addMonitoredHDAccount(boolean isXrandom, byte[] externalPub, byte[] internalPub) {
//        SQLiteDatabase db = this.mDb.getWritableDatabase();
//        db.beginTransaction();
//        ContentValues cv = new ContentValues();
//        cv.put(AbstractDb.ColdHDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
//        cv.put(AbstractDb.ColdHDAccountColumns.EXTERNAL_PUB, Base58.encode(externalPub));
//        cv.put(AbstractDb.ColdHDAccountColumns.INTERNAL_PUB, Base58.encode(internalPub));
//        int seedId = (int) db.insert(AbstractDb.Tables.COLD_HD_ACCOUNT, null, cv);
//        db.setTransactionSuccessful();
//        db.endTransaction();
//        return seedId;
//    }
//
//    @Override
//    public int addHDAccount(String encryptedMnemonicSeed, String encryptSeed, String
//            firstAddress, boolean isXrandom, String addressOfPS, byte[] externalPub, byte[]
//                                    internalPub) {
//        SQLiteDatabase db = this.mDb.getWritableDatabase();
//        db.beginTransaction();
//        ContentValues cv = new ContentValues();
//        cv.put(AbstractDb.ColdHDAccountColumns.ENCRYPT_SEED, encryptSeed);
//        cv.put(AbstractDb.ColdHDAccountColumns.ENCRYPT_MNMONIC_SEED, encryptedMnemonicSeed);
//        cv.put(AbstractDb.ColdHDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
//        cv.put(AbstractDb.ColdHDAccountColumns.HD_ADDRESS, firstAddress);
//        cv.put(AbstractDb.ColdHDAccountColumns.EXTERNAL_PUB, Base58.encode(externalPub));
//        cv.put(AbstractDb.ColdHDAccountColumns.INTERNAL_PUB, Base58.encode(internalPub));
//        int seedId = (int) db.insert(AbstractDb.Tables.COLD_HD_ACCOUNT, null, cv);
//        if (!AddressProvider.getInstance().hasPasswordSeed(db) && !Utils.isEmpty(addressOfPS)) {
//            AddressProvider.getInstance().addPasswordSeed(db, new PasswordSeed(addressOfPS,
//                    encryptedMnemonicSeed));
//        }
//        db.setTransactionSuccessful();
//        db.endTransaction();
//        return seedId;
//    }
//
//    @Override
//    public String getHDFristAddress(int hdSeedId) {
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        Cursor cursor = db.rawQuery("select hd_address from cold_hd_account where hd_account_id=?"
//                , new String[]{Integer.toString(hdSeedId)});
//        String address = null;
//        if (cursor.moveToNext()) {
//            int idColumn = cursor.getColumnIndex(AbstractDb.ColdHDAccountColumns.HD_ADDRESS);
//            if (idColumn != -1) {
//                address = cursor.getString(idColumn);
//            }
//        }
//        cursor.close();
//        return address;
//    }
//
//    @Override
//    public byte[] getExternalPub(int hdSeedId) {
//        byte[] pub = null;
//        try {
//            SQLiteDatabase db = this.mDb.getReadableDatabase();
//            Cursor c = db.rawQuery("select external_pub from cold_hd_account where hd_account_id=? "
//                    , new String[]{Integer.toString(hdSeedId)});
//            if (c.moveToNext()) {
//                int idColumn = c.getColumnIndex(AbstractDb.ColdHDAccountColumns.EXTERNAL_PUB);
//                if (idColumn != -1) {
//                    String pubStr = c.getString(idColumn);
//                    pub = Base58.decode(pubStr);
//                }
//            }
//            c.close();
//        } catch (AddressFormatException e) {
//            e.printStackTrace();
//        }
//
//        return pub;
//    }
//
//    @Override
//    public byte[] getInternalPub(int hdSeedId) {
//        byte[] pub = null;
//        try {
//            SQLiteDatabase db = this.mDb.getReadableDatabase();
//            Cursor c = db.rawQuery("select internal_pub from cold_hd_account where hd_account_id=? "
//                    , new String[]{Integer.toString(hdSeedId)});
//            if (c.moveToNext()) {
//                int idColumn = c.getColumnIndex(AbstractDb.ColdHDAccountColumns.INTERNAL_PUB);
//                if (idColumn != -1) {
//                    String pubStr = c.getString(idColumn);
//                    pub = Base58.decode(pubStr);
//                }
//            }
//            c.close();
//        } catch (AddressFormatException e) {
//            e.printStackTrace();
//        }
//
//
//        return pub;
//    }
//
//    @Override
//    public String getHDAccountEncryptSeed(int hdSeedId) {
//        String hdAccountEncryptSeed = null;
//
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        Cursor c = db.rawQuery("select " + AbstractDb.ColdHDAccountColumns.ENCRYPT_SEED + " from " +
//                "cold_hd_account where hd_account_id=? "
//                , new String[]{Integer.toString(hdSeedId)});
//        if (c.moveToNext()) {
//            int idColumn = c.getColumnIndex(AbstractDb.ColdHDAccountColumns.ENCRYPT_SEED);
//            if (idColumn != -1) {
//                hdAccountEncryptSeed = c.getString(idColumn);
//            }
//        }
//        c.close();
//        return hdAccountEncryptSeed;
//    }
//
//    @Override
//    public String getHDAccountEncryptMnmonicSeed(int hdSeedId) {
//        String hdAccountMnmonicEncryptSeed = null;
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        Cursor c = db.rawQuery("select " + AbstractDb.ColdHDAccountColumns.ENCRYPT_MNMONIC_SEED +
//                " from cold_hd_account where hd_account_id=? "
//                , new String[]{Integer.toString(hdSeedId)});
//        if (c.moveToNext()) {
//            int idColumn = c.getColumnIndex(AbstractDb.ColdHDAccountColumns.ENCRYPT_MNMONIC_SEED);
//            if (idColumn != -1) {
//                hdAccountMnmonicEncryptSeed = c.getString(idColumn);
//            }
//        }
//        c.close();
//        return hdAccountMnmonicEncryptSeed;
//    }
//
//    @Override
//    public boolean hdAccountIsXRandom(int seedId) {
//        boolean result = false;
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        String sql = "select is_xrandom from cold_hd_account where hd_account_id=?";
//        Cursor cursor = db.rawQuery(sql, new String[]{Integer.toString(seedId)});
//        if (cursor.moveToNext()) {
//            int idColumn = cursor.getColumnIndex(AbstractDb.ColdHDAccountColumns.IS_XRANDOM);
//            if (idColumn != -1) {
//                result = cursor.getInt(idColumn) == 1;
//            }
//        }
//        cursor.close();
//        return result;
//    }
//
//    @Override
//    public List<Integer> getHDAccountSeeds() {
//        List<Integer> hdSeedIds = new ArrayList<Integer>();
//        Cursor c = null;
//        try {
//            SQLiteDatabase db = this.mDb.getReadableDatabase();
//            String sql = "select " + AbstractDb.ColdHDAccountColumns.HD_ACCOUNT_ID + " from " +
//                    AbstractDb.Tables.COLD_HD_ACCOUNT;
//            c = db.rawQuery(sql, null);
//            while (c.moveToNext()) {
//                hdSeedIds.add(c.getInt(0));
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            if (c != null) {
//                c.close();
//            }
//        }
//        return hdSeedIds;
//    }
//
//    @Override
//    public boolean hasHDAccountCold() {
//
//        boolean result = false;
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        String sql = "select count(hd_address) cnt from cold_hd_account where  encrypt_seed is not " +
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
//}
