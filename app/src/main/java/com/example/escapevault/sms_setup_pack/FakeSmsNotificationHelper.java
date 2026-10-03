package com.example.escapevault.sms_setup_pack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.example.escapevault.R;

public class FakeSmsNotificationHelper {

    private static final String SMS_CHANNEL_ID = "fake_sms_channel";
    private static final int SMS_NOTIF_ID = 3001;

    public static void showNotification(Context context, String senderName, String phoneNumber, String smsBody, String photoUriStr) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    SMS_CHANNEL_ID,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        // ২. PendingIntent setup
        Intent openIntent = new Intent(context, FakeChatActivity.class);
        openIntent.putExtra("name", senderName);
        openIntent.putExtra("msg", smsBody);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Bitmap senderBitmap = null;
        if (photoUriStr != null && !photoUriStr.isEmpty()) {
            try {
                senderBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(photoUriStr));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String displayName = (senderName != null && !senderName.isEmpty()) ? senderName : phoneNumber;

        Person.Builder personBuilder = new Person.Builder()
                .setName(displayName);

        if (senderBitmap != null) {
            personBuilder.setIcon(IconCompat.createWithBitmap(senderBitmap));
        }

        Person sender = personBuilder.build();

        // ৫. MessagingStyle ব্যবহার করা
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(sender)
                .setConversationTitle(displayName)
                .addMessage(
                        smsBody != null && !smsBody.isEmpty() ? smsBody : "New message received",
                        System.currentTimeMillis(),
                        sender
                );

        // ৬. Notification Builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SMS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_message)
                .setSubText("Messages")
                .setStyle(messagingStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (senderBitmap != null) {
            builder.setLargeIcon(senderBitmap);
        }

        manager.notify(SMS_NOTIF_ID, builder.build());
    }
}