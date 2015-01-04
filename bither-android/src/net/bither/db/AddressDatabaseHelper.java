package net.bither.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.bitherj.db.AbstractDb;

public class AddressDatabaseHelper extends SQLiteOpenHelper {
    public static final int DB_VERSION = 1;
    private static final String DB_NAME = "address.db";

    public AddressDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createHDKeyTable(db);
        createAddressTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void  createAddressTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_ADDRESSES_SQL);
    }

    private void createHDKeyTable(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_HD_KEYS_SQL);
    }
}
