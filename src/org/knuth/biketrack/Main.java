package org.knuth.biketrack;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
        tour_adapter = new ArrayAdapter<Tour>(this, android.R.layout.simple_list_item_multiple_choice);
        tour_list.setAdapter(tour_adapter);
        tour_list.setOnItemClickListener(tour_click);
        tour_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        tour_list.setMultiChoiceModeListener(contextListener);
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
     * Implement contextual functionality for the list of tours.
     */
    private AbsListView.MultiChoiceModeListener contextListener =
            new AbsListView.MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(
                ActionMode actionMode, int position, long id, boolean checked) {
            // Do something when items are de-/ selected.
            actionMode.setTitle(tour_list.getCheckedItemCount()+" selected");
            // Don't allow renaming multiple entries:
            if (tour_list.getCheckedItemCount() > 1){
                actionMode.getMenu().findItem(R.id.main_context_rename).setVisible(false);
            } else {
                actionMode.getMenu().findItem(R.id.main_context_rename).setVisible(true);
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            // Infalte the Menu for the contextual choice:
            actionMode.getMenuInflater().inflate(R.menu.main_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            // Used to refresh a menu. Not implemented as of now.
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
            // Get all selected items:
            List<Tour> selected_tours = new ArrayList<Tour>();
            for (int i = 0; i < tour_adapter.getCount(); i++){
                if (tour_list.getCheckedItemPositions().get(i)){
                    selected_tours.add(tour_adapter.getItem(i));
                }
            }
            // Action was clicked:
            switch (item.getItemId()){
                case R.id.main_context_delete:
                    // Delete the selected items.
                    showDeleteDialog(selected_tours);
                    actionMode.finish();
                    return true;
                case R.id.main_context_rename:
                    // Rename one selected item.
                    showRenameDialog(selected_tours.get(0));
                    actionMode.finish();
                    return true;
                default:
                    // Unsupported action:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            // Left contextual menu.
        }
    };

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

    /**
     * Show the dialog to delete one or multiple tours.
     * @param tours the tours which are selected to be deleted.
     */
    private void showDeleteDialog(final List<Tour> tours){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setCancelable(true).setTitle("Delete Tours").
                setMessage("Are you sure that you want to delete all " +
                        tours.size() + " selected tours?").
                setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            int deleted = Main.this.getHelper().getTourDao().delete(tours);
                            Dao<LocationStamp, Void> stamp_dao = Main.this.getHelper()
                                    .getLocationStampDao();
                            // Recursive delete all LocationStamps of that tour.
                            for (Tour tour : tours) {
                                int deleted2 = stamp_dao.executeRaw("DELETE FROM loc_stamp " +
                                        "WHERE tour_id = "+tour.getId());
                                Log.v(LOG_TAG, "Deleted "+deleted2+" locationstamps from "+tour.getName());
                            }
                            if (deleted == tours.size()) {
                                Toast.makeText(Main.this, "Successfully deleted " + deleted + " tours",
                                        Toast.LENGTH_SHORT).show();
                            }
                            new LoadTours().execute();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).create();
        dialog.show();
    }

    /**
     * Shows the dialog to rename a single tour.
     * @param tour the tour to rename.
     */
    private void showRenameDialog(final Tour tour){
        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater)Main.this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.rename_tour_dialog,
                (ViewGroup) findViewById(R.id.rename_dialog_root));

        final EditText renamed_tour = (EditText)layout.findViewById(R.id.new_tour_name);

        renamed_tour.setText(tour.getName());

        builder = new AlertDialog.Builder(Main.this);
        builder.setView(layout).setCancelable(true)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (renamed_tour.getText().toString().length() < 1){
                            Toast.makeText(Main.this, "Enter a text...",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        tour.setName(renamed_tour.getText().toString());
                        try {
                            Main.this.getHelper().getTourDao().update(tour);
                            // Reload everything:
                            new LoadTours().execute();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
        builder.create().show();
    }

}
