package org.knuth.biketrack;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import com.j256.ormlite.android.apptools.OrmLiteBaseService;
import com.j256.ormlite.dao.Dao;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.LocationStamp;

import java.sql.SQLException;
import java.util.Date;

/**
 * The service, tracking the current position using GPS.
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class TrackingService extends OrmLiteBaseService<DatabaseHelper> {

    /** The listener to process the location-changes */
    private LocationListener locationListener;
    /** The listener to check for the current GPS-status */
    private GpsStatus.Listener gpsListener;
    /** The time of the last location-change */
    private long mLastLocationMillis;

    @Override
    public void onCreate() {
        super.onCreate();
        // Setup the location listener:
        locationListener = new LocationListener() {
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
                    Log.v(Main.LOG_TAG, "Got something!");
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle bundle) {}

            @Override
            public void onProviderEnabled(String provider) {
                Log.v(Main.LOG_TAG, "Provider '" + provider + "' enabled.");
            }

            @Override
            public void onProviderDisabled(String provider) {
                if (provider.equals(LocationManager.GPS_PROVIDER)){
                    Log.v(Main.LOG_TAG, "Provider '"+provider+"' disabled. Shutting Service down.");
                    TrackingService.this.stopSelf();
                }
            }
        };
        // Setup the GPS listener:
        gpsListener = new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                switch (event) {
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        if ((SystemClock.elapsedRealtime() - mLastLocationMillis) > 3000) {
                            // The fix has been lost.
                            // TODO Bind the service to a UI-component and deliver messages here.
                        }
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        // We have a fix!
                        // TODO Bind the service to a UI-component and deliver messages here.
                        break;
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // TODO WHat if started multiple times?
        // Bind the listeners:
        LocationManager loc = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        loc.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
        loc.addGpsStatusListener(gpsListener);
        // Start the service, so it gets recreated when killed.
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        LocationManager loc = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        loc.removeUpdates(locationListener);
        loc.removeGpsStatusListener(gpsListener);
        locationListener = null;
        gpsListener = null;
        Log.v(Main.LOG_TAG, "Being stopped...");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
