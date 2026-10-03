package com.example.escapevault.call_setup_pack;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.escapevault.R;

public class IncomingFakeCallActivity extends AppCompatActivity {

    private TextView callerAvatar, callerName, callerNumber;
    private TextView btnDecline, btnAccept;
    private CircularCallSwipeView circularSwipeView;

    private String name;

    private Ringtone ringtone;
    private Vibrator vibrator;

    // Audio Focus Manager Variables
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupShowOnLockScreen();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incoming_fake_call);

        // ID Find
        callerAvatar = findViewById(R.id.caller_avatar);
        callerName = findViewById(R.id.caller_name);
        callerNumber = findViewById(R.id.caller_number);
        btnDecline = findViewById(R.id.btn_decline);
        btnAccept = findViewById(R.id.btn_accept);
        circularSwipeView = findViewById(R.id.circular_swipe_view);

        // Intent থেকে ডাটা গ্রহণ
        name = getIntent().getStringExtra("CALLER_NAME");
        String number = getIntent().getStringExtra("PHONE_NUMBER");
        String ringtoneUriStr = getIntent().getStringExtra("RINGTONE_URI");

        // UI-তে ডাটা বসানো
        if (name != null && !name.trim().isEmpty()) {
            callerName.setText(name);
            callerAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            callerName.setText("Unknown");
            callerAvatar.setText("U");
        }

        if (number != null && !number.trim().isEmpty()) {
            callerNumber.setText(number + " · incoming call");
        } else {
            callerNumber.setText("mobile · incoming call");
        }

        requestAudioFocus();
        startRingtoneAndVibration(ringtoneUriStr);

        // Button duita circular swipe view-er shathe connect
        circularSwipeView.bindButtons(btnDecline, btnAccept);

        circularSwipeView.setOnSwipeListener(new CircularCallSwipeView.OnSwipeListener() {
            @Override
            public void onAccept() {
                stopRingtoneAndVibration();

                Intent intent = new Intent(IncomingFakeCallActivity.this, CallReceivedActivity.class);
                intent.putExtra("CALLER_NAME", name);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up_in, R.anim.stay_still);
                finish();
            }

            @Override
            public void onDecline() {
                declineCall();
            }
        });
    }

    // ================= Decline logic =================
    private void declineCall() {
        stopRingtoneAndVibration();

        Intent serviceIntent = new Intent(this, CallSchedulerService.class);
        stopService(serviceIntent);

        getSharedPreferences("FakeCallPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        final View root = findViewById(android.R.id.content);
        final int height = Math.max(root.getHeight(), 1);

        ValueAnimator slideDown = ValueAnimator.ofFloat(0f, height);
        slideDown.setDuration(200);
        slideDown.setInterpolator(new android.view.animation.AccelerateInterpolator());
        slideDown.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            root.setTranslationY(value);
            root.setAlpha(1f - (value / height) * 0.3f);
        });
        slideDown.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                android.app.ActivityManager am =
                        (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    for (android.app.ActivityManager.AppTask task : am.getAppTasks()) {
                        task.finishAndRemoveTask();
                    }
                }
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });
        slideDown.start();
    }

    // ================= Audio Focus Management =================
    private void requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;

        audioFocusChangeListener = focusChange -> {};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build();

            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_RING,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            );
        }
    }

    private void releaseAudioFocus() {
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest != null) {
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                }
            } else {
                if (audioFocusChangeListener != null) {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                }
            }
        }
    }

    // ================= Screen Turn On Setup =================
    private void setupShowOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
    }

    // ================= Ringtone & Vibration Helper =================
    private void startRingtoneAndVibration(String ringtoneUriStr) {
        try {
            Uri ringtoneUri;
            if (ringtoneUriStr != null && !ringtoneUriStr.isEmpty()) {
                ringtoneUri = Uri.parse(ringtoneUriStr);
            } else {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.setLooping(true);
                }
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            SharedPreferences prefs = getSharedPreferences("VibrationPrefs", MODE_PRIVATE);
            boolean isVibrationEnabled = prefs.getBoolean("IS_VIBRATION_ENABLED", true);

            if (!isVibrationEnabled) return;

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 1000};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRingtoneAndVibration() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        releaseAudioFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtoneAndVibration();
    }
}
