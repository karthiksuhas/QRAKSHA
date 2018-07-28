package com.example.gnikhil.qraksha_test;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;


public class MessageService extends WearableListenerService {

    private static final String TIMER_SELECTED_PATH = "/qraksha_stop";
    private static final String TIMER_FINISHED_PATH = "/qraksha_start";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(TIMER_SELECTED_PATH) ||
                messageEvent.getPath().equals(TIMER_FINISHED_PATH)) {
            final String message = new String(messageEvent.getData());

            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }

}
