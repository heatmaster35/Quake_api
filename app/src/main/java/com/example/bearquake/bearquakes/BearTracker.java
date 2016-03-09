package com.example.bearquake.bearquakes;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bearquake.bearquakes.response.quakeResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;


public class BearTracker extends AppCompatActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static String LOG_TAG = "Quakes";
    //Need all this stuff to functionally begin creating api calls and getting responses
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location lastLocation;
    public ArrayList<quakeResponse> Quakes = new ArrayList<quakeResponse>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bear_tracker);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //Build the api client
        getquakes();
        buildGoogleApiClient();
        //Begin location requests
        createLocationRequest();

    }

    //Controls the camera movement of google maps, when the map loads, it will move to your current location.
    private void initCamera(Location location) {
        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), null);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //Since target sdk is 23, we need to ask for permissions every god damn time we want to ask for location, so this set of
        //code will be around frequently
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    //Build the google maps API
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

    }

    //What happens when the map is fully loaded
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    //On app start, connect to the maps api
    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    //On stop, disconnect from it
    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    //Self explanatory, getting all the location request information regarding frequency, and
    //intervals at which locations will be procured and updated
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //What happens following the response of a permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createLocationRequest();
            } else {
                // Permission was denied. Display an error message.
            }
        }
    }

    //What happens when we are connected to the API, basically we start location updates and
    // init camera
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (lastLocation != null) {
                startLocationUpdates();
                initCamera(lastLocation);
                Log.e("We called: ", "initCamera");
            }

        }
    }

    //Begin grabbing location updates
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    //Here is where we would try to set a default home location, we would ask them for a home
    //lat and lng before they start and if the app fails to get their location, it should default
    //to whatever they set.
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Create a default location if the Google API Client fails. Placing location at Googleplex
        Log.e("issues", "we failed");
        lastLocation = new Location("");
        lastLocation.setLatitude(167.422535);
        lastLocation.setLongitude(-122.084804);
        initCamera(lastLocation);
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
    }

    public void getquakes() {
        // Let's mock acquiring the weather conditions.
        int id = 0;
        String title = "hi there";
        String link = "";
        String source = "";
        double north = 0.00;
        double west = 0.00;
        double lat = 0.00;
        double lng = 0.00;
        int depth = 0;
        double time = 0.00;
        int timestamp = 0;

        //logs into the web
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        //Goes to link and creates the retrofiter
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.kuakes.com/")
                .addConverterFactory(GsonConverterFactory.create())    //parse Gson string
                .client(httpClient)    //add logging
                .build();

        //Synchs retrofit with service
        Service service = retrofit.create(Service.class);

        //Calls from query to find items
        Call<ArrayList<quakeResponse>> queryResponseCall =
                service.registerQuake(id, title, link, source, north,
                        west, lat, lng, depth, time, timestamp);


        //Calls retrofit asynchronously
        queryResponseCall.enqueue(new Callback<ArrayList<quakeResponse>>() {
            @Override
            public void onResponse(Response<ArrayList<quakeResponse>> response) {
                Log.i(LOG_TAG, "Code is: " + response.code());
                Log.i(LOG_TAG, "The response is: " + response.body().get(0).getResponse());
                Log.i(LOG_TAG, "Message is: " + response.body().get(0).getMessage());
                //checks for code 200
                //checks to see if information is there
                if (response.body().get(0).getResponse() == 1 &&
                        response.body().get(0).getMessage().compareTo("OK") == 0) {
                    //if yes, then add all info to TextViews

                    int count = response.body().get(0).getCount();
                    quakeResponse temp;
                    for (int i = 0; i <= count; i++) {
                        if ((i == 0) && (!Quakes.isEmpty())) {
                            Quakes.get(0).setCount(Quakes.get(0).getCount() +
                                    response.body().get(0).getCount());
                            continue;
                        }
                        temp = response.body().get(i);
                        Quakes.add(temp);
                    }


                    /*TextView itv = (TextView) findViewById(R.id.idView);
                    TextView ttv1 = (TextView) findViewById(R.id.titleView);
                    TextView ltv = (TextView) findViewById(R.id.linkView);
                    TextView stv = (TextView) findViewById(R.id.sourceView);
                    TextView ntv = (TextView) findViewById(R.id.northView);
                    TextView wtv = (TextView) findViewById(R.id.westView);
                    TextView latv = (TextView) findViewById(R.id.latView);
                    TextView lntv = (TextView) findViewById(R.id.lngView);
                    TextView dtv = (TextView) findViewById(R.id.depthView);
                    TextView mtv = (TextView) findViewById(R.id.magView);
                    TextView ttv2 = (TextView) findViewById(R.id.timeView);
                    TextView ttv3 = (TextView) findViewById(R.id.timestampView);
                    Log.i(LOG_TAG, "Code is: " + response.code());
                    Log.i(LOG_TAG, "The response is: " + response.body().get(0).getResponse());

                    itv.setText("Id : " + Quakes.get(1).getId());
                    ttv1.setText("Title : " + Quakes.get(1).getTitle());
                    ltv.setText("Link : " + Quakes.get(1).getLink());
                    stv.setText("Source : " + Quakes.get(1).getSource());
                    ntv.setText("North : " + Quakes.get(1).getNorth());
                    wtv.setText("West : " + Quakes.get(1).getWest());
                    latv.setText("Lat : " + Quakes.get(1).getLat());
                    lntv.setText("Lng : " + Quakes.get(1).getLng());
                    dtv.setText("Depth : " + Quakes.get(1).getDepth());
                    mtv.setText("Mag : " + Quakes.get(1).getMag());
                    ttv2.setText("Time : " + Quakes.get(1).getTime());
                    ttv3.setText("Timestamp : " + Quakes.get(1).getTimestamp());*/
                }

            }

            @Override
            public void onFailure(Throwable t) {
                // Log error here since request failed
            }
        });

    }

    //interface for the Query
    public interface Service {
        @GET("json")
        Call<ArrayList<quakeResponse>> registerQuake(@Query("id") int id,
                                                     @Query("title") String title,
                                                     @Query("link") String link,
                                                     @Query("source") String source,
                                                     @Query("north") double north,
                                                     @Query("west") double west,
                                                     @Query("lat") double lat,
                                                     @Query("lng") double lng,
                                                     @Query("depth") int depth,
                                                     @Query("mag") double time,
                                                     @Query("timestamp") int timestamp);
    }

    public ArrayList<Double[]> PinPoints = new ArrayList<Double[]>();

    //will get the next 10 quake locations and display them on the map
    public void getBears(View v) {
        //Double [] temp = new Double[4];
        Log.i(LOG_TAG, "Quakes message: " + Quakes.get(0).getMessage());
        int counter = 0;
        while (counter < 10 && Quakes.get(0).getCount() >= 2) {
            Log.i(LOG_TAG, "Quakes message: " + Quakes.get(counter).getLat() + ", " + Quakes.get(counter).getLng());
            Double lat = Quakes.get(counter + 1).getLat();
            Double lng = Quakes.get(counter+ 1).getLng();
            Double mag = Quakes.get(counter + 1).getMag();
            Double dep = Double.valueOf(Quakes.get(counter + 1).getDepth());
            //code provided by https://developers.google.com/maps/documentation/android-api/marker#introduction
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .title("Bear Level: " + Quakes.get(counter+1).getMag()));

            Double[] temp = {lat, lng, mag, dep};
            PinPoints.add(temp);
            //Quakes.remove(1);
            counter++;
        }
    }
}