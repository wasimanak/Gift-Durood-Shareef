package com.alpha4technologies.duroodcounter.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alpha4technologies.duroodcounter.model.AppUpdateInfo;
import com.alpha4technologies.duroodcounter.model.DonationInfo;
import com.alpha4technologies.duroodcounter.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DuroodRepository {
    private static final String TAG = "DuroodRepository";
    private static DuroodRepository instance;

    private final FirebaseAuth mAuth;
    private final DatabaseReference mUsersRef;
    private final DatabaseReference mDailyCountsRef;
    private final DatabaseReference mSessionsRef;
    private final DatabaseReference mTasbihRef;
    private final DatabaseReference mAppConfigRef;
    private final DatabaseReference mDonationRef;

    private final MutableLiveData<List<User>> mAllUsersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> mGlobalTodayTotalLiveData = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> mGlobalOverallTotalLiveData = new MutableLiveData<>(0L);
    private final MutableLiveData<String> mTasbihNameLiveData = new MutableLiveData<>("Alhamdulillah");
    private final MutableLiveData<DonationInfo> mDonationInfoLiveData = new MutableLiveData<>();

    private DuroodRepository() {
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Already enabled or initialized
        }

        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        mUsersRef = db.getReference("users");
        mDailyCountsRef = db.getReference("daily_counts");
        mSessionsRef = db.getReference("sessions");
        mTasbihRef = db.getReference("tasbih_info");
        mAppConfigRef = db.getReference("app_config");
        mDonationRef = db.getReference("donation_info");

        listenToLeaderboardData();
        listenToTasbihData();
        listenToDonationData();
    }

    public static synchronized DuroodRepository getInstance() {
        if (instance == null) {
            instance = new DuroodRepository();
        }
        return instance;
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface SyncCallback {
        void onSuccess(long updatedTotal, long updatedToday);
        void onError(String errorMessage);
    }

    /**
     * Start Flow: If profile with matching Full Name & City exists, log in. If not, create new user profile.
     */
    public void startOrLoginUser(String fullName, String city, String country, AuthCallback callback) {
        String cleanName = fullName.trim();
        String cleanCity = city.trim();
        String cleanCountry = country != null ? country.trim() : "";

        mUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User matchedUser = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null && user.getFullName() != null && user.getCity() != null) {
                        if (user.getFullName().equalsIgnoreCase(cleanName) && user.getCity().equalsIgnoreCase(cleanCity)) {
                            matchedUser = user;
                            break;
                        }
                    }
                }

                if (matchedUser != null) {
                    // Profile exists -> Log into existing profile
                    User finalMatchedUser = matchedUser;
                    ensureFirebaseAuth(new AuthCallback() {
                        @Override
                        public void onSuccess(User user) {
                            callback.onSuccess(finalMatchedUser);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            callback.onError(errorMessage);
                        }
                    });
                } else {
                    // Profile does not exist -> Create new registered user
                    ensureFirebaseAuth(new AuthCallback() {
                        @Override
                        public void onSuccess(User authUser) {
                            String uid = authUser.getUserId();
                            String today = PreferencesManager.getTodayDateString();
                            User newUser = new User(
                                    uid,
                                    "",
                                    cleanName,
                                    cleanCity,
                                    cleanCountry,
                                    false,
                                    0,
                                    0,
                                    today,
                                    System.currentTimeMillis()
                            );

                            mUsersRef.child(uid).setValue(newUser)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess(newUser))
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        }

                        @Override
                        public void onError(String errorMessage) {
                            callback.onError(errorMessage);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Convert an existing guest session to a registered user profile.
     */
    public void upgradeGuestToRegistered(String userId, String fullName, String city, String country, AuthCallback callback) {
        if (userId == null || userId.isEmpty()) {
            startOrLoginUser(fullName, city, country, callback);
            return;
        }

        String cleanName = fullName.trim();
        String cleanCity = city.trim();
        String cleanCountry = country != null ? country.trim() : "";

        DatabaseReference userRef = mUsersRef.child(userId);
        userRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User user = currentData.getValue(User.class);
                if (user == null) {
                    user = new User(userId, "", cleanName, cleanCity, cleanCountry, false, 0, 0, PreferencesManager.getTodayDateString(), System.currentTimeMillis());
                } else {
                    user.setFullName(cleanName);
                    user.setCity(cleanCity);
                    user.setCountry(cleanCountry);
                    user.setGuest(false);
                    user.setLastUpdated(System.currentTimeMillis());
                }
                currentData.setValue(user);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    callback.onError(error.getMessage());
                } else if (committed && currentData != null) {
                    User updatedUser = currentData.getValue(User.class);
                    callback.onSuccess(updatedUser);
                } else {
                    callback.onError("Failed to update profile");
                }
            }
        });
    }

    /**
     * Guest Flow: Create anonymous profile with Name: "Unknown User"
     */
    public void startGuestSession(String city, AuthCallback callback) {
        ensureFirebaseAuth(new AuthCallback() {
            @Override
            public void onSuccess(User authUser) {
                String uid = authUser.getUserId();
                String guestCity = (city == null || city.trim().isEmpty()) ? "Unknown" : city.trim();
                String today = PreferencesManager.getTodayDateString();
                String guestUsername = "guest_" + uid.substring(0, Math.min(6, uid.length()));

                User guestUser = new User(
                        uid,
                        guestUsername,
                        "Unknown User",
                        guestCity,
                        "",
                        true,
                        0,
                        0,
                        today,
                        System.currentTimeMillis()
                );

                mUsersRef.child(uid).setValue(guestUser)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(guestUser))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private void ensureFirebaseAuth(AuthCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            callback.onSuccess(new User(currentUser.getUid(), "", "", "", "", true, 0, 0, "", 0));
        } else {
            mAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                    callback.onSuccess(new User(mAuth.getCurrentUser().getUid(), "", "", "", "", true, 0, 0, "", 0));
                } else {
                    Log.w(TAG, "Firebase Auth failed. Fallback to local device UID.", task.getException());
                    String fallbackUid = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    callback.onSuccess(new User(fallbackUid, "", "", "", "", true, 0, 0, "", 0));
                }
            });
        }
    }

    /**
     * Update counts in Firebase using transaction to prevent duplicate writes / race conditions.
     */
    /**
     * Update counts in Firebase for a specific unique RecitationSession using RTDB transaction.
     * Guarantees each session is synced to Firebase only once and updates totalCount and todayCount safely.
     */
    public void syncSession(com.alpha4technologies.duroodcounter.model.RecitationSession session, SyncCallback callback) {
        if (session == null || session.getUserId() == null || session.getUserId().isEmpty() || session.getCount() <= 0) {
            if (callback != null) callback.onError("Invalid session or count");
            return;
        }

        String userId = session.getUserId();
        String sessionId = session.getSessionId();
        long countToAdd = session.getCount();
        String sessionDate = session.getDate() != null ? session.getDate() : PreferencesManager.getTodayDateString();

        DatabaseReference userRef = mUsersRef.child(userId);

        userRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                // Idempotency check: if this specific payload session ID was already processed, do not add count again
                if (currentData.child("synced_sessions").child(sessionId).getValue() != null) {
                    Log.d(TAG, "Payload " + sessionId + " already processed in Firebase. Skipping duplicate addition.");
                    return Transaction.success(currentData);
                }

                User user = currentData.getValue(User.class);
                if (user == null) {
                    user = new User(userId, "", "User", "", "", true, 0, 0, sessionDate, System.currentTimeMillis());
                }

                long currentTotal = user.getTotalCount();
                long currentToday = user.getTodayCount();
                String lastDate = user.getLastDate();

                if (!sessionDate.equals(lastDate)) {
                    currentToday = 0;
                }

                user.setTotalCount(currentTotal + countToAdd);
                user.setTodayCount(currentToday + countToAdd);
                user.setLastDate(sessionDate);
                user.setLastUpdated(System.currentTimeMillis());

                currentData.setValue(user);
                currentData.child("synced_sessions").child(sessionId).setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Session sync transaction failed: " + error.getMessage());
                    if (callback != null) callback.onError(error.getMessage());
                } else if (committed && currentData != null) {
                    User updatedUser = currentData.getValue(User.class);
                    long total = updatedUser != null ? updatedUser.getTotalCount() : 0;
                    long today = updatedUser != null ? updatedUser.getTodayCount() : 0;

                    if (callback != null) callback.onSuccess(total, today);
                } else {
                    if (callback != null) callback.onError("Transaction not committed");
                }
            }
        });
    }

    /**
     * Realtime listener for Leaderboard history of all users.
     */
    private void listenToLeaderboardData() {
        mUsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> userList = new ArrayList<>();
                long globalToday = 0;
                long globalOverall = 0;
                String todayDate = PreferencesManager.getTodayDateString();

                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null) {
                        userList.add(user);
                        globalOverall += user.getTotalCount();
                        if (todayDate.equals(user.getLastDate())) {
                            globalToday += user.getTodayCount();
                        }
                    }
                }

                // Sort by highest total count
                Collections.sort(userList, (u1, u2) -> Long.compare(u2.getTotalCount(), u1.getTotalCount()));

                mAllUsersLiveData.setValue(userList);
                mGlobalTodayTotalLiveData.setValue(globalToday);
                mGlobalOverallTotalLiveData.setValue(globalOverall);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Leaderboard read failed: " + error.getMessage());
            }
        });
    }

    public void updateLocalUserCounts(String userId, long localTotalCount, long localTodayCount) {
        if (userId == null || userId.isEmpty()) return;

        List<User> currentList = mAllUsersLiveData.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = new ArrayList<>(currentList);
        }

        User foundUser = null;
        for (User u : currentList) {
            if (userId.equals(u.getUserId())) {
                foundUser = u;
                break;
            }
        }

        String todayDate = PreferencesManager.getTodayDateString();

        if (foundUser != null) {
            long prevTotal = foundUser.getTotalCount();
            long prevToday = foundUser.getTodayCount();

            long diffTotal = Math.max(0, localTotalCount - prevTotal);
            long diffToday = Math.max(0, localTodayCount - prevToday);

            foundUser.setTotalCount(localTotalCount);
            foundUser.setTodayCount(localTodayCount);
            foundUser.setLastDate(todayDate);

            Long globalOverall = mGlobalOverallTotalLiveData.getValue();
            Long globalToday = mGlobalTodayTotalLiveData.getValue();

            mGlobalOverallTotalLiveData.setValue((globalOverall != null ? globalOverall : 0L) + diffTotal);
            mGlobalTodayTotalLiveData.setValue((globalToday != null ? globalToday : 0L) + diffToday);
        } else {
            User newUser = new User(userId, "", "You", "", "", false, localTotalCount, localTodayCount, todayDate, System.currentTimeMillis());
            currentList.add(newUser);

            Long globalOverall = mGlobalOverallTotalLiveData.getValue();
            Long globalToday = mGlobalTodayTotalLiveData.getValue();

            mGlobalOverallTotalLiveData.setValue((globalOverall != null ? globalOverall : 0L) + localTotalCount);
            mGlobalTodayTotalLiveData.setValue((globalToday != null ? globalToday : 0L) + localTodayCount);
        }

        Collections.sort(currentList, (u1, u2) -> Long.compare(u2.getTotalCount(), u1.getTotalCount()));
        mAllUsersLiveData.setValue(currentList);
    }

    public LiveData<List<User>> getAllUsersLiveData() {
        return mAllUsersLiveData;
    }

    public LiveData<Long> getGlobalTodayTotalLiveData() {
        return mGlobalTodayTotalLiveData;
    }

    public LiveData<Long> getGlobalOverallTotalLiveData() {
        return mGlobalOverallTotalLiveData;
    }

    public LiveData<String> getTasbihNameLiveData() {
        return mTasbihNameLiveData;
    }

    private void listenToTasbihData() {
        mTasbihRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object val = snapshot.child("name").getValue();
                    if (val != null) {
                        String name = String.valueOf(val).trim();
                        if (!name.isEmpty()) {
                            mTasbihNameLiveData.setValue(name);
                            return;
                        }
                    }
                }
                mTasbihNameLiveData.setValue("Alhamdulillah");
                mTasbihRef.child("name").setValue("Alhamdulillah");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Tasbih data read failed: " + error.getMessage());
            }
        });
    }

    public interface AppUpdateCallback {
        void onCheckCompleted(boolean updateRequired, AppUpdateInfo updateInfo);
    }

    public void checkAppUpdate(AppUpdateCallback callback) {
        mAppConfigRef.get().addOnCompleteListener(task -> {
            DataSnapshot snapshot = null;
            if (task.isSuccessful() && task.getResult() != null) {
                snapshot = task.getResult();
            }

            if (snapshot == null || !snapshot.exists()) {
                mAppConfigRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        processUpdateSnapshot(snap, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (callback != null) callback.onCheckCompleted(false, null);
                    }
                });
                return;
            }

            processUpdateSnapshot(snapshot, callback);
        });
    }

    private void processUpdateSnapshot(DataSnapshot snapshot, AppUpdateCallback callback) {
        if (snapshot == null || !snapshot.exists()) {
            AppUpdateInfo defaultConfig = new AppUpdateInfo(
                    1,
                    "1.0",
                    false,
                    "https://play.google.com/store/apps/details?id=com.alpha4technologies.duroodcounter",
                    "Update Available",
                    "A new version of Durood Counter is available. Please update for the best experience."
            );
            mAppConfigRef.setValue(defaultConfig);
            if (callback != null) callback.onCheckCompleted(false, defaultConfig);
            return;
        }

        AppUpdateInfo updateInfo = null;
        try {
            updateInfo = AppUpdateInfo.fromSnapshot(snapshot);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing AppUpdateInfo: " + e.getMessage());
        }

        if (updateInfo == null) {
            if (callback != null) callback.onCheckCompleted(false, null);
            return;
        }

        long currentVersionCode = com.alpha4technologies.duroodcounter.BuildConfig.VERSION_CODE;
        String currentVersionName = com.alpha4technologies.duroodcounter.BuildConfig.VERSION_NAME;

        boolean updateRequired = AppUpdateInfo.isUpdateRequired(currentVersionCode, currentVersionName, updateInfo);
        Log.d(TAG, "App Update Check -> Required: " + updateRequired + ", Force: " + updateInfo.isForceUpdate()
                + ", LatestCode: " + updateInfo.getLatestVersionCode() + ", CurrentCode: " + currentVersionCode);

        if (callback != null) callback.onCheckCompleted(updateRequired, updateInfo);
    }

    public LiveData<DonationInfo> getDonationInfoLiveData() {
        return mDonationInfoLiveData;
    }

    private void listenToDonationData() {
        mDonationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    DonationInfo defaultDonation = new DonationInfo(
                            false,
                            "Meezan Bank",
                            "Durood Counter Community",
                            "0123456789012345",
                            "PK36MEZN0001234567890123",
                            "الحمدللہ! اللہ تعالیٰ کے فضل اور آپ سب کی دعاؤں و محبت کی بدولت ہماری کمیونٹی روز بروز بڑھ رہی ہے۔ زیادہ صارفین کی وجہ سے ہمیں ڈیٹا بیس اور سرور کی سہولیات میں مسلسل اضافہ کرنا پڑتا ہے، جس کے لیے اخراجات بھی بڑھ جاتے ہیں۔ اگر آپ اس صدقۂ جاریہ اور نیکی کے کام میں ہمارا ساتھ دینا چاہیں تو اپنی استطاعت کے مطابق کسی بھی رقم سے تعاون کر سکتے ہیں۔ آپ کی ہر چھوٹی یا بڑی معاونت ہمارے لیے قیمتی ہے۔ اللہ تعالیٰ آپ کو اس کا بہترین اجر عطا فرمائے۔ جزاکم اللہ خیراً۔ 🤍"
                    );
                    mDonationRef.setValue(defaultDonation);
                    mDonationInfoLiveData.setValue(defaultDonation);
                    return;
                }

                DonationInfo donationInfo = null;
                try {
                    donationInfo = DonationInfo.fromSnapshot(snapshot);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing DonationInfo: " + e.getMessage());
                }
                mDonationInfoLiveData.setValue(donationInfo);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Donation info read failed: " + error.getMessage());
            }
        });
    }
}
