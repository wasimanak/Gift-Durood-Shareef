package com.alpha4technologies.duroodcounter.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alpha4technologies.duroodcounter.model.RecitationSession;

import java.util.ArrayList;
import java.util.List;

public class SessionDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "durood_sessions.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_SESSIONS = "recitation_sessions";
    public static final String COLUMN_SESSION_ID = "session_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_COUNT = "count";
    public static final String COLUMN_SYNCED_COUNT = "synced_count";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_SYNCED = "synced";

    private static SessionDatabaseHelper instance;

    public static synchronized SessionDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SessionDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private SessionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_SESSIONS + " (" +
                COLUMN_SESSION_ID + " TEXT PRIMARY KEY, " +
                COLUMN_USER_ID + " TEXT, " +
                COLUMN_COUNT + " INTEGER, " +
                COLUMN_SYNCED_COUNT + " INTEGER DEFAULT 0, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_SYNCED + " INTEGER DEFAULT 0)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_SESSIONS + " ADD COLUMN " + COLUMN_SYNCED_COUNT + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
                onCreate(db);
            }
        }
    }

    public synchronized void insertOrUpdateSession(RecitationSession session) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_ID, session.getSessionId());
        values.put(COLUMN_USER_ID, session.getUserId());
        values.put(COLUMN_COUNT, session.getCount());
        values.put(COLUMN_SYNCED_COUNT, session.getSyncedCount());
        values.put(COLUMN_DATE, session.getDate());
        values.put(COLUMN_TIMESTAMP, session.getTimestamp());
        values.put(COLUMN_SYNCED, session.isSynced() ? 1 : 0);

        db.insertWithOnConflict(TABLE_SESSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized List<RecitationSession> getUnsyncedSessions(String userId) {
        List<RecitationSession> sessionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_USER_ID + " = ? AND " + COLUMN_COUNT + " > " + COLUMN_SYNCED_COUNT;
        String[] selectionArgs = new String[]{userId};

        Cursor cursor = db.query(
                TABLE_SESSIONS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                COLUMN_TIMESTAMP + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int syncedCountIdx = cursor.getColumnIndex(COLUMN_SYNCED_COUNT);
                long syncedCnt = syncedCountIdx != -1 ? cursor.getLong(syncedCountIdx) : 0;

                RecitationSession session = new RecitationSession(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_COUNT)),
                        syncedCnt,
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNCED)) == 1
                );
                sessionList.add(session);
            }
            cursor.close();
        }
        return sessionList;
    }

    public synchronized void updateSessionSyncedCount(String sessionId, long syncedCount) {
        SQLiteDatabase db = this.getWritableDatabase();

        RecitationSession session = getSession(sessionId);
        ContentValues values = new ContentValues();
        values.put(COLUMN_SYNCED_COUNT, syncedCount);
        if (session != null && syncedCount >= session.getCount()) {
            values.put(COLUMN_SYNCED, 1);
        }

        db.update(TABLE_SESSIONS, values, COLUMN_SESSION_ID + " = ?", new String[]{sessionId});
    }

    public synchronized void markSessionSynced(String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        RecitationSession session = getSession(sessionId);

        ContentValues values = new ContentValues();
        values.put(COLUMN_SYNCED, 1);
        if (session != null) {
            values.put(COLUMN_SYNCED_COUNT, session.getCount());
        }

        db.update(TABLE_SESSIONS, values, COLUMN_SESSION_ID + " = ?", new String[]{sessionId});
        cleanupOldSyncedSessions();
    }

    public synchronized void cleanupOldSyncedSessions() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Delete fully synced sessions to keep local phone DB lightweight
            db.delete(TABLE_SESSIONS, COLUMN_SYNCED + " = 1 AND " + COLUMN_SYNCED_COUNT + " >= " + COLUMN_COUNT, null);
        } catch (Exception e) {
            // Ignore
        }
    }

    public synchronized RecitationSession getSession(String sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SESSIONS,
                null,
                COLUMN_SESSION_ID + " = ?",
                new String[]{sessionId},
                null, null, null
        );

        RecitationSession session = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int syncedCountIdx = cursor.getColumnIndex(COLUMN_SYNCED_COUNT);
                long syncedCnt = syncedCountIdx != -1 ? cursor.getLong(syncedCountIdx) : 0;

                session = new RecitationSession(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_COUNT)),
                        syncedCnt,
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNCED)) == 1
                );
            }
            cursor.close();
        }
        return session;
    }
}
