package com.alpha4technologies.duroodcounter;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

public class DuroodApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        applyStandardFontScale(this);
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
