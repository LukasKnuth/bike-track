package org.knuth.biketrack;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.knuth.biketrack.adapter.statistic.ExpandableStatisticAdapter;
import org.knuth.biketrack.adapter.statistic.Statistic;
import org.knuth.biketrack.adapter.statistic.StatisticGroup;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * <p>An {@code Activity}, showing data about one single tour.</p>
 * <p>Tracking can be started/stopped in this activity, too.</p>
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class TourActivity extends BaseActivity{

    /** The tour which is currently shown on this Activity */
    private Tour current_tour;

    private ExpandableListView statistics;
    private Button start_stop;
    private ProgressDialog progress;

    @Override
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        this.setContentView(R.layout.tour);
        statistics = (ExpandableListView)findViewById(R.id.statistics);
        // Get the Tour:
        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.containsKey(TrackingService.TOUR_KEY)){
            current_tour = extras.getParcelable(TrackingService.TOUR_KEY);
        } else {
            Log.e(Main.LOG_TAG, "No tour was supplied to TourActivity!");
        }
        this.setTitle(current_tour.toString());
        // Set the buttons text:
        start_stop = (Button)this.findViewById(R.id.start_stop_tracking);
        if (isTrackingServiceRunning()){
            start_stop.setText("Stop tracking");
        } else {
            start_stop.setText("Start tracking my position!");
        }
        start_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTrackingServiceRunning()){
                    if (stopTracking()) start_stop.setText("Start tracking my position!");
                } else {
                    if (startTracking(current_tour)) start_stop.setText("Stop tracking");
                }
            }
        });
        Log.v(Main.LOG_TAG, "Current tour has id of "+current_tour.getId());
        // Query for the data and create the statistics:
        progress = new ProgressDialog(this);
        progress.setMessage("Doing the Math...");
        progress.setIndeterminate(true);
        new StatisticLoader().execute();
    }

    private class StatisticLoader extends AsyncTask<Void, Void, ExpandableStatisticAdapter>{

        @Override
        protected void onPreExecute(){
            progress.show();
        }

        @Override
        protected ExpandableStatisticAdapter doInBackground(Void... voids) {
            List<LocationStamp> stamps = getStamps();
            if (stamps.isEmpty()) return null;
            // Fill the Adapter:
            ArrayList<StatisticGroup> groups = new ArrayList<StatisticGroup>(1);
            groups.add( getSpeedGroup(stamps) );
            groups.add( getTrackGroup(stamps) );
            groups.add( getTimeGroup(stamps) );

            return new ExpandableStatisticAdapter(TourActivity.this, groups);
        }

        /**
         * Calculate the time spend riding.
         */
        private StatisticGroup getTimeGroup(List<LocationStamp> stamps){
            Date start = stamps.get(0).getTimestamp();
            Date end = stamps.get( stamps.size()-1 ).getTimestamp();
            // Since the Java Date-API sucks...
            long secs = (end.getTime() - start.getTime()) / 1000;
            int mins = (int)(secs / 60);
            SimpleDateFormat format = new SimpleDateFormat("HH:mm");
            // Pack everything up:
            StatisticGroup time_group = new StatisticGroup("Time");
            time_group.add(new Statistic<String>(format.format(start), "", "Start time"));
            time_group.add(new Statistic<String>(format.format(end), "", "End time"));
            time_group.add(new Statistic<Integer>(mins, "min", "Overall time"));
            return time_group;
        }

        /**
         * Calculate the length of the track
         */
        private StatisticGroup getTrackGroup(List<LocationStamp> stamps){
            double total_distance = 0;
            Location location1 = null;
            Location location2 = new Location("pointB");
            // Calculate:
            for (LocationStamp s : stamps){
                if (location1 == null){
                    location1 = new Location("pointA");
                    location1.setLatitude(s.getLatitudeE6() / 1E6);
                    location1.setLongitude(s.getLongitudeE6() / 1E6);
                    continue;
                }
                // Set new goal-LatLon
                location2.setLatitude(s.getLatitudeE6() / 1E6);
                location2.setLongitude(s.getLongitudeE6() / 1E6);
                // Calculate distance:
                total_distance += location1.distanceTo(location2);
                // Set new start-location:
                location1.set(location2);
            }
            total_distance = (double)Math.round((total_distance / 1000) * 100) / 100;
            // Create group and data:
            StatisticGroup track_group = new StatisticGroup("Track");
            track_group.add(new Statistic<Double>(total_distance, "Km", "Total distance"));
            return track_group;
        }

        /**
         * Calculate average- and top-speed
         */
        private StatisticGroup getSpeedGroup(List<LocationStamp> stamps){
            int top_speed = 0;
            int all_speed = 0; // needed for average calculation.
            for (LocationStamp s : stamps){
                all_speed += s.getSpeed();
                if (top_speed < s.getSpeed()) top_speed = s.getSpeed();
            }
            int average_speed = all_speed / stamps.size();
            StatisticGroup speed_group = new StatisticGroup("Speed");
            speed_group.add(new Statistic<Integer>(top_speed, "Km/h", "Top Speed"));
            speed_group.add(new Statistic<Integer>(average_speed, "Km/h", "Average Speed"));
            return speed_group;
        }

        /**
         * Query the list of {@code LocationStamp}s for this tour from the DB.
         */
        private List<LocationStamp> getStamps(){
            try {
                Dao<LocationStamp, Void> location_dao = TourActivity.this.getHelper().getLocationStampDao();
                QueryBuilder<LocationStamp, Void> builder = location_dao.queryBuilder();
                builder.where().eq("tour_id", current_tour.getId());
                builder.orderBy("timestamp", true);
                return builder.query();
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

        @Override
        protected void onPostExecute(ExpandableStatisticAdapter adapter){
            if (adapter != null){
                statistics.setAdapter(adapter);
                // Expand all list entries:
                for (int i = 0; i < adapter.getGroupCount(); i++)
                    statistics.expandGroup(i);
            }
            progress.dismiss();
        }
    };

    /**
     * <p>This method will cause the {@code TrackingService} to start tracking
     *  and recording your position-data.</p>
     * <p>If the service has already been started before, nothing will happen.</p>
     * @param tour the tour to track.
     * @return {@code true} if the tracking-service was successfully started,
     *  {@code false} otherwise.
     * @see #stopTracking()
     */
    private boolean startTracking(Tour tour){
        if (!checkGpsEnabled()) return false;
        if (isTrackingServiceRunning()) return true;
        // Start the service:
        Intent track_service = new Intent(this, TrackingService.class);
        track_service.putExtra(TrackingService.TOUR_KEY, tour);
        if (this.startService(track_service) != null){
            Toast.makeText(this, "Started tracking. Ride like Hell!", Toast.LENGTH_LONG).show();
            return true;
        } else
            Log.e(Main.LOG_TAG, "Couldn't start tracking-service!");
        return false;
    }

    /**
     * <p>This method will cause the {@code TrackingService} to stop .</p>
     * <p>If the service has already been stopped before, nothing will happen.</p>
     * @return {@code true} if the tracking-service was successfully stopped,
     *  {@code false} otherwise.
     * @see #startTracking(org.knuth.biketrack.persistent.Tour)
     */
    private boolean stopTracking(){
        // Stop the service:
        if (this.stopService(new Intent(this, TrackingService.class))){
            Toast.makeText(this, "The drones are no longer following you.", Toast.LENGTH_LONG).show();
            return true;
        } else
            Log.e(Main.LOG_TAG, "Couldn't stopp tracking-service!");
        return false;
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

    /**
     * This will check if the GPS is currently enabled and if not, show a dialog
     *  which brings you to the corresponding settings activity.
     * @return {@code true} if GPS was activated, {@code false} otherwise.
     */
    private boolean checkGpsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // GPS is not enabled:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("GPS is currently disabled. You'll need to enable it.")
                    .setCancelable(false)
                    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
            return false;
        } else return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*
            NOT inflating menu currently.
            See https://github.com/JakeWharton/ActionBarSherlock/issues/562
        */
        //this.getSupportMenuInflater().inflate(R.menu.tour_menu, menu);
        menu.add(R.string.tourActivity_menu_showRecords)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT)
            .setIcon(android.R.drawable.ic_menu_sort_by_size)
            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    showRecords(item);
                    return true;
                }
            });
        menu.add(R.string.tourActivity_menu_showMap)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT)
            .setIcon(android.R.drawable.ic_menu_mapmode)
            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    showMap(item);
                    return true;
                }
            });
        return true;
    }

    public void showRecords(MenuItem v){
        Intent i = new Intent(this, DatabaseActivity.class);
        i.putExtra(TrackingService.TOUR_KEY, current_tour);
        this.startActivity(i);
    }

    public void showMap(MenuItem v){
        Intent i = new Intent(this, TrackMapActivity.class);
        i.putExtra(TrackingService.TOUR_KEY, current_tour);
        this.startActivity(i);
    }
}
