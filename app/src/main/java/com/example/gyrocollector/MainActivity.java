package com.example.gyrocollector;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    static final int CREATE_FILE_ACCELERO = 1;
    static final int CREATE_FILE_GYRO = 2;

    TextView acceleratorText;
    TextView gyroText;
    Timer timer;
    //PowerManager.WakeLock wakeLock;
    String selectedMode;

    SensorManager sensorManager;
    static public Gyroscope gyroscope;
    static public Accelerometer accelerometer;

    Intent services;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        acceleratorText = findViewById(R.id.acceleroData);
        gyroText = findViewById(R.id.gyroData);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        selectedMode = "Slow";

        //Setting up DropDownMenu's items
        Spinner spinner = findViewById(R.id.sp_selectMode);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource
                (this, R.array.dropDownMenuModes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        //Creates wakelock which permits the CPU to work even when the screen is off
        //Context mContext = getApplicationContext();
        //PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK,"app:wakelock");

        //Foreground service kéne a wakelock helyett, vagy a képernyőt bekapcsolva hagyom textField módosításával, hogy ne kapcsoljon ki
        //Starting the wakelock
        //wakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //Starts the data gathering for X minutes
    public void bt_gatherStartOnClick(android.view.View avv) throws InterruptedException{

        //Running the data collecting for X minutes defined in the gatherLength textView, Throws Exception
        TextView time = findViewById(R.id.gatherLength);
        String input = time.getText().toString();
        long minutes = Long.parseLong(input) * 60000;

        //Checking if the wakelock have been stopped to avoid Exception
//        if (!wakeLock.isHeld()){
//            wakeLock.acquire();
//        }

        //TimerTask which will create the file and stops the background data gathering
        timer = new Timer("Timer");
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    stopService(services);
                    //wakeLock.release();
                    //onPause();
                    createFile("ACCELEROMETER");
                    createFile("GYROSCOPE");
                });
            }
        };



        //Sleeps for a bit to properly prepare for the data session
        Thread.sleep(5000);


        //Starts the sensor's data gathering
        services = new Intent(MainActivity.this, ForeGroundService.class);
        startService(services);

        /*accelerometer = new Accelerometer(this);
        gyroscope = new Gyroscope(this);

        accelerometer.setListener((timestamp, tx, ty, ts) -> {
            acceleratorText.setText(tx + "\n" + ty + "\n" + ts);
            acceleroList.add(tx + "," + ty + "," + ts);
        });
        gyroscope.setListener((timestamp, tx, ty, ts) -> {
            gyroText.setText(tx + "\n" + ty + "\n" + ts);
            gyroList.add(tx + "," + ty + "," + ts);
        });

        accelerometer.register();
        gyroscope.register();*/

        //Starts timer
        timer.schedule(task, minutes);

    }

    //Clears axisList
    public void bt_clearAxisListOnClick(android.view.View avv){
        accelerometer.accelerometerList = new ArrayList<>();
        gyroscope.gyroList = new ArrayList<>();
    }

    //Starts the sensor's data gathering
    protected void onResume() {
        super.onResume();




    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    //Stops the sensor's data gathering
    protected void onPause() {
        super.onPause();

        //accelerometer.unRegister();
        //gyroscope.unRegister();

        acceleratorText.setText("Finished data gathering");
    }

    private void createFile(String sensorName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        String fileName = selectedMode + sensorName + java.time.LocalDate.now().toString().split("-")[1]
                + java.time.LocalDate.now().toString().split("-")[2] + ".csv";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        if (sensorName.equals("ACCELEROMETER")){
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
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        selectedMode = adapterView.getItemAtPosition(i).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}