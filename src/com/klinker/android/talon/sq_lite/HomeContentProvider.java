package com.klinker.android.talon.sq_lite;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import android.provider.Settings.System;

public class HomeContentProvider extends ContentProvider {
    static final String TAG = "HomeTimeline";

    public static final String AUTHORITY = "com.klinker.android.talon.provider";
    static final String BASE_PATH = "tweet_id";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private Context context;
    private HomeSQLiteHelper helper;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        context = getContext();
        helper = new HomeSQLiteHelper(context);

        return (helper == null) ? false : true;
    }

    @Override
    public String getType(Uri uri) {
        String ret = getContext().getContentResolver().getType(System.CONTENT_URI);
        Log.d(TAG, "getType returning: " + ret);
        return ret;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert uri: " + uri.toString());

        Uri result = null;

        SQLiteDatabase db = helper.getWritableDatabase();
        long rowID = db.insert(HomeSQLiteHelper.TABLE_HOME, null, values);

        if (rowID > 0) {
            // Return a URI to the newly created row on success
            result = ContentUris.withAppendedId(Uri.parse(AUTHORITY + "/status"), rowID);

            // Notify the Context's ContentResolver of the change
            getContext().getContentResolver().notifyChange(result, null);
        }

        db.close();

        return Uri.parse(BASE_PATH + "/" + rowID);
    }

    // arg[0] is the account
    // arg[1] is the position
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        Log.d(TAG, "update uri: " + uri.toString());
        SQLiteDatabase db = helper.getWritableDatabase();

        HomeDataSource database = new HomeDataSource(context);
        Cursor cursor = database.getUnreadCursor(Integer.parseInt(selectionArgs[0]));

        if (cursor.moveToPosition(Integer.parseInt(selectionArgs[1]))) {
            long tweetId = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));

            ContentValues cv = new ContentValues();
            cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);

            db.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});
        }

        db.close();

        context.getContentResolver().notifyChange(uri, null);

        return 1;
    }

    @Override
    public int delete(Uri uri, String id, String[] selectionArgs) {
        Log.d(TAG, "delete uri: " + uri.toString());
        SQLiteDatabase db = helper.getWritableDatabase();
        int count;

        String segment = uri.getLastPathSegment();
        count = db.delete(HomeSQLiteHelper.TABLE_HOME, HomeSQLiteHelper.COLUMN_TWEET_ID
                + " = " + id, null);


        if (count > 0) {
            // Notify the Context's ContentResolver of the change
            getContext().getContentResolver().notifyChange(uri, null);
        }

        db.close();
        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query with uri: " + uri.toString());

        SQLiteDatabase db = helper.getWritableDatabase();

        // A convenience class to help build the query
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(helper.TABLE_HOME);
        qb.appendWhere(helper.COLUMN_ID + "=" + uri.getLastPathSegment());
        String orderBy = HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC";

        HomeDataSource data = new HomeDataSource(context);
        data.open();
        Cursor c = data.getCursor(Integer.parseInt(selectionArgs[0]));//qb.query(db,
                //projection, HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + selectionArgs[0], null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        c.setNotificationUri(context.getContentResolver(), uri);

        db.close();
        return c;
    }

}
