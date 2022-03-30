package com.example.gyrocollector;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
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
    String selectedMode;

    SensorManager sensorManager;
    static public Gyroscope gyroscope;
    static public Accelerometer accelerometer;

    public ArrayList<String> accelerometerList;
    public ArrayList<String> gyroList;

    static public Boolean hasGyro = false;
    static public Boolean hasAccelero = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        acceleratorText = findViewById(R.id.acceleroData);
        gyroText = findViewById(R.id.gyroData);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        selectedMode = "Slow";

        gyroscope = new Gyroscope(this);
        accelerometer = new Accelerometer(this);
        accelerometerList = new ArrayList<>();
        gyroList = new ArrayList<>();

        //Setting up DropDownMenu's items
        Spinner spinner = findViewById(R.id.sp_selectMode);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource
                (this, R.array.dropDownMenuModes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        //Keeps the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //Starts the data gathering for X minutes
    public void bt_gatherStartOnClick(android.view.View avv) throws InterruptedException {
        //Running the data collecting for X minutes defined in the gatherLength textView, Throws Exception
        TextView time = findViewById(R.id.gatherLength);

        //Checks if the time's text did get a number input
        if (time.getText().toString().matches("\\d+(?:\\.\\d+)?")){
            String input = time.getText().toString();
            long minutes = Long.parseLong(input) * 60000;
            //TimerTask which will create the file and stops the background data gathering
            timer = new Timer("Timer");
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> {
                        onPause();
                        createFile("ACCELEROMETER");
                        createFile("GYROSCOPE");
                    });
                }
            };
            //Sleeps for a bit to properly prepare for the data session
            Thread.sleep(5000);
            //Starts the sensor's data gathering
            startGathering();
            //Starts timer
            timer.schedule(task, minutes);
        }
        else {
            //Sleeps for a bit to properly prepare for the data session
            Thread.sleep(5000);
            //Starts the sensor's data gathering
            startGathering();
        }
    }

    //Stops data gathering by clicking on the stop gathering button
    public void bt_gatherStopOnClick(android.view.View avv) {
        onPause();
        createFile("ACCELEROMETER");
        createFile("GYROSCOPE");
    }

    //Initialises sensors and starts gathering
    public void startGathering(){
        accelerometer.setListener((timestamp, tx, ty, ts) -> {
            acceleratorText.setText(tx + "\n" + ty + "\n" + ts);
            accelerometerList.add(tx + "," + ty + "," + ts);
            Log.d("Accelero", tx + "," + ty + "," + ts);
        });
        gyroscope.setListener((timestamp, tx, ty, ts) -> {
            gyroText.setText(tx + "\n" + ty + "\n" + ts);
            gyroList.add(tx + "," + ty + "," + ts);
            Log.d("Gyro", tx + "," + ty + "," + ts);
        });

        accelerometer.register();
        gyroscope.register();
    }

    //Clears List
    public void bt_clearAxisListOnClick(android.view.View avv){
        accelerometer.accelerometerList.clear();
        gyroscope.gyroList.clear();
        accelerometerList.clear();
        gyroList.clear();
    }

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

        accelerometer.unRegister();
        gyroscope.unRegister();

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