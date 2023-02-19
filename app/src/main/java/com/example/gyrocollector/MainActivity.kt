package com.example.gyrocollector

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gyrocollector.helpers.*
import com.example.gyrocollector.sensors.*
import java.io.IOException
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
    var accelerometerList: ArrayList<String>? = null
    var gyroList: ArrayList<String>? = null
    var gravityList: ArrayList<String>? = null
    var magneticFieldList: ArrayList<String>? = null
    var gmrvList: ArrayList<String>? = null
    var timeList: ArrayList<String>? = null
    var accelerometerListAVG: ArrayList<String>? = null
    var gyroListAVG: ArrayList<String>? = null
    var gravityListAVG: ArrayList<String>? = null
    var magneticFieldListAVG: ArrayList<String>? = null
    var gmrvListAVG: ArrayList<String>? = null
    var timeListAVG: ArrayList<String>? = null

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
        selectedMode = "Slow"
        gyroscope = Gyroscope(this, sensorManager)
        accelerometer = Accelerometer(this, sensorManager)
        gravity = Gravity(this, sensorManager)
        magneticField = MagneticField(this, sensorManager)
        geoMagneticRotationVector = GeoMagneticRotationVector(this, sensorManager)
        accelerometerList = ArrayList()
        gyroList = ArrayList()
        gravityList = ArrayList()
        magneticFieldList = ArrayList()
        gmrvList = ArrayList()
        timeList = ArrayList()
        accelerometerListAVG = ArrayList()
        gyroListAVG = ArrayList()
        gravityListAVG = ArrayList()
        magneticFieldListAVG = ArrayList()
        gmrvListAVG = ArrayList()
        timeListAVG = ArrayList()

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

        //Keeps the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    //Starts the data gathering for X minutes
    @Throws(InterruptedException::class)
    fun bt_gatherStartOnClick() {
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
            predictTimer!!.schedule(predictTask, 60000, 60000) //Periodically runs every minute
        } else {
            //Sleeps for a bit to properly prepare for the data session
            Thread.sleep(5000)
            //Starts the sensor's data gathering
            startGathering()
        }
    }

    //Replaces empty string ("") to default lines ("0,0,0") in ArrayLists
    fun replaceEmptyData() {
        accelerometerListAVG!!.convertEmptyDataToDefault()
        gyroListAVG!!.convertEmptyDataToDefault()
        gravityListAVG!!.convertEmptyDataToDefault()
        magneticFieldListAVG!!.convertEmptyDataToDefault()
        gmrvListAVG!!.convertEmptyDataToDefault()
        timeListAVG!!.convertEmptyDataToDefault()

    }

    //This will make a prediction
    fun doInference(): FloatArray {
        //We're gonna select 1 min of data which will be the input
        val input = Array(1) { Array(60) { FloatArray(15) } }
        var inputIdx = 0
        for (i in 0..57) {
            val accSplits =
                accelerometerListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val gyroSplits =
                gyroListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val gravitySplits =
                gravityListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val mfSplits =
                magneticFieldListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val gmrvSplits =
                gmrvListAVG!![i].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            input[0][inputIdx] = floatArrayOf(
                accSplits[0].toFloat(),
                accSplits[1].toFloat(),
                accSplits[2].toFloat(),
                gyroSplits[0].toFloat(),
                gyroSplits[1].toFloat(),
                gyroSplits[2].toFloat(),
                gravitySplits[0].toFloat(),
                gravitySplits[1].toFloat(),
                gravitySplits[2].toFloat(),
                mfSplits[0].toFloat(),
                mfSplits[1].toFloat(),
                mfSplits[2].toFloat(),
                gmrvSplits[0].toFloat(),
                gmrvSplits[1].toFloat(),
                gmrvSplits[2].toFloat()
            )
            inputIdx++
        }
        accelerometerListAVG!!.clear()
        gyroListAVG!!.clear()
        gravityListAVG!!.clear()
        magneticFieldListAVG!!.clear()
        gmrvListAVG!!.clear()

        //Define the shape of output, the result will be stored here
        val outputVal = Array(1) { FloatArray(3) }

        //Run inference
        tfLiteModel!!.interpreter.run(input, outputVal)

        //Return the result
        return outputVal[0]
    }

    //Stops data gathering by clicking on the stop gathering button
    fun bt_gatherStopOnClick() {
        // Delete the last 50 lines of data bc we had to stop it manually
        onPause()
        createFile("ALL")
        //        createFile("ACCELEROMETER");
//        createFile("GYROSCOPE");
    }

    //Initialises sensors and starts gathering
    fun startGathering() {
        localTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        timeList!!.add(formatter.format(localTime))

        // These are for the average lists
        val oldTime = arrayOf(
            LocalTime.now(), LocalTime.now(),
            LocalTime.now(), LocalTime.now(), LocalTime.now()
        )
        val tempAcceleratorList = ArrayList<String>()
        val tempGyroList = ArrayList<String>()
        val tempGravityList = ArrayList<String>()
        val tempMFList = ArrayList<String>()
        val tempGMRVList = ArrayList<String>()
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            accelerometer!!.setListener { timestamp: Long, tx: Float, ty: Float, ts: Float ->
                acceleratorText!!.text = """
     $tx
     $ty
     $ts
     """.trimIndent()
                accelerometerList!!.add("$tx,$ty,$ts")
                Log.d("Accelerometer", "$tx,$ty,$ts")

                // Check if a minute have passed. If yes then the new time will be added to the list.
                // Otherwise a placeholder text will be added in place
                localTime = LocalTime.now()
                timeList!!.add(formatter.format(localTime))

                //Put to AVG list if a minute have passed
                if (formatter.format(oldTime[0]) != formatter.format(localTime)) {
                    oldTime[0] = localTime
                    accelerometerListAVG!!.add(createAVGSample(tempAcceleratorList))
                    tempAcceleratorList.clear()
                } else {
                    tempAcceleratorList.add("$tx,$ty,$ts")
                }
            }
        }
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscope!!.setListener { timestamp: Long, tx: Float, ty: Float, ts: Float ->
                gyroText!!.text = """
     $tx
     $ty
     $ts
     """.trimIndent()
                gyroList!!.add("$tx,$ty,$ts")
                Log.d("Gyro", "$tx,$ty,$ts")

                //Put to AVG list if a minute have passed
                if (formatter.format(oldTime[1]) != formatter.format(localTime)) {
                    oldTime[1] = localTime
                    gyroListAVG!!.add(createAVGSample(tempGyroList))
                    tempGyroList.clear()
                } else {
                    tempGyroList.add("$tx,$ty,$ts")
                }
            }
        }
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            gravity!!.setListener { timestamp: Long, tx: Float, ty: Float, ts: Float ->
                gravityText!!.text = """
     $tx
     $ty
     $ts
     """.trimIndent()
                gravityList!!.add("$tx,$ty,$ts")
                Log.d("Gravity", "$tx,$ty,$ts")

                //Put to AVG list if a minute have passed
                if (formatter.format(oldTime[2]) != formatter.format(localTime)) {
                    oldTime[2] = localTime
                    gravityListAVG!!.add(createAVGSample(tempGravityList))
                    tempGravityList.clear()
                } else {
                    tempGravityList.add("$tx,$ty,$ts")
                }
            }
        }
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            magneticField!!.setListener { timestamp: Long, tx: Float, ty: Float, ts: Float ->
                magneticFieldText!!.text = """
     $tx
     $ty
     $ts
     """.trimIndent()
                magneticFieldList!!.add("$tx,$ty,$ts")
                Log.d("Magnetic", "$tx,$ty,$ts")

                //Put to AVG list if a minute have passed
                if (formatter.format(oldTime[3]) != formatter.format(localTime)) {
                    oldTime[3] = localTime
                    magneticFieldListAVG!!.add(createAVGSample(tempMFList))
                    tempMFList.clear()
                } else {
                    tempMFList.add("$tx,$ty,$ts")
                }
            }
        }
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null) {
            geoMagneticRotationVector!!.setListener { timestamp: Long, tx: Float, ty: Float, ts: Float ->
                // acceleratorText.setText(tx + "\n" + ty + "\n" + ts);
                gmrvList!!.add("$tx,$ty,$ts")
                Log.d("GeoMagneticRotation", "$tx,$ty,$ts")

                //Put to AVG list if a minute have passed
                if (formatter.format(oldTime[4]) != formatter.format(localTime)) {
                    oldTime[4] = localTime
                    gmrvListAVG!!.add(createAVGSample(tempGMRVList))
                    tempGMRVList.clear()
                } else {
                    tempGMRVList.add("$tx,$ty,$ts")
                }
            }
        }
        accelerometer!!.register()
        gyroscope!!.register()
        gravity!!.register()
        magneticField!!.register()
        geoMagneticRotationVector!!.register()
    }

    fun createAVGSample(tempList: ArrayList<String>): String {
        var avgSample = ""
        if (tempList.size != 0) {
            var sensor_X = 0f
            var sensor_Y = 0f
            var sensor_Z = 0f
            for (item in tempList) {
                val sensorValues =
                    item.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                sensor_X += sensorValues[0].toFloat()
                sensor_Y += sensorValues[1].toFloat()
                sensor_Z += sensorValues[2].toFloat()
            }
            sensor_X /= tempList.size.toFloat()
            sensor_Y /= tempList.size.toFloat()
            sensor_Z /= tempList.size.toFloat()
            avgSample = "$sensor_X,$sensor_Y,$sensor_Z"
        }
        return avgSample
    }

    //Clears List
    fun bt_clearAxisListOnClick() {
        accelerometer!!.sensorList.clear()
        gyroscope!!.sensorList.clear()
        gravity!!.sensorList.clear()
        magneticField!!.sensorList.clear()
        geoMagneticRotationVector!!.sensorList.clear()
        accelerometerList!!.clear()
        gyroList!!.clear()
        gravityList!!.clear()
        magneticFieldList!!.clear()
        gmrvList!!.clear()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
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
                accelerometerList!!.size, gyroList!!.size, gravityList!!.size,
                magneticFieldList!!.size, gmrvList!!.size
            )
        )
        val maxLength = Collections.max(listLengths)
        accelerometerList!!.equalizeSensorList(maxLength, "")
        gyroList!!.equalizeSensorList(maxLength, "")
        gravityList!!.equalizeSensorList(maxLength, "")
        magneticFieldList!!.equalizeSensorList(maxLength, "")
        gmrvList!!.equalizeSensorList(maxLength, "")
        timeList!!.equalizeSensorList(maxLength, "-")
    }

    // This will export all the sensors data into one CSV
    fun exportAllToOneCSV(resultData: Intent?) {
        // Making all sensor lists size equal
        equalizeSensorLists()

        // Creating combinedList
        val combinedList = combineSensorLists(
            accelerometerList!!, gyroList!!,
            gravityList!!, magneticFieldList!!, gmrvList!!, timeList!!,
            accelerometer!!.timesTamp.toString(), selectedMode!!
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