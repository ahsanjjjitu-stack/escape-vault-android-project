package com.example.escapevault.sms_setup_pack;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.escapevault.MainActivity;
import com.example.escapevault.R;
import com.example.escapevault.admob_pack.NetworkUtils;
import com.example.escapevault.call_setup_pack.CallSchedulerService;
import com.example.escapevault.call_setup_pack.CallSetupActivity;
import com.example.escapevault.call_setup_pack.FakeCallReceiver;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FakeSms_Activity extends AppCompatActivity {



    // back btn
    private TextView btn_back_fake_sms;


    // Caller info image
    private ImageView photo_picker_fake_sms;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher galleryLauncher;



    // Contact Picker Launchers
    private TextView btn_add_number_fake_sms;
    private ActivityResultLauncher contactPickerLauncher;
    private ActivityResultLauncher permissionLauncher;




    // user info
    private EditText edit_caller_name_fake_sms, edit_phone_number_fake_sms;




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





    private TextView btn_schedule_sms, btn_shake_sms;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fake_sms);




        // ১. AdMob SDK Initialize করা
        MobileAds.initialize(this, initializationStatus -> {});


        AdView bannerAdView;
        bannerAdView = findViewById(R.id.adView_sms_banner);

        if (NetworkUtils.isInternetAvailable(this)) {
            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAdView.loadAd(adRequest);
        }








        // back id find ==========================================

        btn_back_fake_sms = findViewById(R.id.btn_back_fake_sms);

        btn_back_fake_sms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });







        // image find and pick to gallery =====================================

        photo_picker_fake_sms = findViewById(R.id.photo_picker_fake_sms);


        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        photo_picker_fake_sms.setImageURI(uri);
                        photo_picker_fake_sms.setClipToOutline(true);
                    }
                }
        );



        photo_picker_fake_sms.setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });









        // edit text find ==============================================
        edit_phone_number_fake_sms = findViewById(R.id.edit_phone_number_fake_sms);
        edit_caller_name_fake_sms = findViewById(R.id.edit_caller_name_fake_sms);
        btn_schedule_sms = findViewById(R.id.btn_schedule_sms);
        btn_shake_sms = findViewById(R.id.btn_shake_sms);










        // ========================== contact picker ==================================

        btn_add_number_fake_sms = findViewById(R.id.btn_add_number_fake_sms);

        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri contactUri = result.getData().getData();
                        if (contactUri != null) {
                            fetchContactDetails(contactUri);
                        }
                    }
                }
        );






        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openContactPicker();
                    } else {
                        Toast.makeText(this, "Permission denied to access contacts", Toast.LENGTH_SHORT).show();
                    }
                }
        );





        btn_add_number_fake_sms.setOnClickListener(v -> {
            checkAndRequestContactPermission();
        });

















        // chips id find ============================================================

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
















        // ================= Schedule SMS Button Click =================


        btn_schedule_sms.setOnClickListener(v -> {
            String name = edit_caller_name_fake_sms.getText().toString().trim();
            String number = edit_phone_number_fake_sms.getText().toString().trim();

            EditText editSmsBody = findViewById(R.id.edit_message);
            String messageBody = (editSmsBody != null) ? editSmsBody.getText().toString().trim() : "";

            if (name.isEmpty()) name = "Unknown";
            if (number.isEmpty()) number = "Unknown";
            if (messageBody.isEmpty()) messageBody = "Hey, urgent! Call me back.";

            long delayInMillis = calculateDelayMillis();

            if (delayInMillis <= 0) {
                Toast.makeText(this, "Please select a valid future time!", Toast.LENGTH_SHORT).show();
                return;
            }

            long triggerTimeMs = System.currentTimeMillis() + delayInMillis;
            String photoUriStr = (selectedImageUri != null) ? selectedImageUri.toString() : "";

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

            // ২. BroadcastReceiver-এ পাঠানোর জন্য Intent তৈরি করা
            Intent receiverIntent = new Intent(this, FakeCallReceiver.class);
            receiverIntent.putExtra("SCHEDULE_TYPE", "SMS");
            receiverIntent.putExtra("SENDER_NAME", name);
            receiverIntent.putExtra("PHONE_NUMBER", number);
            receiverIntent.putExtra("SMS_BODY", messageBody);
            receiverIntent.putExtra("IMAGE_URI", photoUriStr);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    1002, // SMS এর জন্য ইউনিক Request Code (Fake Call-এ ছিল 1001)
                    receiverIntent,
                    flags
            );

            // ৩. AlarmManager-এ অ্যালার্ম সেট করা (যা আসল রিসিভারকে ট্রিগার করবে)
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

            // ৪. Save state to SharedPreferences
            getSharedPreferences("FakeCallPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("SCHEDULE_TYPE", "SMS")
                    .putString("SENDER_NAME", name)
                    .putString("PHONE_NUMBER", number)
                    .putString("SMS_BODY", messageBody)
                    .putString("IMAGE_URI", photoUriStr)
                    .putLong("TRIGGER_TIME", triggerTimeMs)
                    .putBoolean("IS_SCHEDULED", true)
                    .apply();

            // ৫. Start Unified Foreground Service (Timer & Notification)
            Intent serviceIntent = new Intent(this, CallSchedulerService.class);
            serviceIntent.putExtra("SCHEDULE_TYPE", "SMS");
            serviceIntent.putExtra("SENDER_NAME", name);
            serviceIntent.putExtra("PHONE_NUMBER", number);
            serviceIntent.putExtra("SMS_BODY", messageBody);
            serviceIntent.putExtra("IMAGE_URI", photoUriStr);
            serviceIntent.putExtra("TRIGGER_TIME", triggerTimeMs);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Toast.makeText(this, "Fake SMS scheduled!", Toast.LENGTH_SHORT).show();

            finish();
            overridePendingTransition(0, 0);
        });







        btn_shake_sms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                String name = edit_caller_name_fake_sms.getText().toString().trim();
                String number = edit_phone_number_fake_sms.getText().toString().trim();

                EditText editSmsBody = findViewById(R.id.edit_message);
                String messageBody = (editSmsBody != null) ? editSmsBody.getText().toString().trim() : "";

                if (name.isEmpty()) name = "Unknown";
                if (number.isEmpty()) number = "Unknown";
                if (messageBody.isEmpty()) messageBody = "Hey, urgent! Call me back.";
                String photoUriStr = (selectedImageUri != null) ? selectedImageUri.toString() : "";

                // ৪. Save state to SharedPreferences
                getSharedPreferences("ShakeTriggerPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("SHAKE_TYPE", "SMS")
                        .putString("SENDER_NAME", name)
                        .putString("PHONE_NUMBER", number)
                        .putString("SMS_BODY", messageBody)
                        .putString("IMAGE_URI", photoUriStr)
                        .putBoolean("IS_SHAKE", true)
                        .apply();


                Toast.makeText(FakeSms_Activity.this, "Fake sms shake!", Toast.LENGTH_SHORT).show();



            }
        });
























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
















    // ====================== contact picker method =====================================

    private void checkAndRequestContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            openContactPicker();
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }



    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }


    private void fetchContactDetails(Uri contactUri) {
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        };

        try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {

                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);

                String name = cursor.getString(nameIndex);
                String number = cursor.getString(numberIndex);
                String photoUriStr = cursor.getString(photoIndex);

                String cleanNumber = "";
                if (number != null) {
                    cleanNumber = number.replaceAll("\\s+", "").replaceAll("-", "");
                    edit_phone_number_fake_sms.setText(cleanNumber);
                }

                if (name != null && !name.trim().isEmpty() && !isPhoneNumber(name, cleanNumber)) {
                    edit_caller_name_fake_sms.setText(name);
                } else {
                    edit_caller_name_fake_sms.setText("Unknown");
                }

                // ৩. ছবির লজিক
                if (photoUriStr != null) {
                    Uri photoUri = Uri.parse(photoUriStr);
                    photo_picker_fake_sms.setImageURI(photoUri);
                    photo_picker_fake_sms.setClipToOutline(true);
                } else {
                    photo_picker_fake_sms.setImageResource(R.drawable.bg_photo_circle);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load contact info", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPhoneNumber(String name, String cleanNumber) {
        String cleanName = name.replaceAll("\\s+", "").replaceAll("-", "");
        return cleanName.equals(cleanNumber) || cleanName.matches("^[+]?[0-9]+$");
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










}