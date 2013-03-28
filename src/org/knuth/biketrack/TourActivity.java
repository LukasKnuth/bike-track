package org.knuth.biketrack;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.knuth.biketrack.adapter.statistic.ExpandableStatisticAdapter;
import org.knuth.biketrack.adapter.statistic.Statistic;
import org.knuth.biketrack.adapter.statistic.StatisticGroup;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;
import org.knuth.biketrack.service.TrackingService;

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
public class TourActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<ExpandableStatisticAdapter>{

    private static final double METER_TO_MILE = 0.000621371192;
    private static final float MS_TO_MPH = 2.23693629f;
    private static final double METER_TO_KILOMETER = 0.001;
    private static final float MS_TO_KMH = 3.6f;

    /** The tour which is currently shown on this Activity */
    private Tour current_tour;

    private ExpandableListView statistics;
    private Button start_stop;
    /** ActionBar item, only shown when tracking to get back to {@code TrackingActivity} */
    private MenuItem live_view;

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
            this.finish();
        }
        this.setTitle(current_tour.toString());
        // Set the buttons text:
        start_stop = (Button)this.findViewById(R.id.start_stop_tracking);
        if (isTrackingServiceRunning(this)){
            start_stop.setText("Stop tracking");
        } else {
            start_stop.setText("Start tracking my position!");
        }
        start_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTrackingServiceRunning(TourActivity.this)){
                    if (stopTracking()) start_stop.setText("Start tracking my position!");
                } else {
                    if (startTracking(current_tour)) start_stop.setText("Stop tracking");
                }
            }
        });
        Log.v(Main.LOG_TAG, "Current tour has id of "+current_tour.getId());
        // Enable going back from the ActionBar:
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set the empty-view for the list:
        View empty_view = this.getLayoutInflater().inflate(R.layout.statistic_empty_view, null);
        ((ViewGroup) statistics.getParent()).addView(empty_view); // See http://stackoverflow.com/q/3727063/717341
        statistics.setEmptyView(empty_view);
        // Query for the data and create the statistics:
        getSupportLoaderManager().initLoader(StatisticLoader.STATISTIC_LOADER_ID, null, this);
    }

    @Override
    public Loader<ExpandableStatisticAdapter> onCreateLoader(int id, Bundle args) {
        if (id == StatisticLoader.STATISTIC_LOADER_ID) {
            // Create the loader:
            return new StatisticLoader(this, current_tour);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<ExpandableStatisticAdapter> loader, ExpandableStatisticAdapter adapter) {
        if (adapter != null){
            statistics.setAdapter(adapter);
            // Expand all list entries:
            for (int i = 0; i < adapter.getGroupCount(); i++)
                statistics.expandGroup(i);
        } else {
            // There are no stamps for this tour:
            statistics.getEmptyView().setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<ExpandableStatisticAdapter> loader) {
    }

    /**
     * Loads the data for the current tour from the DB and puts everything in an Adapter.
     */
    private static class StatisticLoader extends AsyncTaskLoader<ExpandableStatisticAdapter> {

        public static final int STATISTIC_LOADER_ID = 1;
        private final Tour current_tour;
        private final Context context;

        public StatisticLoader(Context context, Tour tour) {
            super(context);
            this.context = context;
            this.current_tour = tour;
        }

        @Override
        protected void onStartLoading() {
            forceLoad(); // This seems to be a bug in the SupportLibrary.
                         // See http://stackoverflow.com/q/8606048/717341
        }

        @Override
        public ExpandableStatisticAdapter loadInBackground() {
            List<LocationStamp> stamps = getStamps();
            if (stamps.isEmpty()) return null;
            // Fill the Adapter:
            ArrayList<StatisticGroup> groups = new ArrayList<StatisticGroup>(1);
            groups.add( getSpeedGroup(stamps) );
            groups.add( getTrackGroup(stamps) );
            groups.add( getTimeGroup(stamps) );

            return new ExpandableStatisticAdapter(context, groups);
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
            // TODO New way (V2) available?
            double total_distance = 0;
            Location location1 = null;
            Location location2 = new Location("pointB");
            // Calculate:
            for (LocationStamp s : stamps){
                if (location1 == null){
                    location1 = new Location("pointA");
                    location1.setLatitude(s.getLatitude());
                    location1.setLongitude(s.getLongitude());
                    continue;
                }
                // Set new goal-LatLon
                location2.setLatitude(s.getLatitude());
                location2.setLongitude(s.getLongitude());
                // Calculate distance:
                total_distance += location1.distanceTo(location2);
                // Set new start-location:
                location1.set(location2);
            }
            // Calculate the distance depending on the set system:
            StatisticGroup track_group = new StatisticGroup("Track");
            if (isMetric()){
                total_distance = (double)Math.round(total_distance * METER_TO_KILOMETER) / 100;
                track_group.add(new Statistic<Double>(total_distance, "Km", "Total distance"));
            } else {
                total_distance = (double)Math.round(total_distance * METER_TO_MILE) / 100;
                track_group.add(new Statistic<Double>(total_distance, "mi", "Total distance"));
            }
            return track_group;
        }

        /**
         * Checks whether the app is set to use the metric system or not.
         * @return {@code true} if the metric system is used, {@code false} otherwise.
         */
        private boolean isMetric(){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getString(
                    context.getString(R.string.prefs_key_system_of_measurement),
                    context.getString(R.string.prefs_value_measure_system_metric)
                   ).equals(context.getString(R.string.prefs_value_measure_system_metric));
        }

        /**
         * Calculate average- and top-speed
         */
        private StatisticGroup getSpeedGroup(List<LocationStamp> stamps){
            float top_speed = 0;
            float all_speed = 0; // needed for average calculation.
            for (LocationStamp s : stamps){
                all_speed += s.getSpeed();
                if (top_speed < s.getSpeed()) top_speed = s.getSpeed();
            }
            float average_speed = all_speed / stamps.size();
            // Calculate the statistics:
            StatisticGroup speed_group = new StatisticGroup("Speed");
            if (isMetric()){
                top_speed = (top_speed / MS_TO_KMH);
                average_speed = (average_speed / MS_TO_KMH);
                speed_group.add(new Statistic<Integer>((int)top_speed, "Km/h", "Top Speed"));
                speed_group.add(new Statistic<Integer>((int)average_speed, "Km/h", "Average Speed"));
            } else {
                top_speed = (top_speed / MS_TO_MPH);
                average_speed = (average_speed / MS_TO_MPH);
                speed_group.add(new Statistic<Integer>((int)top_speed, "mph", "Top Speed"));
                speed_group.add(new Statistic<Integer>((int)average_speed, "mph", "Average Speed"));
            }
            return speed_group;
        }

        /**
         * Query the list of {@code LocationStamp}s for this tour from the DB.
         */
        private List<LocationStamp> getStamps(){
            try {
                Dao<LocationStamp, Void> location_dao = OpenHelperManager.getHelper(
                        context, DatabaseHelper.class
                ).getLocationStampDao();
                QueryBuilder<LocationStamp, Void> builder = location_dao.queryBuilder();
                builder.where().eq("tour_id", current_tour.getId());
                builder.orderBy("timestamp", true);
                return builder.query();
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            } finally {
                OpenHelperManager.releaseHelper(); //Decrease the ref-count!
            }
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
        if (isTrackingServiceRunning(this)) return true;
        // Start the service:
        Intent track_service = new Intent(this, TrackingService.class);
        track_service.putExtra(TrackingService.TOUR_KEY, tour);
        if (this.startService(track_service) != null){
            Toast.makeText(this, "Started tracking. Ride like Hell!", Toast.LENGTH_LONG).show();
            live_view.setVisible(true);
            Intent tracking_activity = new Intent(this, TrackingActivity.class);
            this.startActivity(tracking_activity);
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
            live_view.setVisible(false);
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
    public static boolean isTrackingServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("org.knuth.biketrack.service.TrackingService".equals(service.service.getClassName())) {
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
        */ // TODO Remove the inflater code and the menu XML!
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
        live_view = menu.add(R.string.tourActivtiy_menu_trackingActivity)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT)
            .setIcon(android.R.drawable.ic_menu_mylocation)
            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent tracking_activity = new Intent(TourActivity.this, TrackingActivity.class);
                    TourActivity.this.startActivity(tracking_activity);
                    return true;
                }
            }).setVisible(false);
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
