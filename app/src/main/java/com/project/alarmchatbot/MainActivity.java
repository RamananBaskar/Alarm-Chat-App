package com.project.alarmchatbot;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ImageView;
import android.os.AsyncTask;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Bundle;
import java.lang.ref.WeakReference;
import android.telephony.SmsManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.security.SecureRandom;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    SmsManager smsManager;


    private static final String PREF_MOBILE_NUMBER = "pref_mobile_number";
    private String currentMobileNumber;
    private FingerprintManagerCompat fingerprintManager;
    private Executor executor;
    private BiometricPrompt biometricPrompt;

    // Define a BroadcastReceiver to handle incoming SMS messages
    private final String[] permissions = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.POST_NOTIFICATIONS
            // Add other permissions as needed
    };

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract the message from the intent
            String message = intent.getStringExtra("message");
            // Call addToChat to update the UI
            addToChat(message,Message.SENT_BY_BOT);
        }
    };


    private static final int PERMISSION_REQUEST_CODE = 123; // You can choose any value

    // Request permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        messageList = new ArrayList<>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentMobileNumber = preferences.getString(PREF_MOBILE_NUMBER, "Enter here");


        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        ImageView redCircle = findViewById(R.id.red_circle);
        redCircle.setVisibility(View.VISIBLE);

        // Set up SMS Manager
        smsManager = SmsManager.getDefault();

        IntentFilter intentFilter = new IntentFilter("sms-received");
        LocalBroadcastManager.getInstance(this).registerReceiver(smsReceiver, intentFilter);


        Intent serviceIntent = new Intent(this, SmsForegroundService.class);
        serviceIntent.putExtra("mobileNumber", currentMobileNumber);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
        else {
            startService(serviceIntent);

        }


        // Setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        // Inside onCreate, after initializing messageList
        new DatabaseOperationAsyncTask(this).execute();


        sendButton.setOnClickListener((v) -> {
            String question = messageEditText.getText().toString().trim();

            if (!question.isEmpty()) {
                addToChat(question, Message.SENT_BY_ME);
                Message sentMessage = new Message(question,Message.SENT_BY_ME,getCurrentTimestamp())  ;
                new DatabaseOperationAsyncTask(MainActivity.this).execute(sentMessage);
                messageEditText.setText("");

                String smsMessage;
                if (question.equalsIgnoreCase("On")) {
                    smsMessage = "B";
                } else if (question.equalsIgnoreCase("Off")) {
                    smsMessage = "C";
                } else {
                    smsMessage = question;
                }




                // Send the user's message as an SMS
                smsManager.sendTextMessage(currentMobileNumber, null, smsMessage, null, null);
            }

            else {}
        });

        // Change Number Button
        Button changeNumberButton = findViewById(R.id.changeNumberButton);
        changeNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangeNumberDialog();
            }
        });

        fingerprintManager = FingerprintManagerCompat.from(this);
        executor = Executors.newSingleThreadExecutor();

        // Fingerprint Button
        ImageButton fingerprintButton = findViewById(R.id.fingerprint);
        fingerprintButton.setOnClickListener(v -> authenticateWithFingerprint());
    }

    private void sendMobileNumberIntent(String mobileNumber) {
        Intent serviceIntent = new Intent(this, SmsForegroundService.class);
        serviceIntent.putExtra("mobileNumber", mobileNumber);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
    }

    // Method to show a dialog for changing the GSM module number
    private void showChangeNumberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit GSM Module Number");

        // Add an EditText view for the user to enter the new number
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setText(currentMobileNumber); // Display the current number
        builder.setView(input);

        // Set up the buttons for OK and Cancel
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newMobileNumber = input.getText().toString();

                // Check if the mobile number is changed
                if (!newMobileNumber.equals(currentMobileNumber)) {
                    currentMobileNumber = newMobileNumber;

                    // Save the new number to SharedPreferences
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(PREF_MOBILE_NUMBER, currentMobileNumber);
                    editor.apply();

                    // Make the red_circle invisible
                    ImageView redCircle = findViewById(R.id.red_circle);
                    redCircle.setVisibility(View.GONE);

                    sendMobileNumberIntent(currentMobileNumber);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void authenticateWithFingerprint() {
        if (fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()) {
            biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    sendRandomizedMessage();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    addToChat("Authentication failed! Try again.", Message.SENT_BY_ME);
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authenticate with Fingerprint")
                    .setNegativeButtonText("Cancel")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            addToChat("Authentication failed! Your device might not be compatible or try again after adding new fingerprints.", Message.SENT_BY_ME);

        }
    }

    private void sendRandomizedMessage() {


        // Generate a random 3-character code
        String randomCode = "A";

        // Combine the current password with the random code
        String combinedCode = randomCode;

        // Send the combined code as an SMS
        smsManager.sendTextMessage(currentMobileNumber, null, combinedCode, null, null);

        // Save the new random code as the current password for the next turn

        // Display the message in the chat
        addToChat("Fingerprint authorized. Alarm turned off/on", Message.SENT_BY_ME);
    }



    private String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    private static class DatabaseOperationAsyncTask extends AsyncTask<Message, Void, List<Message>> {
        private WeakReference<MainActivity> activityReference;

        DatabaseOperationAsyncTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected List<Message> doInBackground(Message... messages) {
            if (messages.length > 0) {
                // Insert the new message into the database
                AppDatabase.getInstance(activityReference.get()).messageDao().insert(messages[0]);
            }

            // Retrieve all messages from the database
            return AppDatabase.getInstance(activityReference.get()).messageDao().getAllMessages();
        }


        @Override
        protected void onPostExecute(List<Message> messages) {
            // Update the UI with the result (invoked on the main thread)
            MainActivity activity = activityReference.get();
            if (activity != null) {
                activity.handleDatabaseResult(messages);
            }
        }
    }

    private void handleDatabaseResult(List<Message> messages) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Clear existing messages
                messageList.clear();

                // Add messages from the database
                messageList.addAll(messages);

                // Update the UI
                welcomeTextView.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Unregister the receiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(smsReceiver);
        super.onDestroy();
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Create a new Message object with the current timestamp

                Message newMessage = new Message(message, sentBy,getCurrentTimestamp());

                // Add the new message to the list
                messageList.add(newMessage);
                welcomeTextView.setVisibility(View.GONE);
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());


            }
        });
    }
}
