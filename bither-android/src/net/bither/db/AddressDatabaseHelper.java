package net.bither.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.bitherj.db.AbstractDb;

public class AddressDatabaseHelper extends SQLiteOpenHelper {
    public static final int DB_VERSION = 2;
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("alter table hd_seeds add column encrypt_hd_seed text;");
            db.execSQL(AbstractDb.CREATE_PASSWORD_SEED_SQL);
        }
    }
}
