package com.example.gyrocollector.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.example.gyrocollector.MainActivity;

import java.util.ArrayList;

public class Accelerometer extends BaseSensor{
    //Constructor
    public Accelerometer(Context context, SensorManager sensorManager)
    {
        //Initializing the variables
        this.context = context;
        this.sensorManager = sensorManager;
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorList = new ArrayList<>();

        //Initializing the sensorEventListener
        sensorEventListener = new SensorEventListener() {
            // This method is called when the device's position changes
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // check if listener is different from null
                //sensorEvent.timestamp; This should go on the front of the file
                if (listener != null) {
                    // !(MainActivity.hasGyro.equals(false) && MainActivity.hasAccelero.equals(true))
                    if (!(MainActivity.hasAccelero.equals(true)
                            && MainActivity.hasMagnetic.equals(false)
                            && MainActivity.hasGeoMagneticRotation.equals(false)
                            && MainActivity.hasGravity.equals(false)
                            && MainActivity.hasGyro.equals(false))) {
                        MainActivity.hasAccelero = true;
                        MainActivity.hasMagnetic = false;
                        MainActivity.hasGeoMagneticRotation = false;
                        MainActivity.hasGravity = false;
                        MainActivity.hasGyro = false;

                        timesTamp = sensorEvent.timestamp;
                        sensorList.add(sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2]);

                        // pass the three floats in listener on rotation of axis
                        listener.onTranslation(sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                    }
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {}
        };

        // Setup the BaseSensor's attributes
        Setup(context, sensorManager, sensor, sensorEventListener);
    }
}
