@file:JvmName("TfModel")

package com.example.gyrocollector.tfmodels

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import com.example.gyrocollector.ml.Project1126
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.DataType
import org.tensorflow.lite.DelegateFactory
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime
import org.tensorflow.lite.flex.FlexDelegate
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

//fun test(ctx: Context) {
//    val model = Project1126.newInstance(ctx)
//
//    // Creates inputs for reference.
//    val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 60, 15), DataType.FLOAT32)
//    inputFeature0.loadBuffer(byteBuffer)
//
//    // Runs model inference and gets result.
//    val outputs = model.process(inputFeature0)
//    val outputFeature0 = outputs.outputFeature0AsTensorBuffer
//}

fun initializeInterpeter(ctx: Context): InterpreterApi? {
    //Initalize task and interpreter
    val initializeTask = TfLite.initialize(ctx)
    var interpreter: InterpreterApi? = null;

    //Initialize the interpreter
    initializeTask.addOnSuccessListener { a ->
        try {
            val delegateFactory = DelegateFactory { return@DelegateFactory FlexDelegate() }
            val options: InterpreterApi.Options = Interpreter.Options()
                .setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY)
                .addDelegateFactory(delegateFactory)
            interpreter = InterpreterApi.create(loadModelFile(ctx)!!, options)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }.addOnFailureListener { e: Exception ->
        Log.e(
            "Interpreter", String.format(
                "Cannot initialize interpreter: %s",
                e.message
            )
        )
    }

    return interpreter;
}

//Memory-map the modei file in Assets
@Throws(IOException::class)
private fun loadModelFile(ctx: Context): MappedByteBuffer? {
    //Open the model using an input stream, and memory map it to load
    val fileDescriptor: AssetFileDescriptor = ctx.assets.openFd("project-11-26.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}