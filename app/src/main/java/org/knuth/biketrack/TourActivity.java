package org.knuth.biketrack;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;
import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.knuth.biketrack.adapter.statistic.BarGraphStatistic;
import org.knuth.biketrack.adapter.statistic.Distance;
import org.knuth.biketrack.adapter.statistic.ExpandableStatisticAdapter;
import org.knuth.biketrack.adapter.statistic.LineGraphStatistic;
import org.knuth.biketrack.adapter.statistic.Speed;
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
import java.util.Comparator;
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

    /** The tour which is currently shown on this Activity */
    private Tour current_tour;

    private ExpandableListView statistics;
    private LineGraph speed_graph;
    private Button start_stop;
    private MenuItem menu_item_map;
    private MenuItem menu_item_records;
    private MenuItem menu_item_live;
    private MenuItem menu_item_edit;

    @Override
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        this.setContentView(R.layout.tour);
        statistics = (ExpandableListView)findViewById(R.id.statistics);
        start_stop = (Button)this.findViewById(R.id.start_stop_tracking);
        // Get the Tour:
        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.containsKey(TrackingService.TOUR_KEY)){
            current_tour = extras.getParcelable(TrackingService.TOUR_KEY);
            this.setTitle(current_tour.toString());
        } else {
            current_tour = Tour.UNSTORED_TOUR;
            Log.e(Main.LOG_TAG, "No tour was supplied, so I created one.");
            this.setTitle(getString(R.string.tourActivity_general_newTour));
        }
        // Set the buttons text:
        if (isTrackingServiceRunning(this)){
            start_stop.setText(R.string.tourActivity_button_stopTracking);
        } else {
            if (current_tour != Tour.UNSTORED_TOUR){
                // This is a previously tracked tour. Since the service isn't running, we don't need the stop button.
                start_stop.setVisibility(View.GONE);
            } else {
                start_stop.setText(R.string.tourActivity_button_startTracking);
            }
        }
        start_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTrackingServiceRunning(TourActivity.this)){
                    if (stopTracking()){
                        start_stop.setText(R.string.tourActivity_button_continueTracking);
                        // Show statistics when tour is ended:
                        getSupportLoaderManager().initLoader(
                                StatisticLoader.STATISTIC_LOADER_ID, null, TourActivity.this
                        );
                    }
                } else {
                    if (startTracking()) start_stop.setText(R.string.tourActivity_button_stopTracking);
                }
            }
        });
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
        // TODO refactor this into a) another class and b) to use Executor for multicore support!

        public static final int STATISTIC_LOADER_ID = 1;
        private final Tour load_tour;
        private final Context context;

        public StatisticLoader(Context context, Tour tour) {
            super(context);
            this.context = context;
            this.load_tour = tour;
        }

        @Override
        protected void onStartLoading() {
            forceLoad(); // This seems to be a bug in the SupportLibrary.
                         // See http://stackoverflow.com/q/8606048/717341
        }

        @Override
        public ExpandableStatisticAdapter loadInBackground() {
            List<LocationStamp> stamps = getStamps();
            Log.v(Main.LOG_TAG, "Got " + stamps.size() + " stamps for tour-ID: " + load_tour.getId());
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
            SimpleDateFormat when = new SimpleDateFormat("d. MMM yyyy");
            // Pack everything up:
            StatisticGroup time_group = new StatisticGroup("Time");
            time_group.add(new Statistic<String>(when.format(start), "", "Date"));
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
            double current_distance = 0;
            double downhill_distance = 0;
            double uphill_distance = 0;
            double flat_distance = 0;

            final double flat_tolerance = 0.2;

            double last_altitude = -1;
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
                current_distance = location1.distanceTo(location2);
                total_distance += current_distance;
                // Set new start-location:
                location1.set(location2);
                // Update the bars:
                if (last_altitude == -1){
                    // First run:
                    last_altitude = s.getAltitude();
                } else {
                    if ((s.getAltitude() - last_altitude) > flat_tolerance){
                        uphill_distance += current_distance;
                    } else if ((s.getAltitude() - last_altitude) < -flat_tolerance){
                        downhill_distance += current_distance;
                    } else {
                        flat_distance += current_distance;
                    }
                    last_altitude = s.getAltitude();
                }
            }
            // Calculate the distance depending on the set system:
            StatisticGroup track_group = new StatisticGroup(context.getString(R.string.tourActivity_statistics_track));

            Bar uphill_bar = new Bar();
            uphill_bar.setName(context.getString(R.string.tourActivity_statistics_uphill));
            uphill_bar.setColor(context.getResources().getColor(R.color.statistics_bar_uphill));
            Bar downhill_bar = new Bar();
            downhill_bar.setName(context.getString(R.string.tourActivity_statistics_downhill));
            downhill_bar.setColor(context.getResources().getColor(R.color.statistics_bar_downhill));
            Bar flat_bar = new Bar();
            flat_bar.setName(context.getString(R.string.tourActivity_statistics_flat));
            flat_bar.setColor(context.getResources().getColor(R.color.statistics_bar_flat));

            // Set values:
            uphill_bar.setValue((float)Distance.toCurrentUnit(uphill_distance, context));
            downhill_bar.setValue((float)Distance.toCurrentUnit(downhill_distance, context));
            flat_bar.setValue((float)Distance.toCurrentUnit(flat_distance, context));
            track_group.add(new Statistic<String>(
                            Distance.formatCurrentUnit(total_distance, context),
                            context.getString(R.string.label_unit_kilometers),
                            context.getString(R.string.tourActivity_statistics_distance))
            );
            track_group.add(new BarGraphStatistic(
                            context.getString(R.string.tourActivity_statistics_terrain),
                            Distance.getCurrentUnit(context),
                            uphill_bar, flat_bar, downhill_bar)
            );
            return track_group;
        }

        /**
         * Calculate average- and top-speed
         */
        private StatisticGroup getSpeedGroup(List<LocationStamp> stamps){
            // setup:
            float top_speed_ms = Collections.max(stamps, new Comparator<LocationStamp>() {
                @Override
                public int compare(LocationStamp locationStamp, LocationStamp locationStamp2) {
                    return Float.compare(locationStamp.getSpeed(), locationStamp2.getSpeed());
                }
            }).getSpeed();
            float all_speed = 0; // needed for average calculation. TODO Use "mittelwert" here?
            Line speed_line = new Line();
            speed_line.setShowingPoints(false);
            speed_line.setColor(context.getResources().getColor(R.color.statistics_line_speed));
            Line altitude_line = new Line(); // Uphill/Downhill
            altitude_line.setShowingPoints(false);
            altitude_line.setColor(context.getResources().getColor(R.color.statistics_line_altitude));
            double last_altitude = -1;
            double y = 0.0;

            int x = 0;
            boolean is_second = true;
            // Process stamps:
            for (LocationStamp s : stamps){
                all_speed += s.getSpeed();
                // Draw the speed-line:
                if (is_second){
                    // A little trick to add only every second time-stamp to the graph. looks clearer...
                    speed_line.addPoint(new LinePoint(x, s.getSpeed()));
                    x++;
                    is_second = false;
                } else is_second = true;
                // Draw the uphil/downhil line:
                if (last_altitude == -1){
                    // first round:
                    last_altitude = s.getAltitude();
                } else {
                    if (last_altitude > s.getAltitude()){
                        // Downhill:
                        y -= 0.1;
                    } else if (last_altitude < s.getAltitude()){
                        // Uphill:
                        y += 0.1;
                    }
                    altitude_line.addPoint(new LinePoint(x, (float) y+top_speed_ms/2));
                    last_altitude = s.getAltitude();
                }
            }
            float average_speed_ms = all_speed / stamps.size();
            // Calculate the statistics:
            StatisticGroup speed_group = new StatisticGroup(context.getString(R.string.tourActivity_statistics_speed));
            speed_group.add(new Statistic<String>(
                            Speed.formatCurrentUnit(top_speed_ms, context),
                            context.getString(R.string.label_unit_kmh),
                            context.getString(R.string.tourActivity_statistics_topSpeed))
            );
            speed_group.add(new Statistic<String>(
                            Speed.formatCurrentUnit(average_speed_ms, context),
                            context.getString(R.string.label_unit_kmh),
                            context.getString(R.string.tourActivity_statistics_avgSpeed))
            );
            speed_group.add(new LineGraphStatistic(
                    context.getString(R.string.tourActivity_statistics_speedOverTime),
                    top_speed_ms, speed_line, altitude_line
            ));

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
                builder.where().eq("tour_id", load_tour.getId());
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
     * @return {@code true} if the tracking-service was successfully started,
     *  {@code false} otherwise.
     * @see #stopTracking()
     */
    private boolean startTracking(){
        if (!checkGpsEnabled()) return false;
        if (isTrackingServiceRunning(this)) return true;
        // Check if we need to create the tour in the DB:
        if (current_tour == Tour.UNSTORED_TOUR){
            // Create a new tour in the database!
            try {
                Dao<Tour, Integer> dao = TourActivity.this.getHelper().getTourDao();
                // Make the tour:
                current_tour = new Tour(new Date());
                dao.create(current_tour);
                Log.v(Main.LOG_TAG, "Tour-ID is: "+current_tour.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // Start the service:
        Intent track_service = new Intent(this, TrackingService.class);
        track_service.putExtra(TrackingService.TOUR_KEY, current_tour);
        if (this.startService(track_service) != null){
            Toast.makeText(this, R.string.tourActivity_toast_startTracking, Toast.LENGTH_LONG).show();
            menu_item_live.setVisible(true);
            Intent tracking_activity = new Intent(this, TrackingActivity.class);
            this.startActivity(tracking_activity);
            return true;
        } else {
            Log.e(Main.LOG_TAG, "Couldn't start tracking-service!");
            return false;
        }
    }

    /**
     * <p>This method will cause the {@code TrackingService} to stop .</p>
     * <p>If the service has already been stopped before, nothing will happen.</p>
     * @return {@code true} if the tracking-service was successfully stopped,
     *  {@code false} otherwise.
     * @see #startTracking()
     */
    private boolean stopTracking(){
        // Stop the service:
        if (this.stopService(new Intent(this, TrackingService.class))){
            Toast.makeText(this, R.string.tourActivity_toast_stopTracking, Toast.LENGTH_LONG).show();
            menu_item_live.setVisible(false);
            menu_item_map.setVisible(true);
            menu_item_edit.setVisible(true);
            menu_item_records.setVisible(true);
            return true;
        } else {
            Log.e(Main.LOG_TAG, "Couldn't stopp tracking-service!");
            return false;
        }
    }

    /**
     * Check whether the {@code TrackingService} is already running or not.
     * @return whether the {@code TrackingService} is already running or not.
     * @see <a href="http://stackoverflow.com/a/5921190/717341">SO answer</a>
     */
    public static boolean isTrackingServiceRunning(Context context) {
        // TODO There should be a better way to check this. REFACTOR!
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        String serviceName = TrackingService.class.getName();
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
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
            builder.setMessage(R.string.tourActivity_dialog_gpsDisabled)
                    .setCancelable(false)
                    .setPositiveButton(R.string.tourActivity_dialog_gotoSettings, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
            return false;
        } else return true;
    }

    private void editTour() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(current_tour.getTitle());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.tourActivtiy_menu_edit)
                .setView(input)
                .setCancelable(true)
                .setPositiveButton(R.string.tourActivity_dialog_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        current_tour.setTitle(input.getText().toString());
                        try {
                            Dao<Tour, Integer> dao = getHelper().getTourDao();
                            dao.update(current_tour);
                            setTitle(current_tour.getTitle());
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            dialogInterface.dismiss();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.tour_menu, menu);

        menu_item_map = menu.findItem(R.id.tour_menu_map);
        menu_item_records = menu.findItem(R.id.tour_menu_records);
        menu_item_live = menu.findItem(R.id.tour_menu_live);
        menu_item_edit = menu.findItem(R.id.tour_menu_edit);

        if (current_tour == Tour.UNSTORED_TOUR){
            menu_item_map.setVisible(false);
            menu_item_records.setVisible(false);
            menu_item_edit.setVisible(false);
        }
        if (isTrackingServiceRunning(this)){
            menu_item_live.setVisible(true);
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
            case R.id.tour_menu_live:
                this.startActivity(new Intent(this, TrackingActivity.class));
                return true;
            case R.id.tour_menu_map:
                Intent map = new Intent(this, TrackMapActivity.class);
                map.putExtra(TrackingService.TOUR_KEY, current_tour);
                this.startActivity(map);
                return true;
            case R.id.tour_menu_records:
                Intent records = new Intent(this, DatabaseActivity.class);
                records.putExtra(TrackingService.TOUR_KEY, current_tour);
                this.startActivity(records);
                return true;
            case R.id.tour_menu_edit:
                editTour();
        }
        return false;
    }

}
