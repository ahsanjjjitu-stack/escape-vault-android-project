package com.example.escapevault;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.escapevault.admob_pack.AdManager;
import com.example.escapevault.admob_pack.NetworkUtils;
import com.example.escapevault.call_setup_pack.CallSchedulerService;
import com.example.escapevault.call_setup_pack.CallSetupActivity;
import com.example.escapevault.call_setup_pack.FakeCallReceiver;
import com.example.escapevault.shutdown_pack.Shutdown_SetupActivity;
import com.example.escapevault.sms_setup_pack.FakeSms_Activity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Bottom nav
    private LinearLayout navHomeBtn, navSettingsBtn;
    private TextView navHomeIcon, navHomeText, navSettingsIcon, navSettingsText;

    // Content (included layouts)
    private View layoutHome, layoutSettings;

    // Home screen internals
    private View cardNoEscape, cardFakeCall;
    private View toolFakeCall, toolFakeSms, toolShutdown;
    private View btnCloseFakeCall, btnSettingsGear;

    // Fake Call Active Elements
    private TextView txtFakeCallTimer, callerNameTv, btn_shake_trigger;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);




        MobileAds.initialize(this, initializationStatus -> {});

        AdManager.loadInterstitialAd(this);

        AdView bannerAdView;
        bannerAdView = findViewById(R.id.adView_home_banner);

        if (NetworkUtils.isInternetAvailable(this)) {
            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAdView.loadAd(adRequest);
        }





        // ---- Bottom nav views ----


        navHomeBtn = findViewById(R.id.nav_home_btn);
        navSettingsBtn = findViewById(R.id.nav_settings_btn);
        navHomeIcon = findViewById(R.id.nav_home_icon);
        navHomeText = findViewById(R.id.nav_home_text);
        navSettingsIcon = findViewById(R.id.nav_settings_icon);
        navSettingsText = findViewById(R.id.nav_settings_text);















        // home layout all work =======================================================

        // ---- Included layouts (Home / Settings) ----
        layoutHome = findViewById(R.id.layout_home);
        layoutSettings = findViewById(R.id.layout_settings);

        // ---- Views inside layout_home ----
        cardNoEscape = layoutHome.findViewById(R.id.card_no_escape);
        cardFakeCall = layoutHome.findViewById(R.id.card_fake_call);
        toolFakeCall = layoutHome.findViewById(R.id.tool_fake_call);
        toolFakeSms = layoutHome.findViewById(R.id.tool_fake_sms);
        toolShutdown = layoutHome.findViewById(R.id.tool_shutdown);
        btnCloseFakeCall = layoutHome.findViewById(R.id.btn_close_fake_call);
        btnSettingsGear = layoutHome.findViewById(R.id.btn_settings_gear);
        callerNameTv = layoutHome.findViewById(R.id.callerNameTv);
        txtFakeCallTimer = layoutHome.findViewById(R.id.fake_call_timer);
        btn_shake_trigger = findViewById(R.id.btn_shake_trigger);




        setupBottomNav();
        setupHomeTools();




        btnCloseFakeCall.setOnClickListener(v -> cancelScheduledFakeCall());
        // Shake trigger pill click listener
        btn_shake_trigger.setOnClickListener(v -> showShakeDialog());





















        // setting layout all work =====================================================


        TextView autoStartBtn = findViewById(R.id.auto_startPermission_btn);

        autoStartBtn.setOnClickListener(v -> openAutoStartSettings());






        TextView disableBatteryBtn = findViewById(R.id.disable_battery_optimization_btn);

        disableBatteryBtn.setOnClickListener(v -> requestIgnoreBatteryOptimization());






        ImageView vibrationSwitch = findViewById(R.id.vibration_switch);

       // App start hole ba screen-e asle current saved state load kora
        updateVibrationSwitchUI(vibrationSwitch);

        // Switch Click Listener
        vibrationSwitch.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("VibrationPrefs", MODE_PRIVATE);
            boolean currentStatus = prefs.getBoolean("IS_VIBRATION_ENABLED", true);

            // Toggle state
            boolean newStatus = !currentStatus;
            prefs.edit().putBoolean("IS_VIBRATION_ENABLED", newStatus).apply();

            // UI update
            updateVibrationSwitchUI(vibrationSwitch);
        });









        LinearLayout shakeSensitivityBtn = findViewById(R.id.shake_sensitivity_btn);
        TextView sensitivityPreviewTv = findViewById(R.id.shake_sensitivity_preview_tv);
        updateSensitivityPreviewUI(sensitivityPreviewTv);

        shakeSensitivityBtn.setOnClickListener(v -> showSensitivityDialog(sensitivityPreviewTv));





        // View Finding
        TextView privacyBtn = findViewById(R.id.privacy_policy_btn);
        TextView rateBtn = findViewById(R.id.rate_btn);
        TextView aboutBtn = findViewById(R.id.about_btn);







        // ১. Privacy Policy Click
        privacyBtn.setOnClickListener(v -> {
            String privacyUrl = "https://ahsanjjjitu-stack.github.io/escape-vault-privacy-policy/";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl));
            startActivity(intent);
        });











        // ৩. About Us Click (Popup Dialog)
        aboutBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("About Escape Vault");
            builder.setMessage("Escape Vault v1.0.0\n\nYour subtle exit mechanism from awkward situations using customizable gesture triggers.\n\nDeveloped with ❤");
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            builder.create().show();
        });











    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndShowScheduledCall();
        updateShakePillUI();
    }







    // ================= Shake Trigger Dialog & Service Controls =================

    private void updateShakePillUI() {
        SharedPreferences prefs = getSharedPreferences("ShakeTriggerPrefs", MODE_PRIVATE);

        boolean isShakeEnabled = prefs.getBoolean("IS_SHAKE_ENABLED", false);
        String shakeType = prefs.getString("SHAKE_TYPE", "");

        if (isShakeEnabled && !shakeType.isEmpty()) {
            btn_shake_trigger.setText("● Shake: " + shakeType);
            btn_shake_trigger.setTextColor(getColor(R.color.purple_primary));
        } else {
            btn_shake_trigger.setText("○ Shake trigger off");
            btn_shake_trigger.setTextColor(getColor(R.color.text_gray));
        }
    }













    private void showShakeDialog() {
        SharedPreferences prefs = getSharedPreferences("ShakeTriggerPrefs", MODE_PRIVATE);
        boolean isShakeEnabled = prefs.getBoolean("IS_SHAKE_ENABLED", false);
        String shakeType = prefs.getString("SHAKE_TYPE", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shake Gesture Trigger");

        // যদি ডাটাই না থাকে
        if (shakeType.isEmpty()) {
            builder.setMessage("No shake action configured yet!\nPlease set up Call, SMS, or Shutdown first.");
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            builder.create().show();
            return;
        }

        String statusMsg = isShakeEnabled
                ? "Shake trigger is currently ACTIVE for " + shakeType + "."
                : "Shake trigger is currently INACTIVE for " + shakeType + ".";

        builder.setMessage(statusMsg + "\n\nChoose an action:");

        String toggleBtnText = isShakeEnabled ? "Turn OFF" : "Turn ON";

        builder.setPositiveButton(toggleBtnText, (dialog, which) -> {
            boolean newState = !isShakeEnabled;

            // একদম আলাদা Key-তে অন/অফ সেভ
            prefs.edit().putBoolean("IS_SHAKE_ENABLED", newState).apply();

            Intent serviceIntent = new Intent(MainActivity.this, ShakeService.class);
            if (newState) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(MainActivity.this, "Shake trigger activated!", Toast.LENGTH_SHORT).show();
            } else {
                stopService(serviceIntent);
                Toast.makeText(MainActivity.this, "Shake trigger deactivated!", Toast.LENGTH_SHORT).show();
            }
            updateShakePillUI();
        });

        builder.setNeutralButton("Clear Data", (dialog, which) -> {
            prefs.edit().clear().apply();
            Intent serviceIntent = new Intent(MainActivity.this, ShakeService.class);
            stopService(serviceIntent);

            updateShakePillUI();
            Toast.makeText(MainActivity.this, "Shake settings reset!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }























    // ================= Schedule Check & UI Update =================

    private void checkAndShowScheduledCall() {
        SharedPreferences prefs = getSharedPreferences("FakeCallPrefs", MODE_PRIVATE);

        // ১. SharedPreferences থেকে Consistent Keys দিয়ে ডাটা রিড করা
        boolean isScheduled = prefs.getBoolean("IS_SCHEDULED", false);
        long triggerTime = prefs.getLong("TRIGGER_TIME", 0);
        String senderName = prefs.getString("SENDER_NAME", "Unknown");
        String scheduleType = prefs.getString("SCHEDULE_TYPE", "CALL"); // Default: CALL

        long currentTime = System.currentTimeMillis();
        long remainingTime = triggerTime - currentTime;

        if (isScheduled && remainingTime > 0) {
            cardNoEscape.setVisibility(View.GONE);
            cardFakeCall.setVisibility(View.VISIBLE);

            if ("SMS".equalsIgnoreCase(scheduleType)) {
                callerNameTv.setText("Fake SMS — " + senderName);
            }
            else if ("CALL".equalsIgnoreCase(scheduleType)) {
                callerNameTv.setText("Fake Call — " + senderName);
            }
            else {
                callerNameTv.setText(senderName);
            }

            startCountDown(remainingTime);

        } else {
            resetCallStateUI();
        }
    }








    private void startCountDown(long durationMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = (millisUntilFinished / 1000) % 60;
                long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                txtFakeCallTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                resetCallStateUI();
            }
        }.start();
    }










    // ================= Cancel Scheduled Call =================

    private void cancelScheduledFakeCall() {
        // ১. AlarmManager Cancel করা
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, FakeCallReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        Intent serviceIntent = new Intent(this, CallSchedulerService.class);
        stopService(serviceIntent);

        // ৩. SharedPref Clear করা
        getSharedPreferences("FakeCallPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        resetCallStateUI();

        Toast.makeText(this, "Fake call canceled!", Toast.LENGTH_SHORT).show();
    }














    private void resetCallStateUI() {
        cardFakeCall.setVisibility(View.GONE);
        cardNoEscape.setVisibility(View.VISIBLE);
    }











    // ================= Bottom navigation =================

    private void setupBottomNav() {
        showHome();

        navHomeBtn.setOnClickListener(v -> showHome());
        navSettingsBtn.setOnClickListener(v -> showSettings());

        if (btnSettingsGear != null) {
            btnSettingsGear.setOnClickListener(v -> showSettings());
        }
    }









    private void showHome() {
        layoutHome.setVisibility(View.VISIBLE);
        layoutSettings.setVisibility(View.GONE);
        setNavSelected(true);
    }









    private void showSettings() {
        layoutHome.setVisibility(View.GONE);
        layoutSettings.setVisibility(View.VISIBLE);
        setNavSelected(false);
    }
















    private void setNavSelected(boolean homeSelected) {
        int active = getColor(R.color.purple_primary);
        int inactive = getColor(R.color.text_gray);

        navHomeIcon.setTextColor(homeSelected ? active : inactive);
        navHomeText.setTextColor(homeSelected ? active : inactive);
        navSettingsIcon.setTextColor(homeSelected ? inactive : active);
        navSettingsText.setTextColor(homeSelected ? inactive : active);
    }














    // ================= Home tools =================

    private void setupHomeTools() {
        toolFakeCall.setOnClickListener(v -> {

            AdManager.showInterstitialAd(MainActivity.this, () -> {
                Intent intent = new Intent(MainActivity.this, CallSetupActivity.class);
                startActivity(intent);
            });
        });

        toolFakeSms.setOnClickListener(v -> {

            AdManager.showInterstitialAd(MainActivity.this, () -> {
                Intent intent = new Intent(MainActivity.this, FakeSms_Activity.class);
                startActivity(intent);
            });
        });

        toolShutdown.setOnClickListener(v -> {
            AdManager.showInterstitialAd(MainActivity.this, () -> {
                Intent intent = new Intent(MainActivity.this, Shutdown_SetupActivity.class);
                startActivity(intent);
            });
        });
    }

































    // setting all method work ==================================================



    private void openAutoStartSettings() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Intent intent = new Intent();

        try {
            if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if (manufacturer.contains("oppo")) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (manufacturer.contains("vivo")) {
                intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if (manufacturer.contains("realme")) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"));
            } else if (manufacturer.contains("huawei")) {
                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
            } else if (manufacturer.contains("samsung")) {
                intent.setComponent(new ComponentName("com.samsung.android.looper", "com.samsung.android.sm.ui.battery.BatteryActivity"));
            } else {
                // Stock Android ba onno manufacturer-er jonno default App Info Settings page open hobe
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            }

            startActivity(intent);
            Toast.makeText(this, "Enable Auto-Start for Escape Vault", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            // Jodi device-er specific ComponentName na paoya jay, tahole App Info page launch hobe
            Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallbackIntent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(fallbackIntent);
            Toast.makeText(this, "Please allow Auto-Start / Background activity from settings", Toast.LENGTH_LONG).show();
        }
    }

















    private void requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();

            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                } catch (Exception e) {
                    openBatterySettingsFallback();
                }
            } else {
                openBatterySettingsFallback();
            }
        } else {
            Toast.makeText(this, "Not required for your Android version", Toast.LENGTH_SHORT).show();
        }
    }

    private void openBatterySettingsFallback() {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Manage battery optimization settings for Escape Vault", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }










    private void updateVibrationSwitchUI(ImageView switchView) {
        SharedPreferences prefs = getSharedPreferences("VibrationPrefs", MODE_PRIVATE);
        boolean isVibrationEnabled = prefs.getBoolean("IS_VIBRATION_ENABLED", true); // Default true

        if (isVibrationEnabled) {
            switchView.setImageResource(R.drawable.ic_switch_on);
        } else {
            switchView.setImageResource(R.drawable.ic_switch_off);
        }
    }











    private void showSensitivityDialog(TextView previewTv) {
        String[] levels = {"Low", "Medium", "High"};
        SharedPreferences prefs = getSharedPreferences("ShakeSensitivityPrefs", MODE_PRIVATE);

        String currentLevel = prefs.getString("SHAKE_SENSITIVITY_LEVEL", "Medium");
        int checkedItem = 1;
        if ("Low".equalsIgnoreCase(currentLevel)) checkedItem = 0;
        else if ("High".equalsIgnoreCase(currentLevel)) checkedItem = 2;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Shake Sensitivity");

        builder.setSingleChoiceItems(levels, checkedItem, (dialog, which) -> {
            String selectedLevel = levels[which];
            float threshold = 2.7f; // Default Medium

            if (which == 0) {
                threshold = 3.2f; // Low Sensitivity
            } else if (which == 1) {
                threshold = 2.7f; // Medium Sensitivity
            } else if (which == 2) {
                threshold = 2.2f; // High Sensitivity
            }

            prefs.edit()
                    .putString("SHAKE_SENSITIVITY_LEVEL", selectedLevel)
                    .putFloat("SHAKE_THRESHOLD_VAL", threshold)
                    .apply();

            previewTv.setText(selectedLevel);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void updateSensitivityPreviewUI(TextView previewTv) {
        SharedPreferences prefs = getSharedPreferences("ShakeTriggerPrefs", MODE_PRIVATE);
        String currentLevel = prefs.getString("SHAKE_SENSITIVITY_LEVEL", "Medium");
        previewTv.setText(currentLevel);
    }







}