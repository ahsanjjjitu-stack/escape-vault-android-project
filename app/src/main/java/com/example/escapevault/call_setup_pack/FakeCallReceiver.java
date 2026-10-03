package com.example.escapevault.call_setup_pack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.escapevault.shutdown_pack.Shutdown_Activity;
import com.example.escapevault.sms_setup_pack.FakeSmsNotificationHelper;

public class FakeCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String type_schedule = intent.getStringExtra("SCHEDULE_TYPE");
        if ("CALL".equals(type_schedule)){
            String callerName = intent.getStringExtra("CALLER_NAME");
            String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            String photoUri = intent.getStringExtra("PHOTO_URI");
            String ringtoneUri = intent.getStringExtra("RINGTONE_URI");


            Intent callIntent = new Intent(context, IncomingFakeCallActivity.class);
            callIntent.putExtra("CALLER_NAME", callerName);
            callIntent.putExtra("PHONE_NUMBER", phoneNumber);
            callIntent.putExtra("PHOTO_URI", photoUri);
            callIntent.putExtra("RINGTONE_URI", ringtoneUri);

            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            context.startActivity(callIntent);

        }

        else if ("SMS".equals(type_schedule)){
            String senderName = intent.getStringExtra("SENDER_NAME");
            String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            String messageBody = intent.getStringExtra("SMS_BODY");
            String imageUrl = intent.getStringExtra("IMAGE_URI");



            FakeSmsNotificationHelper.showNotification(context, senderName, phoneNumber, messageBody, imageUrl);

            context.getSharedPreferences("FakeCallPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("IS_SCHEDULED", false)
                    .apply();

        }

        else {
            Intent shutdownIntent = new Intent(context, Shutdown_Activity.class);

            shutdownIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            context.startActivity(shutdownIntent);
            context.getSharedPreferences("FakeCallPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("IS_SCHEDULED", false)
                    .apply();

        }


    }




}
