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
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;

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
    public List<Integer> getHDSeeds() {
        List<Integer> hdSeedIds = new ArrayList<Integer>();
        Cursor c = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            String sql = "select hd_seed_id from hd_seeds";
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
    public String getEncryptSeed(int hdSeedId) {
        String encryptSeed = null;
        Cursor c = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            String sql = "select encrypt_seed from hd_seeds";
            c = db.rawQuery(sql, null);
            if (c.moveToNext()) {
                encryptSeed = c.getString(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return encryptSeed;
    }

    @Override
    public void setEncryptSeed(int hdSeedId, String encryptedSeed) {

    }

    @Override
    public boolean isHDSeedFromXRandom(int hdSeedId) {
        return false;
    }

    @Override
    public int addHDKey(String encryptSeed, boolean isXrandom) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDSeedsColumns.ENCRYPT_SEED, encryptSeed);
        cv.put(AbstractDb.HDSeedsColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        return (int) db.insert(AbstractDb.Tables.HDSeeds, null, cv);
    }

    @Override
    public String getBitherId() {
        String bitherId = null;
        Cursor c = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            String sql = "select bither_id from bither_id";
            c = db.rawQuery(sql, null);
            if (c.moveToNext()) {
                bitherId = c.getString(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return bitherId;
    }

    @Override
    public String getBitherEncryptPassword() {
        String encryptBitherPassword = null;
        Cursor c = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            String sql = "select encrypt_bither_password from bither_id";
            c = db.rawQuery(sql, null);
            if (c.moveToNext()) {
                encryptBitherPassword = c.getString(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return encryptBitherPassword;
    }

    @Override
    public void addBitherId(String bitherId, String encryptBitherPassword) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        boolean isExist = true;
        Cursor c = null;
        try {
            String sql = "select count(0) from bither_id";
            c = db.rawQuery(sql, null);
            if (c.moveToNext()) {
                isExist = c.getInt(0) > 0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        if (!isExist) {
            ContentValues cv = new ContentValues();
            cv.put(AbstractDb.BitherIdColumns.BITHER_ID, bitherId);
            cv.put(AbstractDb.BitherIdColumns.ENCRYPT_BITHER_PASSWORD, encryptBitherPassword);
            db.insert(AbstractDb.Tables.BitherId, null, cv);
        }
    }

    @Override
    public void changeBitherPassword(String encryptBitherPassword) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.BitherIdColumns.ENCRYPT_BITHER_PASSWORD, encryptBitherPassword);
        db.update(AbstractDb.Tables.BitherId, cv, null, null);
    }

    @Override
    public List<HDMAddress> getHDMAddressInUse(HDMKeychain keychain) {
        return null;
    }

    @Override
    public void prepareHDMAddresses(int hdSeedId, List<HDMAddress.Pubs> pubs) {

    }

    @Override
    public ArrayList<HDMAddress.Pubs> getUncompletedHDMAddressPubs(int hdSeedId, int count) {
        return null;
    }

    @Override
    public int maxHDMAddressPubIndex(int hdSeedId) {
        return 0;
    }

    @Override
    public void completeHDMAddresses(int hdSeedId, List<HDMAddress> addresses) {

    }

    @Override
    public int uncompletedHDMAddressCount(int hdSeedId) {
        return 0;
    }


    public List<HDMAddress> getHDMAddressInUse(int hdSeedId) {
        List<HDMAddress> addresses = new ArrayList<HDMAddress>();
        Cursor c = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            String sql = "select hd_seed_index,pub_key1,pub_key2,pub_key3,address,is_synced " +
                    "from hdm_addresses " +
                    "where hd_seed_id=? and address is not null order by hd_seed_index";
            c = db.rawQuery(sql, new String[]{Integer.toString(hdSeedId)});
            while (c.moveToNext()) {
                int hdSeedIndex = c.getInt(0);
                byte[] pub1 = Base58.decode(c.getString(1));
                byte[] pub2 = Base58.decode(c.getString(2));
                byte[] pub3 = Base58.decode(c.getString(3));
                String address = c.getString(4);
                boolean isSynced = c.getInt(5) == 1;
                HDMAddress.Pubs pubs = new HDMAddress.Pubs();
                pubs.index = hdSeedIndex;
                pubs.hot = pub1;
                pubs.cold = pub2;
                pubs.remote = pub3;
                addresses.add(new HDMAddress(pubs, isSynced, null));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return addresses;
    }


    public void addHDMAddress(int hdSeedId, List<Integer> indexes, List<byte[]> pubKeys1, List<byte[]> pubKeys2) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        boolean isExist = false;
        Cursor c = null;
        try {
            for (Integer hdSeedIndex : indexes) {
                String sql = "select count(0) from hdm_addresses where hd_seed_id=? and hd_seed_index=?";
                c = db.rawQuery(sql, new String[]{Integer.toString(hdSeedId), hdSeedIndex.toString()});
                if (c.moveToNext()) {
                    isExist |= c.getInt(0) > 0;
                }
                c.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            isExist = true;
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        if (!isExist) {
            db.beginTransaction();
            for (int i = 0; i < indexes.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put(AbstractDb.HDMAddressesColumns.HD_SEED_ID, hdSeedId);
                cv.put(AbstractDb.HDMAddressesColumns.HD_SEED_INDEX, indexes.get(i));
                cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY1, Base58.encode(pubKeys1.get(i)));
                cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY2, Base58.encode(pubKeys2.get(i)));
                cv.putNull(AbstractDb.HDMAddressesColumns.PUB_KEY3);
                cv.putNull(AbstractDb.HDMAddressesColumns.ADDRESS);
                cv.put(AbstractDb.HDMAddressesColumns.IS_SYNCED, 0);
                db.insert(AbstractDb.Tables.HDMAddresses, null, cv);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }


    public void completeHDMAddresses(int hdSeedId, List<Integer> indexes
            , List<byte[]> pubKeys3, List<String> addresses) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        boolean isExist = true;
        Cursor c = null;
        try {
            for (Integer hdSeedIndex : indexes) {
                String sql = "select count(0) from hdm_addresses " +
                        "where hd_seed_id=? and hd_seed_index=? and address is null";
                c = db.rawQuery(sql, new String[]{Integer.toString(hdSeedId), hdSeedIndex.toString()});
                if (c.moveToNext()) {
                    isExist &= c.getInt(0) > 0;
                }
                c.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            isExist = false;
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        if (isExist) {
            db.beginTransaction();
            for (int i = 0; i < indexes.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY3, Base58.encode(pubKeys3.get(i)));
                cv.put(AbstractDb.HDMAddressesColumns.ADDRESS, addresses.get(i));
                db.update(AbstractDb.Tables.HDMAddresses, cv, " hd_seed_id=? and hd_seed_index=? "
                        , new String[]{Integer.toString(hdSeedId), indexes.get(i).toString()});
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    @Override
    public void syncComplete(int hdSeedId, int hdSeedIndex) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDMAddressesColumns.IS_SYNCED, 1);
        db.update(AbstractDb.Tables.HDMAddresses, cv, " hd_seed_id=? and hd_seed_index=? "
                , new String[]{Integer.toString(hdSeedId), Integer.toString(hdSeedIndex)});
    }

    //normal
    @Override
    public List<Address> getAddresses() {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery("select * from addresses  order by sort_time desc", null);
        List<Address> addressList = new ArrayList<Address>();
        if (c.moveToNext()) {
            Address address = null;
            try {
                address = applyAddressCursor(c);
            } catch (AddressFormatException e) {
                e.printStackTrace();
            }
            if (address != null) {
                addressList.add(address);
            }
        }
        c.close();
        return addressList;
    }

    @Override
    public void addAddress(Address address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = applyContentValues(address);
        db.insert(AbstractDb.Tables.Addresses, null, cv);

    }

    @Override
    public void removeWatchOnlyAddress(Address address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.delete(AbstractDb.Tables.Addresses, AbstractDb.AddressesColumns.ADDRESS + "=? and "
                + AbstractDb.AddressesColumns.ENCRYPT_PRIVATE_KEY + " is null", new String[]{
                address.getAddress()
        });

    }


    @Override
    public void trashPrivKeyAddress(Address address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.AddressesColumns.IS_TRASH, 1);
        db.update(AbstractDb.Tables.Addresses, cv, AbstractDb.AddressesColumns.ADDRESS + "=?"
                , new String[]{address.getAddress()});

    }

    @Override
    public void restorePrivKeyAddress(Address address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.AddressesColumns.IS_TRASH, 0);
        cv.put(AbstractDb.AddressesColumns.SORT_TIME, address.getSortTime());
        cv.put(AbstractDb.AddressesColumns.IS_SYNCED, 0);
        db.update(AbstractDb.Tables.Addresses, cv, AbstractDb.AddressesColumns.ADDRESS + "=?"
                , new String[]{address.getAddress()});
    }

    @Override
    public void updateSyncComplete(Address address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.AddressesColumns.IS_SYNCED, address.isSyncComplete() ? 1 : 0);
        db.update(AbstractDb.Tables.Addresses, cv, AbstractDb.AddressesColumns.ADDRESS + "=?"
                , new String[]{address.getAddress()});
    }

    @Override
    public void updatePrivateKey(Address address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.AddressesColumns.ENCRYPT_PRIVATE_KEY, address.getEncryptPrivKey());
        db.update(AbstractDb.Tables.Addresses, cv, AbstractDb.AddressesColumns.ADDRESS + "=?"
                , new String[]{address.getAddress()});
    }


    private ContentValues applyContentValues(Address address) {
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.AddressesColumns.ADDRESS, address.getAddress());
        if (!Utils.isEmpty(address.getEncryptPrivKey())) {
            cv.put(AbstractDb.AddressesColumns.ENCRYPT_PRIVATE_KEY, address.getEncryptPrivKey());
        }
        cv.put(AbstractDb.AddressesColumns.PUB_KEY, Base58.encode(address.getPubKey()));
        cv.put(AbstractDb.AddressesColumns.IS_XRANDOM, address.isFromXRandom() ? 1 : 0);
        cv.put(AbstractDb.AddressesColumns.IS_SYNCED, address.isSyncComplete() ? 1 : 0);
        cv.put(AbstractDb.AddressesColumns.IS_TRASH, address.isTrashed() ? 1 : 0);
        cv.put(AbstractDb.AddressesColumns.SORT_TIME, address.getSortTime());
        return cv;

    }

    private Address applyAddressCursor(Cursor c) throws AddressFormatException {
        Address address;
        int idColumn = c.getColumnIndex(AbstractDb.AddressesColumns.ADDRESS);
        String addressStr = null;
        String encryptPrivateKey = null;
        byte[] pubKey = null;
        boolean isXRandom = false;
        boolean isSynced = false;
        boolean isTrash = false;
        long sortTime = 0;

        if (idColumn != -1) {
            addressStr = c.getString(idColumn);
            if (!Utils.validBicoinAddress(addressStr)) {
                return null;
            }
        }
        idColumn = c.getColumnIndex(AbstractDb.AddressesColumns.ENCRYPT_PRIVATE_KEY);
        if (idColumn != -1) {
            encryptPrivateKey = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.AddressesColumns.PUB_KEY);
        if (idColumn != -1) {
            pubKey = Base58.decode(c.getString(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.AddressesColumns.IS_XRANDOM);
        if (idColumn != -1) {
            isXRandom = c.getInt(idColumn) == 1;
        }
        idColumn = c.getColumnIndex(AbstractDb.AddressesColumns.IS_SYNCED);
        if (idColumn != -1) {
            isSynced = c.getInt(idColumn) == 1;
        }
        idColumn = c.getColumnIndex(AbstractDb.AddressesColumns.IS_TRASH);
        if (idColumn != -1) {
            isTrash = c.getInt(idColumn) == 1;
        }
        idColumn = c.getColumnIndex(AbstractDb.AddressesColumns.SORT_TIME);
        if (idColumn != -1) {
            sortTime = c.getLong(idColumn);
        }
        address = new Address(addressStr, pubKey, sortTime, isSynced, isXRandom, isTrash, encryptPrivateKey);

        return address;
    }
}
