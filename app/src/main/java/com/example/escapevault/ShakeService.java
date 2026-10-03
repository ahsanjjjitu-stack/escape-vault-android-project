package com.example.escapevault;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;

import com.example.escapevault.R;
import com.example.escapevault.call_setup_pack.IncomingFakeCallActivity;
import com.example.escapevault.shutdown_pack.Shutdown_Activity;
import com.example.escapevault.shutdown_pack.Shutdown_SetupActivity;
import com.example.escapevault.sms_setup_pack.FakeSmsNotificationHelper;

public class ShakeService extends Service {

    private static final String CHANNEL_ID = "ShakeServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mShakeDetector = new ShakeDetector();


            SharedPreferences prefs = getSharedPreferences("ShakeSensitivityPrefs", MODE_PRIVATE);
            float savedThreshold = prefs.getFloat("SHAKE_THRESHOLD_VAL", 2.7f);
            mShakeDetector.setSensitivityThreshold(savedThreshold);



            mShakeDetector.setOnShakeListener(this::handleShakeTrigger);

            if (mAccelerometer != null) {
                mSensorManager.registerListener(
                        mShakeDetector,
                        mAccelerometer,
                        SensorManager.SENSOR_DELAY_UI
                );
            }
        }
    }

    /**
     * শেক ডিটেক্ট হলে এই মেথড রান হবে
     */
    private void handleShakeTrigger() {
        SharedPreferences prefs = getSharedPreferences("ShakeTriggerPrefs", MODE_PRIVATE);

        boolean isShakeEnabled = prefs.getBoolean("IS_SHAKE_ENABLED", false);
        String shakeType = prefs.getString("SHAKE_TYPE", "");

        if (!isShakeEnabled || shakeType.isEmpty()) return;

        triggerVibration();

        switch (shakeType) {
            case "CALL":
                launchFakeCall(prefs);
                break;
            case "SMS":
                launchFakeSms(prefs);
                break;
            case "SHUTDOWN":
                launchFakeShutdown();
                break;
        }
    }

    private void launchFakeCall(SharedPreferences prefs) {
        String name = prefs.getString("SENDER_NAME", "Unknown");
        String number = prefs.getString("PHONE_NUMBER", "");
        String ringtone = prefs.getString("SHAKE_RINGTONE_URI", "");

        Intent intent = new Intent(this, IncomingFakeCallActivity.class);
        intent.putExtra("CALLER_NAME", name);
        intent.putExtra("PHONE_NUMBER", number);
        intent.putExtra("RINGTONE_URI", ringtone);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void launchFakeSms(SharedPreferences prefs) {
        String name = prefs.getString("SENDER_NAME", "Unknown");
        String number = prefs.getString("PHONE_NUMBER", "");
        String messageBody = prefs.getString("SMS_BODY", "");
        String imageUri = prefs.getString("IMAGE_URI", "");

        FakeSmsNotificationHelper.showNotification(this, name, number, messageBody, imageUri);
    }

    private void launchFakeShutdown() {
        Intent intent = new Intent(this, Shutdown_Activity.class);
        intent.putExtra("FROM_SHAKE", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void triggerVibration() {

        SharedPreferences prefs = getSharedPreferences("VibrationPrefs", MODE_PRIVATE);
        boolean isVibrationEnabled = prefs.getBoolean("IS_VIBRATION_ENABLED", true);

        if (!isVibrationEnabled) return;

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(120);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Escape Vault Active")
                .setContentText("Shake detection is running in background.")
                .setSmallIcon(R.mipmap.ic_launcher) // তোর অ্যাপের আইকন
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Shake Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // অ্যাপ কিল হলেও ওএস সার্ভিস রিসেট করে চালু রাখবে
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null && mShakeDetector != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
