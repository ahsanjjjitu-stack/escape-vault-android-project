package com.example.escapevault.call_setup_pack;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.escapevault.R;

public class CallReceivedActivity extends AppCompatActivity {


    private TextView callerAvatar, callerName;
    private Chronometer callTimer;
    private TextView btn_decline_received;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupShowOnLockScreen();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_call_received);







        callerAvatar = findViewById(R.id.call_avatar);
        callerName = findViewById(R.id.call_name);
        callTimer = findViewById(R.id.call_timer);





        String name = getIntent().getStringExtra("CALLER_NAME");

        if (name != null && !name.trim().isEmpty()) {
            callerName.setText(name);
            callerAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            callerName.setText("Unknown");
            callerAvatar.setText("U");
        }



        // Real, actually-ticking call duration
        callTimer.setBase(SystemClock.elapsedRealtime());
        callTimer.start();












        btn_decline_received = findViewById(R.id.btn_decline_received);

        btn_decline_received.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent serviceIntent = new Intent(CallReceivedActivity.this, CallSchedulerService.class);
                stopService(serviceIntent);

                getSharedPreferences("FakeCallPrefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();

                final View root = findViewById(android.R.id.content);

                ValueAnimator slideDown = ValueAnimator.ofFloat(0f, root.getHeight());
                slideDown.setDuration(200);
                slideDown.setInterpolator(new android.view.animation.AccelerateInterpolator());
                slideDown.addUpdateListener(animation -> {
                    float value = (float) animation.getAnimatedValue();
                    root.setTranslationY(value);
                    root.setAlpha(1f - (value / root.getHeight()) * 0.3f); 
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
        });

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











}