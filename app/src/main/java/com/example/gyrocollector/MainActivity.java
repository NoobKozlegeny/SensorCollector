package com.example.gyrocollector;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    static final int CREATE_FILE_ACCELERO = 1;
    static final int CREATE_FILE_GYRO = 2;

    TextView acceleratorText;
    TextView gyroText;
    Timer timer;
    ArrayList<String> axisList;
    ArrayList<String> gyroList;
    PowerManager.WakeLock wakeLock;
    String selectedMode;
    Long timesTamp;

    SensorManager sensorManager;
    //Sensor acceleratorMeter;
    //Sensor gyroMeter;
    Gyroscope gyroscope;
    Accelerometer accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        acceleratorText = findViewById(R.id.acceleroData);
        gyroText = findViewById(R.id.gyroData);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        axisList = new ArrayList<>();
        gyroList = new ArrayList<>();
        selectedMode = "Slow";

        //Setting up DropDownMenu's items
        Spinner spinner = findViewById(R.id.sp_selectMode);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource
                (this, R.array.dropDownMenuModes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        //Creates wakelock which permits the CPU to work even when the screen is off
        Context mContext = getApplicationContext();
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK,"app:wakelock");

        //Foreground service kéne a wakelock helyett, vagy a képernyőt bekapcsolva hagyom textField módosításával, hogy ne kapcsoljon ki
        //Starting the wakelock
        wakeLock.acquire();
    }

    //Starts the data gathering for X minutes
    public void bt_gatherStartOnClick(android.view.View avv) throws InterruptedException{
        //Checking if the wakelock have been stopped to avoid Exception
        if (!wakeLock.isHeld()){
            wakeLock.acquire();
        }

        //acceleratorMeter = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //gyroMeter = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //Running the data collecting for X minutes defined in the gatherLength textView, Throws Exception
        TextView time = findViewById(R.id.gatherLength);
        String input = time.getText().toString();
        long minutes = Long.parseLong(input) * 1000;

        //TimerTask which will create the file and stops the background data gathering
        timer = new Timer("Timer");
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        wakeLock.release();
                        createFile("ACCELEROMETER");
                        createFile("GYROSCOPE");
                        onPause();
                    }
                });
            }
        };

        //Sleeps for a bit to properly prepare for the data session
        //Thread.sleep(5000);
        //Starts the sensor's data gathering
        onResume();
        //Starts timer
        timer.schedule(task, minutes);

//        //Mehh solution, press the button 2 times
//        onPause();
//
//        //Turns off the wakelock
//        wakeLock.release();
//
//        createFile();

    }

    //Clears axisList
    public void bt_clearAxisListOnClick(android.view.View avv){
        axisList = new ArrayList<>();
    }

    //Starts the sensor's data gathering
    protected void onResume() {
        super.onResume();

        //Waits for 5 seconds to actually start.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        accelerometer = new Accelerometer(getApplicationContext());
        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onRotation(long timestamp, float tx, float ty, float ts) {
                acceleratorText.setText(tx + "\n" + ty + "\n" + ts);
            }
        });
        accelerometer.register();

        gyroscope = new Gyroscope(getApplicationContext());
        gyroscope.setListener(new Gyroscope.Listener() {
            @Override
            public void onRotation(long timestamp, float tx, float ty, float ts) {
                gyroText.setText(tx + "\n" + ty + "\n" + ts);
            }
        });
        gyroscope.register();
        //sensorManager.registerListener(this, acceleratorMeter, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(this, gyroMeter, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //Stops the sensor's data gathering
    protected void onPause() {
        super.onPause();
        //Somehow unregistering the Listener doesn't let the wakelock to actually do it's job
        //sensorManager.unregisterListener(this);

        acceleratorText.setText("Finished data gathering");
    }

/*    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        String sensorName = sensorEvent.sensor.getName();
        TextView test = findViewById(R.id.test);
        test.setText(sensorName);
        acceleratorText.setText(sensorEvent.values[0] + "\n" + sensorEvent.values[1] + "\n" + sensorEvent.values[2]);
        timesTamp = sensorEvent.timestamp;

        if (sensorName.contains("ACCELEROMETER")) {
            axisList.add(sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2]);
        }
        else {
            gyroList.add(sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2]);
        }
        //sensorEvent.timestamp; Elejére menjem a timestamp
        //Ha többször elindítom a timestampet és ha jó időpontban akkor jó
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }*/

    private void createFile(String sensorName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        String fileName = selectedMode + sensorName + java.time.LocalDate.now().toString().split("-")[1]
                + java.time.LocalDate.now().toString().split("-")[2] + ".csv";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        if (sensorName == "ACCELEROMETER"){
            startActivityForResult(intent, CREATE_FILE_ACCELERO);
        }
        else {
            startActivityForResult(intent, CREATE_FILE_GYRO);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                accelerometer.ExportToCSV(resultData);
            }
            else {
                gyroscope.ExportToCSV(resultData);
            }
            /*// The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                // Perform operations on the document using its URI.

                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    //Header
                    outputStream.write("X,Y,Z\n".getBytes(StandardCharsets.UTF_8));
                    //Timestamp
                    outputStream.write(("Timestamp: ," + timesTamp.toString() + "\n").getBytes(StandardCharsets.UTF_8));

                    if (resultCode == 1) {
                        for (String line : axisList) {
                            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                            outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    else {
                        for (String line : gyroList) {
                            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                            outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                        }
                    }

                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        selectedMode = adapterView.getItemAtPosition(i).toString();
        ;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}