package com.alpha4technologies.duroodcounter.data;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alpha4technologies.duroodcounter.model.RecitationSession;
import com.alpha4technologies.duroodcounter.model.User;

import java.util.List;

public class CountSyncManager {
    private static final String TAG = "CountSyncManager";
    public static final int BATCH_THRESHOLD = 50;

    private static CountSyncManager instance;

    public static synchronized CountSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new CountSyncManager(context.getApplicationContext());
        }
        return instance;
    }

    private final Context context;
    private final PreferencesManager prefsManager;
    private final SessionDatabaseHelper dbHelper;
    private final DuroodRepository repository;
    private boolean isSyncing = false;
    private boolean isNetworkCallbackRegistered = false;

    public CountSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefsManager = new PreferencesManager(context);
        this.dbHelper = SessionDatabaseHelper.getInstance(context);
        this.repository = DuroodRepository.getInstance();

        registerNetworkCallback();
    }

    private synchronized void registerNetworkCallback() {
        if (isNetworkCallbackRegistered) return;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

                cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                    private boolean isInitialCallback = true;

                    @Override
                    public void onAvailable(@NonNull Network network) {
                        Log.d(TAG, "Internet connection state changed: Available");
                        if (isInitialCallback) {
                            isInitialCallback = false;
                            // Do NOT auto-trigger sync on initial callback registration
                            return;
                        }
                        syncUnsyncedSessions();
                    }
                });
                isNetworkCallbackRegistered = true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not register network callback: " + e.getMessage());
        }
    }

    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    public void onTap() {
        prefsManager.incrementLocalCount();

        User user = prefsManager.getUser();
        if (user != null) {
            String activeSessionId = prefsManager.getActiveSessionId();
            long currentSessionCount = prefsManager.getSessionCount();
            String todayDate = PreferencesManager.getTodayDateString();

            RecitationSession existing = dbHelper.getSession(activeSessionId);
            long syncedCount = existing != null ? existing.getSyncedCount() : 0;

            RecitationSession session = new RecitationSession(
                    activeSessionId,
                    user.getUserId(),
                    currentSessionCount,
                    syncedCount,
                    todayDate,
                    System.currentTimeMillis(),
                    syncedCount >= currentSessionCount && currentSessionCount > 0
            );

            // Persist session locally with current count and syncedCount
            dbHelper.insertOrUpdateSession(session);

            // Reflect count update in History & Leaderboard locally in real-time offline!
            repository.updateLocalUserCounts(user.getUserId(), prefsManager.getTotalCount(), prefsManager.getTodayCount());

            // Update Home Screen Widget display
            updateWidgetDisplay();

            // ONLY sync to Firebase when unsynced delta reaches or exceeds 50 counts!
            if (session.getUnsyncedDelta() >= BATCH_THRESHOLD) {
                syncUnsyncedSessions();
            }
        }
    }

    private void updateWidgetDisplay() {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, com.alpha4technologies.duroodcounter.ui.widget.DuroodCounterWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int id : appWidgetIds) {
                com.alpha4technologies.duroodcounter.ui.widget.DuroodCounterWidgetProvider.updateAppWidget(context, appWidgetManager, id);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public synchronized void syncUnsyncedSessions() {
        if (isSyncing || !isNetworkAvailable()) {
            return;
        }

        User currentUser = prefsManager.getUser();
        if (currentUser == null || currentUser.getUserId() == null || currentUser.getUserId().isEmpty()) {
            return;
        }

        List<RecitationSession> unsyncedList = dbHelper.getUnsyncedSessions(currentUser.getUserId());
        if (unsyncedList.isEmpty()) {
            return;
        }

        isSyncing = true;
        Log.d(TAG, "Found " + unsyncedList.size() + " unsynced session(s). Starting sync...");

        syncNextSession(unsyncedList, 0);
    }

    private void syncNextSession(List<RecitationSession> unsyncedList, int index) {
        if (index >= unsyncedList.size()) {
            isSyncing = false;
            Log.d(TAG, "All unsynced sessions processed.");
            return;
        }

        RecitationSession session = unsyncedList.get(index);
        long deltaToSync = session.getUnsyncedDelta();

        if (deltaToSync <= 0) {
            // Skip already synced session
            syncNextSession(unsyncedList, index + 1);
            return;
        }

        long targetSyncedCount = session.getCount();

        // Immediately update syncedCount locally so process exit never re-syncs this delta!
        dbHelper.updateSessionSyncedCount(session.getSessionId(), targetSyncedCount);

        // Create delta session payload for Firebase transaction
        RecitationSession deltaSessionPayload = new RecitationSession(
                session.getSessionId(),
                session.getUserId(),
                deltaToSync,
                0,
                session.getDate(),
                session.getTimestamp(),
                false
        );

        repository.syncSession(deltaSessionPayload, new DuroodRepository.SyncCallback() {
            @Override
            public void onSuccess(long updatedTotal, long updatedToday) {
                prefsManager.updateRemoteSyncedTotals(updatedTotal, updatedToday);
                Log.d(TAG, "Session delta synced successfully: " + session.getSessionId() + " (delta: " + deltaToSync + ")");

                // If synced session was the active session, start a fresh new active session for subsequent taps
                if (session.getSessionId().equals(prefsManager.getActiveSessionId())) {
                    prefsManager.startNewSession();
                }

                // Proceed to next unsynced session
                syncNextSession(unsyncedList, index + 1);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to sync session delta " + session.getSessionId() + ": " + errorMessage);
                // Revert syncedCount back if network transaction explicitly fails
                dbHelper.updateSessionSyncedCount(session.getSessionId(), session.getSyncedCount());
                isSyncing = false;
            }
        });
    }

    public void flushSync() {
        syncUnsyncedSessions();
    }
}
