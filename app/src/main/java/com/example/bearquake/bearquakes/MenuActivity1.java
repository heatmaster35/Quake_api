package com.example.bearquake.bearquakes;

/**
 * Created by Otto on 3/5/2016.
 */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class MenuActivity1 extends AppCompatActivity {

    private GoogleApiClient mGoogleApiClient;
    private boolean dead;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private LocationData locationData = LocationData.getLocationData();
    public SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu1);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        dead = settings.getBoolean("dead", false);
        //This should check if dead, if true then dont let them open the app
        if (dead == true) {
            SharedPreferences.Editor e = settings.edit();
            e.putBoolean("dead", dead);
            e.commit();
        }


    }

    public void onResume(){
        //check if user already gave permission to use location
        Button startButton = (Button) findViewById(R.id.startBears);
        Button locationButton = (Button)findViewById(R.id.locationbutton);
        TextView buh = (TextView)findViewById(R.id.needLocationText);
        Boolean locationAllowed = checkLocationAllowed();

        if (locationAllowed) {
            requestLocationUpdate();
            buh.setEnabled(false);
            locationButton.setEnabled(false);
        }
        else{
            startButton.setEnabled(false);
            locationButton.setEnabled(true);
        }
        render();
        super.onResume();
    }

    //Pulled from Shobhits code in HW3
    private boolean checkLocationAllowed(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        return settings.getBoolean("location_allowed", false);
    }

    private void setLocationAllowed(boolean allowed){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("location_allowed", allowed);
        editor.commit();
    }

    public void toggleLocation(View v) {
        Button startButton = (Button) findViewById(R.id.startBears);
        Boolean locationAllowed = checkLocationAllowed();
        TextView buh = (TextView)findViewById(R.id.needLocationText);

        if(!locationAllowed){
            //disable it
            buh.setEnabled(false);
            requestLocationUpdate();
            setLocationAllowed(true);//persist this setting
            startButton.setEnabled(true);//now that we cannot use location, we should disable search facility

        } else {
            //enable it

            //setLocationAllowed(true);//persist this setting
        }

        //Set the button text between "Enable Location" or "Disable Location"
        render();
    }


    private void requestLocationUpdate(){
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 35000, 10, locationListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 35000, 10, locationListener);


            }
            else {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                } else {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION);


                }
            }
        } else{

            //prompt user to enable location
            Intent gpsOptionsIntent = new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(gpsOptionsIntent);
        }
    }

    private void render(){
        Button startButton = (Button) findViewById(R.id.startBears);
        Boolean locationAllowed = checkLocationAllowed();
        Button button = (Button) findViewById(R.id.locationbutton);

        if(locationAllowed) {
            startButton.setEnabled(true);
            //button.setText("Disable Location");
        }
        else {
            button.setText("Enable Location");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {

                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 35000, 10, locationListener);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 35000, 10, locationListener);

                        //Log.i(LOG_TAG, "requesting location update");
                    } else{
                        throw new RuntimeException("permission not granted still callback fired");
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onPause(){
        if(checkLocationAllowed())
            removeLocationUpdate();//if the user has allowed location sharing we must disable location updates now
        super.onPause();
    }

    private void removeLocationUpdate() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {

                locationManager.removeUpdates(locationListener);

            }
        }
    }
    //Copied from Prof. Luca's class coe to get the most recent precise location
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Location lastLocation = locationData.getLocation();

            // Do something with the location you receive.
            double newAccuracy = location.getAccuracy();

            long newTime = location.getTime();
            // Is this better than what we had?  We allow a bit of degradation in time.
            boolean isBetter = ((lastLocation == null) ||
                    newAccuracy < lastLocation.getAccuracy() + (newTime - lastLocation.getTime()));
            if (isBetter) {
                // We replace the old estimate by this one.
                locationData.setLocation(location);

                //Now we have the location.
               // Button searchButton = (Button) findViewById(R.id.searchButton);
                //Log.e("Error", "String is: " + locationData.getLocation().getAccuracy());
                //if(checkLocationAllowed() && location.getAccuracy() < 50 && locationData.getLocation() != null)
                   // searchButton.setEnabled(true);//We must enable search button
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };
    public void beginTheBears(View v){
        Intent intent = new Intent (this, BearTracker.class);
        finish();
        startActivity(intent);
    }

}
