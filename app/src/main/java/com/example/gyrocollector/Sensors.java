package com.example.gyrocollector;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Sensors {
    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor accelerometerSensor;
    private final Sensor gyroSensor;
    private final SensorEventListener sensorEventListener;

    public Long timesTamp;
    public ArrayList<String> accelerometerList;
    public ArrayList<String> gyroList;

    // create an interface with one method
    public interface AccelerometerListener {
        // create method with all 3
        // axis translation as argument
        void onTranslation(long timestamp,float tx, float ty, float ts);
    }

    public interface GyroListener {
        // create method with all 3
        // axis translation as argument
        void onRotation(long timestamp,float tx, float ty, float ts);
    }

    // create an instance
    private Sensors.AccelerometerListener accelerometerlistener;
    private Sensors.GyroListener gyroListener;

    // method to set the instance
    public void setAccelerometerListener(Sensors.AccelerometerListener l) { accelerometerlistener = l; }
    public void setGyroListener(Sensors.GyroListener l) { gyroListener = l; }

    //Constructor
    Sensors(Context context)
    {
        //Initializing the variables
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometerList = new ArrayList<>();
        gyroList = new ArrayList<>();

        //Initializing the sensorEventListener
        sensorEventListener = new SensorEventListener() {

            // This method is called when the device's position changes
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // check if listener is different from null
                //sensorEvent.timestamp; This should go on the front of the file
                //Ha többször elindítom a timestampet és ha jó időpontban akkor jó
                if (accelerometerlistener != null && gyroListener != null) {
                    timesTamp = sensorEvent.timestamp;

                    // pass the three floats in listener on rotation of axis
                    if (sensorEvent.sensor.getName().contains("GYRO")){
                        gyroList.add(sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2]);
                        gyroListener.onRotation(sensorEvent.timestamp,sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                    }
                    else if (sensorEvent.sensor.getName().contains("linear_acceleration")){
                        accelerometerList.add(sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2]);
                        accelerometerlistener.onTranslation(sensorEvent.timestamp,sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                    }

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
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
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

                for (String line : accelerometerList) {
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
