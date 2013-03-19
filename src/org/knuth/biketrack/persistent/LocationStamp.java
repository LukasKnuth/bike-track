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
    private int latitudeE6;
    @DatabaseField
    private int longitudeE6;
    @DatabaseField
    private Date timestamp;
    @DatabaseField
    private int speed; // TODO Store speed as the m/s float.
    @DatabaseField(foreign = true)
    private Tour tour;

    // TODO Add the altitude (use air pressure)
    // TODO The E6 notation is outdated. Make it use normal "double" values and drop all old data...

    public LocationStamp(){}

    public LocationStamp(int latitudeE6, int longitudeE6, Date timestamp, int speed, Tour tour){
        this.latitudeE6 = latitudeE6;
        this.longitudeE6 = longitudeE6;
        this.timestamp = timestamp;
        this.speed = speed;
        this.tour = tour;
    }

    /**
     * Get the latitude in the E6-format.
     * @see <a href="http://stackoverflow.com/questions/7049966">SO</a>
     */
    public int getLatitudeE6() {
        return latitudeE6;
    }

    /**
     * Get the latitude in the E6-format.
     * @see <a href="http://stackoverflow.com/questions/7049966">SO</a>
     */
    public int getLongitudeE6() {
        return longitudeE6;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getSpeed() {
        return speed;
    }

}
