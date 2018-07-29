package com.example.gnikhil.qraksha_test;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.telephony.SmsManager;
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

import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class MainActivity extends AppCompatActivity{

    ImageButton panicbutton;
    Button saveMyInfoButton;
    Button saveEmergencyInfoButton;
    TextView myInfoName,myInfoPhone, myInfoEmail, emergencyInfoName, emergencyInfoPhone, emergencyInfoEmail;
    public boolean locationAccepted, sendSmsAccepted, readPhoneStateAccepted, callPhoneAccepted, internetAccepted, accessNetworkStateAccepted, accessCoarseLocationAccepted;

    private static final String PANIC_STARTED_PATH = "/qraksha_started";
    private static final String TAG = "QRaksha";
    private SharedPreferences sharedpreferences;
    public static final String mypreference = "mypref";
    private static final int PERMISSION_REQUEST_CODE = 200;

    LocationManager locationManager;
    private double longitudeNetwork;
    private double latitudeNetwork;

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
        if(!checkPermission()){
            Log.e(TAG, "Missing permissions");
            requestPermission();
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        checkLocation();
    }

    private void checkLocation(){
        if(!isLocationEnabled()) {
            Log.e(TAG, "checkLocation : Location not enabled");
            requestLocation();
        }
        if(isLocationEnabled()){
            Log.e(TAG, "checkLocation : Location enabled");
            getLocation();
        }
        else{
            Log.e(TAG, "checkLocation : Location not enabled even after requesting");
        }
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
    }

    private void requestLocation() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    @SuppressLint("MissingPermission")
    private void getLocation(){
        Location lastKnownLocation;
        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(lastKnownLocation == null){
            Log.e(TAG, "getLocation : GPS_PROVIDER returned NULL");
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if(lastKnownLocation == null){
            Log.e(TAG, "getLocation : NETWORK_PROVIDER returned NULL");
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }
        if(lastKnownLocation == null) {
            Log.e(TAG, "getLocation : PASSIVE_PROVIDER returned NULL");
            return;
        }
        longitudeNetwork = lastKnownLocation.getLongitude();
        latitudeNetwork = lastKnownLocation.getLatitude();
        Log.e(TAG, "getLocation : Longitude : " + Double.toString(longitudeNetwork) + " Latitude : " + Double.toString(latitudeNetwork));
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), SEND_SMS);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_STATE);
        int result3 = ContextCompat.checkSelfPermission(getApplicationContext(), CALL_PHONE);
        int result4 = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);
        int result5 = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_NETWORK_STATE);
        int result6 = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION);

        return ((result == PackageManager.PERMISSION_GRANTED) && (result1 == PackageManager.PERMISSION_GRANTED)
                && (result2 == PackageManager.PERMISSION_GRANTED) && (result3 == PackageManager.PERMISSION_GRANTED)
                && (result4 == PackageManager.PERMISSION_GRANTED) && (result5 == PackageManager.PERMISSION_GRANTED)
                && (result6 == PackageManager.PERMISSION_GRANTED));
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, SEND_SMS, READ_PHONE_STATE, CALL_PHONE, INTERNET, ACCESS_NETWORK_STATE, ACCESS_COARSE_LOCATION},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    sendSmsAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    readPhoneStateAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    callPhoneAccepted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                    internetAccepted = grantResults[4] == PackageManager.PERMISSION_GRANTED;
                    accessNetworkStateAccepted = grantResults[5] == PackageManager.PERMISSION_GRANTED;
                    accessCoarseLocationAccepted = grantResults[6] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && sendSmsAccepted && readPhoneStateAccepted && callPhoneAccepted && internetAccepted && accessNetworkStateAccepted && accessCoarseLocationAccepted)
                        Log.e(TAG, "All permissions granted");
                    else {

                        Log.e(TAG, "Some permissions are denied");
                    }
                }
                break;
        }
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

    @SuppressLint("MissingPermission")
    public void panicStart() {
        String message = "panicStart : PANIC sequence started";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(getApplicationContext(), message, duration);
        toast.show();

        getLocation();

        message = "Help me, I am at Latitude : " + Double.toString(latitudeNetwork) + " Longitude : " + Double.toString(longitudeNetwork);
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(emergencyInfoPhone.getText().toString(), null, message, null, null);

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + emergencyInfoPhone.getText().toString()));
        getApplicationContext().startActivity(intent);

        // TODO : Comment below for testing, Uncomment while submitting
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume;
        try{
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        }catch (NullPointerException Exception) {
            //TODO Handle the exception
        }

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