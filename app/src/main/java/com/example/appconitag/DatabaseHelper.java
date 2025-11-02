package com.example.appconitag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "AssetDatabase.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and columns
    private static final String TABLE_ASSETS = "assets";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ASSET_TAG = "asset_tag";
    private static final String COLUMN_ASSET_NAME = "asset_name";
    private static final String COLUMN_ROOM = "room";
    private static final String COLUMN_CONDITION = "condition";
    private static final String COLUMN_LOCATION = "location";
    private static final String COLUMN_NOTES = "notes";
    private static final String COLUMN_VERIFIED = "verified";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_SUBMITTED_BY = "submitted_by";
    private static final String COLUMN_SYNC_STATUS = "sync_status"; // 0 = not synced, 1 = synced

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_ASSETS_TABLE = "CREATE TABLE " + TABLE_ASSETS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ASSET_TAG + " TEXT,"
                + COLUMN_ASSET_NAME + " TEXT,"
                + COLUMN_ROOM + " TEXT,"
                + COLUMN_CONDITION + " TEXT,"
                + COLUMN_LOCATION + " TEXT,"
                + COLUMN_NOTES + " TEXT,"
                + COLUMN_VERIFIED + " TEXT,"
                + COLUMN_IMAGE_PATH + " TEXT,"
                + COLUMN_SUBMITTED_BY + " INTEGER,"
                + COLUMN_SYNC_STATUS + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_ASSETS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ASSETS);
        onCreate(db);
    }

    // Add asset to local database
    public long addAsset(Asset asset) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ASSET_TAG, asset.getAssetTag());
        values.put(COLUMN_ASSET_NAME, asset.getAssetName());
        values.put(COLUMN_ROOM, asset.getRoom());
        values.put(COLUMN_CONDITION, asset.getCondition());
        values.put(COLUMN_LOCATION, asset.getLocation());
        values.put(COLUMN_NOTES, asset.getNotes());
        values.put(COLUMN_VERIFIED, asset.getVerified());
        values.put(COLUMN_IMAGE_PATH, asset.getImagePath());
        values.put(COLUMN_SUBMITTED_BY, asset.getSubmittedBy());
        values.put(COLUMN_SYNC_STATUS, asset.getSyncStatus());

        long id = db.insert(TABLE_ASSETS, null, values);
        db.close();
        return id;
    }

    // Get all assets from local database
    public List<Asset> getAllAssets() {
        List<Asset> assetList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ASSETS, null);

        if (cursor.moveToFirst()) {
            do {
                Asset asset = new Asset();
                asset.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                asset.setAssetTag(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TAG)));
                asset.setAssetName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)));
                asset.setRoom(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)));
                asset.setCondition(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONDITION)));
                asset.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)));
                asset.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
                asset.setVerified(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VERIFIED)));
                asset.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
                asset.setSubmittedBy(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUBMITTED_BY)));
                asset.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)));
                assetList.add(asset);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return assetList;
    }

    // Get unsynced assets
    public List<Asset> getUnsyncedAssets() {
        List<Asset> assetList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ASSETS + " WHERE " + COLUMN_SYNC_STATUS + " = 0", null);

        if (cursor.moveToFirst()) {
            do {
                Asset asset = new Asset();
                asset.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                asset.setAssetTag(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TAG)));
                asset.setAssetName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)));
                asset.setRoom(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)));
                asset.setCondition(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONDITION)));
                asset.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)));
                asset.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
                asset.setVerified(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VERIFIED)));
                asset.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
                asset.setSubmittedBy(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUBMITTED_BY)));
                asset.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)));
                assetList.add(asset);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return assetList;
    }

    public Asset getAssetById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ASSETS + " WHERE " + COLUMN_ID + " = ?", new String[]{String.valueOf(id)});

        if (cursor != null && cursor.moveToFirst()) {
            Asset asset = new Asset();
            asset.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            asset.setAssetTag(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TAG)));
            asset.setAssetName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)));
            asset.setRoom(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)));
            asset.setCondition(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONDITION)));
            asset.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)));
            asset.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
            asset.setVerified(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VERIFIED)));
            asset.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
            asset.setSubmittedBy(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUBMITTED_BY)));
            asset.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)));
            cursor.close();
            db.close();
            return asset;
        }

        if (cursor != null) cursor.close();
        db.close();
        return null;
    }


    // Update sync status
    public void updateSyncStatus(int id, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SYNC_STATUS, status);
        db.update(TABLE_ASSETS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
