package com.example.gyrocollector;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Provider;

public class ForeGroundService extends Service {

    public Accelerometer accelerometer;
    public Gyroscope gyroscope;
    public Sensors sensors;
    String datapath = "/message_path";
    public static String TAG = "WearListActivity";
    public static NotificationChannel CHANNEL = new NotificationChannel("proba","A neve", NotificationManager.IMPORTANCE_NONE);

    @Override
    public void onCreate(){
        super.onCreate();
        accelerometer = new Accelerometer(this);
        gyroscope = new Gyroscope(this);

        accelerometer.setListener(new Accelerometer.Listener() {
            //on translation method of accelerometer
            @Override
            public void onTranslation(long timestamp,float tx, float ty, float ts) {
                // set the color red if the device moves in positive x axis
                // System.out.println(System.currentTimeMillis()+" ACC:"+tx+","+ty+','+ts);
                Log.d("ACCELERO", tx + "," + ty + "," + ts);
            }
        });

        //create a listener for gyroscope
        gyroscope.setListener(new Gyroscope.Listener() {
            // on rotation method of gyroscope
            @Override
            public void onRotation(long timestamp,float rx, float ry, float rz) {
                // set the color green if the device rotates on positive z axis
                //  System.out.println(System.currentTimeMillis()+" GYR:" + rx + "," + ry + ',' + rz);
                Log.d("GYRO", rx + "," + ry + "," + rz);
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        accelerometer.unRegister();
        gyroscope.unRegister();

        MainActivity.accelerometer = accelerometer;
        MainActivity.gyroscope = gyroscope;
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int stratId){
        accelerometer.register();
        gyroscope.register();


//        CHANNEL.setLightColor(Color.BLUE);
//        CHANNEL.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        service.createNotificationChannel(CHANNEL);
//        Intent notiintent = new Intent(this,MainActivity.class);
//        PendingIntent pendingIntent =
//                PendingIntent.getActivity(this, 0, notiintent, 0);
//
//        Notification notification =
//                new Notification.Builder(this, "proba")
//                        .setContentTitle("megy ugye")
//                        .setContentText("fut az alkalmazas")
//                        .setContentIntent(pendingIntent)
//                        .setTicker("proba")
//                        .build();
//
//        startForeground(101,notification);


        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
