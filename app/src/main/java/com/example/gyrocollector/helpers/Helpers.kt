@file:JvmName("Helpers")

package com.example.gyrocollector.helpers

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDate
import java.util.*


//Saves data to CSV file
fun createIntent(name: String, selectedMode: String): Intent {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "text/csv"
    val fileName = selectedMode + name + LocalDate.now().toString().split("-".toRegex())
        .dropLastWhile { it.isEmpty() }
        .toTypedArray()[1] +LocalDate.now().toString().split("-".toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()[2] + ".csv"
    intent.putExtra(Intent.EXTRA_TITLE, fileName)

    return intent;
}

// This will export all the sensors data into one CSV
fun combineSensorLists(accelerometerList: ArrayList<String>, gyroList: ArrayList<String>,
                       gravityList: ArrayList<String>, magneticFieldList: ArrayList<String>,
                       gmrvList: ArrayList<String>, timeList: ArrayList<String>,
                       timestamp: String, selectedMode: String): ArrayList<String> {
    val combinedList = ArrayList<String>()

    // Adding the header lines to the combinedList
    combinedList.add(",TYPE_LINEAR_ACCELERATION,,,TYPE_GYROSCOPE,,,TYPE_GRAVITY,,,TYPE_MAGNETIC_FIELD,,,TYPE_GEOMAGNETIC_ROTATION_VECTOR")
    combinedList.add("Timestamp: ,$timestamp")
    combinedList.add("X_ACC,Y_ACC,Z_ACC,X_GYRO,Y_GYRO,Z_GYRO,X_GRAVITY,Y_GRAVITY,Z_GRAVITY,X_MF,Y_MF,Z_MF,X_GMRV,Y_GMRV,Z_GMRV,TIME,LABEL")

    var i = 0
    combinedList.add(
        accelerometerList.get(i) + "," + gyroList.get(i)
                + "," + gravityList.get(i) + "," + magneticFieldList.get(i)
                + "," + gmrvList.get(i) + "," + timeList.get(i) + "," + selectedMode
    )
    i++

    // Combining the separate sensor datas into the combinedList
    while (i < accelerometerList.size && i < gyroList.size && i < gravityList.size
        && i < magneticFieldList.size && i < gmrvList.size
    ) {
        val lineToAdd: String = accelerometerList.get(i) + "," + gyroList.get(i) + "," + gravityList.get(i) + "," + magneticFieldList.get(i) + "," + gmrvList.get(i) + "," + timeList.get(i) + "," + selectedMode
        combinedList.add(lineToAdd)
        i++
    }

    // Removing the last X lines from combinedList
    i = combinedList.size - 1
    val newLength = ((combinedList.size - 1 - 20) * 0.98).toInt()
    while (i > newLength) {
        combinedList.removeAt(i)
        i--
    }

    return combinedList;
}

//Memory-map the modei file in Assets
@Throws(IOException::class)
fun loadModelFile(ctx: Context): MappedByteBuffer? {
    //Open the model using an input stream, and memory map it to load
    val fileDescriptor: AssetFileDescriptor = ctx.getAssets().openFd("project-11-26.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

