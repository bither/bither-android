package net.bither.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;

public class AddressDatabaseHelper extends SQLiteOpenHelper {
    public static final int DB_VERSION = 7;
    private static final String DB_NAME = "address.db";

    public AddressDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_ADDRESSES_SQL);
        db.execSQL(AbstractDb.CREATE_HDM_BID_SQL);
        db.execSQL(AbstractDb.CREATE_HD_SEEDS_SQL);
        db.execSQL(AbstractDb.CREATE_HDM_ADDRESSES_SQL);
        db.execSQL(AbstractDb.CREATE_PASSWORD_SEED_SQL);
        db.execSQL(AbstractDb.CREATE_ALIASES_SQL);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT);
        db.execSQL(AbstractDb.CREATE_VANITY_ADDRESS_SQL);
        db.execSQL(AbstractDb.CREATE_ENTERPRISE_HD_ACCOUNT);
        db.execSQL(AbstractDb.CREATE_ENTERPRISE_HDM_ADDRESSES_SQL);
        db.execSQL(AbstractDb.CREATE_MULTI_SIGN_SET);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        if (oldVersion == 1 && newVersion == 2) {
//            db.execSQL("alter table hd_seeds add column encrypt_hd_seed text;");
//            db.execSQL(AbstractDb.CREATE_PASSWORD_SEED_SQL);
//            String passwordSeedStr = AppSharedPreference.getInstance().getPasswordSeedString();
//            if (!Utils.isEmpty(passwordSeedStr)) {
//                db.execSQL("insert into password_seed (password_seed) values (?) ", new String[]{passwordSeedStr});
//            }
//        } else if (oldVersion == 2 && newVersion == 3) {
//            db.execSQL(AbstractDb.CREATE_ALIASES_SQL);
//            db.execSQL("alter table hd_seeds add column singular_mode_backup text;");
//        }
        switch (oldVersion) {
            case 1:
                v1ToV2(db);
            case 2:
                v2ToV3(db);
            case 3:
                v3ToV4(db);
            case 4:
                v4Tov5(db);
            case 5:
                v5ToV6(db);
            case 6:
                v6Tov7(db);

        }
    }

    private void v1ToV2(SQLiteDatabase db) {
        // v1.3.1
        db.execSQL("alter table hd_seeds add column encrypt_hd_seed text;");
        db.execSQL(AbstractDb.CREATE_PASSWORD_SEED_SQL);
        String passwordSeedStr = AppSharedPreference.getInstance().getPasswordSeedString();
        if (!Utils.isEmpty(passwordSeedStr)) {
            db.execSQL("insert into password_seed (password_seed) values (?) ", new String[]{passwordSeedStr});
        }
    }

    private void v2ToV3(SQLiteDatabase db) {
        // v1.3.2
        db.execSQL(AbstractDb.CREATE_ALIASES_SQL);
        db.execSQL("alter table hd_seeds add column singular_mode_backup text;");
    }

    private void v3ToV4(SQLiteDatabase db) {
        // v1.3.3 ensure v2 & v3 's script executed.
        Cursor cursor = db.rawQuery("select count(0) from sqlite_master where name='aliases'", null);
        int cnt = 0;
        if (cursor.moveToNext()) {
            cnt = cursor.getInt(0);
        }
        cursor.close();
        if (cnt == 0) {
            v1ToV2(db);
            v2ToV3(db);
        }
    }

    private void v4Tov5(SQLiteDatabase db) {
        //v1.3.4
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT);
    }

    private void v5ToV6(SQLiteDatabase db) {
        //v1.3.5
        db.execSQL(AbstractDb.CREATE_VANITY_ADDRESS_SQL);

    }

    private void v6Tov7(SQLiteDatabase db) {
        //1.3.7
        db.execSQL(AbstractDb.CREATE_ENTERPRISE_HD_ACCOUNT);
        db.execSQL(AbstractDb.CREATE_ENTERPRISE_HDM_ADDRESSES_SQL);
        db.execSQL(AbstractDb.CREATE_MULTI_SIGN_SET);
    }
}
