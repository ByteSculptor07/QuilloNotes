package com.github.bytesculptor07.quillo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.github.bytesculptor07.quillo.Utils.SyncUtils;
import com.github.bytesculptor07.quillo.models.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotesDatabaseHelper extends SQLiteOpenHelper {
    private static NotesDatabaseHelper instance;
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 4; // Incremented to reflect schema changes

    // Table names
    private static final String TABLE_PAGES = "pages";

    // Column names
    //private static final String COLUMN_PAGE_ID = "id";
    private static final String COLUMN_PAGE_UUID = "uuid";
    private static final String COLUMN_NOTE_ID = "noteid";
    private static final String COLUMN_PAGE_DATA = "data";
    private static final String COLUMN_PAGE_NUMBER = "number";
    private static final String COLUMN_BACKGROUND = "background"; // New column
    private static final String COLUMN_PDF = "pdf";               // New column
    private static final String COLUMN_PDF_PAGE = "pdf_page";     // New column
    private static final String COLUMN_PAGE_STATUS = "status";
    private static final String COLUMN_PAGE_HASH = "hash";

    public static synchronized NotesDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NotesDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public NotesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Define the pages table and its columns, including the new columns
        String createPageTable = "CREATE TABLE " + TABLE_PAGES +
                " (" + COLUMN_PAGE_UUID + " TEXT PRIMARY KEY, " +
                COLUMN_NOTE_ID + " TEXT, " +
                COLUMN_PAGE_NUMBER + " INTEGER, " +
                COLUMN_PAGE_DATA + " TEXT, " +
                COLUMN_BACKGROUND + " TEXT, " +  // New column
                COLUMN_PDF + " TEXT, " +        // New column
                COLUMN_PDF_PAGE + " INTEGER, " + // New column
                COLUMN_PAGE_HASH + " STRING, " +
                COLUMN_PAGE_STATUS + " INTEGER)";

        // Execute the SQL statement to create the table
        db.execSQL(createPageTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade database schema
        if (oldVersion < 2) {
            // Add new columns one by one
            db.execSQL("ALTER TABLE " + TABLE_PAGES + " ADD COLUMN " + COLUMN_BACKGROUND + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_PAGES + " ADD COLUMN " + COLUMN_PDF + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_PAGES + " ADD COLUMN " + COLUMN_PDF_PAGE + " INTEGER");
        }
        if (oldVersion < 3) {
            // Add new columns one by one
            //db.execSQL("ALTER TABLE " + TABLE_PAGES + " ADD COLUMN " + COLUMN_PAGE_CHANGES + " INTEGER");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_PAGES + " ADD COLUMN " + COLUMN_PAGE_STATUS + " INTEGER");
            //db.execSQL("UPDATE " + TABLE_PAGES + " SET " + COLUMN_PAGE_STATUS + " = " + COLUMN_PAGE_CHANGES);
        }

        if (oldVersion < 5) {
            deleteAll();
        }
    }
    
    public long addPage(Page page) {
        return addPage(page, SyncUtils.STATUS_SYNC_PENDING);
    }

    // Method to add a page entry to the database
    public long addPage(Page page, int status) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_ID, page.getNoteId());
        values.put(COLUMN_PAGE_NUMBER, page.getNumber());
        values.put(COLUMN_PAGE_DATA, page.getData());
        values.put(COLUMN_BACKGROUND, page.getBackground()); // New column
        values.put(COLUMN_PDF, page.getPdf());               // New column
        values.put(COLUMN_PDF_PAGE, page.getPdfPage());      // New column
        values.put(COLUMN_PAGE_STATUS, status);

        if (page.getUUID() == null) {
            values.put(COLUMN_PAGE_UUID, UUID.randomUUID().toString()); //generate new UUID
        } else {
            values.put(COLUMN_PAGE_UUID, page.getUUID());
        }
        //values.put(COLUMN_PAGE_CHANGES, changes); //backward compatibility

        // Insert the record into the "pages" table
        long newRowId = db.insert(TABLE_PAGES, null, values);

        // Close the database to release resources
        //db.close();

        return newRowId;
    }

    public void updatePageWithOldPage(Page oldPage, Page updatedPage, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NOTE_ID, updatedPage.getNoteId());
        values.put(COLUMN_PAGE_NUMBER, updatedPage.getNumber());
        values.put(COLUMN_PAGE_DATA, updatedPage.getData());
        values.put(COLUMN_BACKGROUND, updatedPage.getBackground()); // New column
        values.put(COLUMN_PDF, updatedPage.getPdf());               // New column
        values.put(COLUMN_PDF_PAGE, updatedPage.getPdfPage());      // New column
        //values.put(COLUMN_PAGE_CHANGES, changes); //changes are made
        values.put(COLUMN_PAGE_STATUS, status);

        String selection = COLUMN_PAGE_UUID + " = ?";
        String[] selectionArgs = {String.valueOf(oldPage.getUUID())};

        db.update(TABLE_PAGES, values, selection, selectionArgs);
    }
    
    public void updatePage(Page updatedPage, boolean useUUID) {
        updatePage(updatedPage, SyncUtils.STATUS_SYNC_PENDING, useUUID);
    }

    // Method to update a page entry
    public void updatePage(Page updatedPage, int status, boolean useUUID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NOTE_ID, updatedPage.getNoteId());
        values.put(COLUMN_PAGE_NUMBER, updatedPage.getNumber());
        values.put(COLUMN_PAGE_DATA, updatedPage.getData());
        values.put(COLUMN_BACKGROUND, updatedPage.getBackground()); // New column
        values.put(COLUMN_PDF, updatedPage.getPdf());               // New column
        values.put(COLUMN_PDF_PAGE, updatedPage.getPdfPage());      // New column
        //values.put(COLUMN_PAGE_CHANGES, changes); //changes are made
        values.put(COLUMN_PAGE_STATUS, status);

        String selection;
        String[] selectionArgs;
        if (useUUID) {
            selection = COLUMN_PAGE_UUID + " = ?";
            selectionArgs = new String[]{updatedPage.getUUID()};
        } else {
            selection = COLUMN_NOTE_ID + " = ? AND " + COLUMN_PAGE_NUMBER + " = ?";
            selectionArgs = new String[]{updatedPage.getNoteId(), String.valueOf(updatedPage.getNumber())};
        }

        db.update(TABLE_PAGES, values, selection, selectionArgs);

        //db.close();
    }

    /*
    public void deletePage(Page page) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Define the WHERE clause
        String whereClause = COLUMN_NOTE_ID + " = ? AND " + COLUMN_PAGE_NUMBER + " = ? AND " + COLUMN_PAGE_STATUS + " = ?";
        String[] whereArgs = {page.getNoteId(), String.valueOf(page.getNumber()), String.valueOf(page.getStatus())};

        db.delete(TABLE_PAGES, whereClause, whereArgs);
    }

     */

    /*
    public void removePageChanges(Page updatedPage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NOTE_ID, updatedPage.getNoteId());
        values.put(COLUMN_PAGE_NUMBER, updatedPage.getNumber());
        values.put(COLUMN_PAGE_STATUS, SyncUtils.STATUS_SYNCED);

        String selection = COLUMN_NOTE_ID + " = ? AND " + COLUMN_PAGE_NUMBER + " = ?";
        String[] selectionArgs = {updatedPage.getNoteId(), String.valueOf(updatedPage.getNumber())};

        db.update(TABLE_PAGES, values, selection, selectionArgs);

        //db.close();
    }

     */

    public void updateHash(Page page, String hash) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        //values.put(COLUMN_NOTE_ID, page.getNoteId());
        //values.put(COLUMN_PAGE_NUMBER, page.getNumber());
        values.put(COLUMN_PAGE_HASH, hash);

        String selection = COLUMN_PAGE_UUID + " = ?";
        String[] selectionArgs = {page.getUUID()};

        db.update(TABLE_PAGES, values, selection, selectionArgs);
    }
    
    // Method to get all pages with changes
    public List<Page> getPagesWithChanges() {
        List<Page> pageList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
    
        // Define the columns to retrieve, including the new columns
        String[] projection = {
            COLUMN_PAGE_UUID, COLUMN_NOTE_ID, COLUMN_PAGE_NUMBER, COLUMN_PAGE_DATA,
            COLUMN_BACKGROUND, COLUMN_PDF, COLUMN_PDF_PAGE, COLUMN_PAGE_STATUS
        };
    
        // Define the selection criteria to filter pages that have changes (changes = 1)
        String selection = COLUMN_PAGE_STATUS + " = ? OR " + COLUMN_PAGE_STATUS + " = ? OR " + COLUMN_PAGE_STATUS + " = ? OR " + COLUMN_PAGE_STATUS + " = ?";
        String[] selectionArgs = {
                String.valueOf(SyncUtils.STATUS_SYNC_PENDING),
                String.valueOf(SyncUtils.STATUS_ATTACHMENT_SYNC_PENDING),
                String.valueOf(SyncUtils.STATUS_SOFT_DELETION_PENDING),
                String.valueOf(SyncUtils.STATUS_HARD_DELETION_PENDING)
        };


        // Define the sorting order (optional, based on your needs)
        String sortOrder = COLUMN_PAGE_NUMBER + " ASC"; // Sort by page number in ascending order
    
        // Query the "pages" table to get pages with changes
        Cursor cursor = db.query(TABLE_PAGES, projection, selection, selectionArgs, null, null, sortOrder);
    
        // Iterate through the cursor and add entries to the list
        while (cursor.moveToNext()) {
            Page page = new Page();
            page.setUUID(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_UUID)));
            page.setNoteId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID)));
            page.setNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGE_NUMBER)));
            page.setData(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_DATA)));
            page.setBackground(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BACKGROUND)));
            page.setPdf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PDF)));
            page.setPdfPage(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PDF_PAGE)));
            page.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGE_STATUS)));
            pageList.add(page);
        }
    
        // Close the cursor and database to release resources
        cursor.close();
        //db.close();
    
        return pageList;
    }

    // Method to get pages by note ID
    public List<Page> getPagesByNoteId(String noteId){
        return getPagesByNoteId(noteId, false);
    }
    public List<Page> getPagesByNoteId(String noteId, boolean returnAll) {
        List<Page> pageList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve, including the new columns
        String[] projection = {
            COLUMN_PAGE_UUID, COLUMN_NOTE_ID, COLUMN_PAGE_NUMBER, COLUMN_PAGE_DATA,
            COLUMN_BACKGROUND, COLUMN_PDF, COLUMN_PDF_PAGE, COLUMN_PAGE_STATUS // New columns
        };

        // Define the sorting order
        String sortOrder = COLUMN_PAGE_NUMBER + " ASC"; // ASC for ascending order

        // Define the selection criteria
        String selection = COLUMN_NOTE_ID + " = ?";
        String[] selectionArgs = {noteId};

        // Query the "pages" table
        Cursor cursor = db.query(TABLE_PAGES, projection, selection, selectionArgs, null, null, sortOrder);

        // Iterate through the cursor and add entries to the list
        while (cursor.moveToNext()) {
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGE_STATUS));
            if ((status == SyncUtils.STATUS_SYNCED) ||
                    (status == SyncUtils.STATUS_SYNC_PENDING) ||
                    (status == SyncUtils.STATUS_ATTACHMENT_SYNC_PENDING) ||
                    returnAll) {

                Page page = new Page();
                page.setUUID(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_UUID)));
                page.setNoteId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID)));
                page.setNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGE_NUMBER)));
                page.setData(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_DATA)));
                page.setBackground(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BACKGROUND))); // New column
                page.setPdf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PDF)));               // New column
                page.setPdfPage(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PDF_PAGE)));         // New column
                pageList.add(page);
            }
        }

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return pageList;
    }/*
    public String getHashByNoteIDAndNumber(String noteId, int number) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve, including the new columns
        String[] projection = {
                COLUMN_PAGE_UUID, COLUMN_NOTE_ID, COLUMN_PAGE_NUMBER, COLUMN_PAGE_DATA,
                COLUMN_BACKGROUND, COLUMN_PDF, COLUMN_PDF_PAGE, COLUMN_PAGE_HASH // New columns
        };

        // Define the sorting order
        //String sortOrder = COLUMN_PAGE_NUMBER + " ASC"; // ASC for ascending order

        // Define the selection criteria
        String selection = COLUMN_NOTE_ID + " = ? AND " + COLUMN_PAGE_NUMBER + " = ?";
        String[] selectionArgs = {noteId, String.valueOf(number)};

        // Query the "pages" table
        Cursor cursor = db.query(TABLE_PAGES, projection, selection, selectionArgs, null, null, null);

        String hash = null;
        if (cursor.moveToFirst()) {
            hash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_HASH));
        }

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return hash;
    }
    */

    public String getHashByUUID(String uuid) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve, including the new columns
        String[] projection = {
                COLUMN_PAGE_UUID, COLUMN_PAGE_HASH // New columns
        };

        // Define the sorting order
        //String sortOrder = COLUMN_PAGE_NUMBER + " ASC"; // ASC for ascending order

        // Define the selection criteria
        String selection = COLUMN_PAGE_UUID + " = ?";
        String[] selectionArgs = {uuid};

        // Query the "pages" table
        Cursor cursor = db.query(TABLE_PAGES, projection, selection, selectionArgs, null, null, null);

        String hash = null;
        if (cursor.moveToFirst()) {
            hash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_HASH));
        }

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return hash;
    }

    // Method to check if a page exists
    public boolean pageExist(Page pageToCheck, boolean useUUID) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve
        String[] projection = {COLUMN_PAGE_UUID};

        String selection;
        String[] selectionArgs;
        // Define the selection criteria
        if (useUUID) {
            selection = COLUMN_PAGE_UUID + " = ?";
            selectionArgs = new String[]{pageToCheck.getUUID()};
        } else {
            selection = COLUMN_NOTE_ID + " = ? AND " + COLUMN_PAGE_NUMBER + " = ?";
            selectionArgs = new String[]{pageToCheck.getNoteId(), String.valueOf(pageToCheck.getNumber())};

        }
        // Query the "pages" table
        Cursor cursor = db.query(TABLE_PAGES, projection, selection, selectionArgs, null, null, null);

        boolean exists = cursor.moveToFirst();

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return exists;
    }

    // Method to delete all pages
    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PAGES, null, null);
    }

    // Method to get all pages from the database
    public List<Page> getAllPages() {
        List<Page> pageList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns to retrieve, including the new columns
        String[] projection = {
                COLUMN_PAGE_UUID,
                COLUMN_NOTE_ID,
                COLUMN_PAGE_NUMBER,
                COLUMN_PAGE_DATA,
                COLUMN_BACKGROUND, // New column
                COLUMN_PDF,        // New column
                COLUMN_PDF_PAGE    // New column
        };

        Cursor cursor = db.query(
                TABLE_PAGES,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        // Iterate through the cursor and add entries to the list
        while (cursor.moveToNext()) {
            Page page = new Page();
            page.setUUID(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_UUID)));
            page.setNoteId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID)));
            page.setNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGE_NUMBER)));
            page.setData(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_DATA)));
            page.setBackground(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BACKGROUND))); // New column
            page.setPdf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PDF)));               // New column
            page.setPdfPage(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PDF_PAGE)));         // New column
            pageList.add(page);
        }

        // Close the cursor and database to release resources
        cursor.close();
        //db.close();

        return pageList;
    }

    @Override
    public void close() {
        super.close();
        instance = null; // Reset the instance so it can be reinitialized later
    }
}