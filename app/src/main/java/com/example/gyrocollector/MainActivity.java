package com.example.gyrocollector;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
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

import com.example.gyrocollector.helpers.Helpers;
import com.example.gyrocollector.sensors.Accelerometer;
import com.example.gyrocollector.sensors.GeoMagneticRotationVector;
import com.example.gyrocollector.sensors.Gravity;
import com.example.gyrocollector.sensors.Gyroscope;
import com.example.gyrocollector.sensors.MagneticField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tflite.java.TfLite;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.DelegateFactory;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime;
import org.tensorflow.lite.RuntimeFlavor;
import  org.tensorflow.lite.flex.FlexDelegate;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    static final int CREATE_FILE_ACCELERO = 1;
    static final int CREATE_FILE_GYRO = 2;
    static final int CREATE_FILE_ALL = 3;

    TextView testText, acceleratorText, gyroText, gravityText, magneticFieldText, predictionText;
    Timer dataGatheringTimer, predictTimer;
    LocalTime localTime;
    String selectedMode;

    public SensorManager sensorManager;
    public Gyroscope gyroscope;
    public Accelerometer accelerometer;
    public Gravity gravity;
    public MagneticField magneticField;
    public GeoMagneticRotationVector geoMagneticRotationVector;

    public ArrayList<String> accelerometerList, gyroList, gravityList, magneticFieldList, gmrvList;
    public ArrayList<String> timeList;

    public ArrayList<String> accelerometerListAVG, gyroListAVG, gravityListAVG, magneticFieldListAVG, gmrvListAVG;
    public ArrayList<String> timeListAVG;

    static public Boolean hasGyro = false;
    static public Boolean hasAccelero = false;
    static public Boolean hasGravity = false;
    static public Boolean hasMagnetic = false;
    static public Boolean hasGeoMagneticRotation = false;

    //Interpreter tflite;
    private InterpreterApi interpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        testText = findViewById(R.id.test);
        acceleratorText = findViewById(R.id.acceleroData);
        gyroText = findViewById(R.id.gyroData);
        gravityText = findViewById(R.id.gravityData);
        magneticFieldText = findViewById(R.id.magneticFieldData);
        predictionText = findViewById(R.id.tv_predictionData);
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
        timeList = new ArrayList<>();

        accelerometerListAVG = new ArrayList<>();
        gyroListAVG = new ArrayList<>();
        gravityListAVG = new ArrayList<>();
        magneticFieldListAVG = new ArrayList<>();
        gmrvListAVG = new ArrayList<>();
        timeListAVG = new ArrayList<>();

        //Initalize task
        Task<Void> initializeTask = TfLite.initialize(this);

        //Initialize the interpreter
        initializeTask.addOnSuccessListener(a -> {
            try {
                DelegateFactory delegateFactory = new DelegateFactory() {
                    @Override
                    public Delegate create(RuntimeFlavor runtimeFlavor) {
                        return new FlexDelegate();
                    }
                };
                InterpreterApi.Options options = new Interpreter.Options()
                        .setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY)
                        .addDelegateFactory(delegateFactory);
                interpreter = InterpreterApi.create(loadModelFile(), options);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).addOnFailureListener(e -> {
            Log.e("Interpreter", String.format("Cannot initialize interpreter: %s",
                    e.getMessage()));
        });

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

    //Memory-map the modei file in Assets
    private MappedByteBuffer loadModelFile() throws IOException {
        //Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("project-11-26.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    //Starts the data gathering for X minutes
    public void bt_gatherStartOnClick(android.view.View avv) throws InterruptedException {
        //Running the data collecting for X minutes defined in the gatherLength textView, Throws Exception
        TextView time = findViewById(R.id.gatherLength);

        //Checks if the time's text did get a number input
        if (time.getText().toString().matches("\\d+(?:\\.\\d+)?")){
            //Getting the minute which have been added to the text
            String input = time.getText().toString();
            long minutes = Long.parseLong(input) * 60000;

            //This variable will decide which part of the collected datas will be predicted on
            AtomicInteger fromMinute = new AtomicInteger();

            //TimerTask which will create the file and stops the background data gathering
            dataGatheringTimer = new Timer("dataGatheringTimer");
            predictTimer = new Timer("predictTimer");
            TimerTask dataGatheringTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> {
                        //Kills the currently running prediction task
                        predictTimer.purge();

                        onPause();
                        createFile("ALL");
                    });
                }
            };
            TimerTask predictTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> {
                        //Replaces empty string ("") to default lines ("0,0,0") in ArrayLists
                        replaceEmptyData();
                        //Make a prediction and get the confidences
                        float[] predictions = doInference(fromMinute.get());
                        //Increase the fromMinute so next time we won't predict on the same data again
                        fromMinute.addAndGet(1);
                        //Display the prediction result onto the predictionData text
                        String newLine = System.getProperty("line.separator");
                        predictionText.setText(String.join(newLine,
                                "Confidence results:",
                                "0 (Slow): " + predictions[0],
                                "1 (Normal): " + predictions[1],
                                "2 (Fast): " + predictions[2]));
                    });
                }
            };
            //Sleeps for a bit to properly prepare for the data session
            Thread.sleep(5000);
            //Starts the sensor's data gathering
            startGathering();
            //Starts timers
            dataGatheringTimer.schedule(dataGatheringTask, minutes);
            predictTimer.schedule(predictTask, 60000, 60000); //Periodically runs every minute
        }
        else {
            //Sleeps for a bit to properly prepare for the data session
            Thread.sleep(5000);
            //Starts the sensor's data gathering
            startGathering();
        }
    }

    //Replaces empty string ("") to default lines ("0,0,0") in ArrayLists
    public void replaceEmptyData() {
        accelerometerListAVG.convertEmptyDataToDefault();
        gyroListAVG.convertEmptyDataToDefault();
        gravityListAVG.convertEmptyDataToDefault();
        magneticFieldListAVG.convertEmptyDataToDefault();
        gmrvListAVG.convertEmptyDataToDefault();
        timeListAVG.convertEmptyDataToDefault();
    }

    //This will make a prediction
    public float[] doInference(int fromMinute) {
        //We're gonna select 1 min of data which will be the input
        float[][][] input = new float[1][60][15];
        int inputIdx = 0;

        for (int i = 0; i < 58; i++) {
            String[] accSplits = accelerometerListAVG.get(i).split(",");
            String[] gyroSplits = gyroListAVG.get(i).split(",");
            String[] gravitySplits = gravityListAVG.get(i).split(",");
            String[] mfSplits = magneticFieldListAVG.get(i).split(",");
            String[] gmrvSplits = gmrvListAVG.get(i).split(",");
            input[0][inputIdx] = new float[]{Float.parseFloat(accSplits[0]), Float.parseFloat(accSplits[1]), Float.parseFloat(accSplits[2]),
                Float.parseFloat(gyroSplits[0]), Float.parseFloat(gyroSplits[1]), Float.parseFloat(gyroSplits[2]),
                Float.parseFloat(gravitySplits[0]), Float.parseFloat(gravitySplits[1]), Float.parseFloat(gravitySplits[2]),
                Float.parseFloat(mfSplits[0]), Float.parseFloat(mfSplits[1]), Float.parseFloat(mfSplits[2]),
                Float.parseFloat(gmrvSplits[0]), Float.parseFloat(gmrvSplits[1]), Float.parseFloat(gmrvSplits[2])};

            inputIdx++;
        }

        accelerometerListAVG.clear();
        gyroListAVG.clear();
        gravityListAVG.clear();
        magneticFieldListAVG.clear();
        gmrvListAVG.clear();

        //Define the shape of output, the result will be stored here
        float[][] outputVal = new float[1][3];

        //Run inference
        interpreter.run(input, outputVal);

        //Return the result
        return outputVal[0];
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
        localTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        timeList.add(formatter.format(localTime));

        // These are for the average lists
        final LocalTime[] oldTime = {LocalTime.now(), LocalTime.now(),
                LocalTime.now(), LocalTime.now(), LocalTime.now()};
        ArrayList<String> tempAcceleratorList = new ArrayList<>();
        ArrayList<String> tempGyroList = new ArrayList<>();
        ArrayList<String> tempGravityList = new ArrayList<>();
        ArrayList<String> tempMFList = new ArrayList<>();
        ArrayList<String> tempGMRVList = new ArrayList<>();

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            accelerometer.setListener((timestamp, tx, ty, ts) -> {
                acceleratorText.setText(tx + "\n" + ty + "\n" + ts);
                accelerometerList.add(tx + "," + ty + "," + ts);
                Log.d("Accelerometer", tx + "," + ty + "," + ts);

                // Check if a minute have passed. If yes then the new time will be added to the list.
                // Otherwise a placeholder text will be added in place
                localTime = LocalTime.now();
                timeList.add(formatter.format(localTime));

                //Put to AVG list if a minute have passed
                if (!formatter.format(oldTime[0]).equals(formatter.format(localTime))) {
                    oldTime[0] = localTime;
                    accelerometerListAVG.add(createAVGSample(tempAcceleratorList));
                    tempAcceleratorList.clear();
                }
                else {
                    tempAcceleratorList.add(tx + "," + ty + "," + ts);
                }
            });
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscope.setListener((timestamp, tx, ty, ts) -> {
                gyroText.setText(tx + "\n" + ty + "\n" + ts);
                gyroList.add(tx + "," + ty + "," + ts);
                Log.d("Gyro", tx + "," + ty + "," + ts);

                //Put to AVG list if a minute have passed
                if (!formatter.format(oldTime[1]).equals(formatter.format(localTime))) {
                    oldTime[1] = localTime;
                    gyroListAVG.add(createAVGSample(tempGyroList));
                    tempGyroList.clear();
                }
                else {
                    tempGyroList.add(tx + "," + ty + "," + ts);
                }
            });
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            gravity.setListener((timestamp, tx, ty, ts) -> {
                gravityText.setText(tx + "\n" + ty + "\n" + ts);
                gravityList.add(tx + "," + ty + "," + ts);
                Log.d("Gravity", tx + "," + ty + "," + ts);

                //Put to AVG list if a minute have passed
                if (!formatter.format(oldTime[2]).equals(formatter.format(localTime))) {
                    oldTime[2] = localTime;
                    gravityListAVG.add(createAVGSample(tempGravityList));
                    tempGravityList.clear();
                }
                else {
                    tempGravityList.add(tx + "," + ty + "," + ts);
                }
            });
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            magneticField.setListener((timestamp, tx, ty, ts) -> {
                magneticFieldText.setText(tx + "\n" + ty + "\n" + ts);
                magneticFieldList.add(tx + "," + ty + "," + ts);
                Log.d("Magnetic", tx + "," + ty + "," + ts);

                //Put to AVG list if a minute have passed
                if (!formatter.format(oldTime[3]).equals(formatter.format(localTime))) {
                    oldTime[3] = localTime;
                    magneticFieldListAVG.add(createAVGSample(tempMFList));
                    tempMFList.clear();
                }
                else {
                    tempMFList.add(tx + "," + ty + "," + ts);
                }
            });
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null) {
            geoMagneticRotationVector.setListener((timestamp, tx, ty, ts) -> {
                // acceleratorText.setText(tx + "\n" + ty + "\n" + ts);
                gmrvList.add(tx + "," + ty + "," + ts);
                Log.d("GeoMagneticRotation", tx + "," + ty + "," + ts);

                //Put to AVG list if a minute have passed
                if (!formatter.format(oldTime[4]).equals(formatter.format(localTime))) {
                    oldTime[4] = localTime;
                    gmrvListAVG.add(createAVGSample(tempGMRVList));
                    tempGMRVList.clear();
                }
                else {
                    tempGMRVList.add(tx + "," + ty + "," + ts);
                }
            });
        }

        accelerometer.register();
        gyroscope.register();
        gravity.register();
        magneticField.register();
        geoMagneticRotationVector.register();
    }

    public String createAVGSample(ArrayList<String> tempList) {
        String avgSample = "";

        if (tempList.size() != 0) {
            Float sensor_X = (float) 0;
            Float sensor_Y = (float) 0;
            Float sensor_Z = (float) 0;
            for (String item : tempList) {
                String[] sensorValues = item.split(",");
                sensor_X += Float.parseFloat(sensorValues[0]);
                sensor_Y += Float.parseFloat(sensorValues[1]);
                sensor_Z += Float.parseFloat(sensorValues[2]);
            }
            sensor_X /= tempList.size();
            sensor_Y /= tempList.size();
            sensor_Z /= tempList.size();

            avgSample = sensor_X + "," + sensor_Y + "," + sensor_Z;
        }
        return avgSample;
    }

    //Clears List
    public void bt_clearAxisListOnClick(android.view.View avv){
        accelerometer.sensorList.clear();
        gyroscope.sensorList.clear();
        gravity.sensorList.clear();
        magneticField.sensorList.clear();
        geoMagneticRotationVector.sensorList.clear();
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
        Intent intent = Helpers.createIntent(name, selectedMode);

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

    public void equalizeSensorLists() {
        // Get the max length (This will be the size of all the lists
        ArrayList<Integer> listLengths = new ArrayList<Integer>
                (Arrays.asList(accelerometerList.size(), gyroList.size(), gravityList.size(),
                        magneticFieldList.size(), gmrvList.size()));
        int maxLength = Collections.max(listLengths);

        accelerometerList.equalizeSensorList(maxLength, "");
        gyroList.equalizeSensorList(maxLength, "");
        gravityList.equalizeSensorList(maxLength, "");
        magneticFieldList.equalizeSensorList(maxLength, "");
        gmrvList.equalizeSensorList(maxLength, "");
        timeList.equalizeSensorList(maxLength, "-");
    }

    // This will export all the sensors data into one CSV
    public void exportAllToOneCSV(Intent resultData){
        // Making all sensor lists size equal
        equalizeSensorLists();

        // Creating combinedList
        ArrayList<String> combinedList = Helpers.combineSensorLists(accelerometerList, gyroList,
                gravityList, magneticFieldList, gmrvList, timeList,
                accelerometer.timesTamp.toString(), selectedMode);

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