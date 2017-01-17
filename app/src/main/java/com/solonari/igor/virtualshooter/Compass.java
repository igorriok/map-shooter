package com.solonari.igor.virtualshooter;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;



public class Compass extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String TAG = "Compass";
    private static boolean DEBUG = false;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private DrawSurfaceView mDrawView;
    private final float[] mRotationMatrix = new float[16];
    private final float[] mOrientation = new float[9];
    private float mHeading;
    private GeomagneticField mGeomagneticField;
    private Location mLocation;
    private static final int ARM_DISPLACEMENT_DEGREES = 6;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 5000;  /* 5 secs */
    private long FASTEST_INTERVAL = 1000; /* 1 secs */
    protected Location location;
    protected static final int REQUEST_CAMERA_PERMISSION = 2;


    private SensorEventListener mListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {

            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);

            if (mDrawView != null) {
                float magneticHeading = (float) Math.toDegrees(mOrientation[0]);
                mHeading = MathUtils.mod(computeTrueNorth(magneticHeading), 360.0f)
                        - ARM_DISPLACEMENT_DEGREES;
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

    //@SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Set activity with no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Set full-screen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        setContentView(R.layout.activity_main);

        mDrawView = (DrawSurfaceView) findViewById(R.id.drawSurfaceView);

        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void onLocationChanged(Location location) {
        // do something here to save this new location
        this.location = location;
        Log.d(TAG, "Location Changed");
        mDrawView.setMyLocation(location.getLatitude(), location.getLongitude());
        mDrawView.invalidate();
        updateGeomagneticField();

        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        super.onStop();
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

}
