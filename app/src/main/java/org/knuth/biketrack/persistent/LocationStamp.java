package org.knuth.biketrack.persistent;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * ORMlite mapper class for one single location-stamp.
 * @author Lukas Knuth
 * @version 1.0
 */
@DatabaseTable(tableName = "loc_stamp")
public class LocationStamp {

    @DatabaseField
    private double latitude;
    @DatabaseField
    private double longitude;
    @DatabaseField
    private double altitude;
    @DatabaseField
    private Date timestamp;
    @DatabaseField
    private float speed;
    @DatabaseField(foreign = true)
    private Tour tour;

    public LocationStamp(){}

    public LocationStamp(double latitude, double longitude, double altitude, Date timestamp, float speed, Tour tour){
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.timestamp = timestamp;
        this.speed = speed;
        this.tour = tour;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude(){
        return altitude;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public float getSpeed() {
        return speed;
    }

}
