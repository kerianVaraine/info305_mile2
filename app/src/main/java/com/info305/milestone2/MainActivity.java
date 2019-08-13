package com.info305.milestone2;

import androidx.appcompat.app.AppCompatActivity;

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

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float x, y, z;

    private TextView currentX, currentY, currentZ, currentMag;
//Graph stuff
private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeries;

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
        GraphView graph = (GraphView) findViewById(R.id.graph);

        // first mSeries is a line
        mSeries = new LineGraphSeries<>();
        mSeries.setDrawDataPoints(true);
        mSeries.setDrawBackground(true);
        graph.addSeries(mSeries);
    }




    @Override
    public void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] accelValues = sensorEvent.values;
        x = accelValues[0];
        y = accelValues[1];
        z = accelValues[2];
        displayValues();
    }

    public float getX() {
        return x;
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mTimer = new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 0.25d;
                mHandler.postDelayed(this, 330);
                mSeries.appendData(new DataPoint(graphLastXValue, getX(), true, 22);
            }
        };
        mHandler.postDelayed(mTimer, 1500);

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
