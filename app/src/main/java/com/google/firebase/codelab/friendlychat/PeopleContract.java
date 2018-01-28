package com.google.firebase.codelab.friendlychat;

import com.google.android.gms.location.LocationCallback;

import java.util.HashMap;

/**
 * Created by ammarasmro on 2018-01-27.
 */

public class PeopleContract {


    String id;
    String latitude;
    String longitude;
    String person;
//    HashMap access;

    public PeopleContract(){}

    public PeopleContract(String latitude, String longitude, String person){
        this.latitude = latitude;
        this.longitude = longitude;
        this.person = person;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

//    public HashMap getAccess(){ return access; }
//
//    public void setAccess(HashMap access){ this.access = access; }
}
