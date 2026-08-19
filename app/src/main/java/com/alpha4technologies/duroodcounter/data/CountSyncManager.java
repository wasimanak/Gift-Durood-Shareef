package com.alpha4technologies.duroodcounter.data;

import android.content.Context;
import android.util.Log;

import com.alpha4technologies.duroodcounter.model.User;

public class CountSyncManager {
    private static final String TAG = "CountSyncManager";
    public static final int BATCH_THRESHOLD = 100;

    private final PreferencesManager prefsManager;
    private final DuroodRepository repository;
    private boolean isSyncing = false;

    public CountSyncManager(Context context) {
        this.prefsManager = new PreferencesManager(context);
        this.repository = DuroodRepository.getInstance();
    }

    public void onTap() {
        prefsManager.incrementLocalCount();
        long pending = prefsManager.getPendingSyncCount();
        if (pending >= BATCH_THRESHOLD) {
            flushSync();
        }
    }

    public synchronized void flushSync() {
        long pending = prefsManager.getPendingSyncCount();
        User currentUser = prefsManager.getUser();

        if (pending <= 0 || currentUser == null || isSyncing) {
            return;
        }

        isSyncing = true;
        Log.d(TAG, "Flushing sync for pending count: " + pending);

        repository.syncCountBatch(currentUser.getUserId(), pending, new DuroodRepository.SyncCallback() {
            @Override
            public void onSuccess(long updatedTotal, long updatedToday) {
                prefsManager.resetPendingSyncCount();
                prefsManager.updateRemoteSyncedTotals(updatedTotal, updatedToday);
                isSyncing = false;
                Log.d(TAG, "Sync success! Updated Total: " + updatedTotal + ", Today: " + updatedToday);
            }

            @Override
            public void onError(String errorMessage) {
                isSyncing = false;
                Log.e(TAG, "Sync failed: " + errorMessage + ". Will retry on next batch/flush.");
            }
        });
    }
}
