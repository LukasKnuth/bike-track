package org.knuth.biketrack;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;
import org.knuth.biketrack.service.TrackingService;

import java.sql.SQLException;
import java.util.List;

/**
 * The activity which shows a previously recorded track.
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class TrackMapActivity extends FragmentActivity {

    private GoogleMap map;
    private PolylineOptions track;

    private ProgressDialog progress;

    private Tour current_tour;

    // TODO Don't reload on Orientation-change
    // TODO Include the Google Play services license (point in preferences?). See https://developers.google.com/maps/documentation/android/intro

    @Override
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        if (readyToGo()){
            this.setContentView(R.layout.trackmap);
            map = ((SupportMapFragment) this.getSupportFragmentManager().findFragmentById(R.id.mapview)).getMap();
        } else {
            // No G-Play services on the device...
            Log.e(Main.LOG_TAG, "No Google Play Services on this device!");
            finish();
        }
        // Setup the track-paint:
        track = new PolylineOptions();
        track.color(Color.GREEN).width(3);

        // Get the current tour:
        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.containsKey(TrackingService.TOUR_KEY)){
            current_tour = extras.getParcelable(TrackingService.TOUR_KEY);
        } else {
            Log.e(Main.LOG_TAG, "No tour was supplied to TrackMapActivity!");
        }
        this.setTitle("Map for '"+current_tour.getName()+"'");

        // Load data from Database and display the Track:
        progress = new ProgressDialog(this);
        progress.setIndeterminate(true);
        new LoadTrack().execute(current_tour);
    }

    /**
     * Checks if the "Google Play Services" (and therefor the Maps API) are amiable on this device.
     * @return {@code true} if everything looks good, false otherwise.
     */
    private boolean readyToGo() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            return true;
        } else {
            // TODO Test this!
            GooglePlayServicesUtil.getErrorDialog(status, this, 1122).show();
        }
        return false;
    }

    /**
     * Loads a specific track from the Database and displays it over the Map.
     */
    private class LoadTrack extends AsyncTask<Tour, LatLng, LatLng>{

        @Override
        protected void onPreExecute(){
            progress.show();
        }

        @Override
        protected LatLng doInBackground(Tour... tours) {
            try {
                Dao<LocationStamp, Void> location_dao = TrackMapActivity.this.getHelper().getLocationStampDao();
                List<LocationStamp> stamps = location_dao.queryForEq("tour_id", tours[0].getId());
                if (stamps.size() == 0) return null;
                // Push the LatLng objects:
                for (LocationStamp stamp : stamps){
                    LatLng location = new LatLng(stamp.getLatitude(), stamp.getLongitude());
                    this.publishProgress(location);
                }
                return new LatLng(stamps.get(0).getLatitude(), stamps.get(0).getLongitude());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(LatLng... location){
            // Append the new location:
            track.add(location[0]);
        }

        @Override
        protected void onPostExecute(LatLng start){
            map.addPolyline(track);
            map.moveCamera(CameraUpdateFactory.newLatLng(start));
            map.moveCamera(CameraUpdateFactory.zoomTo(19.0f));
            // TODO When entering, Zoom out to see the FULL track.
            progress.dismiss();
        }
    }

    /*
        -------------- Database ORMlite stuff ---------------
        See http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html#SEC40
     */

    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }
}
