package com.info305.milestone2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

//graphView Stuff
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float x, y, z;

    private TextView currentX, currentY, currentZ;




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

    //GraphView Stuff
        GraphView graph = (GraphView) findViewById(R.id.graph);

    }


    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public void displayValues(){
        currentX.setText(Float.toString(x));
        currentY.setText(Float.toString(y));
        currentZ.setText(Float.toString(z));

    }
}
