package com.alpha4technologies.duroodcounter.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.alpha4technologies.duroodcounter.R;
import com.alpha4technologies.duroodcounter.data.CountSyncManager;
import com.alpha4technologies.duroodcounter.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private CountSyncManager syncManager;
    private final Fragment counterFragment = new CounterFragment();
    private final Fragment historyFragment = new HistoryFragment();
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

        syncManager = new CountSyncManager(this);

        fragmentManager.beginTransaction()
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
            }
            return false;
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Flush unsynced counts when activity stops or goes to background
        if (syncManager != null) {
            syncManager.flushSync();
        }
    }
}
