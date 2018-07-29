package com.example.gnikhil.qraksha_test;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity  {

    ImageButton panicbutton;
    Button saveMyInfoButton;
    Button saveEmergencyInfoButton;
    TextView myInfoName,myInfoPhone, myInfoEmail, emergencyInfoName, emergencyInfoPhone, emergencyInfoEmail;

    private static final String PANIC_STARTED_PATH = "/qraksha_started";
    private static final String TAG = "QRaksha";
    private SharedPreferences sharedpreferences;
    public static final String mypreference = "mypref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myInfoName = findViewById(R.id.name_field);
        myInfoPhone = findViewById(R.id.phone_field);
        myInfoEmail = findViewById(R.id.email_field);

        emergencyInfoName = findViewById(R.id.emergency_name_field);
        emergencyInfoPhone = findViewById(R.id.emergency_phone_field);
        emergencyInfoEmail = findViewById(R.id.emergency_email_field);

        panicbutton = findViewById(R.id.panicButton);
        panicbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panicStart();
            }
        });

        saveEmergencyInfoButton = findViewById(R.id.save_emergency_info_id);
        saveEmergencyInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                putEmergencyInfo(v);
            }
        });

        saveMyInfoButton = findViewById(R.id.save_my_info_id);
        saveMyInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                putMyInfo(v);
            }
        });

        //Register to receive local broadcasts, which we'll be creating in the next step//
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        sharedpreferences = getSharedPreferences(mypreference,Context.MODE_PRIVATE); // 0 - for private mode
        getMyInfo();
        getEmergencyInfo();
    }

    public void putMyInfo(View v){
        SharedPreferences.Editor editor = sharedpreferences .edit();
        editor.putString("my_info_name", myInfoName.getText().toString());
        editor.putString("my_info_phone", myInfoPhone.getText().toString());
        editor.putString("my_info_email", myInfoEmail.getText().toString());

        editor.apply(); // commit changes
        Log.e(TAG, "Committed my_info");
    }

    public void getMyInfo(){

        if(sharedpreferences.contains("my_info_name")){
            myInfoName.setText(sharedpreferences .getString("my_info_name", ""));
        }
        if(sharedpreferences.contains("my_info_phone")){
            myInfoPhone.setText(sharedpreferences .getString("my_info_phone", ""));
        }
        if(sharedpreferences.contains("my_info_email")){
            myInfoEmail.setText(sharedpreferences .getString("my_info_email", ""));
        }
        Log.e(TAG, "Extracted my_info");
    }

    public void putEmergencyInfo(View v){
        SharedPreferences.Editor editor = sharedpreferences .edit();
        editor.putString("emergency_info_name", emergencyInfoName.getText().toString());
        editor.putString("emergency_info_phone", emergencyInfoPhone.getText().toString());
        editor.putString("emergency_info_email", emergencyInfoEmail.getText().toString());

        editor.apply(); // commit changes
        Log.e(TAG, "Committed emergency_info");
    }

    public void getEmergencyInfo(){

        if(sharedpreferences.contains("emergency_info_name")){
            emergencyInfoName.setText(sharedpreferences .getString("emergency_info_name", ""));
        }
        if(sharedpreferences.contains("emergency_info_phone")){
            emergencyInfoPhone.setText(sharedpreferences .getString("emergency_info_phone", ""));
        }
        if(sharedpreferences.contains("emergency_info_email")){
            emergencyInfoEmail.setText(sharedpreferences .getString("emergency_info_email", ""));
        }
        Log.e(TAG, "Extracted emergency_info");
    }

    //Define a nested class that extends BroadcastReceiver//
    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Upon receiving each message from the wearable, display the following text//
            String message = "onReceive : SOS on wearable";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, message, duration);
            toast.show();

            panicStart();

            new NewThread(PANIC_STARTED_PATH, message).start();
            Log.e(TAG, message);
        }
    }

    public void panicStart() {
        String message = "panicStart : PANIC sequence started";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(getApplicationContext(), message, duration);
        toast.show();

        // TODO : Comment below for testing, Uncomment while submitting
        //AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.police_siren);
        mediaPlayer.start();

        //Sending a message can block the main UI thread, so use a new thread//
        new NewThread(PANIC_STARTED_PATH, message).start();
        Log.e(TAG, message);
    }

    class NewThread extends Thread {
        String path;
        String message;

        //Constructor for sending information to the Data Layer//
        NewThread(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {
            //Retrieve the connected devices, known as nodes//
            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                    //Send the message//
                    Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());
                    try {
                        //Block on a task and get the result synchronously//
                        Integer result = Tasks.await(sendMessageTask);
                        String message = "Message sent successfully to wearable";
                        Log.e(TAG, message);

                        //if the Task fails, then…..//
                    } catch (ExecutionException exception) {
                        //TODO: Handle the exception//
                    } catch (InterruptedException exception) {
                        //TODO: Handle the exception//
                    }
                }
            } catch (ExecutionException exception) {
                //TODO: Handle the exception//
            } catch (InterruptedException exception) {
                //TODO: Handle the exception//
            }
        }
    }
}