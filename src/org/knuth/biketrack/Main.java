package org.knuth.biketrack;

import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.LocationStamp;

import java.sql.SQLException;
import java.util.Date;

public class Main extends OrmLiteBaseActivity<DatabaseHelper> {

    // TODO Spawn service instead of doing it in Activity!
    // TODO Add possibility to create a "Tour", which capsules multiple LocationStamps
    // TODO Create a clearer data-view (for tours) including average speed, track-length, etc.
    // TODO Add charts for all collected data (AchartEngine, JavaDoc for ChartFactory).
    // TODO Add a context-menu and use the ActionBar
    // TODO Add measuring altitude (see http://stackoverflow.com/questions/6141390)

    /** The Tag to use when logging from this application! */
    public static final String LOG_TAG = "BikeTrack";

    /** The listener to process the location-changes */
    private LocationListener listener;
    /** The time of the last location-change */
    private long mLastLocationMillis;

    private EditText log;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        log = (EditText)this.findViewById(R.id.log);
    }

    @Override
    public void onStop(){
        stopTracking(null);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Called to start the tracking-procedure.
     * @param v the view that was clicked.
     */
    public void startTracking(View v){
        if (listener != null) return;
        // Setup the location stuff:
        LocationManager loc = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                try {
                    Dao<LocationStamp, Void> location_dao = getHelper().getDao();
                    location_dao.create(new LocationStamp(
                            (int)(location.getLatitude() * 1E6), // Use the E6-format.
                            (int)(location.getLongitude() * 1E6),
                            new Date(),
                            (int)(location.getSpeed() * 3.6) // Calculate Km/h
                    ));
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    mLastLocationMillis = SystemClock.elapsedRealtime();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle bundle) {}

            @Override
            public void onProviderEnabled(String provider) {
                Log.v(LOG_TAG, "Provider '"+provider+"' enabled.");
            }

            @Override
            public void onProviderDisabled(String provider) {
                if (provider.equals(LocationManager.GPS_PROVIDER)){
                    Toast.makeText(Main.this, "GPS is disabled!", Toast.LENGTH_LONG).show();
                    stopTracking(null);
                }
                Log.v(LOG_TAG, "Provider '"+provider+"' disabled.");
            }
        };
        loc.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, listener);
        loc.addGpsStatusListener(new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                switch (event) {
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        if ((SystemClock.elapsedRealtime() - mLastLocationMillis) > 3000) {
                            // The fix has been lost.
                            log.append("We lost our Fix!\n");
                        }
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        // Do something.
                        log.append("Got our first Fix!\n");
                        break;
                }
            }
        });
        //                                          Every 2 seconds, or every 10 meters!
        Log.v(LOG_TAG, "Successfully started tracking!");
        log.append("Successfully started tracking!\n");
    }

    /**
     * Called when we should stop tracking the user
     */
    public void stopTracking(View v){
        if (listener == null) return; // Haven't listened yet.
        LocationManager loc = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        loc.removeUpdates(listener);
        listener = null;
        Log.v(LOG_TAG, "Successfully stopped tracking!");
        log.append("Successfully stopped tracking!\n");
    }

    public void showRecords(MenuItem v){
        this.startActivity(new Intent(this, DatabaseActivity.class));
    }

    public void showMap(MenuItem v){
        this.startActivity(new Intent(this, TrackMapActivity.class));
    }
}
