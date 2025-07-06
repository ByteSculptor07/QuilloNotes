package com.github.bytesculptor07.quillo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.github.bytesculptor07.quillo.Utils.SyncUtils;
import com.github.bytesculptor07.quillo.models.Folder;

import java.util.ArrayList;
import java.util.List;

public class FoldersDatabaseHelper extends SQLiteOpenHelper {
    private static FoldersDatabaseHelper instance;
    private static final String DATABASE_NAME = "folders.db";
    private static final int DATABASE_VERSION = 4;

    // Table name
    private static final String TABLE_FOLDERS = "folders";

    // Column names
    private static final String COLUMN_FOLDER_ID = "id";
    private static final String COLUMN_FOLDER_NAME = "name";
    private static final String COLUMN_FOLDER_ICON = "icon";
    private static final String COLUMN_FOLDER_COLOR = "color";
    private static final String COLUMN_FOLDER_PATH = "path";
    private static final String COLUMN_FOLDER_STATUS = "status";
    private static final String COLUMN_FOLDER_HASH = "hash";

    public static synchronized FoldersDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FoldersDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    public FoldersDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Define the folders table and its columns
        String createFoldersTable = "CREATE TABLE " + TABLE_FOLDERS +
                " (" + COLUMN_FOLDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_FOLDER_NAME + " TEXT, " +
                COLUMN_FOLDER_ICON + " INTEGER, " +
                COLUMN_FOLDER_COLOR + " INTEGER, " +
                COLUMN_FOLDER_STATUS + " INTEGER, " +
                COLUMN_FOLDER_HASH + " STRING, " +
                COLUMN_FOLDER_PATH + " TEXT)";

