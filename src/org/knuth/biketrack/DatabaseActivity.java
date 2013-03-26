package org.knuth.biketrack;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;
import org.knuth.biketrack.service.TrackingService;

import java.sql.SQLException;
import java.util.List;

/**
 * Description
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class DatabaseActivity extends BaseActivity {

    private ProgressDialog progress;
    private TableLayout table;
    private AsyncTask loading_task;

    private Tour current_tour;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        table = new TableLayout(this);
        TableRow headlines = new TableRow(this);
        headlines.addView( makeHeadline("Latitude"));
        headlines.addView( makeHeadline("Longitude"));
        headlines.addView( makeHeadline("Altitude") );
        headlines.addView( makeHeadline("Speed (km/h)"));
        table.addView(headlines);
        table.setStretchAllColumns(true);
        ScrollView table_scroll = new ScrollView(this);
        table_scroll.addView(table);
        setContentView(table_scroll);
        // Get the current tour:
        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.containsKey(TrackingService.TOUR_KEY)){
            current_tour = extras.getParcelable(TrackingService.TOUR_KEY);
        } else {
            Log.e(Main.LOG_TAG, "No tour was supplied to DatabaseActivity!");
        }
        this.setTitle("Location Stamps for '"+current_tour.toString()+"'");
        // Show the data:
        progress = new ProgressDialog(this);
        progress.setIndeterminate(true);
        progress.setTitle("Reading from the Database.");
        progress.setMessage("Cancel with [back]");
        progress.setCanceledOnTouchOutside(false);
        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (loading_task != null && !loading_task.isCancelled())
                    loading_task.cancel(false);
            }
        });
        loading_task = new QueryAll().execute(current_tour);
    }

    private TextView makeHeadline(String content){
        TextView view = new TextView(this);
        view.setText(content);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private class QueryAll extends AsyncTask<Tour, TableRow, Void>{

        @Override
        protected void onPreExecute(){
            progress.show();
        }

        @Override
        protected Void doInBackground(Tour... tours) {
            try {
                Dao<LocationStamp, Void> location_dao = DatabaseActivity.this.getHelper().getLocationStampDao();
                QueryBuilder<LocationStamp, Void> builder = location_dao.queryBuilder();
                builder.where().eq("tour_id", current_tour.getId());
                builder.orderBy("timestamp", true);
                List<LocationStamp> stamps = builder.query();
                for (LocationStamp stamp : stamps){
                    TableRow row = new TableRow(DatabaseActivity.this);
                    row.addView( makeTextView(String.valueOf(stamp.getLatitude())) );
                    row.addView( makeTextView(String.valueOf(stamp.getLongitude())) );
                    row.addView( makeTextView(String.valueOf(stamp.getAltitude())) );
                    row.addView( makeTextView(String.valueOf(stamp.getSpeed())) );
                    publishProgress(row);
                    // Check if cancelled.
                    if (isCancelled()) break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        private TextView makeTextView(String content){
            TextView textView = new TextView(DatabaseActivity.this);
            textView.setText(content);
            return textView;
        }

        @Override
        protected void onProgressUpdate(TableRow... rows){
            table.addView(rows[0]);
        }

        @Override
        protected void onPostExecute(Void voids){
            progress.dismiss();
        }

        @Override
        protected void onCancelled(){
            Toast.makeText(DatabaseActivity.this,
                    "Cancelled list creation.", Toast.LENGTH_SHORT).show();
            progress.dismiss();
        }
    }

}
