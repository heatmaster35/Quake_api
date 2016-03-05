package com.example.shobhit.findrestaurant;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.shobhit.findrestaurant.response.Example;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MainActivity extends AppCompatActivity {

    public static String LOG_TAG = "MyApplication";
    //creates the application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    //interface for the Query
    public interface Service {
        @GET("json")
        Call<ArrayList<Example>> registerQuake(@Query("id") int id,
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
    //calls get wheather when button is clicked
    public void onClick(View v) {
        getquakes();
    }

    public ArrayList<Example> Quakes = new ArrayList<Example>();

    //searches online for weather and pushes the info on to the Views
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
        Call<ArrayList<Example>> queryResponseCall =
                service.registerQuake(id, title, link, source, north,
                        west, lat, lng, depth, time, timestamp);


        //Calls retrofit asynchronously
        queryResponseCall.enqueue(new Callback<ArrayList<Example>>() {
            @Override
            public void onResponse(Response<ArrayList<Example>> response) {
                Log.i(LOG_TAG, "Code is: " + response.code());
                Log.i(LOG_TAG, "The response is: " + response.body().get(0).getResponse());
                Log.i(LOG_TAG, "Message is: " + response.body().get(0).getMessage());
                //checks for code 200
                //checks to see if information is there
                    if (response.body().get(0).getResponse() == 1 &&
                            response.body().get(0).getMessage().compareTo("OK") == 0) {
                        //if yes, then add all info to TextViews

                        int count = response.body().get(0).getCount();
                        Example temp;
                        for(int i = 0; i <= count; i++){
                            if((i==0)&&(!Quakes.isEmpty())){
                                Quakes.get(0).setCount(Quakes.get(0).getCount() +
                                        response.body().get(0).getCount());
                                continue;
                            }
                            temp = response.body().get(i);
                            Quakes.add(temp);
                        }


                        TextView itv = (TextView) findViewById(R.id.idView);
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
                        ttv3.setText("Timestamp : " + Quakes.get(1).getTimestamp());
                    }

                }

            @Override
            public void onFailure(Throwable t) {
                // Log error here since request failed
            }
        });

    }

}
