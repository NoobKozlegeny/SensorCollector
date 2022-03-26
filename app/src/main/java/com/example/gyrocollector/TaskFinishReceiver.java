package com.example.gyrocollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TaskFinishReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //enqueueWork will pass service/work intent in the JobIntentService class
        TaskJobIntentService.enqueueWork(context, intent);
    }
}
