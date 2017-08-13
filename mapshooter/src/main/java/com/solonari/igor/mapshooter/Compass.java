package com.solonari.igor.mapshooter;

import android.annotation.SuppressLint;
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
import android.os.Looper;
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
import java.util.Calendar;


public class Compass extends AppCompatActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener, Handler.Callback {

    private static final String TAG = "Compass";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private DrawSurfaceView mDrawView;
    private final float[] mRotationMatrix = new float[16];
    private final float[] mOrientation = new float[9];
    private float mHeading;
    private GeomagneticField mGeomagneticField;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    long UPDATE_INTERVAL = 5000;  /* 5 secs */
    long FASTEST_INTERVAL = 1000; /* 1 secs */
    protected Location location;
    protected static final int REQUEST_CAMERA_PERMISSION = 2;
    TCPService mService;
    //private static mHandler;
    public ArrayList<Point> props;
    final int SHIP_CODE = 3;
    ArrayList<String> line;
    String shipName;
    SharedPreferences settings;
    static final String PREF_FILE = "PREF_FILE";
    boolean mBound = false;
    ArrayList<String> missleArray;
    String shipID;
    Button fire;
    long shootTimer;
    static final String SHOOT_TIMER = "shootTimer";
    static final String TIME = "time";
    long time;
    long startTimer;
    private final static String MISSILE = "MISSILE";
    private Handler mHandler = new Handler(Looper.getMainLooper(), this);


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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                    fireTimer();
                    missleArray = new ArrayList<>();
                    missleArray.add(MISSILE);
                    missleArray.add(shipID);
                    missleArray.add(Float.toString(mHeading));
                    missleArray.add(Double.toString(location.getLatitude()));
                    missleArray.add(Double.toString(location.getLongitude()));
                    mService.sendMessage(missleArray);
                }
            });

        buildGoogleApiClient();

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


    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case SHIP_CODE:
                line = (ArrayList) msg.obj;
                props = new ArrayList<>();
                for (int i = 1; i < line.size(); i = i + 4) {
                    if (line.get(i).equals(shipName))
                        continue;
                    props.add(new Point(Double.parseDouble(line.get(i + 1)), Double.parseDouble(line.get(i + 2)), line.get(i)));
                }
                mDrawView.setPoints(props);
                break;
        }
        return true;
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
        settings = getSharedPreferences(PREF_FILE, 0);
        if(settings != null) {
            shipID = settings.getString("ID", "");
        }
        // Restore value of members from saved state
        startTimer = 10000;
        if (settings != null) {
            shootTimer = settings.getLong(SHOOT_TIMER, 0);
            if (shootTimer > 0) {
                time = settings.getLong(TIME, Calendar.getInstance().getTimeInMillis());
                long timeDif = Calendar.getInstance().getTimeInMillis() - time;
                if (timeDif/1000 < shootTimer) {
                    startTimer = shootTimer*1000 - timeDif;
                    fireTimer();
                }
            }
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
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putLong(SHOOT_TIMER, shootTimer);
        savedInstanceState.putLong(TIME, Calendar.getInstance().getTimeInMillis());
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "onSaveInstanceState");
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
        SharedPreferences settings = getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(SHOOT_TIMER, shootTimer);
        editor.putLong(TIME, Calendar.getInstance().getTimeInMillis());
        editor.apply();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
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
        settings = getSharedPreferences(PREF_FILE, 0);
        if(settings != null) {
            shipName = settings.getString("shipName", "");
        }
    }
    
    private void fireTimer() {
        fire.setBackgroundColor(Color.GRAY);
        fire.setEnabled(false);
        Toast.makeText(getApplicationContext(), getString(R.string.recharging), Toast.LENGTH_SHORT).show();
        new CountDownTimer(startTimer, 1000) {
            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {
                shootTimer = millisUntilFinished / 1000;
                fire.setText(Long.toString(shootTimer));
            }
            public void onFinish() {
                fire.setText(R.string.fire);
                fire.setEnabled(true);
                fire.setBackgroundColor(Color.parseColor("#e53935"));
                startTimer = 10000;
            }
        }.start();
    }
    
    private void sendShip() {
        if (location != null) {
            ArrayList<String> shipArray = new ArrayList<>();
            shipArray.add("SHIP_CODE");
            settings = getSharedPreferences(PREF_FILE, 0);
            String ID = settings.getString("ID", "");
            shipArray.add(ID);
            String shipName = settings.getString("shipName", "");
            shipArray.add(shipName);
            shipArray.add(Double.toString(location.getLatitude()));
            shipArray.add(Double.toString(location.getLongitude()));
            shipArray.add(Float.toString(location.getBearing()));
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
