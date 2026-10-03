package com.example.escapevault.call_setup_pack;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.escapevault.MainActivity;
import com.example.escapevault.R;

public class CallSchedulerService extends Service {

    private static final String CHANNEL_ID = "fake_scheduler_channel";
    private static final int NOTIF_ID = 2001;
    private CountDownTimer countDownTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            long triggerTimeMs = intent.getLongExtra("TRIGGER_TIME", 0);
            String senderName = intent.getStringExtra("SENDER_NAME");
            String scheduleType = intent.getStringExtra("SCHEDULE_TYPE");

            if (senderName == null || senderName.isEmpty()) {
                senderName = "Unknown";
            }
            if (scheduleType == null || scheduleType.isEmpty()) {
                scheduleType = "CALL";
            }

            long remainingMillis = triggerTimeMs - System.currentTimeMillis();

            if (remainingMillis > 0) {
                // Initial Title Setup
                String title;
                if (scheduleType.equalsIgnoreCase("SHUTDOWN")) {
                    title = "Fake Shutdown Scheduled";
                } else if (scheduleType.equalsIgnoreCase("SMS")) {
                    title = "Fake SMS Scheduled";
                } else {
                    title = "Fake Call Scheduled";
                }

                String initialText = scheduleType.equalsIgnoreCase("SHUTDOWN")
                        ? "Preparing Fake Shutdown..."
                        : "Preparing for " + senderName + "...";

                Notification notification = buildNotification(title, initialText);
                startForeground(NOTIF_ID, notification);
                startTimer(remainingMillis, senderName, scheduleType);
            } else {
                stopSelf();
            }
        }
        return START_NOT_STICKY;
    }

    private void startTimer(long totalMillis, String senderName, String scheduleType) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(totalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                long minutes = seconds / 60;
                long remainingSeconds = seconds % 60;

                String timeFormatted = String.format("%02d:%02d", minutes, remainingSeconds);

                String notifTitle;
                String notifText;

                // Type অনুযায়ী ডায়নামিক টেক্সট সেট
                if (scheduleType.equalsIgnoreCase("SHUTDOWN")) {
                    notifTitle = "Fake Shutdown Active";
                    notifText = "System shutdown in " + timeFormatted;
                } else if (scheduleType.equalsIgnoreCase("SMS")) {
                    notifTitle = "Scheduled Fake SMS Active";
                    notifText = "Incoming SMS from " + senderName + " in " + timeFormatted;
                } else {
                    notifTitle = "Scheduled Fake Call Active";
                    notifText = "Incoming call from " + senderName + " in " + timeFormatted;
                }

                Notification updatedNotif = buildNotification(notifTitle, notifText);
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.notify(NOTIF_ID, updatedNotif);
                }
            }

            @Override
            public void onFinish() {
                stopSelf();
            }
        };

        countDownTimer.start();
    }

    private Notification buildNotification(String title, String content) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fake Scheduler Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows remaining time for scheduled events");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}