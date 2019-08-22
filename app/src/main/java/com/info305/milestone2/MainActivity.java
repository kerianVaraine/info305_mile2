package com.info305.milestone2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
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

    private TextView currentX, currentY, currentZ, currentMag, currentFFTMag;

//Graph stuff for raw data
    int count;

    private final Handler mHandler = new Handler();
    private LineGraphSeries seriesX;
    private LineGraphSeries seriesY;
    private LineGraphSeries seriesZ;
    private LineGraphSeries seriesMag;



    //FFT graph
    private int windowSize = 64;
    private double doubleWindowSize = windowSize;

    private LineGraphSeries<DataPoint> seriesFFTMag;
    private FFT transform = new FFT(windowSize);

    private ArrayList<Double> fftMag = new ArrayList<Double>();

    double[] fftImaginary = new double[windowSize];
    double[] fftMagDouble = new double[windowSize];

    private double[] setImaginary() {
        double[] temp= new double[windowSize];
        for (int i = 0; i < windowSize; i++) {
            temp[i] = 0;
        }
        return temp;
    }


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
        seriesX.setColor(Color.RED);
        graph.addSeries(seriesX);

        seriesY = new LineGraphSeries<DataPoint>();
        seriesY.setColor(Color.GREEN);
        graph.addSeries(seriesY);

        seriesZ = new LineGraphSeries<DataPoint>();
        seriesZ.setColor(Color.BLUE);
        graph.addSeries(seriesZ);
        seriesMag = new LineGraphSeries<DataPoint>();
        seriesMag.setColor(Color.WHITE);
        graph.addSeries(seriesMag);


        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-40);
        graph.getViewport().setMaxY(40);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(64);

        graph.getViewport().setBackgroundColor(Color.BLACK);

        //FFT graph
        GraphView fftGraph = (GraphView) findViewById(R.id.fftGraph);
        seriesFFTMag = new LineGraphSeries<DataPoint>();

        fftGraph.addSeries(seriesFFTMag);

        fftGraph.getViewport().setYAxisBoundsManual(true);
        fftGraph.getViewport().setMinY(-40);
        fftGraph.getViewport().setMaxY(40);

        fftGraph.getViewport().setXAxisBoundsManual(true);
        fftGraph.getViewport().setMinX(0);
        fftGraph.getViewport().setMaxX(64);



    }




    @Override
    public void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        count++;

        float[] accelValues = sensorEvent.values;
        x = accelValues[0];
        y = accelValues[1];
        z = accelValues[2];
        displayValues();

        seriesX.appendData(new DataPoint(count,x), true, 256);
        seriesY.appendData(new DataPoint(count,y), true, 256);
        seriesZ.appendData(new DataPoint(count,z), true, 256);
        seriesMag.appendData(new DataPoint(count,calcMagnitude()), true, 256);

    //FFT data stuff
        fftMag.add(calcMagnitude());

        if(fftMag.size() == windowSize) {
            fftImaginary = setImaginary();
            for(int i = 0; i < windowSize; i++) {
                fftMagDouble[i] = fftMag.get(i);
            }
            fftMag.remove(0);
        }

        transform.fft(fftMagDouble, fftImaginary);

        // loop through and create new datapoint[]
        DataPoint[] currentMagFFT = new DataPoint[windowSize];
        for(int i = 0; i < windowSize; i++) {
            currentMagFFT[i] =
                    new DataPoint(i, fftMagDouble[i]);
            }
        seriesFFTMag.resetData(currentMagFFT);
        }



    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}


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
