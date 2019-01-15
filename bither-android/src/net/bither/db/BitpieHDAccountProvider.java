package net.bither.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.imp.AbstractBitpieHDAccountProvider;
import net.bither.bitherj.db.imp.base.IDb;
import net.bither.bitherj.utils.Base58;
import net.bither.db.base.AndroidDb;

public class BitpieHDAccountProvider extends AbstractBitpieHDAccountProvider {

    private static BitpieHDAccountProvider bitpieHdAccountProvider =
            new BitpieHDAccountProvider(BitherApplication.mAddressDbHelper);

    public static BitpieHDAccountProvider getInstance() {
        return bitpieHdAccountProvider;
    }

    private SQLiteOpenHelper helper;
    public BitpieHDAccountProvider(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    @Override
    protected int insertHDAccountToDb(IDb db, String encryptedMnemonicSeed, String encryptSeed,
                                      String firstAddress, boolean isXrandom,
                                      byte[] externalPub, byte[] internalPub) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountColumns.ENCRYPT_SEED, encryptSeed);
        cv.put(AbstractDb.HDAccountColumns.ENCRYPT_MNMONIC_SEED, encryptedMnemonicSeed);
        cv.put(AbstractDb.HDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.HDAccountColumns.HD_ADDRESS, firstAddress);
        cv.put(AbstractDb.HDAccountColumns.EXTERNAL_PUB, Base58.encode(externalPub));
        cv.put(AbstractDb.HDAccountColumns.INTERNAL_PUB, Base58.encode(internalPub));
        return  (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.BITPIE_HD_ACCOUNT, null, cv);
    }

    @Override
    protected boolean hasPasswordSeed(IDb db) {
        return AddressProvider.getInstance().hasPasswordSeed(db);
    }

    @Override
    protected void addPasswordSeed(IDb db, PasswordSeed passwordSeed) {
        AddressProvider.getInstance().addPasswordSeed(db, passwordSeed);
    }

    @Override
    protected int insertMonitorHDAccountToDb(IDb db, String firstAddress, boolean isXrandom, byte[] externalPub, byte[] internalPub) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountColumns.HD_ADDRESS, firstAddress);
        cv.put(AbstractDb.HDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.HDAccountColumns.EXTERNAL_PUB, Base58.encode(externalPub));
        cv.put(AbstractDb.HDAccountColumns.INTERNAL_PUB, Base58.encode(internalPub));
        return (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.BITPIE_HD_ACCOUNT, null, cv);
    }

    @Override
    protected void insertHDAccountSegwitPubToDb(IDb db, int hdAccountId, byte[] segwitExternalPub, byte[] segwitInternalPub) {

    }

    @Override
    public IDb getReadDb() {
        return new AndroidDb(this.helper.getReadableDatabase());
    }

    @Override
    public IDb getWriteDb() {
        return new AndroidDb(this.helper.getWritableDatabase());
    }
}
