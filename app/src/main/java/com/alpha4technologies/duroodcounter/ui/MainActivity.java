package com.alpha4technologies.duroodcounter.ui;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.alpha4technologies.duroodcounter.R;
import com.alpha4technologies.duroodcounter.data.CountSyncManager;
import com.alpha4technologies.duroodcounter.databinding.ActivityMainBinding;
import com.alpha4technologies.duroodcounter.util.DuroodNotificationScheduler;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_CODE_POST_NOTIFICATIONS = 2001;

    private ActivityMainBinding binding;
    private CountSyncManager syncManager;
    private final Fragment counterFragment = new CounterFragment();
    private final Fragment historyFragment = new HistoryFragment();
    private final Fragment aboutFragment = new AboutFragment();
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private Fragment activeFragment = counterFragment;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.alpha4technologies.duroodcounter.DuroodApplication.getFixedFontScaleContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        syncManager = CountSyncManager.getInstance(this);

        fragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, aboutFragment, "3").hide(aboutFragment)
                .add(R.id.fragmentContainer, historyFragment, "2").hide(historyFragment)
                .add(R.id.fragmentContainer, counterFragment, "1")
                .commit();

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_counter) {
                fragmentManager.beginTransaction().hide(activeFragment).show(counterFragment).commit();
                activeFragment = counterFragment;
                return true;
            } else if (itemId == R.id.navigation_history) {
                fragmentManager.beginTransaction().hide(activeFragment).show(historyFragment).commit();
                activeFragment = historyFragment;
                return true;
            } else if (itemId == R.id.navigation_about) {
                fragmentManager.beginTransaction().hide(activeFragment).show(aboutFragment).commit();
                activeFragment = aboutFragment;
                return true;
            }
            return false;
        });

        checkNotificationPermissionAndSchedule();
    }

    private void checkNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_CODE_POST_NOTIFICATIONS);
                return;
            }
        }
        DuroodNotificationScheduler.scheduleDailyNotifications(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE_POST_NOTIFICATIONS) {
            DuroodNotificationScheduler.scheduleDailyNotifications(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (syncManager != null) {
            syncManager.flushSync();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Flush unsynced counts when activity stops or goes to background
        if (syncManager != null) {
            syncManager.flushSync();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Flush unsynced counts when app exits or activity is destroyed
        if (syncManager != null) {
            syncManager.flushSync();
        }
    }
}
