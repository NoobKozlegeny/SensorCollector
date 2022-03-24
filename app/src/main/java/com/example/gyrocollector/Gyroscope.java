package com.example.gyrocollector;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;

public class Gyroscope {

    private Context context;
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorEventListener;

    public Long timesTamp;
    public ArrayList<String> gyroList;

    // create an interface with one method
    public interface Listener {
        // create method with all 3
        // axis translation as argument
        void onRotation(long timestamp,float tx, float ty, float ts);
    }

    // create an instance
    private Listener listener;

    // method to set the instance
    public void setListener(Listener l) {
        listener = l;
    }

    //Constructor
    Gyroscope(Context context)
    {
        //Initializing the variables
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        this.context = context;
        gyroList = new ArrayList<>();

        //Initializing the sensorEventListener
        sensorEventListener = new SensorEventListener() {

            // This method is called when the device's position changes
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // check if listener is different from null
                if (listener != null) {
                    String sensorName = sensorEvent.sensor.getName();
                    timesTamp = sensorEvent.timestamp;

                    gyroList.add(sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2]);

                    //   System.out.println(sensorEvent.timestamp+" Gyro"+sensorEvent.values[0]+" "+sensorEvent.values[1]+" "+sensorEvent.values[2]);
                    // pass the three floats in listener on rotation of axis
                    listener.onRotation(sensorEvent.timestamp,sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
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
        Uri uri = null;
        if (resultData != null) {
            uri = resultData.getData();
            // Perform operations on the document using its URI.

            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                //Header
                outputStream.write("X,Y,Z\n".getBytes(StandardCharsets.UTF_8));
                //Timestamp
                outputStream.write(("Timestamp: ," + timesTamp.toString() + "\n").getBytes(StandardCharsets.UTF_8));

                for (String line : gyroList) {
                    outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                    outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                }

                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
