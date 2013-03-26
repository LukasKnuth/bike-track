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
    private Date timestamp;
    @DatabaseField
    private int speed; // TODO Store speed as the m/s float.
    @DatabaseField(foreign = true)
    private Tour tour;

    public LocationStamp(){}

    public LocationStamp(double latitude, double longitude, Date timestamp, int speed, Tour tour){
        this.latitude = latitude;
        this.longitude = longitude;
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

    public Date getTimestamp() {
        return timestamp;
    }

    public int getSpeed() {
        return speed;
    }

}
