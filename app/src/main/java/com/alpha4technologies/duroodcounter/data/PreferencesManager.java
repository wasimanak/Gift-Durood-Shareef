package com.alpha4technologies.duroodcounter.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.alpha4technologies.duroodcounter.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PreferencesManager {
    private static final String PREF_NAME = "durood_counter_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_CITY = "city";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_IS_GUEST = "is_guest";
    private static final String KEY_SESSION_COUNT = "session_count";
    private static final String KEY_TODAY_COUNT = "today_count";
    private static final String KEY_TOTAL_COUNT = "total_count";
    private static final String KEY_PENDING_SYNC = "pending_sync_count";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_ACTIVE_SESSION_ID = "active_session_id";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        checkAndResetDailyCount();
    }

    public String getOrCreateDeviceId() {
        String id = prefs.getString("device_uid", null);
        if (id == null) {
            id = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            prefs.edit().putString("device_uid", id).apply();
        }
        return id;
    }

    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    public void checkAndResetDailyCount() {
        String today = getTodayDateString();
        String lastDate = prefs.getString(KEY_LAST_DATE, "");
        if (!today.equals(lastDate)) {
            prefs.edit()
                    .putString(KEY_LAST_DATE, today)
                    .putLong(KEY_TODAY_COUNT, 0)
                    .apply();
        }
    }

    public String getActiveSessionId() {
        String sessionId = prefs.getString(KEY_ACTIVE_SESSION_ID, null);
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "session_" + UUID.randomUUID().toString().replace("-", "");
            prefs.edit().putString(KEY_ACTIVE_SESSION_ID, sessionId).apply();
        }
        return sessionId;
    }

    public String startNewSession() {
        return startNewSessionWithCount(0);
    }

    public String startNewSessionWithCount(long initialCount) {
        String newSessionId = "session_" + UUID.randomUUID().toString().replace("-", "");
        prefs.edit()
                .putString(KEY_ACTIVE_SESSION_ID, newSessionId)
                .putLong(KEY_SESSION_COUNT, Math.max(0, initialCount))
                .apply();
        return newSessionId;
    }

    public void updateActiveSessionCount(long newCount) {
        prefs.edit()
                .putLong(KEY_SESSION_COUNT, Math.max(0, newCount))
                .apply();
    }

    public void saveUser(User user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.putString(KEY_CITY, user.getCity());
        editor.putString(KEY_COUNTRY, user.getCountry());
        editor.putBoolean(KEY_IS_GUEST, user.isGuest());

        // FIX #3: Never overwrite local totals with a lower Firebase value.
        // If user was offline for 1-2 days, Firebase may return stale lower counts.
        // Always keep the higher of the two values to prevent counter reversal.
        long localTotal = prefs.getLong(KEY_TOTAL_COUNT, 0);
        long localToday = prefs.getLong(KEY_TODAY_COUNT, 0);
        editor.putLong(KEY_TOTAL_COUNT, Math.max(localTotal, user.getTotalCount()));
        editor.putLong(KEY_TODAY_COUNT, Math.max(localToday, user.getTodayCount()));

        editor.putString(KEY_LAST_DATE, getTodayDateString());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public User getUser() {
        if (!isLoggedIn()) return null;
        return new User(
                prefs.getString(KEY_USER_ID, ""),
                prefs.getString(KEY_USERNAME, ""),
                prefs.getString(KEY_FULL_NAME, ""),
                prefs.getString(KEY_CITY, ""),
                prefs.getString(KEY_COUNTRY, ""),
                prefs.getBoolean(KEY_IS_GUEST, false),
                prefs.getLong(KEY_TOTAL_COUNT, 0),
                prefs.getLong(KEY_TODAY_COUNT, 0),
                prefs.getString(KEY_LAST_DATE, getTodayDateString()),
                System.currentTimeMillis()
        );
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public long getSessionCount() {
        return prefs.getLong(KEY_SESSION_COUNT, 0);
    }

    public long getTodayCount() {
        checkAndResetDailyCount();
        return prefs.getLong(KEY_TODAY_COUNT, 0);
    }

    public long getTotalCount() {
        return prefs.getLong(KEY_TOTAL_COUNT, 0);
    }

    public long getPendingSyncCount() {
        return prefs.getLong(KEY_PENDING_SYNC, 0);
    }

    public void incrementLocalCount() {
        checkAndResetDailyCount();
        long session = getSessionCount() + 1;
        long today = getTodayCount() + 1;
        long total = getTotalCount() + 1;
        long pending = getPendingSyncCount() + 1;

        prefs.edit()
                .putLong(KEY_SESSION_COUNT, session)
                .putLong(KEY_TODAY_COUNT, today)
                .putLong(KEY_TOTAL_COUNT, total)
                .putLong(KEY_PENDING_SYNC, pending)
                .apply();
    }

    public void resetPendingSyncCount() {
        prefs.edit().putLong(KEY_PENDING_SYNC, 0).apply();
    }

    public void resetSessionCount() {
        startNewSession();
    }

    public void updateRemoteSyncedTotals(long remoteTotal, long remoteToday) {
        // FIX #1: Only overwrite local counts with remote values if remote is >= local.
        // After 1-2 days offline, Firebase can return a stale value lower than our local count.
        // Taking max() ensures we never lose locally accumulated offline counts.
        long currentLocalTotal = prefs.getLong(KEY_TOTAL_COUNT, 0);
        long currentLocalToday = prefs.getLong(KEY_TODAY_COUNT, 0);
        prefs.edit()
                .putLong(KEY_TOTAL_COUNT, Math.max(currentLocalTotal, remoteTotal))
                .putLong(KEY_TODAY_COUNT, Math.max(currentLocalToday, remoteToday))
                .apply();
    }
}
