package com.alpha4technologies.duroodcounter.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.alpha4technologies.duroodcounter.DuroodApplication;
import com.alpha4technologies.duroodcounter.data.DuroodRepository;
import com.alpha4technologies.duroodcounter.data.PreferencesManager;
import com.alpha4technologies.duroodcounter.databinding.ActivitySplashBinding;
import com.alpha4technologies.duroodcounter.model.AppUpdateInfo;
import com.alpha4technologies.duroodcounter.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 2000;
    private static final int NETWORK_TIMEOUT_MS = 6000;

    private ActivitySplashBinding binding;
    private PreferencesManager prefsManager;
    private DuroodRepository repository;
    private long startTimeMs;
    private boolean routed = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(DuroodApplication.getFixedFontScaleContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefsManager = new PreferencesManager(this);
        repository = DuroodRepository.getInstance();
        startTimeMs = System.currentTimeMillis();

        checkVersionAndUpdate();
    }

    private void checkVersionAndUpdate() {
        if (!isNetworkAvailable(this)) {
            // Offline fallback: Internet not available, do not block user! Proceed directly after splash delay.
            Log.d(TAG, "Offline mode detected. Proceeding to main app.");
            new Handler(Looper.getMainLooper()).postDelayed(this::routeNextScreen, SPLASH_DELAY_MS);
            return;
        }

        // Network timeout fallback: If connection is poor and Firebase takes > 6s, proceed to app!
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            Log.w(TAG, "Version check timed out. Proceeding to next screen.");
            routeNextScreen();
        };
        timeoutHandler.postDelayed(timeoutRunnable, NETWORK_TIMEOUT_MS);

        repository.checkAppUpdate((updateRequired, updateInfo) -> {
            // Cancel timeout as soon as Firebase responds!
            timeoutHandler.removeCallbacks(timeoutRunnable);

            if (isFinishing() || isDestroyed() || routed) return;

            long elapsed = System.currentTimeMillis() - startTimeMs;
            long remainingDelay = Math.max(0, SPLASH_DELAY_MS - elapsed);

            if (updateRequired && updateInfo != null) {
                Log.d(TAG, "App update required! Showing update dialog immediately...");
                showUpdateDialog(updateInfo);
            } else {
                Log.d(TAG, "App is up to date. Proceeding to next screen.");
                new Handler(Looper.getMainLooper()).postDelayed(this::routeNextScreen, remainingDelay);
            }
        });
    }

    private boolean isNetworkAvailable(Context context) {
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
        return true;
    }

    private void showUpdateDialog(AppUpdateInfo updateInfo) {
        if (isFinishing() || isDestroyed()) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(updateInfo.getUpdateTitle() != null && !updateInfo.getUpdateTitle().trim().isEmpty()
                ? updateInfo.getUpdateTitle().trim() : "Update Available");
        builder.setMessage(updateInfo.getUpdateMessage() != null && !updateInfo.getUpdateMessage().trim().isEmpty()
                ? updateInfo.getUpdateMessage().trim() : "A new version of Durood Counter is available. Please update for the best experience.");

        builder.setPositiveButton("Update Now", (dialog, which) -> {
            String url = updateInfo.getUpdateUrl() != null && !updateInfo.getUpdateUrl().trim().isEmpty()
                    ? updateInfo.getUpdateUrl().trim()
                    : "https://play.google.com/store/apps/details?id=" + getPackageName();
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            } catch (Exception e) {
                // Fallback
            }
            if (updateInfo.isForceUpdate()) {
                finish();
            }
        });

        if (updateInfo.isForceUpdate()) {
            builder.setCancelable(false);
        } else {
            builder.setCancelable(true);
            builder.setNegativeButton("Later", (dialog, which) -> {
                dialog.dismiss();
                routeNextScreen();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(!updateInfo.isForceUpdate());
        dialog.show();
    }

    private synchronized void routeNextScreen() {
        if (isFinishing() || isDestroyed() || routed) return;
        routed = true;

        User currentUser = prefsManager.getUser();

        Intent intent;
        if (prefsManager.isLoggedIn() && currentUser != null && !currentUser.isGuest()) {
            // Registered user -> Go directly to MainActivity
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // New user or Guest -> Go to WelcomeActivity
            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
