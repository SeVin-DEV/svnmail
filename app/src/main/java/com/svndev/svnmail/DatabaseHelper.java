package com.svndev.svnmail;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "svnwebmail.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "email TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "last_uid INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS accounts");
        onCreate(db);
    }

    public long addAccount(String email, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("email", email);
        cv.put("password", password);
        return db.insertWithOnConflict("accounts", null, cv,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removeAccount(long id) {
        getWritableDatabase().delete("accounts", "id=?",
                new String[]{String.valueOf(id)});
    }

    public List<Account> getAccounts() {
        List<Account> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, email, password FROM accounts ORDER BY id", null);
        while (c.moveToNext()) {
            list.add(new Account(c.getLong(0), c.getString(1), c.getString(2)));
        }
        c.close();
        return list;
    }

    public long getLastUid(long accountId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT last_uid FROM accounts WHERE id=?",
                new String[]{String.valueOf(accountId)});
        long uid = 0;
        if (c.moveToFirst()) uid = c.getLong(0);
        c.close();
        return uid;
    }

    public void setLastUid(long accountId, long uid) {
        ContentValues cv = new ContentValues();
        cv.put("last_uid", uid);
        getWritableDatabase().update("accounts", cv, "id=?",
                new String[]{String.valueOf(accountId)});
    }
}
