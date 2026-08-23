package com.alpha4technologies.duroodcounter;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

public class DuroodApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Force Light (Day) Mode system-wide and disable Night Theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        applyStandardFontScale(this);

        // Schedule 2-hour periodic background sync using WorkManager
        com.alpha4technologies.duroodcounter.util.DuroodSyncWorker.enqueuePeriodicSync(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(getFixedFontScaleContext(base));
    }

    public static Context getFixedFontScaleContext(Context context) {
        Configuration config = context.getResources().getConfiguration();
        if (config.fontScale != 1.0f) {
            config.fontScale = 1.0f;
            return context.createConfigurationContext(config);
        }
        return context;
    }

    public static void applyStandardFontScale(Context context) {
        Configuration config = context.getResources().getConfiguration();
        if (config.fontScale != 1.0f) {
            config.fontScale = 1.0f;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            }
        }
    }
}
