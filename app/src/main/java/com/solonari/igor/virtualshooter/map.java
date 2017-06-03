package com.solonari.igor.virtualshooter;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Objects;


public class map extends AppCompatActivity implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener,
        Handler.Callback, View.OnClickListener, ShipNameFragment.NoticeDialogListener {

    private GoogleMap mMap;
    protected GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 5000;  /* 5 secs */
    private long FASTEST_INTERVAL = 1000; /* 1 secs */
    private static String TAG = "Map";
    private Handler mHandler = new Handler(Looper.getMainLooper(), this);
    final String mTag = "Handler";
    private String idToken;
    protected static final String Pref_file = "Pref_file";
    protected SharedPreferences settings;
    HandlerThread shipThread;
    LatLng latLng;
    private static final int points = 2;
    private static final int ship = 3;
    private static final int missleArray = 5;
    private static final int exp = 6;
    private static final int startCom = 1;
    ArrayList<String> shipList;
    ArrayList<Marker> shipMarkers;
    ArrayList<Marker> missleMarkers;
    ArrayList<Marker> expMarkers;
    AppCompatActivity thisActivity = this;
    Intent shootIntent;
    TCPService mService;
    ArrayList<String> missleList;
    ArrayList<String> expList;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;
    Location location;
    protected final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    protected static final int CAMERA_PERMISSION_REQUEST_CODE = 2;
    private GoogleApiClient client;
    boolean mBound = false;
    View dView;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set activity with no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.content_map);

        findViewById(R.id.shipName).setOnClickListener(this);
        findViewById(R.id.signOut).setOnClickListener(this);
        findViewById(R.id.exit).setOnClickListener(this);
        findViewById(R.id.shootButton).setOnClickListener(this);
        findViewById(R.id.myLocationButton).setOnClickListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);

        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        SharedPreferences settings = getSharedPreferences(Pref_file, 0);
        String id = settings.getString("Token", "");
        if (!id.equals("")) {
            idToken = id;
        } else {
            goToSignIn();
        }

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

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.shipName:
                showNoticeDialog();
                break;
            case R.id.signOut:
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                SharedPreferences settings = getSharedPreferences(Pref_file, 0);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("Token", "");
                                editor.apply();
                                Intent signInIntent = new Intent(map.this, SignInActivity.class);
                                startActivity(signInIntent);
                            }
                        });
                break;
            case R.id.exit:
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.shootButton:
                shootIntent = new Intent(map.this, Compass.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startActivity(shootIntent);
                    } else {
                        PermissionUtils.requestPermission(thisActivity, CAMERA_PERMISSION_REQUEST_CODE,
                                Manifest.permission.CAMERA, false);
                    }
                } else {
                    startActivity(shootIntent);
                }
                break;
            case R.id.myLocationButton:
                if (mMap != null && latLng != null) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
                    mMap.animateCamera(cameraUpdate);
                }
                break;

        }
    }

    public void showNoticeDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment ShipDialog = new ShipNameFragment();
        ShipDialog.show(getFragmentManager(), "shipNameFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String shipName) {
        // User touched the dialog's positive button
        if(!shipName.equals("")) {
            SharedPreferences settings = getSharedPreferences(Pref_file, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("shipName", shipName);
            editor.apply();
            displayShipName();
            Log.d(TAG, "New Ship name: " + shipName);
        } else {
            showNoticeDialog();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog, String shipName) {
        // User touched the dialog's negative button
        if(Objects.equals(shipName, "")){
            showNoticeDialog();
        }
    }

    private void goToSignIn() {
        Intent signInIntent = new Intent(map.this, SignInActivity.class);
        startActivity(signInIntent);
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case points:
                String message = (String) msg.obj;
                TextView Rating = (TextView) findViewById(R.id.rating);
                Rating.setText(message);
                Log.d("Display Points", message);
                break;
            case ship:
                String shipName = settings.getString("shipName", "");
                shipList = (ArrayList) msg.obj;
                if (shipMarkers != null) {
                    for (Marker markerName : shipMarkers) {
                        markerName.remove();
                    }
                }
                shipMarkers = new ArrayList<>();
                if (mMap != null) {
                    for(int i = 1; i < shipList.size(); i = i + 3) {
                        if (shipList.get(i).equals(shipName)) {
                            shipMarkers.add(mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(shipList.get(i+1)), Double.parseDouble(shipList.get(i+2))))
                                    .title(shipList.get(i))
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fighter))
                                    .anchor(0.5f, 0.5f)));
                            continue;
                        }
                        shipMarkers.add(mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(shipList.get(i+1)), Double.parseDouble(shipList.get(i+2))))
                                .title(shipList.get(i))
                                .flat(true)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.enemy))
                                .anchor(0.5f, 0.5f)));
                    }
                }
                Log.d(TAG, "Adding shipMarkers on map");
                break;
            case missleArray:
                missleList = (ArrayList) msg.obj;
                if (missleMarkers != null) {
                    for (Marker missleMarker : missleMarkers) {
                        missleMarker.remove();
                    }
                }
                missleMarkers = new ArrayList<>();
                if (mMap != null) {
                    for (int i = 1; i < missleList.size(); i = i + 3) {
                        missleMarkers.add(mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(missleList.get(i+1)), Double.parseDouble(missleList.get(i+2))))
                                .rotation(Float.parseFloat(missleList.get(i)))
                                .flat(true)
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.missile))
                                .anchor(0.5f, 0.5f)));
                    }
                }
                break;
            case exp:
                expList = (ArrayList) msg.obj;
                if (expMarkers != null) {
                    for (Marker expMarker : expMarkers) {
                        expMarker.remove();
                    }
                }
                expMarkers = new ArrayList<>();
                if (mMap != null) {
                    for (int i = 1; i < expList.size(); i = i + 2) {
                        expMarkers.add(mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(expList.get(1)), Double.parseDouble(expList.get(i+1))))
                                .flat(true)
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.explosion))
                                .anchor(0.5f, 0.5f)));
                    }
                }
                break;
            case startCom:
                sendShip();
            default:
                break;
        }
        return true;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            TCPService.LocalBinder binder = (TCPService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setHandler(getHandler());
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private Handler getHandler(){
        return mHandler;
    }

    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        if (mMap != null) {
            // Now that map has loaded, let's get our location
            enableMyLocation();
            mMap.getUiSettings().setMapToolbarEnabled(false);
        }
    }

    protected void enableMyLocation() {
        // Access to the location has been granted to the app.
        //mMap.setMyLocationEnabled(true);
        if (latLng != null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            mMap.animateCamera(cameraUpdate);
        } else {
            Toast.makeText(this, "Current location was not found, please enable GPS", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    startLocationUpdates();
                }
            }
            break;
            /*case CAMERA_PERMISSION_REQUEST_CODE:
                if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.CAMERA)) {
                    startActivity(shootIntent);
                } else {
                    PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getSupportFragmentManager(), "dialog");
                }
                break;*/
            default:
                break;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }


    /*
     * Called when the Activity becomes visible.
    */
    @Override
    protected void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
        // Bind to the service
        if (mService == null && idToken != null) {
            bindService(new Intent(this, TCPService.class), mConnection, Context.BIND_AUTO_CREATE);
        } else {
            Log.d(TAG, "no idToken!!!");
        }
        Log.d(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        settings = getSharedPreferences(Pref_file, 0);
        if(settings != null) {
            String ship = settings.getString("shipName", "");
            if(!ship.equals("")) {
                displayShipName();
            } else {
                showNoticeDialog();
            }
        } else {
            showNoticeDialog();
        }
        if (mService != null) {
            mService.setHandler(getHandler());
        }
        Log.d(TAG, "onResume");
    }

    protected void displayShipName(){
        SharedPreferences settings = getSharedPreferences(Pref_file, 0);
        ((TextView) findViewById(R.id.ship)).setText(settings.getString("shipName", ""));
    }

    /*
    * Handle results returned to the FragmentActivity by Google Play services
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                //If the result code is Activity.RESULT_OK, try to connect again
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mGoogleApiClient.connect();
                        break;
                }
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(location != null) {
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
                enableMyLocation();
            }
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings");
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(map.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
        if (mLocationPermissionGranted) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    public void onLocationChanged(Location location) {
        //Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
    }


    /*
     * Called by Location Services if the connection to the location client
     * drops because of an error.
     */
    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Called by Location Services if the attempt to Location Services fails.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Sorry. Location services not available", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("map Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }


    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroyed");
        if(shipThread != null) {
            shipThread.quit();
            Log.d(TAG, "shipThread quit");
        }
        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        if(mBound) {
            unbindService(mConnection);
        }
    }


    public void sendShip() {

        shipThread = new HandlerThread("ShipThread");
        shipThread.start();
        Looper looper = shipThread.getLooper();
        final Handler shipHandler = new Handler(looper);

        //send token to get ID
        shipHandler.post(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> idArray = new ArrayList<>();
                idArray.add("id");
                idArray.add(idToken);
                try {
                    mService.sendMessage(idArray);
                } catch (Exception e) {
                    Log.e(TAG, "cant send message", e);
                }
            }
        });

        //send ship location
        shipHandler.post(new Runnable() {
            @Override
            public void run() {
                if (latLng != null) {
                    ArrayList<String> shipArray = new ArrayList<>();
                    shipArray.add("ship");
                    settings = getSharedPreferences(Pref_file, 0);
                    String ID = settings.getString("ID", "");
                    shipArray.add(ID);
                    String shipName = settings.getString("shipName", "");
                    shipArray.add(shipName);
                    shipArray.add(Double.toString(latLng.latitude));
                    shipArray.add(Double.toString(latLng.longitude));
                    Log.d(TAG, shipArray.toString());

                    if (!shipName.equals("") && !ID.equals("")) {
                        try {
                            mService.sendMessage(shipArray);
                        } catch (Exception e) {
                            Log.e(TAG, "cant send location", e);
                        }
                    }
                }
                shipHandler.postDelayed(this, 5000);
            }
        });

        //request missiles
        shipHandler.post(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> missileArray = new ArrayList<>();
                missileArray.add("missileArray");
                try {
                    mService.sendMessage(missileArray);
                } catch (Exception e) {
                    Log.e(TAG, "cant send location", e);
                }
                shipHandler.postDelayed(this, 1000);
            }
        });
    }
}
