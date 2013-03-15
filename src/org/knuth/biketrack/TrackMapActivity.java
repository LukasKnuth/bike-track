package org.knuth.biketrack;

import android.app.ProgressDialog;
import android.graphics.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.google.android.maps.*;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;
import org.knuth.biketrack.service.TrackingService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The activity which shows a previously recorded track.
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class TrackMapActivity extends MapActivity {

    private MapView map;
    private Paint track_paint;

    private ProgressDialog progress;

    private Tour current_tour;

    // TODO Don't reload on Orientation-change
    // TODO Make this one Full-Screen.

    @Override
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        this.setContentView(R.layout.trackmap);
        map = (MapView) findViewById(R.id.mapview);
        map.setBuiltInZoomControls(true);

        // Setup the track-paint:
        track_paint = new Paint();
        track_paint.setColor(Color.GREEN);
        track_paint.setStyle(Paint.Style.STROKE);
        track_paint.setStrokeJoin(Paint.Join.ROUND);
        track_paint.setStrokeCap(Paint.Cap.ROUND);
        track_paint.setStrokeWidth(3);

        // Get the current tour:
        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.containsKey(TrackingService.TOUR_KEY)){
            current_tour = extras.getParcelable(TrackingService.TOUR_KEY);
        } else {
            Log.e(Main.LOG_TAG, "No tour was supplied to TrackMapActivity!");
        }
        this.setTitle("Map for '"+current_tour.getName()+"'");

        // Load data from Database and display the Track:
        progress = new ProgressDialog(this);
        progress.setIndeterminate(true);
        new LoadTrack().execute(current_tour);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    /**
     * Loads a specific track from the Database and displays it over the Map.
     */
    private class LoadTrack extends AsyncTask<Tour, TrackOverlay, GeoPoint>{

        @Override
        protected void onPreExecute(){
            progress.show();
        }

        @Override
        protected GeoPoint doInBackground(Tour... tours) {
            try {
                Dao<LocationStamp, Void> location_dao = TrackMapActivity.this.getHelper().getLocationStampDao();
                List<LocationStamp> stamps = location_dao.queryForEq("tour_id", tours[0].getId());
                if (stamps.size() == 0) return null;
                // Create overlay:
                TrackOverlay overlay = new TrackOverlay();
                overlay.addAll(stamps);
                this.publishProgress(overlay);
                return new GeoPoint(stamps.get(0).getLatitudeE6(), stamps.get(0).getLongitudeE6());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(TrackOverlay... overlay){
            // Display overlay:
            map.getOverlays().add(overlay[0]);
        }

        @Override
        protected void onPostExecute(GeoPoint start){
            if (start != null){
                // TODO When entering, Zoom out to see the FULL track.
                // Show the start-point:
                MapController controller = map.getController();
                controller.animateTo(start);
                controller.setZoom(19);
            }
            progress.dismiss();
        }
    }

    /**
     * The overlay-class to draw the actual track on the map.
     */
    private class TrackOverlay extends Overlay {

        private final ArrayList<LocationStamp> stamps;

        public TrackOverlay(){
            stamps = new ArrayList<LocationStamp>();
        }

        public void addAll(Collection<LocationStamp> coll){
            if (coll == null)
                throw new NullPointerException("Collection can't be null!");
            stamps.addAll(coll);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow){
            super.draw(canvas, mapView, shadow);
            if (shadow) return; // We don't draw shadow! (Yet)
            // Draw all the points:
            Projection projection = mapView.getProjection();
            Path track = new Path();
            // Set the start-point:
            Point p = new Point();
            projection.toPixels(new GeoPoint(
                    stamps.get(0).getLatitudeE6(), stamps.get(0).getLongitudeE6()
            ), p);
            track.moveTo((float)p.x, (float)p.y);
            // Itterate:
            for (int i = 1; i < stamps.size(); i++){
                projection.toPixels(new GeoPoint(
                        stamps.get(i).getLatitudeE6(), stamps.get(i).getLongitudeE6()
                ), p);
                // add the next line-point:
                track.lineTo((float)p.x, (float)p.y);
            }
            // Draw:
            canvas.drawPath(track, track_paint);
        }
    }

    /*
        -------------- Database ORMlite stuff ---------------
        See http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html#SEC40
     */

    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }
}
