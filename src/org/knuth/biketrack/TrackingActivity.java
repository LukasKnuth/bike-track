package org.knuth.biketrack;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;
import org.knuth.biketrack.photo.PhotoUtils;
import org.knuth.biketrack.service.TrackingListener;
import org.knuth.biketrack.service.TrackingService;
import org.knuth.biketrack.service.TrackingService.TrackingBinder;

import java.io.*;
import java.nio.channels.FileChannel;
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

    private static final int CAMERA_REQUEST_CODE = 42;

    private PhotoWorker scheduled_worker;

    private LocationStamp last_location;
    private Tour current_tour;
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
            last_location = data;
            // TODO Add using the measurement system from application prefs here!
            current_speed.setText(data.getSpeed()+" Km/h");
        }
    };

    @Override
    public void onStart(){
        super.onStart();
        Intent intent = new Intent(this, TrackingService.class);
        if (this.bindService(intent, tracking_connection, 0)){
            // Success!
            isBound = true;
            // Check if we can start a scheduled worker:
            if (this.scheduled_worker != null){
                this.scheduled_worker.execute();
            }
        }
    }

    @Override
    public void onStop(){
        super.onStop();
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
            last_location = binder.getLatestLocation();
            current_tour = binder.getTrackedTour();
            ((TextView) findViewById(R.id.tracking_tour_name)).setText(current_tour.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(Main.LOG_TAG, "Service connection Lost...");
        }
    };

    /**
     * <p>Takes care of finding, moving and Geo-Tagging a new taken image.</p>
     * <p>This should be called after the Activity is fully created, since it
     *  uses global Activity variables!</p>
     */
    private class PhotoWorker extends AsyncTask<Void, Void, Void>{

        private final ContentResolver resolver;

        public PhotoWorker(ContentResolver resolver){
            super();
            this.resolver = resolver;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // An image was taken, now the fun begins:
            try {
                // Get the Image last added to the Gallery:
                File gallery_file = PhotoUtils.getLastImagePath(resolver);
                Log.v(Main.LOG_TAG, "Photo: " + gallery_file.getAbsolutePath());
                // Copy it to the BikeTrack folder:
                FileChannel source = new FileInputStream(gallery_file).getChannel();
                File moved_file = PhotoUtils.generateNewTourImage(current_tour);
                FileChannel dest = new FileOutputStream(moved_file).getChannel();
                try {
                    dest.transferFrom(source, 0, source.size());
                } finally {
                    source.close();
                    dest.close();
                }
                // Delete it from the Gallery:
                gallery_file.delete();
                PhotoUtils.updateGalleryFile(resolver, gallery_file, moved_file);
                gallery_file = null;
                // Geo-Tag it!
                // TODO What do we do if no location is yet available?
                PhotoUtils.geoTagImage(moved_file, last_location.getLatitude(), last_location.getLongitude());
            } catch (IllegalStateException e){
                // The Gallery is empty...
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                // Only "schedule" the worker, don't execute:
                this.scheduled_worker = new PhotoWorker(this.getContentResolver());
            }
        }
    }

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
                            Intent take_photo = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            // TODO This only works with DHD. Specify the OUTPUT-intent for Xoom
                            startActivityForResult(take_photo, CAMERA_REQUEST_CODE);
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
