package com.example.escapevault.call_setup_pack;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
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
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.escapevault.MainActivity;
import com.example.escapevault.R;
import com.example.escapevault.admob_pack.NetworkUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CallSetupActivity extends AppCompatActivity {

    // Top bar
    private TextView btnBack;



    // Caller info image
    private ImageView photoPicker;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher galleryLauncher;




    // Caller info name number ringtone
    private EditText editCallerName, editPhoneNumber;
    private TextView btnAddNumber;









    // Contact Picker Launchers
    private ActivityResultLauncher contactPickerLauncher;
    private ActivityResultLauncher permissionLauncher;








    // Ringtone call
    private TextView fieldRingtone;
    private Uri selectedRingtoneUri = null;
    private ActivityResultLauncher ringtonePickerLauncher;










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
    private TextView textPreview, btnScheduleCall, btn_schedule_shake_call;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_call_setup);





        // ১. AdMob SDK Initialize করা
        MobileAds.initialize(this, initializationStatus -> {});


        AdView bannerAdView;
        bannerAdView = findViewById(R.id.adView_callSetup_banner);

        if (NetworkUtils.isInternetAvailable(this)) {
            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAdView.loadAd(adRequest);
        }










        // all id find *********************************************************

        btnBack = findViewById(R.id.btn_back);

        photoPicker = findViewById(R.id.photo_picker);
        editCallerName = findViewById(R.id.edit_caller_name);
        editPhoneNumber = findViewById(R.id.edit_phone_number);
        btnAddNumber = findViewById(R.id.btn_add_number);
        fieldRingtone = findViewById(R.id.field_ringtone);

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

        textPreview = findViewById(R.id.text_preview);
        btnScheduleCall = findViewById(R.id.btn_schedule_call);
        btn_schedule_shake_call = findViewById(R.id.btn_schedule_shake_call);













        // set up back btn *******************************************************

        setupBackButton();


















        // ================= Gallery Image Picker Setup =================


        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        photoPicker.setImageURI(uri);
                        photoPicker.setClipToOutline(true);
                    }
                }
        );



        photoPicker.setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });

























        // ========================== contact picker ==================================

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





        btnAddNumber.setOnClickListener(v -> {
            checkAndRequestContactPermission();
        });






















        // ======================================= ringtone call ===========================================

        selectedRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (selectedRingtoneUri != null) {
            Ringtone defaultRingtone = RingtoneManager.getRingtone(this, selectedRingtoneUri);
            if (defaultRingtone != null) {
                fieldRingtone.setText(defaultRingtone.getTitle(this));
            }
        }



        ringtonePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

                        if (uri != null) {
                            selectedRingtoneUri = uri;
                            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                            fieldRingtone.setText(ringtone.getTitle(this));
                        } else {
                            selectedRingtoneUri = null;
                            fieldRingtone.setText("Silent");
                        }
                    }
                }
        );

        fieldRingtone.setOnClickListener(v -> openRingtonePicker());




















           // ================= Delay chips =================

            chip5s.setOnClickListener(v -> selectChip(chip5s, "5s"));
            chip10s.setOnClickListener(v -> selectChip(chip10s, "10s"));
            chip30s.setOnClickListener(v -> selectChip(chip30s, "30s"));
            chip1m.setOnClickListener(v -> selectChip(chip1m, "1m"));
            chip1h.setOnClickListener(v -> selectChip(chip1h, "1h"));
            chipCustom.setOnClickListener(v -> selectChip(chipCustom, "Custom"));





        // Default state: first chip selected, date/time hidden
        selectChip(chip5s, "5s");

        updatePreview();


















        // ================= Date / Time pickers =================

            fieldDate.setOnClickListener(v -> showDatePicker());
            fieldTime.setOnClickListener(v -> showTimePicker());






        // Live preview

            editCallerName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    updatePreview();
                }
            });

















        // schedule btn click ***********************************************************

        btnScheduleCall.setOnClickListener(v -> {
            scheduleFakeCall();
        });



        btn_schedule_shake_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String callerName = editCallerName.getText().toString().trim();
                if (callerName.isEmpty()) {
                    callerName = "Unknown";
                }

                String phoneNumber = editPhoneNumber.getText().toString().trim();
                if (phoneNumber.isEmpty()) {
                    Toast.makeText(CallSetupActivity.this, "Please select number!", Toast.LENGTH_SHORT).show();
                    return;
                }


                String ringtoneUriStr = (selectedRingtoneUri != null) ? selectedRingtoneUri.toString() : "";





                // ================= SharedPreferences Data Save =================
                getSharedPreferences("ShakeTriggerPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("SHAKE_TYPE", "CALL")
                        .putString("SENDER_NAME", callerName)
                        .putString("PHONE_NUMBER", phoneNumber)
                        .putString("SHAKE_RINGTONE_URI", ringtoneUriStr)
                        .putBoolean("IS_SHAKE", true)
                        .apply();

                Toast.makeText(CallSetupActivity.this, "Fake call shake!", Toast.LENGTH_SHORT).show();




            }
        });







    }










    // ================= Top bar =================

    private void setupBackButton() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
                    editPhoneNumber.setText(cleanNumber);
                }

                if (name != null && !name.trim().isEmpty() && !isPhoneNumber(name, cleanNumber)) {
                    editCallerName.setText(name);
                } else {
                    editCallerName.setText("Unknown");
                }

                // ৩. ছবির লজিক
                if (photoUriStr != null) {
                    Uri photoUri = Uri.parse(photoUriStr);
                    photoPicker.setImageURI(photoUri);
                    photoPicker.setClipToOutline(true);
                } else {
                    photoPicker.setImageResource(R.drawable.bg_photo_circle);
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

















    // ================================  Ringtone method ===========================================

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

        if (selectedRingtoneUri != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtoneUri);
        }

        ringtonePickerLauncher.launch(intent);
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

        updatePreview();
    }










    // ================= Delay chips update preview =================

    private void updatePreview() {
        String callerName = editCallerName.getText().toString().trim();
        if (callerName.isEmpty()) {
            callerName = "Unknown";
        }

        String when;
        if (isCustomSelected) {
            when = "on " + fieldDate.getText() + ", " + fieldTime.getText();
        } else {
            when = "in " + selectedDelayLabel;
        }

        String preview = String.format(Locale.getDefault(),
                "Preview: incoming call from \"%s\" %s", callerName, when);
        textPreview.setText(preview);
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
                    updatePreview();
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
                    updatePreview();
                },
                scheduledCalendar.get(Calendar.HOUR_OF_DAY),
                scheduledCalendar.get(Calendar.MINUTE),
                false
        ).show();
    }
























