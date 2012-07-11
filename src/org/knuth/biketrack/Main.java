package org.knuth.biketrack;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import org.knuth.biketrack.persistent.DatabaseHelper;

public class Main extends OrmLiteBaseActivity<DatabaseHelper> {

    // TODO Add possibility to create a "Tour", which capsules multiple LocationStamps
    // TODO Create a clearer data-view (for tours) including average speed, track-length, etc.
    // TODO Add charts for all collected data (AchartEngine, JavaDoc for ChartFactory).
    // TODO Add a context-menu and use the ActionBar
    // TODO Add measuring altitude (see http://stackoverflow.com/questions/6141390)

    /** The Tag to use when logging from this application! */
    public static final String LOG_TAG = "BikeTrack";

    private EditText log;
    private Button track_start_stop;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        log = (EditText)this.findViewById(R.id.log);
        track_start_stop = (Button)this.findViewById(R.id.tracking_start_stop);
        track_start_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTrackingServiceRunning()){
                    stopTracking();
                    track_start_stop.setText("Start Tracking my position!");
                } else {
                    startTracking();
                    track_start_stop.setText("Stop Tracking...");
                }
            }
        });
        // Check if service is running and set Text accordingly:
        if (isTrackingServiceRunning())
            track_start_stop.setText("Stop Tracking...");
        else
            track_start_stop.setText("Start Tracking my position!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * <p>This method will cause the {@code TrackingService} to start tracking
     *  and recording your position-data.</p>
     * <p>If the service has already been started before, nothing will happen.</p>
     * @see #stopTracking()
     */
    private void startTracking(){
        if (isTrackingServiceRunning()) return;
        // Start the service:
        if (this.startService(new Intent(this, TrackingService.class)) != null)
            log.append("Successfully started tracking!\n");
        else
            Log.e(LOG_TAG, "Couldn't start tracking-service!");
    }

    /**
     * <p>This method will cause the {@code TrackingService} to stop .</p>
     * <p>If the service has already been stopped before, nothing will happen.</p>
     * @see #startTracking()
     */
    private void stopTracking(){
        // Stop the service:
        if (this.stopService(new Intent(this, TrackingService.class)))
            log.append("Successfully stopped tracking!\n");
        else
            Log.e(LOG_TAG, "Couldn't stopp tracking-service!");
    }

    /**
     * Check whether the {@code TrackingService} is already running or not.
     * @return whether the {@code TrackingService} is already running or not.
     * @see <a href="http://stackoverflow.com/a/5921190/717341">SO answer</a>
     */
    private boolean isTrackingServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("org.knuth.biketrack.TrackingService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void showRecords(MenuItem v){
        this.startActivity(new Intent(this, DatabaseActivity.class));
    }

    public void showMap(MenuItem v){
        this.startActivity(new Intent(this, TrackMapActivity.class));
    }
}
