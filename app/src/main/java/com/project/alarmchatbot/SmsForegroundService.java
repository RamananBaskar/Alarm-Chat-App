package com.project.alarmchatbot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.AsyncTask;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SmsForegroundService extends Service {

    private static final String NOTIFICATION_CHANNEL_ID = "ForegroundServiceChannel";
    private static final String NOTIFICATION_CHANNEL_NAME = "Default Channel";

    private SmsManager smsManager;
    private BroadcastReceiver smsReceiver;

    private NotificationManager notificationManager;


    @Override
    public void onCreate() {
        super.onCreate();
        smsManager = SmsManager.getDefault();
        registerSmsReceiver();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

    }

    private void createNotificationChannel(){
        // Create a notification channel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = NOTIFICATION_CHANNEL_ID;
            String channelName = NOTIFICATION_CHANNEL_NAME;
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void showNotification(String title, String content) {

        // Create an intent to open your app when the notification is tapped
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notifications_icon) // Replace with your own icon
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification
        notificationManager.notify(0, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent Intent = new Intent(this, MainActivity.class);
        PendingIntent pendingintent = PendingIntent.getActivity(this, 0, Intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Alarm is looking for alerts")
                .setContentText("Service is running in Foreground")
                .setContentIntent(pendingintent)
                .setSmallIcon(R.drawable.notifications_icon);

        startForeground(2,builder.build());

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This service does not support binding
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSmsReceiver();
    }

    private void registerSmsReceiver() {
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Handle incoming SMS
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                            String messageBody = smsMessage.getMessageBody();
                            String senderPhoneNumber = smsMessage.getOriginatingAddress();


                            // Notify UI using LocalBroadcastManager
                            notifyUI(messageBody,senderPhoneNumber);
                        }
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, intentFilter);
    }

    private void unregisterSmsReceiver() {
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
        }
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return dateFormat.format(new Date());
    }
    private void insertMessageIntoDatabase(String message) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                String receivedMessage = params[0];
                String timestamp = getCurrentTimestamp();

                Message message = new Message(receivedMessage, Message.SENT_BY_BOT, timestamp);

                // Insert the new message into the database
                AppDatabase.getInstance(SmsForegroundService.this).messageDao().insert(message);

                return null;
            }
        }.execute(message);
    }

    private void notifyUI(String message,String senderPhoneNumber) {
        Intent intent = new Intent("sms-received");
        intent.putExtra("message", message);
        intent.putExtra("mobileNumber",senderPhoneNumber);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        insertMessageIntoDatabase(message);
        showNotification("New Alert from Alarm:", message);
        }

    }
