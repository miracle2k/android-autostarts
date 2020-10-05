package com.elsdoerfer.android.autostarts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "common.db";
    static final int DATABASE_VERSION = 3;

    static final String TABLE_CHANGED = "changed";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) { }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	if (oldVersion <= 2)
    		// This table is no longer required in new versions.
    		db.execSQL("DROP TABLE IF EXISTS changed");
        onCreate(db);
    }
}
