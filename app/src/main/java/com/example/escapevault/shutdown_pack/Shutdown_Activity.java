package com.example.escapevault.shutdown_pack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.escapevault.R;

public class Shutdown_Activity extends AppCompatActivity {

    private View rootShutdown, batteryGroup;
    private FrameLayout flashOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shutdown);


        rootShutdown = findViewById(R.id.root_shutdown);
        flashOverlay = findViewById(R.id.flash_overlay);
        batteryGroup = findViewById(R.id.battery_group);

        setupImmersiveBlackout();
        setupExitOnLongPress();

        vibrateShort();
        playBatteryFlash();

        // at ~900ms the overlay fades out, revealing the pure-black root
        rootShutdown.postDelayed(this::fadeOverlayToBlack, 900);




    }



    // ================= Immersive fullscreen + min brightness =================
    private void setupImmersiveBlackout() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0.0f; // minimum brightness, matches a dying screen
        getWindow().setAttributes(lp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    // ================= Short triple-buzz, like a flicker before shutdown =================
    private void vibrateShort() {

        SharedPreferences prefs = getSharedPreferences("VibrationPrefs", MODE_PRIVATE);
        boolean isVibrationEnabled = prefs.getBoolean("IS_VIBRATION_ENABLED", true);


        try {

            if (!isVibrationEnabled) return;

            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;

            long[] pattern = {0, 40, 40, 40, 40, 40};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= 🔋 "Battery critically low" fade in → hold → fade out (~1s) =================
    private void playBatteryFlash() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(batteryGroup, "alpha", 0f, 1f);
        fadeIn.setDuration(180);

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(batteryGroup, "alpha", 1f, 0f);
        fadeOut.setDuration(220);
        fadeOut.setStartDelay(520); // hold visible, then fade out before the overlay goes black

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(fadeIn, fadeOut);
        set.start();
    }

    // ================= Overlay fades away, revealing pure #000000 =================
    private void fadeOverlayToBlack() {
        ObjectAnimator fade = ObjectAnimator.ofFloat(flashOverlay, "alpha", 1f, 0f);
        fade.setDuration(1100);
        fade.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                flashOverlay.setVisibility(View.GONE); // pure black root is now fully visible
            }
        });
        fade.start();
    }

    // ================= Long-press anywhere to exit the fake shutdown =================
    private void setupExitOnLongPress() {
        rootShutdown.setOnLongClickListener(v -> {
            finish();
            return true;
        });
    }

    // Intentionally NOT finishing on back-press — a real dead phone doesn't
    // respond to a back button. Only a long press exits, like the warning says.
    @Override
    public void onBackPressed() {
        // no-op, on purpose
    }




}