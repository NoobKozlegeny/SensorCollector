package com.example.gyrocollector.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.example.gyrocollector.MainActivity;

import java.util.ArrayList;

public class Gyroscope extends BaseSensor{

    //Constructor
    Gyroscope(Context context, SensorManager sensorManager)
    {
        //Initializing the variables
        this.context = context;
        this.sensorManager = sensorManager;
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorList = new ArrayList<>();

        //Initializing the sensorEventListener
        sensorEventListener = new SensorEventListener() {
            // This method is called when the device's position changes
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // check if listener is different from null
                //sensorEvent.timestamp; This should go on the front of the file
                if (listener != null) {
                    // !(MainActivity.hasGyro.equals(true) && MainActivity.hasAccelero.equals(false)
                    if (!(MainActivity.hasAccelero.equals(false)
                            && MainActivity.hasMagnetic.equals(false)
                            && MainActivity.hasGeoMagneticRotation.equals(false)
                            && MainActivity.hasGravity.equals(false)
                            && MainActivity.hasGyro.equals(true))) {
                        MainActivity.hasAccelero = false;
                        MainActivity.hasMagnetic = false;
                        MainActivity.hasGeoMagneticRotation = false;
                        MainActivity.hasGravity = false;
                        MainActivity.hasGyro = true;

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
