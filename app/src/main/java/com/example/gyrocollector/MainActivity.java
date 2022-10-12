package com.example.gyrocollector;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    static final int CREATE_FILE_ACCELERO = 1;
    static final int CREATE_FILE_GYRO = 2;
    static final int CREATE_FILE_ALL = 3;

    TextView testText;
    TextView acceleratorText;
    TextView gyroText;
    TextView gravityText;
    TextView magneticFieldText;
    Timer timer;
    String selectedMode;

    public SensorManager sensorManager;
    public Gyroscope gyroscope;
    public Accelerometer accelerometer;
    public Gravity gravity;
    public MagneticField magneticField;
    public GeoMagneticRotationVector geoMagneticRotationVector;

    public ArrayList<String> accelerometerList, gyroList, gravityList, magneticFieldList, gmrvList;
//    public ArrayList<String> gyroList;
//    public ArrayList<String> gravityList;
//    public ArrayList<String> magneticFieldList;
//    public ArrayList<String> gmrvList;

    static public Boolean hasGyro = false;
    static public Boolean hasAccelero = false;
    static public Boolean hasGravity = false;
    static public Boolean hasMagnetic = false;
    static public Boolean hasGeoMagneticRotation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        testText = findViewById(R.id.test);
        acceleratorText = findViewById(R.id.acceleroData);
        gyroText = findViewById(R.id.gyroData);
        gravityText = findViewById(R.id.gravityData);
        magneticFieldText = findViewById(R.id.magneticFieldData);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        selectedMode = "Slow";

        gyroscope = new Gyroscope(this, sensorManager);
        accelerometer = new Accelerometer(this, sensorManager);
        gravity = new Gravity(this, sensorManager);
        magneticField = new MagneticField(this, sensorManager);
        geoMagneticRotationVector = new GeoMagneticRotationVector(this, sensorManager);
        accelerometerList = new ArrayList<>();
        gyroList = new ArrayList<>();
        gravityList = new ArrayList<>();
        magneticFieldList = new ArrayList<>();
        gmrvList = new ArrayList<>();


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
                        createFile("ALL");
//                        createFile("ACCELEROMETER");
//                        createFile("GYROSCOPE");
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
        // Delete the last 50 lines of data bc we had to stop it manually

        onPause();
        createFile("ALL");
