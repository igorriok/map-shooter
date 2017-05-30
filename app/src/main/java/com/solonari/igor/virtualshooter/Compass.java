package com.solonari.igor.virtualshooter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;


public class Compass extends AppCompatActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    private static final String TAG = "Compass";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static boolean DEBUG = false;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private DrawSurfaceView mDrawView;
    private final float[] mRotationMatrix = new float[16];
    private final float[] mOrientation = new float[9];
    private float mHeading;
    private GeomagneticField mGeomagneticField;
    private static final int ARM_DISPLACEMENT_DEGREES = 6;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 5000;  /* 5 secs */
    private long FASTEST_INTERVAL = 1000; /* 1 secs */
    protected Location location;
    protected static final int REQUEST_CAMERA_PERMISSION = 2;
    TCPService mService;
    private Handler mHandler;
    public ArrayList<Point> props;
    private final int ship = 3;
    private ArrayList<String> line;
    String shipName;
    SharedPreferences settings;
    static final String Pref_file = "Pref_file";
    boolean mBound = false;
    ArrayList<String> missleArray;
    String shipID;
    Button fire;


    private SensorEventListener mListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {

            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);

            if (mDrawView != null) {
                float magneticHeading = (float) Math.toDegrees(mOrientation[0]);
                float axisY = (float) Math.toDegrees(mOrientation[1]);
                float axisZ = (float) Math.toDegrees(mOrientation[2]);
                mDrawView.setYZ(axisY, axisZ);
                mHeading = MathUtils.mod(computeTrueNorth(magneticHeading), 360.0f);
                mDrawView.setOffset(mHeading);
                mDrawView.invalidate();
            }
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private float computeTrueNorth(float heading) {
        if (mGeomagneticField != null) {
            return heading + mGeomagneticField.getDeclination();
        } else {
            return heading;
        }
    }

    private void updateGeomagneticField() {
        mGeomagneticField = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(),
                location.getTime());
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Set activity with no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        setContentView(R.layout.periscope);

        mDrawView = (DrawSurfaceView) findViewById(R.id.drawSurfaceView);

        Button map = (Button) findViewById(R.id.map);

        // Set a click listener on shoot button
        map.setOnClickListener(new View.OnClickListener() {
            // The code in this method will be executed when the shoot View is clicked on.
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        fire = (Button) findViewById(R.id.fire);
        fire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    fire.setBackgroundColor(Color.GRAY);
                    fire.setEnabled(false);
                    fireTimer();
                    missleArray = new ArrayList<>();
                    missleArray.add("missile");
                    missleArray.add(shipID);
                    missleArray.add(Float.toString(mHeading));
                    missleArray.add(Double.toString(location.getLatitude()));
                    missleArray.add(Double.toString(location.getLongitude()));
                    mService.sendMessage(missleArray);
                    Toast.makeText(getApplicationContext(), "Recharging", Toast.LENGTH_SHORT).show();
                }
            });

        buildGoogleApiClient();

        setHandler();

        setShipName();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    private void setHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case ship:
                        line = (ArrayList) msg.obj;
                        props = new ArrayList<>();
                        for (int i = 1; i < line.size(); i = i + 3) {
                            if (line.get(i).equals(shipName))
                                continue;
                            props.add(new Point(Double.parseDouble(line.get(i + 1)), Double.parseDouble(line.get(i + 2)), line.get(i)));
                        }
                        mDrawView.setPoints(props);
                        break;
                }
            }
        };
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, TCPService.class), mConnection, Context.BIND_AUTO_CREATE);
        settings = getSharedPreferences(Pref_file, 0);
        if(settings != null) {
            shipID = settings.getString("ID", "");
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        startLocationUpdates();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    protected void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void onLocationChanged(Location location) {
        // do something here to save this new location
        this.location = location;
        Log.d(TAG, "Location Changed");
        mDrawView.setMyLocation(location.getLatitude(), location.getLongitude());
        mDrawView.invalidate();
        updateGeomagneticField();

        /*String msg = "Updated Location: " +
        *        Double.toString(location.getLatitude()) + "," +
        *        Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mListener, mSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        mSensorManager.unregisterListener(mListener);
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
    
    private void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access camera is missing.
            PermissionUtils.requestPermission(this, REQUEST_CAMERA_PERMISSION,
                    Manifest.permission.CAMERA, true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CAMERA_PERMISSION) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.CAMERA)) {
            // Enable the my location layer if the permission has been granted.
            return;
        } else {
            // Display the missing permission error dialog when the fragments resume.
            
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            TCPService.LocalBinder binder = (TCPService.LocalBinder) service;
            mService = binder.getService();
            mService.setHandler(getHandler());
            mBound = true;
            sendShip();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private Handler getHandler(){
        return mHandler;
    }

    private void setShipName() {
        settings = getSharedPreferences(Pref_file, 0);
        if(settings != null) {
            shipName = settings.getString("shipName", "");
        }
    }
    
    private void fireTimer() {
        new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {  
                fire.setText(Long.toString(millisUntilFinished / 1000));
            }
            public void onFinish() {
                fire.setText(R.string.shoot);
                    fire.setEnabled(true);
                    fire.setBackgroundColor(Color.parseColor("#e53935"));
            }
        }.start();
    }
    
    private void sendShip() {
        if (location != null) {
            ArrayList<String> shipArray = new ArrayList<>();
            shipArray.add("ship");
            settings = getSharedPreferences(Pref_file, 0);
            String ID = settings.getString("ID", "");
            shipArray.add(ID);
            String shipName = settings.getString("shipName", "");
            shipArray.add(shipName);
            shipArray.add(Double.toString(location.getLatitude()));
            shipArray.add(Double.toString(location.getLongitude()));
            Log.d(TAG, shipArray.toString());

            if (!shipName.equals("") && !ID.equals("")) {
                try {
                    mService.sendMessage(shipArray);
                } catch (Exception e) {
                    Log.e(TAG, "cant send location", e);
                }
            }
        }
    }
}
