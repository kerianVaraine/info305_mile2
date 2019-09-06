package com.info305.milestone2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float x, y, z;
    private double mag;

    //FFT inits -- window size = 16, sampleRate = 60hz
    int count;
    private int windowSize = 16;

    // in microSeconds -- 1000000 ms = 1hz
    //init at 60hz
    private int sampleRate = 60000000;

    private FFT transform = new FFT(windowSize);

    double[] fftImaginary = new double[windowSize];
    double[] fftMagDouble = new double[windowSize];

    private ArrayList<Double> fftMag = new ArrayList<Double>();

    //Media stuff
    private boolean isPlayingSounds = false;
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
    private LocationListener locationListener;
    private double currentSpeed;
    private int speedLimit = 12;

    //final soundfiles!
    private MediaPlayer[] standingSounds;
    private MediaPlayer[] walkingSounds;
    private MediaPlayer[] joggingSounds;

    private boolean soundHasEnded = true;

    private Random r = new Random();
    private MediaPlayer padSound;
    private MediaPlayer padSound2;

    //loop Hack for pad seamless layered looping
    private Timer HACK_loopTimer;
    private TimerTask Hack_loopTask ;
    private long waitingTime;

    //Different mediaPlayer approach - single mediaPlayer given filenames in int.
//    private static final MediaPlayer singleMedia = new MediaPlayer();

    private static MediaPlayer singleMedia;

    private int[][] allTracks = new int[3][4];
    private int[] standingArr = new int[4];
    private int[] walkingArr = new int[4];
    private int[] joggingArr = new int[4];


    //fft imaginary init
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

        //Accelerometer sensor inits
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //location shit
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();


        //populate x fft for init
        for (int i = 0; i < windowSize; i++) {
            fftMagDouble[i] = 0;
        }
        fftImaginary = setImaginary();

        //speed stuff
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //media stuff
//        registerMedia();
//        registerTracks();
    }


    //INITS
    public void initViews() {
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
                registerPad();
                registerMedia();
                registerTracks();
                isPlayingSounds = true;
                padSound.start();
                padSound2.seekTo(15000);
                padSound2.start();
                return true;
            }
        });

        stopSounds.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                playSounds.setEnabled(true);
                stopSounds.setEnabled(false);
                isPlayingSounds = false;
                stopSounds();
                return true;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        stopSounds();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        count++;

        float[] accelValues = sensorEvent.values;
        x = accelValues[0];
        y = accelValues[1];
        z = accelValues[2];
        mag = calcMagnitude();

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

        ///Sound BUTTON ON/OFF
        if(isPlayingSounds) {
            String act = fftThresholds(fftMagDouble[windowSize / 2], count % meanCounter);
            if(soundHasEnded) {
                playSound(act);
            }
        }else {
//            pauseSounds();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, sampleRate);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0,locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopSounds();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    public double calcMagnitude (){
        return java.lang.Math.sqrt(pow(x, 2) + pow(y, 2) + pow(z, 2));
    }

//    SOUND  STUFF

    private void registerMedia() {
        standingSounds = new MediaPlayer[4];
        standingSounds[0] = MediaPlayer.create(this, R.raw.standing1);
        standingSounds[1] = MediaPlayer.create(this, R.raw.standing2);
        standingSounds[2] = MediaPlayer.create(this, R.raw.standing3);
        standingSounds[3] = MediaPlayer.create(this, R.raw.standing4);

        walkingSounds = new MediaPlayer[4];
        walkingSounds[0] = MediaPlayer.create(this, R.raw.walk1);
        walkingSounds[1] = MediaPlayer.create(this, R.raw.walk2);
        walkingSounds[2] = MediaPlayer.create(this, R.raw.walking3);
        walkingSounds[3] = MediaPlayer.create(this, R.raw.walking4);

        joggingSounds = new MediaPlayer[4];
        joggingSounds[0] = MediaPlayer.create(this, R.raw.jog1);
        joggingSounds[1] = MediaPlayer.create(this, R.raw.jog2);
        joggingSounds[2] = MediaPlayer.create(this, R.raw.jog3);
        joggingSounds[3] = MediaPlayer.create(this, R.raw.jog4);
    }
    private void registerPad() {
        padSound = MediaPlayer.create(this, R.raw.all_stretch);
        padSound.setLooping(true);
        padSound.setVolume(0.1f, 0.1f);

        padSound2 = MediaPlayer.create(this, R.raw.all_stretch);
        padSound2.setLooping(true);
        padSound2.setVolume(0.1f,0.1f);
    }

    private void pauseSounds() {
        for(int i = 0; i <standingSounds.length; i++) {
            standingSounds[i].reset();
            walkingSounds[i].reset();
            joggingSounds[i].reset();
        }
        padSound.reset();
        padSound2.reset();

//        padSoundLoop.reset();
    }

    private void stopSounds() {
        for(int i = 0; i <4; i++) {
            standingSounds[i].stop();
            walkingSounds[i].stop();
            joggingSounds[i].stop();
        }
        padSound.stop();
        padSound2.stop();

    }


    private void isItfinished(final MediaPlayer m){
        soundHasEnded = false;
        m.start();

        m.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                System.out.println(mp.getAudioSessionId() + "        RELEASED");
                mp.reset();
                mp.release();
                soundHasEnded = true;

            }
        });


