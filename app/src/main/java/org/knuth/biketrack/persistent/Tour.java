package org.knuth.biketrack.persistent;

import android.os.Parcel;
import android.os.Parcelable;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * <p>A tour capsules multiple {@code LocationStamp}s, a date and a
 *  name to organize multiple routes (e.g. tours) in the database.</p>
 * <p>Instances of this class can be used as a parameter for the
 *  {@code TrackerService}, because it implements the {@code Parcelable}-
 *  interface.</p>
 *
 * @author Lukas Knuth
 * @version 1.0
 */
@DatabaseTable(tableName = "tours")
public class Tour implements Parcelable{

    public static final Tour UNSTORED_TOUR = new Tour();

    // TODO Add statistics-fields for average speed, top-speed, track-length, etc (maybe new table??)

    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField
    private Date date;
    @DatabaseField
    private String first_location;
    @DatabaseField
    private String second_location;
    @DatabaseField
    private int tour_type;
    @DatabaseField
    private String title;

    /** A tour, that goes from point A to point B */
    public static final int TOUR_TYPE_PATH = 0;
    /** A tour, that goes from point A back to point A */
    public static final int TOUR_TYPE_CIRCLE = 1;

    public Tour(){}

    public Tour(Date date){
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    /**
     * <p>Returns the name of the location where the first
     *  {@code LocationStamp} was taken.</p>
     * <p>This will normally be the City/Village name of where the tour was started.</p>
     * @return a string-representation of the first-location.
     */
    public String getFirstLocation(){
        return first_location;
    }

    /**
     * <p>Returns the name of <i>a</i> second location in the tour.</p>
     * <p>If the tour was of {@code TOUR_TYPE_PATH}, this will be the location where
     *  the last {@code LocationStamp} was taken.</p>
     * <p>Otherwise, in case it is a {@code TOUR_TYPE_CIRCLE}, another location
     *  <b>along</b> the track will be chosen.</p>
     * @return the string-representation of a first-location.
     */
    public String getSecondLocation(){
        return second_location;
    }

    public int getTourType(){
        return tour_type;
    }

    public void setFirstLocation(String first_location){
        this.first_location = first_location;
    }

    public void setSecondLocation(String second_location){
        this.second_location = second_location;
    }

    public void setTourType(int type){
        this.tour_type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString(){
        return this.title;
    }

    /*
        ------------ Parcelable stuff
     */
    public static final Parcelable.Creator<Tour> CREATOR = new Parcelable.Creator<Tour>(){

        @Override
        public Tour createFromParcel(Parcel parcel) {
            return new Tour(parcel);
        }

        @Override
        public Tour[] newArray(int i) {
            return new Tour[i];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeLong(date.getTime());
        parcel.writeString(first_location);
        parcel.writeString(second_location);
        parcel.writeInt(tour_type);
        parcel.writeString(title);
    }

    /**
     * Create a tour from a {@code Parcel}-object.
     */
    private Tour(Parcel parcel){
        this.id = parcel.readInt();
        this.date = new Date(parcel.readLong());
        this.first_location = parcel.readString();
        this.second_location = parcel.readString();
        this.tour_type = parcel.readInt();
        this.title = parcel.readString();
    }
}
