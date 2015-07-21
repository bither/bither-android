package net.bither.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.common.base.Function;

import net.bither.BitherApplication;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.imp.AbstractAddressProvider;
import net.bither.bitherj.db.imp.base.ICursor;
import net.bither.bitherj.db.imp.base.IDb;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;
import net.bither.db.base.AndroidCursor;
import net.bither.db.base.AndroidDb;

public class Address2Provider extends AbstractAddressProvider {
    private static Address2Provider addressProvider = new Address2Provider(BitherApplication.mAddressDbHelper);

    public static Address2Provider getInstance() {
        return addressProvider;
    }

    private SQLiteOpenHelper helper;

    public Address2Provider(SQLiteOpenHelper helper) {
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

    @Override
    protected int insertHDKeyToDb(IDb db, String encryptedMnemonicSeed, String encryptHdSeed, String firstAddress, boolean isXrandom) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDSeedsColumns.ENCRYPT_MNEMONIC_SEED, encryptedMnemonicSeed);
        cv.put(AbstractDb.HDSeedsColumns.ENCRYPT_HD_SEED, encryptHdSeed);
        cv.put(AbstractDb.HDSeedsColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.HDSeedsColumns.HDM_ADDRESS, firstAddress);
        return (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.HDSEEDS, null, cv);
    }

    @Override
    protected int insertEnterpriseHDKeyToDb(IDb db, String encryptedMnemonicSeed, String encryptHdSeed, String firstAddress, boolean isXrandom) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.EnterpriseHDAccountColumns.ENCRYPT_MNEMONIC_SEED, encryptedMnemonicSeed);
        cv.put(AbstractDb.EnterpriseHDAccountColumns.ENCRYPT_SEED, encryptHdSeed);
        cv.put(AbstractDb.EnterpriseHDAccountColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.EnterpriseHDAccountColumns.HD_ADDRESS, firstAddress);
        return (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.ENTERPRISE_HD_ACCOUNT, null, cv);
    }

    @Override
    protected void insertHDMAddressToDb(IDb db, String address, int hdSeedId, int index, byte[] pubKeysHot, byte[] pubKeysCold, byte[] pubKeysRemote, boolean isSynced) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDMAddressesColumns.HD_SEED_ID, hdSeedId);
        cv.put(AbstractDb.HDMAddressesColumns.HD_SEED_INDEX, index);
        cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY_HOT, Base58.encode(pubKeysHot));
        cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY_COLD, Base58.encode(pubKeysCold));
        if (Utils.isEmpty(address)) {
            cv.putNull(AbstractDb.HDMAddressesColumns.ADDRESS);
        } else {
            cv.put(AbstractDb.HDMAddressesColumns.ADDRESS, address);
        }
        if (pubKeysRemote == null) {
            cv.putNull(AbstractDb.HDMAddressesColumns.PUB_KEY_REMOTE);
        } else {
            cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY_REMOTE, Base58.encode(pubKeysRemote));
        }
        cv.put(AbstractDb.HDMAddressesColumns.IS_SYNCED, isSynced ? 1 : 0);
        mdb.getSQLiteDatabase().insert(AbstractDb.Tables.HDMADDRESSES, null, cv);
    }

    @Override
    protected void insertAddressToDb(IDb db, Address address) {
        AndroidDb mdb = (AndroidDb)db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.AddressesColumns.ADDRESS, address.getAddress());
        if (address.hasPrivKey()) {
            cv.put(AbstractDb.AddressesColumns.ENCRYPT_PRIVATE_KEY, address.getEncryptPrivKeyOfDb());
        }
        cv.put(AbstractDb.AddressesColumns.PUB_KEY, Base58.encode(address.getPubKey()));
        cv.put(AbstractDb.AddressesColumns.IS_XRANDOM, address.isFromXRandom() ? 1 : 0);
        cv.put(AbstractDb.AddressesColumns.IS_SYNCED, address.isSyncComplete() ? 1 : 0);
        cv.put(AbstractDb.AddressesColumns.IS_TRASH, address.isTrashed() ? 1 : 0);
        cv.put(AbstractDb.AddressesColumns.SORT_TIME, address.getSortTime());
        mdb.getSQLiteDatabase().insert(AbstractDb.Tables.Addresses, null, cv);
    }
}
