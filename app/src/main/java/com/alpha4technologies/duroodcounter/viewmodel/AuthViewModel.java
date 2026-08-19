package com.alpha4technologies.duroodcounter.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alpha4technologies.duroodcounter.data.DuroodRepository;
import com.alpha4technologies.duroodcounter.data.PreferencesManager;
import com.alpha4technologies.duroodcounter.model.User;

public class AuthViewModel extends AndroidViewModel {
    private final DuroodRepository repository;
    private final PreferencesManager prefsManager;

    private final MutableLiveData<AuthResult> authResultLiveData = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = DuroodRepository.getInstance();
        prefsManager = new PreferencesManager(application);
    }

    public LiveData<AuthResult> getAuthResultLiveData() {
        return authResultLiveData;
    }

    public void startOrLoginUser(String fullName, String city, String country) {
        if (fullName.trim().isEmpty() || city.trim().isEmpty()) {
            authResultLiveData.setValue(AuthResult.error("Please enter both Full Name and City."));
            return;
        }

        authResultLiveData.setValue(AuthResult.loading());
        repository.startOrLoginUser(fullName, city, country, new DuroodRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                prefsManager.saveUser(user);
                authResultLiveData.setValue(AuthResult.success(user));
            }

            @Override
            public void onError(String errorMessage) {
                authResultLiveData.setValue(AuthResult.error(errorMessage));
            }
        });
    }

    public void upgradeGuestUser(String fullName, String city, String country) {
        if (fullName.trim().isEmpty() || city.trim().isEmpty()) {
            authResultLiveData.setValue(AuthResult.error("Please enter both Full Name and City."));
            return;
        }

        User current = prefsManager.getUser();
        String uid = current != null ? current.getUserId() : "";

        authResultLiveData.setValue(AuthResult.loading());
        repository.upgradeGuestToRegistered(uid, fullName, city, country, new DuroodRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                prefsManager.saveUser(user);
                authResultLiveData.setValue(AuthResult.success(user));
            }

            @Override
            public void onError(String errorMessage) {
                authResultLiveData.setValue(AuthResult.error(errorMessage));
            }
        });
    }

    public void startGuestSession(String city) {
        authResultLiveData.setValue(AuthResult.loading());
        repository.startGuestSession(city, new DuroodRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                prefsManager.saveUser(user);
                authResultLiveData.setValue(AuthResult.success(user));
            }

            @Override
            public void onError(String errorMessage) {
                authResultLiveData.setValue(AuthResult.error(errorMessage));
            }
        });
    }

    public static class AuthResult {
        public enum Status { LOADING, SUCCESS, ERROR }

        public final Status status;
        public final User user;
        public final String errorMessage;

        private AuthResult(Status status, User user, String errorMessage) {
            this.status = status;
            this.user = user;
            this.errorMessage = errorMessage;
        }

        public static AuthResult loading() {
            return new AuthResult(Status.LOADING, null, null);
        }

        public static AuthResult success(User user) {
            return new AuthResult(Status.SUCCESS, user, null);
        }

        public static AuthResult error(String msg) {
            return new AuthResult(Status.ERROR, null, msg);
        }
    }
}
