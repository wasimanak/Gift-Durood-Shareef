package com.alpha4technologies.duroodcounter.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alpha4technologies.duroodcounter.data.CountSyncManager;
import com.alpha4technologies.duroodcounter.data.PreferencesManager;
import com.alpha4technologies.duroodcounter.model.User;

public class CounterViewModel extends AndroidViewModel implements CountSyncManager.OnSyncCompleteListener {
    private final PreferencesManager prefsManager;
    private final CountSyncManager syncManager;

    private final MutableLiveData<Long> sessionCount = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> todayCount = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalCount = new MutableLiveData<>(0L);
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    public CounterViewModel(@NonNull Application application) {
        super(application);
        prefsManager = new PreferencesManager(application);
        syncManager = CountSyncManager.getInstance(application);
        syncManager.addOnSyncCompleteListener(this);
        refreshCounts();
    }

    @Override
    public void onSyncComplete() {
        refreshCounts();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        syncManager.removeOnSyncCompleteListener(this);
    }

    public void refreshCounts() {
        currentUser.setValue(prefsManager.getUser());
        sessionCount.setValue(prefsManager.getSessionCount());
        todayCount.setValue(prefsManager.getTodayCount());
        totalCount.setValue(prefsManager.getTotalCount());
    }

    public LiveData<Long> getSessionCount() {
        return sessionCount;
    }

    public LiveData<Long> getTodayCount() {
        return todayCount;
    }

    public LiveData<Long> getTotalCount() {
        return totalCount;
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<String> getTasbihName() {
        return com.alpha4technologies.duroodcounter.data.DuroodRepository.getInstance().getTasbihNameLiveData();
    }

    public LiveData<com.alpha4technologies.duroodcounter.model.DonationInfo> getDonationInfo() {
        return com.alpha4technologies.duroodcounter.data.DuroodRepository.getInstance().getDonationInfoLiveData();
    }

    public LiveData<Long> getGlobalOverallTotal() {
        return com.alpha4technologies.duroodcounter.data.DuroodRepository.getInstance().getGlobalOverallTotalLiveData();
    }

    public void incrementCount() {
        syncManager.onTap();
        sessionCount.setValue(prefsManager.getSessionCount());
        todayCount.setValue(prefsManager.getTodayCount());
        totalCount.setValue(prefsManager.getTotalCount());
    }

    public void resetSessionCount() {
        prefsManager.resetSessionCount();
        sessionCount.setValue(0L);
    }

    public void flushPendingSync() {
        syncManager.flushSync();
        refreshCounts();
    }
}
