package net.bither.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.IAddressProvider;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;
import net.bither.util.LogUtil;

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
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDSeedsColumns.ENCRYPT_SEED, encryptedSeed);
        db.update(AbstractDb.Tables.HDSeeds, cv, "hd_seed_id=?"
                , new String[]{Integer.toString(hdSeedId)});

    }

    @Override
    public boolean isHDSeedFromXRandom(int hdSeedId) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select is_xrandom from hd_seeds where hd_seed_id=?"
                , new String[]{Integer.toString(hdSeedId)});
        boolean isXRandom = false;
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex("is_xrandom");
            if (idColumn != -1) {
                isXRandom = cursor.getInt(idColumn) == 1;
            }
        }
        return isXRandom;
    }


    @Override
    public String getHDMFristAddress(int hdSeedId) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select hdm_address from hd_seeds where hd_seed_id=?"
                , new String[]{Integer.toString(hdSeedId)});
        String address = null;
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDSeedsColumns.HDM_ADDRESS);
            if (idColumn != -1) {
                address = cursor.getString(idColumn);
            }
        }
        cursor.close();
        return address;
    }

    @Override
    public int addHDKey(String encryptSeed, String firstAddress, boolean isXrandom) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDSeedsColumns.ENCRYPT_SEED, encryptSeed);
        cv.put(AbstractDb.HDSeedsColumns.IS_XRANDOM, isXrandom ? 1 : 0);
        cv.put(AbstractDb.HDSeedsColumns.HDM_ADDRESS, firstAddress);
        return (int) db.insert(AbstractDb.Tables.HDSeeds, null, cv);
    }

    @Override
    public HDMBId getHDMBId() {
        HDMBId hdmbId;
        Cursor c = null;
        String address = null;
        String encryptBitherPassword = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            String sql = "select " + AbstractDb.HDMBIdColumns.HDM_BID + "," + AbstractDb.HDMBIdColumns.ENCRYPT_BITHER_PASSWORD + " from " +
                    AbstractDb.Tables.HDM_BID;
            c = db.rawQuery(sql, null);
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex(AbstractDb.HDMBIdColumns.HDM_BID);
                if (idColumn != -1) {
                    address = c.getString(idColumn);
                }
                idColumn = c.getColumnIndex(AbstractDb.HDMBIdColumns.ENCRYPT_BITHER_PASSWORD);
                if (idColumn != -1) {
                    encryptBitherPassword = c.getString(idColumn);
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        hdmbId = new HDMBId(address, encryptBitherPassword);
        return hdmbId;
    }


    @Override
    public void addHDMBId(HDMBId hdmId) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        boolean isExist = true;
        Cursor c = null;
        try {
            String sql = "select count(0) from " + AbstractDb.Tables.HDM_BID;
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
            cv.put(AbstractDb.HDMBIdColumns.HDM_BID, hdmId.getAddress());
            cv.put(AbstractDb.HDMBIdColumns.ENCRYPT_BITHER_PASSWORD, hdmId.getEncryptedBitherPasswordString());
            db.insert(AbstractDb.Tables.HDM_BID, null, cv);
        }
    }

    @Override
    public void changeHDMBIdPassword(String encryptBitherPassword) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDMBIdColumns.ENCRYPT_BITHER_PASSWORD, encryptBitherPassword);
        db.update(AbstractDb.Tables.HDM_BID, cv, null, null);
    }

    @Override
    public List<HDMAddress> getHDMAddressInUse(HDMKeychain keychain) {
        List<HDMAddress> addresses = new ArrayList<HDMAddress>();
        Cursor c = null;
        try {
            SQLiteDatabase db = this.mDb.getReadableDatabase();
            String sql = "select hd_seed_index,pub_key_hot,pub_key_cold,pub_key_remote,address,is_synced " +
                    "from hdm_addresses " +
                    "where hd_seed_id=? and address is not null order by hd_seed_index";
            c = db.rawQuery(sql, new String[]{Integer.toString(keychain.getHdSeedId())});
            while (c.moveToNext()) {
                HDMAddress hdmAddress = applyHDMAddress(c, keychain);
                if (hdmAddress != null) {
                    addresses.add(hdmAddress);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return addresses;
    }


    @Override
    public void prepareHDMAddresses(int hdSeedId, List<HDMAddress.Pubs> pubsList) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        boolean isExist = false;
        Cursor c = null;
        try {
            for (HDMAddress.Pubs pubs : pubsList) {
                String sql = "select count(0) from hdm_addresses where hd_seed_id=? and hd_seed_index=?";
                c = db.rawQuery(sql, new String[]{Integer.toString(hdSeedId), Integer.toString(pubs.index)});
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
            for (int i = 0; i < pubsList.size(); i++) {
                HDMAddress.Pubs pubs = pubsList.get(i);
                ContentValues cv = applyHDMAddressContentValues(hdSeedId, pubs.index, pubs.hot, pubs.cold);
                db.insert(AbstractDb.Tables.HDMAddresses, null, cv);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }

    }

    @Override
    public List<HDMAddress.Pubs> getUncompletedHDMAddressPubs(int hdSeedId, int count) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        List<HDMAddress.Pubs> pubsList = new ArrayList<HDMAddress.Pubs>();
        Cursor cursor = db.rawQuery("select * from hdm_addresses where hd_seed_id=? and pub_key_remote is null limit ? ", new String[]{
                Integer.toString(hdSeedId), Integer.toString(count)
        });
        try {
            while (cursor.moveToNext()) {
                HDMAddress.Pubs pubs = applyPubs(cursor);
                if (pubs != null) {
                    pubsList.add(pubs);
                }
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }

        cursor.close();
        return pubsList;
    }

    @Override
    public int maxHDMAddressPubIndex(int hdSeedId) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();

        Cursor cursor = db.rawQuery("select ifnull(max(hd_seed_index),-1)  hd_seed_index from hdm_addresses where hd_seed_id=?  ", new String[]{
                Integer.toString(hdSeedId)
        });
        int maxIndex = -1;
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex(AbstractDb.HDMAddressesColumns.HD_SEED_INDEX);
            if (idColumn != -1) {
                maxIndex = cursor.getInt(idColumn);
            }
        }
        cursor.close();
        return maxIndex;
    }

    @Override
    public int uncompletedHDMAddressCount(int hdSeedId) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(0) cnt from hdm_addresses where hd_seed_id=?  and pub_key_remote is null "
                , new String[]{
                Integer.toString(hdSeedId)
        });
        int count = 0;
        if (cursor.moveToNext()) {
            int idColumn = cursor.getColumnIndex("cnt");
            if (idColumn != -1) {
                count = cursor.getInt(idColumn);
            }
        }
        cursor.close();
        return count;


    }


    @Override
    public void completeHDMAddresses(int hdSeedId, List<HDMAddress> addresses) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        boolean isExist = true;
        Cursor c = null;
        try {
            for (HDMAddress address : addresses) {

                String sql = "select count(0) from hdm_addresses " +
                        "where hd_seed_id=? and hd_seed_index=? and address is null";
                c = db.rawQuery(sql, new String[]{Integer.toString(hdSeedId), Integer.toString(address.getIndex())});
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
            for (int i = 0; i < addresses.size(); i++) {
                HDMAddress address = addresses.get(i);
                ContentValues cv = new ContentValues();
                cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY_REMOTE, Base58.encode(address.getPubRemote()));
                cv.put(AbstractDb.HDMAddressesColumns.ADDRESS, address.getAddress());
                db.update(AbstractDb.Tables.HDMAddresses, cv, " hd_seed_id=? and hd_seed_index=? "
                        , new String[]{Integer.toString(hdSeedId), Integer.toString(address.getIndex())});
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }


    private ContentValues applyHDMAddressContentValues(int hdSeedId, int index, byte[] pubKeysHot, byte[] pubKeysCold) {
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDMAddressesColumns.HD_SEED_ID, hdSeedId);
        cv.put(AbstractDb.HDMAddressesColumns.HD_SEED_INDEX, index);
        cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY_HOT, Base58.encode(pubKeysHot));
        cv.put(AbstractDb.HDMAddressesColumns.PUB_KEY_COLD, Base58.encode(pubKeysCold));
        cv.putNull(AbstractDb.HDMAddressesColumns.PUB_KEY_REMOTE);
        cv.putNull(AbstractDb.HDMAddressesColumns.ADDRESS);
        cv.put(AbstractDb.HDMAddressesColumns.IS_SYNCED, 0);
        return cv;
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
        while (c.moveToNext()) {
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
        if (address.hasPrivKey()) {
            SQLiteDatabase db = this.mDb.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(AbstractDb.AddressesColumns.ENCRYPT_PRIVATE_KEY, address.getEncryptPrivKeyOfDb());
            db.update(AbstractDb.Tables.Addresses, cv, AbstractDb.AddressesColumns.ADDRESS + "=?"
                    , new String[]{address.getAddress()});
        }
    }


    private ContentValues applyContentValues(Address address) {
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
        return cv;

    }

    private HDMAddress applyHDMAddress(Cursor c, HDMKeychain keychain) throws AddressFormatException

    {
        HDMAddress hdmAddress;

        String address = null;
        boolean isSynced = false;

        int idColumn = c.getColumnIndex(AbstractDb.HDMAddressesColumns.ADDRESS);
        if (idColumn != -1) {
            address = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDMAddressesColumns.IS_SYNCED);
        if (idColumn != -1) {
            isSynced = c.getInt(idColumn) == 1;
        }
        HDMAddress.Pubs pubs = applyPubs(c);
        hdmAddress = new HDMAddress(pubs, address, isSynced, keychain);
        return hdmAddress;

    }

    private HDMAddress.Pubs applyPubs(Cursor c) throws AddressFormatException {
        int hdSeedIndex = 0;
        byte[] hot = null;
        byte[] cold = null;
        byte[] remote = null;
        int idColumn = c.getColumnIndex(AbstractDb.HDMAddressesColumns.HD_SEED_INDEX);
        if (idColumn != -1) {
            hdSeedIndex = c.getInt(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDMAddressesColumns.PUB_KEY_HOT);
        if (idColumn != -1 && !c.isNull(idColumn)) {
            hot = Base58.decode(c.getString(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.HDMAddressesColumns.PUB_KEY_COLD);
        if (idColumn != -1 && !c.isNull(idColumn)) {
            cold = Base58.decode(c.getString(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.HDMAddressesColumns.PUB_KEY_REMOTE);
        if (idColumn != -1 && !c.isNull(idColumn)) {
            remote = Base58.decode(c.getString(idColumn));
        }
        HDMAddress.Pubs pubs = new HDMAddress.Pubs(hot, cold, remote, hdSeedIndex);
        return pubs;

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
