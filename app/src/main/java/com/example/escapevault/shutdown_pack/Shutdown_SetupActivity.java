package com.example.escapevault.shutdown_pack;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.escapevault.R;
import com.example.escapevault.admob_pack.NetworkUtils;
import com.example.escapevault.call_setup_pack.CallSchedulerService;
import com.example.escapevault.call_setup_pack.FakeCallReceiver;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Shutdown_SetupActivity extends AppCompatActivity {

    private TextView btnBack;


    // Delay chips
    private final List<TextView> delayChips = new ArrayList<>();
    private TextView chip5s, chip10s, chip30s, chip1m, chip1h, chipCustom;
    private TextView selectedChip;
    private String selectedDelayLabel = "5s";
    private boolean isCustomSelected = false;




    // Date / Time (only relevant when Custom is selected)
    private View dateTimeLayout;
    private TextView fieldDate, fieldTime;
    private final Calendar scheduledCalendar = Calendar.getInstance();





    // Preview + action
    private TextView btn_schedule_shutdown, btn_shake_shutdown;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupShowOnLockScreen();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shutdown_setup);






        // ১. AdMob SDK Initialize করা
        MobileAds.initialize(this, initializationStatus -> {});


        AdView bannerAdView;
        bannerAdView = findViewById(R.id.adView_shutdown_banner);

        if (NetworkUtils.isInternetAvailable(this)) {
            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAdView.loadAd(adRequest);
        }





        // all id find *********************************************************

        btnBack = findViewById(R.id.btn_back);


        chip5s = findViewById(R.id.chip_5s);
        chip10s = findViewById(R.id.chip_10s);
        chip30s = findViewById(R.id.chip_30s);
        chip1m = findViewById(R.id.chip_1m);
        chip1h = findViewById(R.id.chip_1h);
        chipCustom = findViewById(R.id.chip_custom);

        delayChips.add(chip5s);
        delayChips.add(chip10s);
        delayChips.add(chip30s);
        delayChips.add(chip1m);
        delayChips.add(chip1h);
        delayChips.add(chipCustom);

        dateTimeLayout = findViewById(R.id.date_time_layout);
        fieldDate = findViewById(R.id.field_date);
        fieldTime = findViewById(R.id.field_time);

        btn_schedule_shutdown = findViewById(R.id.btn_schedule_shutdown);
        btn_shake_shutdown = findViewById(R.id.btn_shake_shutdown);





        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });







        // ================= Delay chips =================

        chip5s.setOnClickListener(v -> selectChip(chip5s, "5s"));
        chip10s.setOnClickListener(v -> selectChip(chip10s, "10s"));
        chip30s.setOnClickListener(v -> selectChip(chip30s, "30s"));
        chip1m.setOnClickListener(v -> selectChip(chip1m, "1m"));
        chip1h.setOnClickListener(v -> selectChip(chip1h, "1h"));
        chipCustom.setOnClickListener(v -> selectChip(chipCustom, "Custom"));





        // Default state: first chip selected, date/time hidden
        selectChip(chip5s, "5s");






        // ================= Date / Time pickers =================

        fieldDate.setOnClickListener(v -> showDatePicker());
        fieldTime.setOnClickListener(v -> showTimePicker());












        // ================= Schedule Shutdown Button Click =================
        btn_schedule_shutdown.setOnClickListener(v -> {
            long delayInMillis = calculateDelayMillis();

            if (delayInMillis <= 0) {
                Toast.makeText(this, "Please select a valid future time!", Toast.LENGTH_SHORT).show();
                return;
            }

            long triggerTimeMs = System.currentTimeMillis() + delayInMillis;

            // ১. Android 12+ (API 31+) Exact Alarm Permission Check
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Please grant permission to set exact alarm", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                    return;
                }
            }

            // ২. BroadcastReceiver-এ পাঠানোর জন্য Intent তৈরি
            Intent receiverIntent = new Intent(this, FakeCallReceiver.class);
            receiverIntent.putExtra("SCHEDULE_TYPE", "SHUTDOWN");

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    1003, // Fake Shutdown-এর জন্য ইউনিক Request Code
                    receiverIntent,
                    flags
            );

            // ৩. AlarmManager-এ অ্যালার্ম সেট করা
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTimeMs,
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerTimeMs,
                            pendingIntent
                    );
                }
            }

            // ৪. SharedPreferences-এ ডাটা সেভ করা
            getSharedPreferences("FakeCallPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("SCHEDULE_TYPE", "SHUTDOWN")
                    .putString("SENDER_NAME", "Fake Shutdown")
                    .putLong("TRIGGER_TIME", triggerTimeMs)
                    .putBoolean("IS_SCHEDULED", true)
                    .apply();

            // ৫. Foreground Service (Timer & Notification) স্টার্ট করা
            Intent serviceIntent = new Intent(this, CallSchedulerService.class);
            serviceIntent.putExtra("SCHEDULE_TYPE", "SHUTDOWN");
            serviceIntent.putExtra("SENDER_NAME", "Fake Shutdown");
            serviceIntent.putExtra("TRIGGER_TIME", triggerTimeMs);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Toast.makeText(this, "Fake Shutdown scheduled!", Toast.LENGTH_SHORT).show();

            finish();
            overridePendingTransition(0, 0);
        });













        btn_shake_shutdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSharedPreferences("ShakeTriggerPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("SHAKE_TYPE", "SHUTDOWN")
                        .putBoolean("IS_SHAKE", true)
                        .apply();
                Toast.makeText(Shutdown_SetupActivity.this, "Fake shutdown shake!", Toast.LENGTH_SHORT).show();
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










    private long calculateDelayMillis() {
        if (isCustomSelected) {
            long customTime = scheduledCalendar.getTimeInMillis();
            long now = System.currentTimeMillis();
            return customTime - now;
        }

        switch (selectedDelayLabel) {
            case "10s":
                return 10 * 1000L;
            case "30s":
                return 30 * 1000L;
            case "1m":
                return 60 * 1000L;
            case "1h":
                return 60 * 60 * 1000L;
            case "5s":
            default:
                return 5 * 1000L;
        }
    }













    // ================= Delay chips =================


    private void selectChip(TextView chip, String label) {
        // Reset all chips to unselected style
        for (TextView c : delayChips) {
            c.setBackgroundResource(R.drawable.bg_chip_unselected);
            c.setTextColor(getColor(R.color.text_dark));
        }

        // Apply selected style to the tapped chip
        chip.setBackgroundResource(R.drawable.bg_chip_selected);
        chip.setTextColor(getColor(R.color.white));

        selectedChip = chip;
        selectedDelayLabel = label;
        isCustomSelected = (chip == chipCustom);

        // Date/Time layout only shows up for "Custom"
        dateTimeLayout.setVisibility(isCustomSelected ? View.VISIBLE : View.GONE);

    }









    // ================= setup date and time picker =====================





    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    scheduledCalendar.set(Calendar.YEAR, year);
                    scheduledCalendar.set(Calendar.MONTH, month);
                    scheduledCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    String formatted = android.text.format.DateFormat.format(
                            "dd MMM yyyy", scheduledCalendar).toString();
                    fieldDate.setText(formatted);
                },
                scheduledCalendar.get(Calendar.YEAR),
                scheduledCalendar.get(Calendar.MONTH),
                scheduledCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    scheduledCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    scheduledCalendar.set(Calendar.MINUTE, minute);

                    String formatted = android.text.format.DateFormat.format(
                            "hh:mm a", scheduledCalendar).toString();
                    fieldTime.setText(formatted);
                },
                scheduledCalendar.get(Calendar.HOUR_OF_DAY),
                scheduledCalendar.get(Calendar.MINUTE),
                false
        ).show();
    }







}