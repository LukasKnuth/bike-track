package org.knuth.biketrack.photo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.ExifInterface;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import org.knuth.biketrack.Main;
import org.knuth.biketrack.persistent.Tour;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>This is a Utility class, which offers static methods to be performed on a taken Photo.</p>
 * <p>This is necessary, because the Camera API sucks and every device-vendor implements the
 *  {@code MediaStore.ACTION_IMAGE_CAPTURE}-intent like he pleases.</p>
 *
 * <p>Examples include:</p>
 * <ul>
 *     <li>http://stackoverflow.com/q/6390163/717341</li>
 *     <li>http://stackoverflow.com/q/7109457/717341</li>
 * </ul>
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class PhotoUtils {

    private static final String[] COLS = {
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DISPLAY_NAME,
            BaseColumns._ID
    };
    private static final File EXTERNAL_TOP_DIR = Environment.getExternalStorageDirectory();
    private static final String BIKETRACK_DIR = ".biketrack/";
    private static final String IMAGE_SUBDIR = "/pic/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("H-m-s");
    private static final FileFilter IMAGE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return (file.isFile() && file.getName().endsWith(".jpg"));
        }
    };

    /**
     * Get the path to the image last added to the Gallery.
     * @param resolver to query the gallery.
     * @return the path to the last added file.
     */
    public static File getLastImagePath(ContentResolver resolver){
        Cursor c = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, COLS,
                null, null,
                MediaStore.Images.ImageColumns.DATE_ADDED+" DESC LIMIT 1");
        if (c != null && c.getCount() > 0){
            c.moveToFirst();
            // Get the Data:
            Log.v(Main.LOG_TAG, "The value from DATA is: "+c.getString(0));
            return new File(c.getString(0));
        } else {
            throw new IllegalStateException("The Gallery is empty.");
        }
    }

    /**
     * <p>Generates a unique {@code File} to store a new picture for the given {@code Tour}</p>
     * @param tour the tour to which the picture belongs.
     * @return the generated, unique file.
     */
    public static File generateNewTourImage(Tour tour){
        File dir = new File(EXTERNAL_TOP_DIR,BIKETRACK_DIR+tour.getId()+IMAGE_SUBDIR);
        dir.mkdirs();
        return new File(dir, DATE_FORMAT.format(new Date())+".jpg");
    }

    /**
     * <p>Get all images associated with the given tour.</p>
     * @param tour the tour to get the images from.
     * @return a list of image-files from the given tour.
     */
    public static File[] getTourImages(Tour tour){
        File files[] = new File(EXTERNAL_TOP_DIR,BIKETRACK_DIR+tour.getId()+IMAGE_SUBDIR).listFiles(IMAGE_FILTER);
        if (files == null){
            return new File[0];
        } else return files;
    }

    /**
     * <p>Updates the file-path of the given gallery-image to the new path.</p>
     * @param resolver to write to the gallery.
     * @param old_path the path to the <b>old</b> gallery-image.
     * @param new_path the <b>new</b> path to the image.
     */
    public static void updateGalleryFile(ContentResolver resolver, File old_path, File new_path){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.ImageColumns.DATA, new_path.getAbsolutePath());
        // Execute update:
        int updated = resolver.update(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values,
                MediaStore.Images.ImageColumns.DATA+" = ?", new String[]{
                    old_path.getAbsolutePath()
        });
        Log.v(Main.LOG_TAG, "Updated "+updated+" rows");
    }

    /**
     * Add geographical information to the picture at the given {@code path}.
     * @param path the path to the picture.
     * @param latitude the latitude to put into the image-file.
     * @param longitude the longitude to put into the image.
     */
    public static void geoTagImage(File path, double latitude, double longitude){
        try {
            ExifInterface exif = new ExifInterface(path.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDMS(latitude));
            if (latitude > 0){
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
            } else {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
            }
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDMS(longitude));
            if (longitude > 0){
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
            } else {
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
            }
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Converts a decimal representation of a coordinate (Latitude or Longitude) into the DMS
     *  representation</p>
     * <p>This is the required format to store the coordinate in EXIF</p>
     * @param decimal_degree the decimal-representation of a coordinate.
     * @return the DMS representation as a String.
     * @see <a href="http://en.wikipedia.org/wiki/Geographic_coordinate_conversion#Conversion_from_Decimal_Degree_to_DMS">Wikipedia article</a>
     */
    private static String convertToDMS(double decimal_degree){
        String degrees, minutes, seconds;

        double mod = decimal_degree % 1;
        int intPart = (int)decimal_degree;
        degrees = String.valueOf(intPart);

        decimal_degree = mod * 60;
        mod = decimal_degree % 1;
        intPart = (int)decimal_degree;
        minutes = String.valueOf(intPart);

        decimal_degree = mod * 60;
        intPart = (int)decimal_degree;
        seconds = String.valueOf(intPart);

        Log.v(Main.LOG_TAG, degrees + "/1," + minutes + "/1," + seconds + "/1");
        return degrees + "/1," + minutes + "/1," + seconds + "/1";
    }
}