        // Execute the SQL statement to create the table
        db.execSQL(createFoldersTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Add the new column to the existing table if the database version is upgraded
            db.execSQL("ALTER TABLE " + TABLE_FOLDERS + " ADD COLUMN " + COLUMN_FOLDER_STATUS + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_FOLDERS + " ADD COLUMN " + COLUMN_FOLDER_HASH + " STRING");
        }
    }
    
    public long addFolder(Folder folder) {
        return addFolder(folder, SyncUtils.STATUS_SYNC_PENDING);
    }

    // Method to add a folder entry to the database
    public long addFolder(Folder folder, int status) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_FOLDER_NAME, folder.getName());
        values.put(COLUMN_FOLDER_ICON, folder.getIcon());
        values.put(COLUMN_FOLDER_COLOR, folder.getColor());
        values.put(COLUMN_FOLDER_PATH, folder.getPath());
        //values.put(COLUMN_FOLDER_CHANGES, changes);
        values.put(COLUMN_FOLDER_STATUS, status);

        // Insert the record into the "folders" table
        long newRowId = db.insert(TABLE_FOLDERS, null, values);

        // Close the database to release resources
        //db.close();

        return newRowId;
    }
    
    public void updateFolder(Folder updatedFolder) {
        updateFolder(updatedFolder, SyncUtils.STATUS_SYNC_PENDING);
    }

    public void updateFolder(Folder updatedFolder, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_FOLDER_NAME, updatedFolder.getName());
        values.put(COLUMN_FOLDER_ICON, updatedFolder.getIcon());
        values.put(COLUMN_FOLDER_COLOR, updatedFolder.getColor());
        values.put(COLUMN_FOLDER_PATH, updatedFolder.getPath());
        //values.put(COLUMN_FOLDER_CHANGES, changes);
        values.put(COLUMN_FOLDER_STATUS, status);

        String selection = COLUMN_FOLDER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(updatedFolder.getId())};

        db.update(TABLE_FOLDERS, values, selection, selectionArgs);

        //db.close();
    }
    
    public void removeFolderChanges(Folder updatedFolder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        //values.put(COLUMN_FOLDER_CHANGES, 0);
        values.put(COLUMN_FOLDER_STATUS, SyncUtils.STATUS_SYNCED);

        String selection = COLUMN_FOLDER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(updatedFolder.getId())};

        db.update(TABLE_FOLDERS, values, selection, selectionArgs);

        //db.close();
    }

    public List<Folder> getFolders() {
        List<Folder> folderList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve
        String[] projection = {
            COLUMN_FOLDER_ID, COLUMN_FOLDER_NAME, COLUMN_FOLDER_ICON, COLUMN_FOLDER_COLOR, COLUMN_FOLDER_PATH
        };

        // Query the "folders" table
        Cursor cursor = db.query(TABLE_FOLDERS, projection, null, null, null, null, null);

        // Iterate through the cursor and add entries to the list
        while (cursor.moveToNext()) {
            Folder folder = new Folder();
            folder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_ID)));
            folder.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_NAME)));
            folder.setIcon(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_ICON)));
            folder.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_COLOR)));
            folder.setPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_PATH)));
            folderList.add(folder);
        }

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return folderList;
    }

    public void updateHash(Folder folder, String hash) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_FOLDER_ID, folder.getId());
        //values.put(COLUMN_PAGE_NUMBER, page.getNumber());
        values.put(COLUMN_FOLDER_HASH, hash);

        String selection = COLUMN_FOLDER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(folder.getId())};

        db.update(TABLE_FOLDERS, values, selection, selectionArgs);
    }
    
    public List<Folder> getFolderWithChanges() {
        List<Folder> folderList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
    
        // Define the columns to retrieve, including the new columns
        String[] projection = {
            COLUMN_FOLDER_ID, COLUMN_FOLDER_NAME, COLUMN_FOLDER_ICON, COLUMN_FOLDER_COLOR, COLUMN_FOLDER_PATH
        };
    
        // Define the selection criteria to filter pages that have changes (changes = 1)
        String selection = COLUMN_FOLDER_STATUS + " = ?";
        String[] selectionArgs = {String.valueOf(SyncUtils.STATUS_SYNC_PENDING)};
    
        // Query the "pages" table to get pages with changes
        Cursor cursor = db.query(TABLE_FOLDERS, projection, selection, selectionArgs, null, null, null);
    
        // Iterate through the cursor and add entries to the list
        while (cursor.moveToNext()) {
            Folder folder = new Folder();
            folder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_ID)));
            folder.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_NAME)));
            folder.setIcon(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_ICON)));
            folder.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_COLOR)));
            folder.setPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_PATH)));
            folderList.add(folder);
        }
    
        // Close the cursor and database to release resources
        cursor.close();
        //db.close();
    
        return folderList;
    }

    public String getHashByFolderId(String id) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve, including the new columns
        String[] projection = {
                COLUMN_FOLDER_ID, COLUMN_FOLDER_NAME, COLUMN_FOLDER_ICON, COLUMN_FOLDER_COLOR, COLUMN_FOLDER_PATH, COLUMN_FOLDER_HASH
        };

        // Define the sorting order
        //String sortOrder = COLUMN_PAGE_NUMBER + " ASC"; // ASC for ascending order

        // Define the selection criteria
        String selection = COLUMN_FOLDER_ID + " = ?";
        String[] selectionArgs = {id};

        // Query the "pages" table
        Cursor cursor = db.query(TABLE_FOLDERS, projection, selection, selectionArgs, null, null, null);

        String hash = null;
        if (cursor.moveToFirst()) {
            hash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_HASH));
        }

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return hash;
    }

    public boolean folderExists(Folder folderToCheck) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve
        String[] projection = {COLUMN_FOLDER_ID};

        // Define the selection criteria
        String selection = COLUMN_FOLDER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(folderToCheck.getId())};

        // Query the "folders" table
        Cursor cursor = db.query(TABLE_FOLDERS, projection, selection, selectionArgs, null, null, null);

        boolean exists = cursor.moveToFirst();

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return exists;
    }

    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FOLDERS, null, null);
        //db.close();
    }

    public List<Folder> getFoldersByPath(String path) {
        List<Folder> folderList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve
        String[] projection = {
            COLUMN_FOLDER_ID,
            COLUMN_FOLDER_NAME,
            COLUMN_FOLDER_ICON,
            COLUMN_FOLDER_COLOR,
            COLUMN_FOLDER_PATH
        };

        // Define the selection criteria
        String selection = COLUMN_FOLDER_PATH + " = ?";
        String[] selectionArgs = {path};

        // Query the "folders" table
        Cursor cursor =
                db.query(TABLE_FOLDERS, projection, selection, selectionArgs, null, null, null);

        // Iterate through the cursor and add entries to the list
        while (cursor.moveToNext()) {
            Folder folder = new Folder();
            folder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_ID)));
            folder.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_NAME)));
            folder.setIcon(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_ICON)));
            folder.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_COLOR)));
            folder.setPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_PATH)));
            folderList.add(folder);
        }

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return folderList;
    }

    @Override
    public void close() {
        super.close();
        instance = null; // Reset the instance so it can be reinitialized later
    }
}