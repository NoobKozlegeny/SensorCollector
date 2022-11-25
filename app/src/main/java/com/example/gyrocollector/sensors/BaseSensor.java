package com.example.gyrocollector.sensors;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public abstract class BaseSensor {
    protected Context context;
    protected SensorManager sensorManager;
    protected Sensor sensor;
    protected SensorEventListener sensorEventListener;

    public Long timesTamp;
    public ArrayList<String> sensorList;

    protected void Setup(Context context, SensorManager sensorManager, Sensor sensor, SensorEventListener sensorEventListener) {
        this.context = context;
        this.sensorManager = sensorManager;
        this.sensor = sensor;
        this.sensorEventListener = sensorEventListener;
    }

    // create an interface with one method
    public interface Listener {
        // create method with all 3
        // axis translation as argument
        void onTranslation(long timestamp,float tx, float ty, float ts);
    }

    // create an instance
    protected BaseSensor.Listener listener;

    // method to set the instance
    public void setListener(BaseSensor.Listener l) {
        listener = l;
    }

    // create register method
    // for sensor notifications
    public void register() {
        // call sensor manger's register listener and pass the required arguments
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // create method to unregister
    // from sensor notifications
    public void unRegister() {
        // call sensor manger's unregister listener
        // and pass the required arguments
        sensorManager.unregisterListener(sensorEventListener);
    }

    public void ExportToCSV(Intent resultData){
        // The result data contains a URI for the document or directory that
        // the user selected.
        if (resultData != null) {
            Uri uri = resultData.getData();
            // Perform operations on the document using its URI.

            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                //Header
                outputStream.write("X,Y,Z\n".getBytes(StandardCharsets.UTF_8));
                //Timestamp
                outputStream.write(("Timestamp: ," + timesTamp.toString() + "\n").getBytes(StandardCharsets.UTF_8));

                for (String line : sensorList) {
                    outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                    outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                }

                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
