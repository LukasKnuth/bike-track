package org.knuth.biketrack;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.LocationStamp;

import java.sql.SQLException;
import java.util.List;

/**
 * Description
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class DatabaseActivity extends OrmLiteBaseActivity<DatabaseHelper> {

    private ProgressDialog progress;

    private TableLayout table;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        table = new TableLayout(this);
        TableRow headlines = new TableRow(this);
        headlines.addView( makeHeadline("Date") );
        headlines.addView( makeHeadline("Latitude"));
        headlines.addView( makeHeadline("Longitude"));
        headlines.addView( makeHeadline("Speed (km/h)"));
        table.addView(headlines);

        table.setStretchAllColumns(true);
        setContentView(table);
        progress = new ProgressDialog(this);
        progress.setIndeterminate(true);
        new QueryAll().execute();
    }

    private TextView makeHeadline(String content){
        TextView view = new TextView(this);
        view.setText(content);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private class QueryAll extends AsyncTask<Void, TableRow, Void>{

        @Override
        protected void onPreExecute(){
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Dao<LocationStamp, Void> location_dao = DatabaseActivity.this.getHelper().getDao();
                List<LocationStamp> stamps = location_dao.queryForAll();
                for (LocationStamp stamp : stamps){
                    TableRow row = new TableRow(DatabaseActivity.this);
                    row.addView( makeTextView(stamp.getTimestamp().toLocaleString()) );
                    row.addView( makeTextView(stamp.getLatitudeE6()+"") );
                    row.addView( makeTextView(stamp.getLongitudeE6()+"") );
                    row.addView( makeTextView(stamp.getSpeed()+"") );
                    publishProgress(row);
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
    }

}
