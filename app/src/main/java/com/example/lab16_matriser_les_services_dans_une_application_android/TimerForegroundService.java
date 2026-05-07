package com.example.lab16_matriser_les_services_dans_une_application_android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimerForegroundService extends Service {

    private static final String TAG = "TimerService";
    private final IBinder timerBinder = new TimerBinder();
    
    private int elapsedSeconds = 0;
    private boolean timerActive = false;
    private ScheduledExecutorService scheduler;
    private static final int FOREGROUND_TIMER_ID = 5000;
    private static final String CHANNEL_ID = "timer_notifications_channel";
    private NotificationManager notifyManager;

    public interface TimerUpdateListener {
        void onTimeUpdated(String formattedTime);
    }

    private TimerUpdateListener updateListener;

    public void setUpdateListener(TimerUpdateListener listener) {
        this.updateListener = listener;
    }

    public class TimerBinder extends Binder {
        public TimerForegroundService getService() {
            return TimerForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        setupNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String actionRequest = (intent != null) ? intent.getAction() : null;

        if ("STOP_TIMER_ACTION".equals(actionRequest)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!timerActive) {
            timerActive = true;
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(FOREGROUND_TIMER_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(FOREGROUND_TIMER_ID, notification);
            }
            initiateTimer();
        }
        return START_STICKY;
    }

    private void initiateTimer() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                elapsedSeconds++;
                final String timeStr = getFormattedTime(elapsedSeconds);
                refreshNotification();

                if (updateListener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (updateListener != null) {
                            updateListener.onTimeUpdated(timeStr);
                        }
                    });
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "Chronomètre Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chan.setSound(null, null);
            notifyManager.createNotificationChannel(chan);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service Chronomètre")
                .setContentText("Écoulé : " + getFormattedTime(elapsedSeconds))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void refreshNotification() {
        if (timerActive) {
            notifyManager.notify(FOREGROUND_TIMER_ID, buildNotification());
        }
    }

    private String getFormattedTime(int totalSecs) {
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return timerBinder;
    }

    @Override
    public void onDestroy() {
        timerActive = false;
        if (scheduler != null) {
            scheduler.shutdown();
        }
        stopForeground(true);
        super.onDestroy();
    }
}
