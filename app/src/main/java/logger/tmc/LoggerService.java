package logger.tmc;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Math;
import android.hardware.SensorEvent;

public class LoggerService extends Service implements SensorEventListener, LocationListener{
    private Toast toast;
    private Timer timer;
    private TimerTask timerTask;
    public static boolean serviceIsRunning=false;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    LocationManager locationManager;

    private String accelerometer_data="";
    private double[] position_data=new double[2];
    private float speed_data=0;

    private static final int accelSampleRatePerSec=20;
    private static final long gpsSampleRateMs=1000;
    private static final long logToFileRateMs=4000;
    private String loggerFilePath;
    private File loggerFile;

    private String dataPath;

    private double[][][] acc_values = new double[1000][1000][3]; //lat / lon / acc

    long lastUpdateAcceleration=0;


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            long now=System.currentTimeMillis();
            if(now-lastUpdateAcceleration > 1000/accelSampleRatePerSec) {
                if(Math.abs(sensorEvent.values[0])>0.1 || Math.abs(sensorEvent.values[1])>0.1 || Math.abs(sensorEvent.values[2])>0.1) {
                    int hashLat = makeHashFromCoord(position_data[0]);
                    int hashLon = makeHashFromCoord(position_data[1]);
                    double acc1 = acc_values[hashLat][hashLon][0];
                    double acc2 = acc_values[hashLat][hashLon][1];
                    double acc3 = acc_values[hashLat][hashLon][2];
                    if(acc1 != 0 || acc2 != 0 || acc3 != 0){ //jesli byl juz rekord dla danej pozycji usredniamy wartosci
                        acc1 = (acc1 + sensorEvent.values[0]) / 2;
                        acc2 = (acc2 + sensorEvent.values[1]) / 2;
                        acc3 = (acc3 + sensorEvent.values[2]) / 2;
                    }
                    else{ //jesli nie bylo rekordu wpisujemy nowe wartosci
                        acc1 = sensorEvent.values[0];
                        acc2 = sensorEvent.values[1];
                        acc3 = sensorEvent.values[2];
                    }
                    acc_values[hashLat][hashLon][0] = acc1;
                    acc_values[hashLat][hashLon][1] = acc2;
                    acc_values[hashLat]][hashLon][2] = acc3;
                    accelerometer_data += "A: " + String.format("%.2f",acc1)
                                        + " " + String.format("%.2f",acc2)
                                        + " " + String.format("%.2f",acc3) + "\n";
                    lastUpdateAcceleration=System.currentTimeMillis();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        position_data[0]=location.getLatitude();
        position_data[1]=location.getLongitude();
        speed_data=location.getSpeed();
    }

    public int makeHashFromCoord(double coord){

        int hash = Math.floor(coord * 100); //przesuwamy o 2 miejsca po przecinku, zaokraglamy.
        hash =  hash % 1000;
        return hash;
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

    private void loadFromFile(){
        dataPath = Environment.getExternalStorageDirectory().getPath()+"/"+MainActivity.WORKING_DIRECTORY_NAME+"/"+MainActivity.SUB_WORKING_DIRECTORIES[0]+"/data.txt"
        FileReader input = new FileReader(dataPath);
        BufferReader bufRead = new BufferReader(input);
        String line = null;
        double lat = 0;
        double lon = 0;
        int counter = 0;
        int latHash = 0;
        int lonHash = 0;
        while((line = bufRead.readLine()) != null){
            String[] tab = line.split(" ");
            if(tab[0].equals("P:")){
                if(counter > 0){
                    //usrednianie wartosci akcelerometru dla punktu
                    acc_values[latHash][lonHash][0] /= counter;
                    acc_values[latHash][lonHash][1] /= counter;
                    acc_values[latHash][lonHash][2] /= counter;
                }
                counter = 0;
                lat = Double.parseDouble(tab[1]);
                lon = Double.parseDouble(tab[2]);
                latHash = makeHashFromCoord(lat);
                lonHash = makeHashFromCoord(lon);
            }
            else if (tab[0].equals("A:")){
                //zliczanie wskazan akcelerometru dla punktu
                counter++;
                acc_values[latHash][lonHash][0] += Double.parseDouble(tab[1]);
                acc_values[latHash][lonHash][1] += Double.parseDouble(tab[2]);
                acc_values[latHash][lonHash][2] += Double.parseDouble(tab[3]);
            }
        }
    }

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            //If position was updated and file exist
            if(position_data[0] !=0 && position_data[1] !=0 && loggerFile.exists()){
                try {
                    FileOutputStream fo = new FileOutputStream(loggerFile,true);
                    //position
                    String data="P: "+ String.format("%.2f",position_data[0])+ " " +String.format("%.2f",position_data[1])+"\n";
                    fo.write(data.getBytes());
                    writeToLogs(data);
                    //speed
                    data="V: "+ speed_data+"\n";
                    fo.write(data.getBytes());
                    writeToLogs(data);
                    //accelerometr
                    fo.write(accelerometer_data.getBytes());
                    writeToLogs(accelerometer_data);

                    fo.close();
                    accelerometer_data="";
                } catch ( IOException ioe) {
                    writeToLogs("catch ( InterruptedException |IOException ioe)");
                    accelerometer_data="";
                    return;
                }
            }
            else{
                writeToLogs("Positon is equal to 0 or file was not created");
                accelerometer_data="";
            }

        }
    }

    private void writeToLogs(String message) {
        Log.d("TMC LOGGER SERVICE: ", message);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        writeToLogs("Called onCreate() method.");
        serviceIsRunning=true;
        timer = new Timer();
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        loadFromFile();
        createFile();
        setupAccelerometerSensor();
        setupLocationSensor();


    }

    private void setupAccelerometerSensor(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        /*
        On my phone: samsung galaxy S5 plus
        SENSOR_DELAY_FASTEST= ~4ms
        SENSOR_DELAY_GAME = ~20ms
        SENSOR_DELAY_NORMAL = ~180ms
        */
        int linearAccelerationRate;
        if(1000/accelSampleRatePerSec<20)
            linearAccelerationRate=SensorManager.SENSOR_DELAY_FASTEST;
        else if(1000/accelSampleRatePerSec<180)
            linearAccelerationRate=SensorManager.SENSOR_DELAY_GAME;
        else
            linearAccelerationRate=SensorManager.SENSOR_DELAY_NORMAL;

        mSensorManager.registerListener(this, mSensor , linearAccelerationRate);
    }

    private void setupLocationSensor(){
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(locationGPS==null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsSampleRateMs, 2, this);
        }
}

    private void createFile(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String FileName = sdf.format(new Date())+".txt";

        loggerFilePath= Environment.getExternalStorageDirectory().getPath()+"/"+MainActivity.WORKING_DIRECTORY_NAME+"/"+MainActivity.SUB_WORKING_DIRECTORIES[0]+"/"+FileName;
        loggerFile= new File(loggerFilePath);
        try {
            loggerFile.createNewFile();
        } catch (IOException e) {
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        writeToLogs("Called onStartCommand() methond");
        clearTimerSchedule();
        timerTask = new MyTimerTask();
        timer.scheduleAtFixedRate(timerTask, 5000,  logToFileRateMs);
        return super.onStartCommand(intent, flags, startId);
    }

    private void clearTimerSchedule() {
        if(timerTask != null) {
            timerTask.cancel();
            timer.purge();
        }
    }

    @Override
    public void onDestroy() {
        serviceIsRunning=false;
        clearTimerSchedule();
        mSensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