// ================= Schedule Call Core Logic =================

    private void scheduleFakeCall() {
        String callerName = editCallerName.getText().toString().trim();
        if (callerName.isEmpty()) {
            callerName = "Unknown";
        }

        String phoneNumber = editPhoneNumber.getText().toString().trim();
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please select number!", Toast.LENGTH_SHORT).show();
            return;
        }

        long triggerTimeMillis;

        if (isCustomSelected) {
            triggerTimeMillis = scheduledCalendar.getTimeInMillis();
            if (triggerTimeMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, "Please select a future time!", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            long delayMillis = getDelayMillis(selectedDelayLabel);
            triggerTimeMillis = System.currentTimeMillis() + delayMillis;
        }



        // ৩. Android 12+ (API 31+) Exact Alarm Permission Check
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Please grant permission to set exact alarm", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return;
            }
        }




        // ৪. BroadcastReceiver-এ পাঠানোর জন্য Intent তৈরি করা
        Intent intent = new Intent(this, FakeCallReceiver.class);
        intent.putExtra("SCHEDULE_TYPE", "CALL");
        intent.putExtra("CALLER_NAME", callerName);
        intent.putExtra("PHONE_NUMBER", phoneNumber);

        String photoUriStr = (selectedImageUri != null) ? selectedImageUri.toString() : "";
        String ringtoneUriStr = (selectedRingtoneUri != null) ? selectedRingtoneUri.toString() : "";

        intent.putExtra("PHOTO_URI", photoUriStr);
        intent.putExtra("RINGTONE_URI", ringtoneUriStr);





        // PendingIntent তৈরি (Flags)
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                1001, // Unique Request Code
                intent,
                flags
        );




        // ৫. AlarmManager-এ অ্যালার্ম সেট করা
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
            }





            // ================= Notification Service Start =================

            if (triggerTimeMillis > System.currentTimeMillis()) {
                Intent serviceIntent = new Intent(this, CallSchedulerService.class);
                serviceIntent.putExtra("SCHEDULE_TYPE", "CALL");
                serviceIntent.putExtra("TRIGGER_TIME", triggerTimeMillis);
                serviceIntent.putExtra("SENDER_NAME", callerName);
                serviceIntent.putExtra("PHONE_NUMBER", phoneNumber);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }

            // ================= SharedPreferences Data Save =================
            getSharedPreferences("FakeCallPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("SCHEDULE_TYPE", "CALL")
                    .putString("SENDER_NAME", callerName)
                    .putString("PHONE_NUMBER", phoneNumber)
                    .putString("IMAGE_URI", photoUriStr)
                    .putString("SCHEDULED_RINGTONE_URI", ringtoneUriStr)
                    .putLong("TRIGGER_TIME", triggerTimeMillis)
                    .putBoolean("IS_SCHEDULED", true)
                    .apply();

            Toast.makeText(this, "Fake call scheduled!", Toast.LENGTH_SHORT).show();


            finish();
            overridePendingTransition(0, 0);
        }
    }




    private long getDelayMillis(String label) {
        switch (label) {
            case "5s":  return 5 * 1000L;
            case "10s": return 10 * 1000L;
            case "30s": return 30 * 1000L;
            case "1m":  return 60 * 1000L;
            case "1h":  return 60 * 60 * 1000L;
            default:    return 5 * 1000L;
        }
    }















}