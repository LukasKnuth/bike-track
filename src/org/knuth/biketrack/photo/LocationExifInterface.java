package org.knuth.biketrack.photo;

import android.media.ExifInterface;

import java.io.IOException;

/**
 * Description
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class LocationExifInterface extends ExifInterface {

    public LocationExifInterface(String filename) throws IOException {
        super(filename);
    }

    // TODO Method for putting in a simple LatLon or Location object.
}
