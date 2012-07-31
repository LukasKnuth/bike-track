package org.knuth.biketrack;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.j256.ormlite.dao.Dao;
import org.achartengine.ChartFactory;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;

import java.sql.SQLException;
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

    private Button start_stop;
    private LinearLayout charts;
    private ProgressDialog progress;

    @Override
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        this.setContentView(R.layout.tour);
        charts = (LinearLayout)this.findViewById(R.id.charts);
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
        progress.setTitle("Doing the Math...");
        progress.setIndeterminate(true);
        new SpeedStatistics().execute();
    }

    /**
     * Do the math for the speed-statistics
     */
    private class SpeedStatistics extends AsyncTask<Void, Integer, XYMultipleSeriesDataset>{

        @Override
        protected void onPreExecute(){
            progress.show();
        }

        @Override
        protected XYMultipleSeriesDataset doInBackground(Void... data) {
            List<LocationStamp> locs;
            try {
                Dao<LocationStamp, Void> dao = TourActivity.this.getHelper().getLocationStampDao();
                locs = dao.queryForEq("tour_id", current_tour.getId());
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
            int counter = 0;
            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            TimeSeries speed_series = new TimeSeries("Your Speed (in Km/h)");
            for (LocationStamp stamp : locs){
                speed_series.add(
                        stamp.getTimestamp(),
                        stamp.getSpeed()
                );
                publishProgress(++counter, locs.size());
            }
            dataset.addSeries(speed_series);
            return dataset;
        }

        @Override
        protected void onProgressUpdate(Integer... stats){
            progress.setMessage("Progressing "+stats[0]+" of "+stats[1]+" items.");
        }

        @Override
        protected void onPostExecute(XYMultipleSeriesDataset dataset){
            if (dataset != null){
                XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
                seriesRenderer.setFillPoints(false);
                seriesRenderer.setLineWidth(2);
                seriesRenderer.setStroke(BasicStroke.SOLID);

                XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
                renderer.setYTitle("Speed (in Km/h)");
                renderer.setXTitle("Time");
                renderer.setXLabels(8);
                renderer.addSeriesRenderer(seriesRenderer);
                charts.addView(ChartFactory.getTimeChartView(
                        TourActivity.this, dataset, renderer, null));
            }
            progress.dismiss();
        }
    }

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
