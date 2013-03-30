package org.knuth.biketrack;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.service.TrackingListener;
import org.knuth.biketrack.service.TrackingService;
import org.knuth.biketrack.service.TrackingService.TrackingBinder;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private ScheduledThreadPoolExecutor clock = new ScheduledThreadPoolExecutor(2);
    private ScheduledFuture hide_timer;

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
        // Show ActionBar if we click *somewhere*:
        this.findViewById(R.id.tracking_root).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Hide it automatically after 2 seconds:
                if (hide_timer != null){
                    hide_timer.cancel(false);
                } else {
                    TrackingActivity.this.getSupportActionBar().show();
                }
                hide_timer = clock.schedule(new Runnable() {
                    @Override
                    public void run() {
                        TrackingActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TrackingActivity.this.getSupportActionBar().hide();
                            }
                        });
                        hide_timer = null;
                    }
                }, 2, TimeUnit.SECONDS);
            }
        });
        // Enable going back from the ActionBar:
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Make sure the user sees how the ActionBar disappears:
        clock.schedule(new Runnable() {
            @Override
            public void run() {
                TrackingActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Do this only ONCE...
                        TrackingActivity.this.getSupportActionBar().hide();
                    }
                });
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    private TrackingListener callback = new TrackingListener() {
        @Override
        public void update(LocationStamp data) {
            // Last update on activity...
            // TODO Add using the measurement system from application prefs here!
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
            String tour_name = binder.getTrackedTour().toString();
            ((TextView) findViewById(R.id.tracking_tour_name)).setText(tour_name);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(Main.LOG_TAG, "Service connection Lost...");
        }
    };

    /** ---- ActionBar Magic ---- */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            menu.add(R.string.tracking_menu_take_photo).
                    setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).
                    setIcon(android.R.drawable.ic_menu_camera).
                    setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Intent camera_activity = new Intent(TrackingActivity.this, CameraActivity.class);
                            TrackingActivity.this.startActivity(camera_activity);
                            return true;
                        }
                    });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            // If the Logo in the ActionBar is pressed, simulate a "BACK"-button press.
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

}
