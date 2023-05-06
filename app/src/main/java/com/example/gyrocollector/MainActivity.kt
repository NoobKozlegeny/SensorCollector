package com.example.gyrocollector

import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gyrocollector.helpers.*
import com.example.gyrocollector.sensors.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    var testText: TextView? = null
    var acceleratorText: TextView? = null
    var gyroText: TextView? = null
    var gravityText: TextView? = null
    var magneticFieldText: TextView? = null
    var predictionText: TextView? = null
    var dataGatheringTimer: Timer? = null
    var predictTimer: Timer? = null
    var localTime: LocalTime? = null
    var selectedMode: String? = null
    var sensorManager: SensorManager? = null
    var gyroscope: Gyroscope? = null
    var accelerometer: Accelerometer? = null
    var gravity: Gravity? = null
    var magneticField: MagneticField? = null
    var geoMagneticRotationVector: GeoMagneticRotationVector? = null
    var timeList: ArrayList<String>? = null

    //Interpreter tflite;
    var tfLiteModel: TfLiteModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        testText = findViewById(R.id.test)
        acceleratorText = findViewById(R.id.acceleroData)
        gyroText = findViewById(R.id.gyroData)
        gravityText = findViewById(R.id.gravityData)
        magneticFieldText = findViewById(R.id.magneticFieldData)
        predictionText = findViewById(R.id.tv_predictionData)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        selectedMode = "False"
        gyroscope = Gyroscope(this, sensorManager)
        accelerometer = Accelerometer(this, sensorManager)
        gravity = Gravity(this, sensorManager)
        magneticField = MagneticField(this, sensorManager)
        geoMagneticRotationVector = GeoMagneticRotationVector(this, sensorManager)
        timeList = ArrayList()

        // Initializng interpreter
        tfLiteModel = TfLiteModel(this)
        tfLiteModel!!.initialize()

        //Setting up DropDownMenu's items
        val spinner = findViewById<Spinner>(R.id.sp_selectMode)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.dropDownMenuModes,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this

        // Keeps the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    //Starts the data gathering for X minutes
    @Throws(InterruptedException::class)
    fun bt_gatherStartOnClick(v: View?) {
        //Running the data collecting for X minutes defined in the gatherLength textView, Throws Exception
        val time = findViewById<TextView>(R.id.gatherLength)

        //Checks if the time's text did get a number input
        if (time.text.toString().toIntOrNull() != null) {
            //Getting the minute which have been added to the text
            val input = time.text.toString()
            val minutes = input.toLong() * 60000

            //This variable will decide which part of the collected datas will be predicted on
            val fromMinute = AtomicInteger()

            //TimerTask which will create the file and stops the background data gathering
            dataGatheringTimer = Timer("dataGatheringTimer")
            predictTimer = Timer("predictTimer")
            val dataGatheringTask: TimerTask = object : TimerTask() {
                override fun run() {
                    runOnUiThread {

                        //Kills the currently running prediction task
                        predictTimer!!.purge()
                        onPause()
                        createFile("ALL")
                    }
                }
            }
            val predictTask: TimerTask = object : TimerTask() {
                override fun run() {
                    runOnUiThread {

                        //Replaces empty string ("") to default lines ("0,0,0") in ArrayLists
                        replaceEmptyData()
                        //Make a prediction and get the confidences
                        val predictions = doInference()
                        //Increase the fromMinute so next time we won't predict on the same data again
                        fromMinute.addAndGet(1)
                        //Display the prediction result onto the predictionData text
                        val newLine = System.getProperty("line.separator")
                        predictionText!!.text = java.lang.String.join(
                            newLine,
                            "Confidence results:",
                            "0 (Slow): " + predictions[0],
                            "1 (Normal): " + predictions[1],
                            "2 (Fast): " + predictions[2]
                        )
                    }
                }
            }
            //Sleeps for a bit to properly prepare for the data session
            Thread.sleep(5000)
            //Starts the sensor's data gathering
            startGathering()
            //Starts timers
            dataGatheringTimer!!.schedule(dataGatheringTask, minutes)
            if (selectedMode.equals("True")) {
                predictTimer!!.schedule(predictTask, 60000, 60000) //Periodically runs every minute
            }
            else {
                predictionText!!.text = "Not doing prediction"
            }
        } else {
            //Sleeps for a bit to properly prepare for the data session
            Thread.sleep(5000)
            //Starts the sensor's data gathering
            startGathering()
        }
    }

    //Replaces empty string ("") to default lines ("0,0,0") in ArrayLists
    fun replaceEmptyData() {
        accelerometer?.sensorListAVG!!.convertEmptyDataToDefault()
        gyroscope?.sensorListAVG!!.convertEmptyDataToDefault()
        gravity?.sensorListAVG!!.convertEmptyDataToDefault()
        magneticField?.sensorListAVG!!.convertEmptyDataToDefault()
        geoMagneticRotationVector?.sensorListAVG!!.convertEmptyDataToDefault()
    }

    //This will make a prediction
    fun doInference(): FloatArray {
        //We're gonna select 1 min of data which will be the input
        val input = Array(1) { Array(60) { FloatArray(15) } }
        var inputIdx = 1
        for (i in 1..57) {
            val accSplits =
                accelerometer?.sensorListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val gyroSplits =
                gyroscope?.sensorListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val gravitySplits =
                gravity?.sensorListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val mfSplits =
                magneticField?.sensorListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val gmrvSplits =
                geoMagneticRotationVector?.sensorListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            input[0][inputIdx] = floatArrayOf(
                accSplits[0].toFloat(), accSplits[1].toFloat(), accSplits[2].toFloat(),
                gyroSplits[0].toFloat(), gyroSplits[1].toFloat(), gyroSplits[2].toFloat(),
                gravitySplits[0].toFloat(), gravitySplits[1].toFloat(), gravitySplits[2].toFloat(),
                mfSplits[0].toFloat(), mfSplits[1].toFloat(), mfSplits[2].toFloat(),
                gmrvSplits[0].toFloat(), gmrvSplits[1].toFloat(), gmrvSplits[2].toFloat()
            )
            inputIdx++
        }
        accelerometer?.sensorListAVG!!.clear()
        gyroscope?.sensorListAVG!!.clear()
        gravity?.sensorListAVG!!.clear()
        magneticField?.sensorListAVG!!.clear()
        geoMagneticRotationVector?.sensorListAVG!!.clear()

        //Define the shape of output, the result will be stored here
        val outputVal = Array(1) { FloatArray(3) }

        //Run inference
        tfLiteModel!!.interpreter.run(input, outputVal)

        //Return the result
        return outputVal[0]
    }

    //Stops data gathering by clicking on the stop gathering button
    fun bt_gatherStopOnClick(v: View?) {
        onPause()
        createFile("ALL")
    }

    //Initialises sensors and starts gathering
    fun startGathering() {
        localTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        timeList!!.add(formatter.format(localTime))

        // These are for the average lists
        val oldTime = LocalTime.now()
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            acceleratorText?.setText("Collecting data ^^")
            accelerometer?.setSensorListener(timeList, formatter, oldTime)
            // setSensorListener(accelerometer, accelerometerList, timeList, formatter, oldTime, accelerometerListAVG, tempAcceleratorList)
        }
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroText?.setText("Collecting data ^^")
            gyroscope?.setSensorListener(formatter, oldTime)
        }
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            gravityText?.setText("Collecting data ^^")
            gravity?.setSensorListener(formatter, oldTime)
        }
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            magneticFieldText?.setText("Collecting data ^^")
            magneticField?.setSensorListener(formatter, oldTime)
        }
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null) {
            geoMagneticRotationVector?.setSensorListener(formatter, oldTime)
        }
        accelerometer!!.register()
        gyroscope!!.register()
        gravity!!.register()
        magneticField!!.register()
        geoMagneticRotationVector!!.register()
    }

    //Clears List
    fun bt_clearAxisListOnClick(v: View?) {
        accelerometer!!.sensorList.clear()
        gyroscope!!.sensorList.clear()
        gravity!!.sensorList.clear()
        magneticField!!.sensorList.clear()
        geoMagneticRotationVector!!.sensorList.clear()
    }

    //Stops the sensor's data gathering
    override fun onPause() {
        super.onPause()
        accelerometer!!.unRegister()
        gyroscope!!.unRegister()
        gravity!!.unRegister()
        magneticField!!.unRegister()
        geoMagneticRotationVector!!.unRegister()
        testText!!.text = "Finished data gathering"
    }

    //Saves data to CSV file
    private fun createFile(name: String) {
        val intent = createIntent(name, selectedMode!!)
        if (name == "ACCELEROMETER") {
            startActivityForResult(intent, CREATE_FILE_ACCELERO)
        } else if (name == "GYROSCOPE") {
            startActivityForResult(intent, CREATE_FILE_GYRO)
        } else {
            startActivityForResult(intent, CREATE_FILE_ALL)
        }
    }

    //This activity will call the methods which will save the datas to a CSV
    public override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        resultData: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                accelerometer!!.ExportToCSV(resultData)
            } else if (requestCode == 2) {
                gyroscope!!.ExportToCSV(resultData)
            } else {
                exportAllToOneCSV(resultData)
            }
        }
    }

    fun equalizeSensorLists() {
        // Get the max length (This will be the size of all the lists
        val listLengths = ArrayList(
            Arrays.asList(
                accelerometer?.sensorListAVG!!.size, gyroscope?.sensorListAVG!!.size,
                gravity?.sensorListAVG!!.size, magneticField?.sensorListAVG!!.size,
                geoMagneticRotationVector?.sensorListAVG!!.size
            )
        )
        val maxLength = Collections.max(listLengths)
        accelerometer?.sensorListAVG!!.equalizeSensorList(maxLength, "")
        gyroscope?.sensorListAVG!!.equalizeSensorList(maxLength, "")
        gravity?.sensorListAVG!!.equalizeSensorList(maxLength, "")
        magneticField?.sensorListAVG!!.equalizeSensorList(maxLength, "")
        geoMagneticRotationVector?.sensorListAVG!!.equalizeSensorList(maxLength, "")
        timeList!!.equalizeSensorList(maxLength, "-")
    }

    // This will export all the sensors data into one CSV
    fun exportAllToOneCSV(resultData: Intent?) {
        // Making all sensor lists size equal
        equalizeSensorLists()

        // Creating combinedList
        val combinedList = combineSensorLists(
            accelerometer?.sensorList!!, gyroscope?.sensorList!!,
            gravity?.sensorList!!, magneticField?.sensorList!!,
            geoMagneticRotationVector?.sensorList!!, timeList!!,
            accelerometer!!.timesTamp.toString()
        )

        // Exporting the combinedList to a CSV file
        if (resultData != null) {
            val uri = resultData.data
            // Perform operations on the document using its URI.
            try {
                val cr = contentResolver
                val outputStream = cr.openOutputStream(uri!!)
                for (line in combinedList) {
                    outputStream!!.write(line.toByteArray(StandardCharsets.UTF_8))
                    outputStream.write("\n".toByteArray(StandardCharsets.UTF_8))
                }
                outputStream!!.close()
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        selectedMode = adapterView.getItemAtPosition(i).toString()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}

    companion object {
        const val CREATE_FILE_ACCELERO = 1
        const val CREATE_FILE_GYRO = 2
        const val CREATE_FILE_ALL = 3
        @JvmField
        var hasGyro = false
        @JvmField
        var hasAccelero = false
        @JvmField
        var hasGravity = false
        @JvmField
        var hasMagnetic = false
        @JvmField
        var hasGeoMagneticRotation = false
    }
}