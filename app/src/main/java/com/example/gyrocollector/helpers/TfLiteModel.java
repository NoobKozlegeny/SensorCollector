package com.example.gyrocollector.helpers;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import com.google.android.gms.tasks.Task;
//import com.google.android.gms.tflite.java.TfLite;

//import org.tensorflow.lite.Delegate;
//import org.tensorflow.lite.DelegateFactory;
import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.InterpreterApi;
//import org.tensorflow.lite.RuntimeFlavor;
//import org.tensorflow.lite.flex.FlexDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TfLiteModel {

    private final Context ctx;
    public Interpreter interpreter;

    public TfLiteModel(Context ctx) {
        this.ctx = ctx;
    }

    public void initialize() {
        try {
            interpreter = new Interpreter(loadModelFile());
        } catch (IOException e) {
            Log.e("Interpreter", String.format("Cannot initialize interpreter: %s",
                    e.getMessage()));
        }
    }

    //Memory-map the modei file in Assets
    private MappedByteBuffer loadModelFile() throws IOException {
        //Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = ctx.getAssets().openFd("project-11-26.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
