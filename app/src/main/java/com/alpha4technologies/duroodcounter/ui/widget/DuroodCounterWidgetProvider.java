package com.alpha4technologies.duroodcounter.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.RemoteViews;

import com.alpha4technologies.duroodcounter.R;
import com.alpha4technologies.duroodcounter.data.CountSyncManager;
import com.alpha4technologies.duroodcounter.data.DuroodRepository;
import com.alpha4technologies.duroodcounter.data.PreferencesManager;
import com.alpha4technologies.duroodcounter.ui.MainActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class DuroodCounterWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_WIDGET_TAP = "com.alpha4technologies.duroodcounter.ACTION_WIDGET_TAP";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_WIDGET_TAP.equals(intent.getAction())) {
            // Provide instant haptic vibration feedback
            vibrate(context);

            // Count increment via CountSyncManager (Handles offline SQLite DB, 50-count batching, Delta Sync, and History updates!)
            CountSyncManager syncManager = CountSyncManager.getInstance(context);
            syncManager.onTap();

            // Update all active widget instances immediately
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, DuroodCounterWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        PreferencesManager prefsManager = new PreferencesManager(context);
        long todayCount = prefsManager.getTodayCount();

        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        String formattedCount = formatter.format(todayCount);

        String tasbihName = DuroodRepository.getInstance().getTasbihNameLiveData().getValue();
        if (tasbihName == null || tasbihName.isEmpty()) {
            tasbihName = "Durood Shareef";
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_durood_counter);
        views.setTextViewText(R.id.tvWidgetCount, formattedCount);
        views.setTextViewText(R.id.tvWidgetTasbihName, tasbihName);

        // Tap Intent for counting directly from home screen without opening app
        Intent tapIntent = new Intent(context, DuroodCounterWidgetProvider.class);
        tapIntent.setAction(ACTION_WIDGET_TAP);
        PendingIntent tapPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        views.setOnClickPendingIntent(R.id.btnWidgetTap, tapPendingIntent);

        // Open App Intent
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context,
                1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        views.setOnClickPendingIntent(R.id.btnWidgetOpenApp, openAppPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void vibrate(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(35);
                }
            }
        } catch (Exception e) {
            // Ignore if vibration unavailable
        }
    }
}