//        createFile("ACCELEROMETER");
//        createFile("GYROSCOPE");
    }

    //Initialises sensors and starts gathering
    public void startGathering(){
        accelerometer.setListener((timestamp, tx, ty, ts) -> {
            acceleratorText.setText(tx + "\n" + ty + "\n" + ts);
            accelerometerList.add(tx + "," + ty + "," + ts);
            Log.d("Accelerometer", tx + "," + ty + "," + ts);
        });
        gyroscope.setListener((timestamp, tx, ty, ts) -> {
            gyroText.setText(tx + "\n" + ty + "\n" + ts);
            gyroList.add(tx + "," + ty + "," + ts);
            Log.d("Gyro", tx + "," + ty + "," + ts);
        });
        gravity.setListener((timestamp, tx, ty, ts) -> {
            gravityText.setText(tx + "\n" + ty + "\n" + ts);
            gravityList.add(tx + "," + ty + "," + ts);
            Log.d("Gravity", tx + "," + ty + "," + ts);
        });
        magneticField.setListener((timestamp, tx, ty, ts) -> {
            magneticFieldText.setText(tx + "\n" + ty + "\n" + ts);
            magneticFieldList.add(tx + "," + ty + "," + ts);
            Log.d("Magnetic", tx + "," + ty + "," + ts);
        });
        geoMagneticRotationVector.setListener((timestamp, tx, ty, ts) -> {
            // acceleratorText.setText(tx + "\n" + ty + "\n" + ts);
            gmrvList.add(tx + "," + ty + "," + ts);
            Log.d("GeoMagneticRotation", tx + "," + ty + "," + ts);
        });

        accelerometer.register();
        gyroscope.register();
        gravity.register();
        magneticField.register();
        geoMagneticRotationVector.register();
    }

    //Clears List
    public void bt_clearAxisListOnClick(android.view.View avv){
        accelerometer.accelerometerList.clear();
        gyroscope.gyroList.clear();
        gravity.gravityList.clear();
        magneticField.magneticFieldList.clear();
        geoMagneticRotationVector.gmrvList.clear();
        accelerometerList.clear();
        gyroList.clear();
        gravityList.clear();
        magneticFieldList.clear();
        gmrvList.clear();
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
        gravity.unRegister();
        magneticField.unRegister();
        geoMagneticRotationVector.unRegister();

        testText.setText("Finished data gathering");
    }

    //Saves data to CSV file
    private void createFile(String name) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        String fileName = selectedMode + name + java.time.LocalDate.now().toString().split("-")[1]
                + java.time.LocalDate.now().toString().split("-")[2] + ".csv";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        if (name.equals("ACCELEROMETER")){
            startActivityForResult(intent, CREATE_FILE_ACCELERO);
        }
        else if (name.equals("GYROSCOPE")){
            startActivityForResult(intent, CREATE_FILE_GYRO);
        }
        else {
            startActivityForResult(intent, CREATE_FILE_ALL);
        }

    }

    //This activity will call the methods which will save the datas to a CSV
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                accelerometer.ExportToCSV(resultData);
            }
            else if (requestCode == 2) {
                gyroscope.ExportToCSV(resultData);
            }
            else {
                exportAllToOneCSV(resultData);
            }
        }
    }

    // This will export all the sensors data into one CSV
    public void exportAllToOneCSV(Intent resultData){
        ArrayList<String> combinedList = new ArrayList<>();

        // Adding the header lines to the combinedList
        combinedList.add(",TYPE_LINEAR_ACCELERATION,,,TYPE_GYROSCOPE,,,TYPE_GRAVITY,,,TYPE_MAGNETIC_FIELD,,,TYPE_GEOMAGNETIC_ROTATION_VECTOR");
        combinedList.add("Timestamp: ," + accelerometer.timesTamp.toString());
        combinedList.add("X_ACC,Y_ACC,Z_ACC,X_GYRO,Y_GYRO,Z_GYRO,X_GRAVITY,Y_GRAVITY,Z_GRAVITY,X_MF,Y_MF,Z_MF,X_GMRV,Y_GMRV,Z_GMRV,TIME,LABEL");

        // Adding the first combined line to the combinedList bc of the time column
        int i = 0;
        LocalTime localTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        combinedList.add(accelerometerList.get(i) + "," + gyroList.get(i)
                + "," + gravityList.get(i) + "," + magneticFieldList.get(i)
                + "," + gmrvList.get(i) + "," + formatter.format(localTime)
                + "," + selectedMode);
        i++;

        // Combining the separate sensor datas into the combinedList
        while (i < accelerometerList.size() && i < gyroList.size() && i < gravityList.size()
        && i < magneticFieldList.size() && i < gmrvList.size()) {
            String lineToAdd = accelerometerList.get(i) + "," + gyroList.get(i)
                    + "," + gravityList.get(i) + "," + magneticFieldList.get(i)
                    + "," + gmrvList.get(i);

            // Check if a minute have passed. If yes then the new time will be inserted
            // into that specific column
            if (localTime.getMinute() - LocalTime.now().getMinute() != 0) {
                localTime = LocalTime.now();
                combinedList.add(lineToAdd + "," + formatter.format(localTime)
                        + "," + selectedMode);
            }
            else {
                combinedList.add(lineToAdd + ",," + selectedMode);
            }
            i++;
        }

        // Exporting the combinedList to a CSV file
        if (resultData != null) {
            Uri uri = resultData.getData();
            // Perform operations on the document using its URI.
            try {
                ContentResolver cr = getContentResolver();
                OutputStream outputStream = cr.openOutputStream(uri);

                for (String line : combinedList) {
                    outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                    outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                }

                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
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