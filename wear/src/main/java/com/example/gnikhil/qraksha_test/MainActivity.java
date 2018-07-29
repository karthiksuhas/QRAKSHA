package com.example.gnikhil.qraksha_test;

import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.Node;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends WearableActivity implements
        DelayedConfirmationView.DelayedConfirmationListener {
    private static final String TAG = "QRaksha";
    private static final int NUM_SECONDS = 5;

    private static final String TIMER_FINISHED_PATH = "/qraksha_start";

    private DelayedConfirmationView delayedConfirmationView;	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        delayedConfirmationView = (DelayedConfirmationView) findViewById(R.id.delayed_confirmation);
        delayedConfirmationView.setTotalTimeMs(NUM_SECONDS * 1000);

        // Vibrate for 2 seconds to indicate that QRaksha app started Timer
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VibrationEffect.createOneShot(2000,VibrationEffect.DEFAULT_AMPLITUDE));

        // 2 second delay before starting the timer for the app initializations to complete
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onStartTimer(delayedConfirmationView);
            }
        }, 2000);
		
        //Register the local broadcast receiver//
        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);

        // TODO : Comment below for testing, Uncomment while submitting
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    /**
     * Starts the DelayedConfirmationView when user presses "Start Timer" button.
     */
    public void onStartTimer(View view) {
        Log.e(TAG, "onStartTimer");
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        delayedConfirmationView.setTotalTimeMs(0);
        ((DelayedConfirmationView) v).setListener(null);
        String onClickMessage = "Operation cancelled bt the user";
        Log.e(TAG, "onTimerSelected : " + onClickMessage);
        finish();
    }

    @Override
    public void onTimerFinished(View v) {
        String onClickMessage = "Start the panic sequence ";
        new SendMessage(TIMER_FINISHED_PATH, onClickMessage).start();
        Log.e(TAG, "onTimerFinished: " + onClickMessage);
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String onMessageReceived = "Panic sequence started on the mobile";
            Log.e(TAG, "BroadcastReceiver: " + onMessageReceived);

            DelayedConfirmationView imgView;
            imgView =  findViewById(R.id.delayed_confirmation);
            imgView.setImageResource(R.mipmap.sos);

            MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.police_siren);
            mediaPlayer.start();

            // 5 second delay before exiting the interface
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 5000);
        }
    }

    class SendMessage extends Thread {
        String path;
        String message;

        //Constructor///
        SendMessage(String p, String m) {
            path = p;
            message = m;
        }

        //Send the message via the thread. This will send the message to all the currently-connected devices//
        public void run() {
            //Get all the nodes//
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                //Block on a task and get the result synchronously//
                List<Node> nodes = Tasks.await(nodeListTask);

                //Send the message to each device//
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {
                        Integer result = Tasks.await(sendMessageTask);
                        //Handle the errors//
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