//        soundHasEnded = false;
//    HACK_loopTimer = new Timer();
//    Hack_loopTask = new TimerTask() {
//        @Override
//        public void run() {
//            if(!soundHasEnded){
//                soundHasEnded = true;
//            }
//        }
//    };

    waitingTime = m.getDuration();

//    HACK_loopTimer.schedule(Hack_loopTask, waitingTime, waitingTime);
    }

    //Single player attempt:

    private void registerTracks(){

        standingArr[0] = R.raw.standing1;
        standingArr[1] = R.raw.standing2;
        standingArr[2] = R.raw.standing3;
        standingArr[3] = R.raw.standing4;
        walkingArr[0] = R.raw.walk1;
        walkingArr[1] = R.raw.walk2;
        walkingArr[2] = R.raw.walking3;
        walkingArr[3] = R.raw.walking4;
        joggingArr[0] = R.raw.jog1;
        joggingArr[1] = R.raw.jog2;
        joggingArr[2] = R.raw.jog3;
        joggingArr[3] = R.raw.jog4;

        //combine to 2d array
        allTracks[0] = standingArr;
        allTracks[1] = walkingArr;
        allTracks[2] = joggingArr;
//        singleMedia = SingleMP.getInstance();
    }


    //playsound(fftThresholds(chuck fft[windowsize / 2] in there)
    private void playSound(String activity){
        meanFFTthresh =0;
        int randomTrack = r.nextInt(4);

        MediaPlayer m = new MediaPlayer();

        switch (activity) {
                case ("standing"):
//            standingSounds[randomTrack].start();
//            isItfinished(standingSounds[randomTrack]);
//            isItfinished(SingleMP.getInstance().create(this, allTracks[0][randomTrack]));
                    isItfinished(m.create(this, allTracks[0][randomTrack]));
                    break;
                case ("walking"):
//            walkingSounds[randomTrack].start();
//            isItfinished(walkingSounds[randomTrack]);
//                    isItfinished(SingleMP.getInstance().create(this, allTracks[1][randomTrack]));
                    isItfinished(m.create(this, allTracks[1][randomTrack]));
                    break;
                case ("jogging"):
//            joggingSounds[randomTrack].start();
//            isItfinished(joggingSounds[randomTrack]);
//                    isItfinished(SingleMP.getInstance().create(this, allTracks[2][randomTrack]));
                    isItfinished(m.create(this, allTracks[2][randomTrack]));
                    break;
                case ("pause"):
                    m.release();
                    pauseSounds();
                    break;
            }

}

            private String fftThresholds (double fftData, int counter) {
                fftData = abs(fftData);
                threshFFTarr[counter] = fftData;
                //values for bin = windowWidth / 2
                if (counter == 0) {
                    //get mean of all counts
                    for (int i = 0; i < threshFFTarr.length; i++) {
                        meanFFTthresh += threshFFTarr[i];
                    }
                    meanFFTthresh /= threshFFTarr.length;
                    //
                    if (currentSpeed < speedLimit) {
                        if (meanFFTthresh < 3) {
                            return "standing";
                        } else if (meanFFTthresh >= 3 && meanFFTthresh < 34) {
                            return "walking";
                        } else if (meanFFTthresh >= 34) {
                            return "jogging";
                        }
                    } else if (currentSpeed >= speedLimit) {
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
            //calc  speed km/h
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
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {}

        @Override
        public void onProviderDisabled(String s) {}
    }
    }



