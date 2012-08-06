package org.knuth.biketrack;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.service.TrackingListener;
import org.knuth.biketrack.service.TrackingService;
import org.knuth.biketrack.service.TrackingService.TrackingBinder;

/**
 * <p>The Activity which is shown, when currently tracking the position.</p>
 * <p>This Activity binds to the {@code TrackingService} to receive updates
 *  from it and show the "live" on the screen.</p>
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class TrackingActivity extends BaseActivity {

    private boolean isBound;

    private TextView current_speed;

    // TODO Make this Activity with WakeLog (later commit) see (http://stackoverflow.com/questions/3660464)

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        // Check if tracking-service is running:
        if (!TourActivity.isTrackingServiceRunning(this)){
            Log.e(Main.LOG_TAG, "Tracking Service is not running...");
            this.finish();
        }
        this.setContentView(R.layout.tracking);
        current_speed = (TextView) findViewById(R.id.tracking_current_speed);
    }

    private TrackingListener callback = new TrackingListener() {
        @Override
        public void update(LocationStamp data) {
            // Last update on activity...
            current_speed.setText(data.getSpeed()+" Km/h");
        }
    };

    @Override
    public void onResume(){
        super.onResume();
        Intent intent = new Intent(this, TrackingService.class);
        if (this.bindService(intent, tracking_connection, 0)){
            // Success!
            isBound = true;
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if (isBound){
            // Unbind from the Service:
            unbindService(tracking_connection);
            isBound = false;
        }
    }

    private ServiceConnection tracking_connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TrackingBinder binder = (TrackingBinder) iBinder;
            binder.requestUpdates(callback);
            // Get the current tours information:
            String tour_name = binder.getTrackedTour().getName();
            ((TextView) findViewById(R.id.tracking_tour_name)).setText(tour_name);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(Main.LOG_TAG, "Service connection Lost...");
        }
    };

}
