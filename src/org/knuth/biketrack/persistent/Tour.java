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

    // TODO Add mutable tour-length field and let user increase it.
    // TODO Add statistics-fields for average speed, top-speed, track-length, etc (maybe new table??)

    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField
    private String name; // TODO Remove the name from tours.
    @DatabaseField
    private Date date; // TODO Remove the data and use the one from the first location-stamp of this tour.

    public Tour(){}

    public Tour(String name, Date date){
        this.name = name;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString(){
        return name+" - "+date.toLocaleString();
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
        parcel.writeString(name);
        parcel.writeLong(date.getTime());
    }

    /**
     * Create a tour from a {@code Parcel}-object.
     */
    private Tour(Parcel parcel){
        this.id = parcel.readInt();
        this.name = parcel.readString();
        this.date = new Date(parcel.readLong());
    }
}
