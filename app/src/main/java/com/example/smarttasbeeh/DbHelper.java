package com.example.smarttasbeeh;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tasbeeh_db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_COUNTS = "counts";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_COUNT = "count";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_IS_PINNED = "is_pinned";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_COUNTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_COUNT + " INTEGER, " +
                COL_TIMESTAMP + " TEXT, " +
                COL_IS_PINNED + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
             db.execSQL("ALTER TABLE " + TABLE_COUNTS + " ADD COLUMN " + COL_IS_PINNED + " INTEGER DEFAULT 0");
        }
    }

    public long saveCount(String title, int count, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_COUNT, count);
        values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_IS_PINNED, 0);
        return db.insert(TABLE_COUNTS, null, values);
    }

    public List<SavedCount> getAllCounts() {
        List<SavedCount> counts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_COUNTS + " ORDER BY " + COL_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COUNT));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
                long pinnedTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_IS_PINNED));
                counts.add(new SavedCount(id, title, count, timestamp, pinnedTimestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return counts;
    }

    public void deleteCount(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_COUNTS, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public boolean isTitleExists(String title) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_COUNTS + " WHERE " + COL_TITLE + " = ? COLLATE NOCASE", new String[]{title});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    public boolean isIdExists(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_COUNTS + " WHERE " + COL_ID + " = ?", new String[]{String.valueOf(id)});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    public void updateCount(int id, int count, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_COUNT, count);
        values.put(COL_TIMESTAMP, timestamp);
        db.update(TABLE_COUNTS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }


    public void togglePin(int id, boolean isPinned) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_PINNED, isPinned ? System.currentTimeMillis() : 0);
        db.update(TABLE_COUNTS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int getPinnedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COUNTS + " WHERE " + COL_IS_PINNED + " > 0", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}