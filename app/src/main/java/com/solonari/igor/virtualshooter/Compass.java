package com.solonari.igor.virtualshooter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


public class Compass extends Activity {

    private static final String TAG = "Compass";
    private static boolean DEBUG = false;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private DrawSurfaceView mDrawView;
    LocationManager locMgr;
    private final float[] mRotationMatrix = new float[16];
    private final float[] mOrientation = new float[9];
    private float mHeading;
    private GeomagneticField mGeomagneticField;
    private Location mLocation;
    private static final int ARM_DISPLACEMENT_DEGREES = 6;


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
        mGeomagneticField = new GeomagneticField((float) mLocation.getLatitude(),
                (float) mLocation.getLongitude(), (float) mLocation.getAltitude(),
                mLocation.getTime());
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        setContentView(R.layout.activity_main);

        mDrawView = (DrawSurfaceView) findViewById(R.id.drawSurfaceView);

        locMgr = (LocationManager) this.getSystemService(LOCATION_SERVICE); // <2>
        LocationProvider high = locMgr.getProvider(locMgr.getBestProvider(
                LocationUtils.createFineCriteria(), true));

        // using high accuracy provider... to listen for updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locMgr.requestLocationUpdates(high.getName(), 0, 0f, new LocationListener() {
            public void onLocationChanged(Location location) {
                // do something here to save this new location
                Log.d(TAG, "Location Changed");
                mDrawView.setMyLocation(location.getLatitude(), location.getLongitude());
                mDrawView.invalidate();
                mLocation = location;
                updateGeomagneticField();

            }

            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            public void onProviderEnabled(String s) {
                // try switching to a different provider
            }

            public void onProviderDisabled(String s) {
                // try switching to a different provider
            }
        });

    }

    @Override
    protected void onResume() {
        if (DEBUG)
            Log.d(TAG, "onResume");
        super.onResume();

        mSensorManager.registerListener(mListener, mSensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop() {
        if (DEBUG)
            Log.d(TAG, "onStop");
        mSensorManager.unregisterListener(mListener);
        super.onStop();
    }
}
