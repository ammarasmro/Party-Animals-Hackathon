package com.google.firebase.codelab.friendlychat;

/**
 * Created by ammarasmro on 2018-01-28.
 */

public class LocationContract {

    String title;
    String latitude;
    String longitude;
    String numberOfPeople;

    public LocationContract(){}

    public LocationContract(String title, String latitude, String longitude, String numberOfPeople){
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
        this.numberOfPeople = numberOfPeople;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getNumberOfPeople() {
        return numberOfPeople;
    }

    public void setNumberOfPeople(String numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
    }

}
