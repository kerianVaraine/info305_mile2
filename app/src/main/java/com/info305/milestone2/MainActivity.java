package com.info305.milestone2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

//graphView Stuff
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float x, y, z, fftX, fftY, fftZ;

    private ArrayList<Float> xValue = new ArrayList<Float>();

    private TextView currentX, currentY, currentZ, currentMag, currentFFTX,currentFFTY,currentFFTZ,currentFFTMag;

//Graph stuff for raw data
private final Handler mHandler = new Handler();
    private Runnable mTimer1;
    private Runnable mTimer2;
    private double graph2LastXValue = 5d;
    private LineGraphSeries seriesX;
    private LineGraphSeries<DataPoint> seriesY;
    private LineGraphSeries<DataPoint> seriesZ;
    private LineGraphSeries<DataPoint> seriesMag;

    //FFT graph
    private LineGraphSeries<DataPoint> fftmSeries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //main init for values etc...
        initViews();

        //Accelerometer sensor inits
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //INITS
    public void initViews() {
    currentX = findViewById(R.id.currentX);
    currentY = findViewById(R.id.currentY);
        currentZ = findViewById(R.id.currentZ);
        currentMag = findViewById(R.id.currentMag);

    //GraphView Stuff
        //Raw
        GraphView graph = (GraphView) findViewById(R.id.graph);

        // first series is a line
        seriesX = new LineGraphSeries<DataPoint>();
        seriesX.setDrawDataPoints(true);
        seriesX.setColor(-256);
        graph.addSeries(seriesX);
//        graph.getViewport().setMinX(0);
        graph.getViewport().setXAxisBoundsManual(true);
//        graph.getViewport().setMaxX(40);


        //FFT graph
        GraphView fftGraph = (GraphView) findViewById(R.id.fftGraph);

    }




    @Override
    public void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mTimer1);
//        mHandler.removeCallbacks(mTimer2);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] accelValues = sensorEvent.values;
        x = accelValues[0];
        y = accelValues[1];
        z = accelValues[2];
        displayValues();
        //wanna push new values into array, shift all array down, then get last one to plot.
        xValue.add(x);

    }

//    public DataPoint[] getX() {
//        int count = 256;
////        DataPoint[] values = new DataPoint[count];
////        for (int i = 0; i < count; i++) {
////            DataPoint v = new DataPoint(xValue.get(i), x);
////            values[i] = v;
////        }
////        return values;
////        DataPoint[] values = new DataPoint[count];
////        return values[0];
//
//    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mTimer1 = new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                graph2LastXValue += 1d;
                seriesX.appendData(new DataPoint(graph2LastXValue,calcMagnitude()), true, 256);
            }
        };
        mHandler.postDelayed(mTimer1, 20);

//        mTimer2 = new Runnable() {
//            @Override
//            public void run() {
//                graph2LastXValue += 1d;
//                seriesX.appendData(new DataPoint(graph2LastXValue, getX()), true, 256);
//                mHandler.postDelayed(this, 200);
//            }
//        };
//        mHandler.postDelayed(mTimer2, 1000);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public void displayValues(){
        currentX.setText(Float.toString(x));
        currentY.setText(Float.toString(y));
        currentZ.setText(Float.toString(z));
        currentMag.setText(Double.toString(calcMagnitude()));
    }

    public double calcMagnitude (){

        return java.lang.Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }
}
