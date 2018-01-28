package com.google.firebase.codelab.friendlychat;

/**
 * Created by ammarasmro on 2018-01-28.
 */

public class LocationDataPoint {
    String latitude;
    String longitude;

    public LocationDataPoint(String latitude, String longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
}
