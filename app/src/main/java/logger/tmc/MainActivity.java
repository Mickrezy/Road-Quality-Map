package logger.tmc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RUNTIME_PERMISSION = 1;

    Button startLoggerButton;
    Button stopLoggerButton;

    public static final String WORKING_DIRECTORY_NAME ="tmcLogger";
    /*We dont know if in the future we will need more different files so this array simplify adding them to different directories*/
    public static final String SUB_WORKING_DIRECTORIES[]={"logs"/*,"logs2"*/};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupGuiElements();
        detectWorkingDirectory();
        requestPermissions();

        if(LoggerService.serviceIsRunning)
            startLoggerButton.setEnabled(false);
        else
            startLoggerButton.setEnabled(true);
    }

    void setupGuiElements() {
        startLoggerButton =(Button)findViewById(R.id.startLoggerButton);
        stopLoggerButton =(Button)findViewById(R.id.stopLoggerButton);

        startLoggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //here we run our logger service class
                startLoggerService();
                startLoggerButton.setEnabled(false);
            }
        });

        stopLoggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //here we close our logger service class
                stopLoggerService();
                startLoggerButton.setEnabled(true);
            }
        });
    }

    void detectWorkingDirectory(){
        //this might be a little misleading, because getExternalStorageDirectory returns path to main memory of our device not an SD card
        String path= Environment.getExternalStorageDirectory().getPath()+"/";

        if(!new File(path+WORKING_DIRECTORY_NAME).exists())
            new File(path+WORKING_DIRECTORY_NAME).mkdir();

        for (String s:SUB_WORKING_DIRECTORIES)
            if(! new File(path+WORKING_DIRECTORY_NAME +"/"+s).exists())
                new File(path+WORKING_DIRECTORY_NAME +"/"+s).mkdir();
    }

    void requestPermissions(){
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_RUNTIME_PERMISSION);
    }

    private void startLoggerService(){
        Intent serviceIntent = new Intent(this, LoggerService.class);
        startService(serviceIntent);
    }

    private void stopLoggerService(){
        Intent serviceIntent = new Intent(this, LoggerService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_RUNTIME_PERMISSION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! close the application!
                    Toast.makeText(MainActivity.this, "Permission denied to External storage and/or GPS position", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }
}
