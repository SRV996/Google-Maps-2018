package com.codingwithmitch.googlemaps2018.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

// THIS IS A NEWLY CREATED JAVA FILE.
// CREATING DATA STRUCTURE FOR USER LOCATIONS & HIS DETAILS IN FIRESTORE DATABASE.
public class UserLocation implements Parcelable {

    private GeoPoint geo_Point;
    private @ServerTimestamp Date timestamp;  // while u pass null to the timestamp when u insert this object into the database it will automatically insert a timestamp of the exact time it is created.
    private User user;

    // click alt+insert to insert below constructor.
    public UserLocation(GeoPoint geo_Point, Date timestamp, User user) {
        this.geo_Point = geo_Point;
        this.timestamp = timestamp;
        this.user = user;
    }
    public UserLocation(){
    }


    protected UserLocation(Parcel in) {
        user = in.readParcelable(User.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(user, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserLocation> CREATOR = new Creator<UserLocation>() {
        @Override
        public UserLocation createFromParcel(Parcel in) {
            return new UserLocation(in);
        }

        @Override
        public UserLocation[] newArray(int size) {
            return new UserLocation[size];
        }
    };

    // click alt+insert to insert below getter & setter method.
    public GeoPoint getGeo_Point() {
        return geo_Point;
    }

    public void setGeo_Point(GeoPoint geo_Point) {
        this.geo_Point = geo_Point;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // click alt+insert to insert below tostring method for log.
    @Override
    public String toString() {
        return "UserLocation{" +
                "geo_Point=" + geo_Point +
                ", timestamp=" + timestamp +
                ", user=" + user +
                '}';
    }

}
