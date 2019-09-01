package com.info305.milestone2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

//graphView Stuff
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float x, y, z;
    private double mag;


    private TextView currentX, currentY, currentZ, currentMag, windowSizeView, sampleRateView;

    //Graph stuff for raw data
    int count;

    private LineGraphSeries seriesX;
    private LineGraphSeries seriesY;
    private LineGraphSeries seriesZ;
    private LineGraphSeries seriesMag;


    //FFT graph
    private int windowSize = 8;
    private SeekBar windowSizeBar;

    // in microSeconds -- 1000000 ms = 1hz
    private int sampleRate = 1000000;
    private int sampleRateHz;
    private SeekBar sampleRateBar;


    private LineGraphSeries<DataPoint> seriesFFTMag;
    private FFT transform = new FFT(windowSize);

    double[] fftImaginary = new double[windowSize];
    double[] fftMagDouble = new double[windowSize];

    private ArrayList<Double> fftMag = new ArrayList<Double>();

    GraphView fftGraph;

    //LOGGING INITS
    private FileWriter writer;
    private boolean loggingNow;
    private final String TAG = "SensorLog";

    private Button startLoggingButt;
    private Button stopLoggingButt;
    private String samplog;
    private int sampCount;

    /////

    //Media stuff
    private boolean isplayingSounds = false;
    private Button playSounds;
    private Button stopSounds;


    private MediaPlayer standingSound = new MediaPlayer();
    private MediaPlayer walkingSound = new MediaPlayer();
    private MediaPlayer joggingSound = new MediaPlayer();

    private int meanCounter = 20;//this and sample rate combined decide time it takes sample rate 60hz,
    private double meanFFTthresh;
    private double[] threshFFTarr = new double[meanCounter];

    //Speed stuff

    private LocationManager locationManager;
    private Location oldLocation;
    private Location newLocation;
    private LocationListener locationListener;
    private double currentSpeed;


    private double[] setImaginary() {
        double[] temp = new double[windowSize];
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
        loggingNow = false;

        //Accelerometer sensor inits
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sampleRateRefresh(60);

        //location shit
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        oldLocation = new Location(locationManager.GPS_PROVIDER);
        newLocation = new Location(locationManager.GPS_PROVIDER);
        locationListener = new MyLocationListener();


        //populate x fft for init
        for (int i = 0; i < windowSize; i++) {
            fftMagDouble[i] = 0;
        }
        fftImaginary = setImaginary();

//        //
//        //Media Stuff
//        //
//        standingSound = MediaPlayer.create(this, R.raw.standing);
//        walkingSound = MediaPlayer.create(this, R.raw.walking);
//        joggingSound = MediaPlayer.create(this, R.raw.jogging);

        //speed stuff
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //INITS
    public void initViews() {
        currentX = findViewById(R.id.currentX);
        currentY = findViewById(R.id.currentY);
        currentZ = findViewById(R.id.currentZ);
        currentMag = findViewById(R.id.currentMag);

        //Logging button Stuff

        startLoggingButt = findViewById(R.id.startLoggingButt);
        stopLoggingButt = findViewById(R.id.stopLoggingButt);

        stopLoggingButt.setEnabled(false);

        startLoggingButt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                startLoggingButt.setEnabled(false);
                stopLoggingButt.setEnabled(true);
                sampCount = 0;
                Log.d(TAG, "Writing to: " + getStorageDirectory());

                try {
                    writer = new FileWriter(new File(getStorageDirectory(), "acc_" + System.currentTimeMillis() + "SR" + sampleRateHz + "WS" + getWindowSize() + ".csv"));
                    writer.append("sample, mag");
                    for (Integer i = 0; i < getWindowSize(); i++) {
                        writer.append(", " + i.toString());
                    }
                    writer.append("\n");

                } catch (IOException e) {
                    e.printStackTrace();
                }
                loggingNow = true;
                return true;
            }
        });

        stopLoggingButt.setOnTouchListener(new View.OnTouchListener() {
                                               @Override
                                               public boolean onTouch(View view, MotionEvent motionEvent) {
                                                   startLoggingButt.setEnabled(true);
                                                   stopLoggingButt.setEnabled(false);
                                                   loggingNow = false;

                                                   try {
                                                       writer.close();
                                                   } catch (IOException e) {
                                                       e.printStackTrace();
                                                   }

                                                   return true;
                                               }
                                           }

        );


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
        updateGraph();

        //fft slider shit
        windowSizeView = findViewById(R.id.window_size_int);
        sampleRateView = findViewById(R.id.sample_rate_int);

        windowSizeBar = findViewById(R.id.windowSeekBar);
        windowSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {


            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                windowSize = (int) pow(2, (i + 1) - 1);
                transform.setWindowSize((windowSize));
                fftImaginary = new double[transform.getWindowSize()];
                fftMag.clear();
                fftMagDouble = new double[transform.getWindowSize()];
                updateGraph();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        sampleRateBar = findViewById(R.id.sampleRateSeekBar);

        sampleRateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sampleRateRefresh(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        /////
        //MEDIA STUFF
        /////
    playSounds = findViewById(R.id.playSounds);
    stopSounds = findViewById(R.id.stopSounds);
        stopSounds.setEnabled(false);


        playSounds.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                playSounds.setEnabled(false);
                stopSounds.setEnabled(true);
                registerMedia();
                isplayingSounds = true;
                return true;
            }
        });

        stopSounds.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                playSounds.setEnabled(true);
                stopSounds.setEnabled(false);
                isplayingSounds = false;
                pauseSounds();
                return true;
            }
        });


    }

    private String getStorageDirectory() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }


    public void updateGraph() {
        seriesFFTMag = new LineGraphSeries<DataPoint>();

        fftGraph = (GraphView) findViewById(R.id.fftGraph);
        fftGraph.removeAllSeries();
        fftGraph.addSeries(seriesFFTMag);

        fftGraph.getViewport().setYAxisBoundsManual(true);
        fftGraph.getViewport().setMinY(-40);
        fftGraph.getViewport().setMaxY(40);

        fftGraph.getViewport().setXAxisBoundsManual(true);
        fftGraph.getViewport().setMinX(0);
        fftGraph.getViewport().setMaxX(windowSize);
        fftImaginary = setImaginary();
    }


    //in microSeconds
    public void sampleRateRefresh(int hz) {
        sampleRate = hz * 1000000;
        sampleRateHz = hz;
        sensorManager.unregisterListener(this);
        sensorManager.registerListener(this, accelerometer, sampleRate);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        stopSounds();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        count++;

//        System.out.println(getSpeed(oldLocation, newLocation));

        float[] accelValues = sensorEvent.values;
        x = accelValues[0];
        y = accelValues[1];
        z = accelValues[2];
        mag = calcMagnitude();
        displayValues();

        seriesX.appendData(new DataPoint(count, x), true, 256);
        seriesY.appendData(new DataPoint(count, y), true, 256);
        seriesZ.appendData(new DataPoint(count, z), true, 256);
        seriesMag.appendData(new DataPoint(count, mag), true, 256);

        //FFT data stuff

        if (fftMag.size() == windowSize) {
            fftImaginary = setImaginary();
            for (int i = 0; i < windowSize; i++) {
                fftMagDouble[i] = fftMag.get(i);
            }
            fftMag.remove(0);
        }

        fftMag.add(calcMagnitude());

        transform.fft(fftMagDouble, fftImaginary);

        // loop through and create new datapoint[]
        DataPoint[] currentMagFFT = new DataPoint[windowSize];
        for (int i = 0; i < getWindowSize(); i++) {
            currentMagFFT[i] = new DataPoint(i, fftMagDouble[i]);
        }

        ///BUTTON ON/OFF
        if(isplayingSounds) {
            playSound(fftThresholds(fftMagDouble[windowSize / 2], count % meanCounter));
        }else {
            pauseSounds();
        }

        seriesFFTMag.resetData(currentMagFFT);

        /////////
        //LOGGING STUFF HERE
        /////////

        if (loggingNow) {
            try {
                sampCount++;
                samplog = "" + sampCount;
                writer.append(samplog + ", " + mag);
                for (int i = 0; i < getWindowSize(); i++) {
                    writer.append(", " + fftMagDouble[i]);
                }
                writer.append("\n");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, sampleRate);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0,locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        //
        //Media Stuff
        //

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    public void displayValues(){
        currentX.setText(getResources().getString(R.string.x, currentSpeed)); /// changed to test get speed
        currentY.setText(getResources().getString(R.string.y, y));
        currentZ.setText(getResources().getString(R.string.z, z));
        currentMag.setText(getResources().getString(R.string.mag, (float) mag));

        windowSizeView.setText(getResources().getString(R.string.window_size_int, getWindowSize()));//https://github.com/arpitgandhi9/soundtouch-example-android
        sampleRateView.setText(getResources().getString(R.string.sample_rate_int, getSampleRate()));


    }

    public double calcMagnitude (){
        return java.lang.Math.sqrt(pow(x, 2) + pow(y, 2) + pow(z, 2));
    }

    private float getWindowSize(){
        return (int) transform.getWindowSize();
    }

    private float getSampleRate(){
        return sampleRateHz;
    }

//    SOUND  STUFF

    private void registerMedia(){
        standingSound = MediaPlayer.create(this, R.raw.standing);
        walkingSound = MediaPlayer.create(this, R.raw.walking);
        joggingSound = MediaPlayer.create(this, R.raw.jogging);
    }

    private void pauseSounds() {
        standingSound.reset();
        walkingSound.reset();
        joggingSound.reset();
    }

    private void stopSounds() {
        standingSound.stop();
        walkingSound.stop();
        joggingSound.stop();
    }

    //playsound(fftThresholds(chuck fft[windowsize / 2] in there)
    private void playSound(String activity){

        meanFFTthresh =0;

        switch(activity){
            case("standing"):
                standingSound.start();

                break;
            case("walking"):
                walkingSound.start();
                break;
                case("jogging"):
                    joggingSound.start();
                    break;
            case("pause"):
                pauseSounds();
                break;
                }
            }

            //take bin 32
            private String fftThresholds (double fftData, int counter) {
                fftData = abs(fftData);
                threshFFTarr[counter] = fftData;
                //values for bin = windowwidth / 2
                if (counter == 0) {

                    //get mean of all counts
                    for (int i = 0; i < threshFFTarr.length; i++) {
                        meanFFTthresh += threshFFTarr[i];
                    }
                    meanFFTthresh /= threshFFTarr.length;
                    //
                    if (currentSpeed < 13) {
                        if (meanFFTthresh < 2) {
                            return "standing";
                        } else if (meanFFTthresh >= 2 && meanFFTthresh < 20) {
                            return "walking";
                        } else if (meanFFTthresh >= 20) {
                            return "jogging";
                        }
                    } else if (currentSpeed >= 12) {
                        return "pause";
                    }

                }
                return "";

            }

            ///SPEED STUFF


    private class MyLocationListener implements LocationListener{
        private Location mLastLocation;

        @Override
        public void onLocationChanged(Location pCurrentLocation) {
            //calcul manually speed
            double speed = 0;
            if (this.mLastLocation != null)
                speed = Math.sqrt(
                        Math.pow(pCurrentLocation.getLongitude() - mLastLocation.getLongitude(), 2)
                                + Math.pow(pCurrentLocation.getLatitude() - mLastLocation.getLatitude(), 2)
                ) / (pCurrentLocation.getTime() - this.mLastLocation.getTime());
            //if there is speed from location
            if (pCurrentLocation.hasSpeed())
                //get location speed

//                speed = pCurrentLocation.getSpeed(); m/s
            speed = (int) ((pCurrentLocation.getSpeed() * 3600) / 1000); //km/h
            this.mLastLocation = pCurrentLocation;
            currentSpeed = speed;
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    }




    }


