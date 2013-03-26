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
    // TODO Instead of name, do lookup: https://developers.google.com/maps/documentation/geocoding/#ReverseGeocoding

    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField
    private Date date;

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

    @Override
    public String toString(){
        /*
            When changing this implementation, check usages.
            It is used in many places that can't handle line-breaks. Most of those
            can be removed anyways...
        */
        return "Tour #"+id;
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
    }

    /**
     * Create a tour from a {@code Parcel}-object.
     */
    private Tour(Parcel parcel){
        this.id = parcel.readInt();
        this.date = new Date(parcel.readLong());
    }
}
