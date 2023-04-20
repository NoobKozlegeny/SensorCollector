package com.example.gyrocollector.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.example.gyrocollector.MainActivity;

import java.util.ArrayList;

public class MagneticField extends BaseSensor {

    //Constructor
    public MagneticField(Context context, SensorManager sensorManager)
    {
        //Initializing the variables
        this.context = context;
        this.sensorManager = sensorManager;
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorList = new ArrayList<>();
        sensorListAVG = new ArrayList<>();

        //Initializing the sensorEventListener
        sensorEventListener = new SensorEventListener() {
            // This method is called when the device's position changes
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // check if listener is different from null
                //sensorEvent.timestamp; This should go on the front of the file
                if (listener != null) {
                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - lastUpdate) > delay) {
                        timesTamp = sensorEvent.timestamp;
                        sensorList.add(sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2]);

                        // pass the three floats in listener on rotation of axis
                        listener.onTranslation(sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);

                        lastUpdate = currentTime;
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
