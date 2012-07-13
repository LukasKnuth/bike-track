package org.knuth.biketrack;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.Tour;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class Main extends OrmLiteBaseActivity<DatabaseHelper> {

    /** The Tag to use when logging from this application! */
    public static final String LOG_TAG = "BikeTrack";

    private ListView tour_list;
    private ArrayAdapter<Tour> tour_adapter;
    private ProgressDialog progress;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tour_list = (ListView)this.findViewById(R.id.tour_list);
        tour_adapter = new ArrayAdapter<Tour>(this, android.R.layout.simple_list_item_1);
        tour_list.setAdapter(tour_adapter);
        tour_list.setOnItemClickListener(tour_click);
        // Load the content a-sync:
        progress = new ProgressDialog(this);
        progress.setIndeterminate(true);
        new LoadTours().execute();
    }

    /**
     * Create a new Tour.
     */
    public void newTour(MenuItem item){
        AlertDialog.Builder builder;
        AlertDialog alertDialog;

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.new_tour_dialog,
                (ViewGroup) findViewById(R.id.tour_dialog_root));

        builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setTitle("Create a new Tour").setCancelable(true).
                setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DatePicker date = (DatePicker)layout.findViewById(R.id.tour_date);
                        EditText name = (EditText)layout.findViewById(R.id.tour_name);
                        if (name.getText().toString().length() > 0){
                            try {
                                Dao<Tour, Integer> dao = Main.this.getHelper().getTourDao();
                                dao.create(new Tour(
                                        name.getText().toString(),
                                        new Date(
                                                date.getYear(),
                                                date.getMonth(),
                                                date.getDayOfMonth()
                                        )
                                ));
                                new LoadTours().execute();
                                dialogInterface.dismiss();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(Main.this, "Insert a name!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Handle a selected item in the {@code tour_list}.
     */
    private AdapterView.OnItemClickListener tour_click = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
            Tour tour = tour_adapter.getItem(pos);
            Intent intent = new Intent(Main.this, TourActivity.class);
            intent.putExtra(TrackingService.TOUR_KEY, tour);
            Main.this.startActivity(intent);
        }
    };

    /**
     * This will asynchronously load all current tours from the database and display
     *  them in the {@code tour_list}.
     */
    private class LoadTours extends AsyncTask<Void, Void, Collection<Tour>>{

        @Override
        protected void onPreExecute(){
            progress.show();
        }

        @Override
        protected Collection<Tour> doInBackground(Void... voids) {
            try {
                Dao<Tour, Integer> tour_dao = Main.this.getHelper().getTourDao();
                return tour_dao.queryForAll();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new ArrayList<Tour>(0);
        }

        @Override
        protected void onPostExecute(Collection<Tour> tours){
            tour_adapter.clear();
            tour_adapter.addAll(tours);
            progress.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

}
