package com.example.bearquake.bearquakes;

/**
 * Created by Otto on 3/5/2016.
 */
import android.location.Location;

public class LocationData {

    private static LocationData instance = null;

    private LocationData(){}

    private Location location;


    public Location getLocation(){
        return location;
    }

    public void setLocation(Location _location){
        location = _location;
    }

    public static LocationData getLocationData(){
        if(instance == null){
            instance = new LocationData();
        }
        return instance;
    }
}
