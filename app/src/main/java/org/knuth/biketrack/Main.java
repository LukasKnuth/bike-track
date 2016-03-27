package org.knuth.biketrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.j256.ormlite.dao.Dao;
import org.knuth.biketrack.adapter.simple.TourListAdapter;
import org.knuth.biketrack.async.ToursLoader;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;
import org.knuth.biketrack.service.TrackingService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Main extends BaseActivity implements LoaderManager.LoaderCallbacks<Collection<Tour>>, AbsListView.MultiChoiceModeListener, AdapterView.OnItemClickListener {

    /** The Tag to use when logging from this application! */
    public static final String LOG_TAG = "BikeTrack";

    private ListView tour_list;
    private ArrayAdapter<Tour> tour_adapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tour_list = (ListView)this.findViewById(R.id.tour_list);
        tour_list.setOnItemClickListener(this);
        tour_list.setMultiChoiceModeListener(this);
        tour_adapter = new TourListAdapter(this);
        tour_list.setAdapter(tour_adapter);
        // Set the empty-view for the list:
        View empty_view = this.getLayoutInflater().inflate(R.layout.statistic_empty_view, null);
        ((ViewGroup) tour_list.getParent()).addView(empty_view); // See http://stackoverflow.com/q/3727063/717341
        tour_list.setEmptyView(empty_view);
        // Load the content a-sync:
        this.getSupportLoaderManager().initLoader(ToursLoader.TOUR_LOADER_ID, null, this);
    }

    // ------------------------------ LOADER -----------------------------

    @Override
    public Loader<Collection<Tour>> onCreateLoader(int id, Bundle bundle) {
        if (id == ToursLoader.TOUR_LOADER_ID){
            return new ToursLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Collection<Tour>> collectionLoader, Collection<Tour> tours) {
        if (tours.size() > 0){
            tour_adapter.clear();
            for (Tour t : tours){
                tour_adapter.add(t);
            }
        } else {
            // There are no tours, yet.
            tour_list.getEmptyView().setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Collection<Tour>> collectionLoader) {}

    // ------------------------ Action Bar, tour selected ---------------------

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.main_context_menu, menu);
        return true;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
        actionMode.setTitle(getString(R.string.main_actionbar_titleSelected, tour_list.getCheckedItemCount()));
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
        for (int i = 0; i < tour_adapter.getCount(); i++) {
            if (tour_list.getCheckedItemPositions().get(i)) {
                selected_tours.add(tour_adapter.getItem(i));
            }
        }
        // Action was clicked:
        switch (item.getItemId()) {
            case R.id.main_context_delete:
                // Delete the selected items.
                showDeleteDialog(selected_tours);
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

    // ------------------------ Action Bar normal -----------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case R.id.main_menu_new:
                // Simply start the TourActivity without adding a tour-extra:
                this.startActivity(new Intent(this, TourActivity.class));
                return true;
            case R.id.main_menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return false;
        }
    }

    /**
     * Handle a selected item in the {@code tour_list}.
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
        Tour tour = tour_adapter.getItem(pos);
        Intent intent = new Intent(Main.this, TourActivity.class);
        intent.putExtra(TrackingService.TOUR_KEY, tour);
        startActivity(intent);
    }

    /**
     * Show the dialog to delete one or multiple tours.
     * @param tours the tours which are selected to be deleted.
     */
    private void showDeleteDialog(final List<Tour> tours){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).setTitle(R.string.main_dialog_deleteTitle).
                setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            int deleted = Main.this.getHelper().getTourDao().delete(tours);
                            Dao<LocationStamp, Void> stamp_dao = Main.this.getHelper()
                                    .getLocationStampDao();
                            // Recursive delete all LocationStamps of that tour.
                            for (final Tour tour : tours) {
                                // TODO Use the PDO here!
                                int deleted2 = stamp_dao.executeRaw("DELETE FROM loc_stamp " +
                                        "WHERE tour_id = "+tour.getId());
                                Log.v(LOG_TAG, "Deleted "+deleted2+" locationstamps from "+tour.toString());
                                // Remove and animate:
                                final View animate_me = tour_list.getChildAt(tour_adapter.getPosition(tour));
                                Animation animation = AnimationUtils.loadAnimation(Main.this, android.R.anim.slide_out_right); // TODO Make it slide out LEFT
                                animation.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        if (!animation.hasEnded()){
                                            /*
                                                This is a bug in the platform. The animation is done "on-Screen", but not
                                                "really". So, cancel all animations on the view.
                                                See http://stackoverflow.com/q/4750939/717341
                                            */
                                            //
                                            animate_me.clearAnimation();
                                        } else {
                                            // The finished animation has been canceled and is no really done.
                                            tour_adapter.remove(tour);
                                            tour_list.getEmptyView().setVisibility(View.GONE);
                                            // Notify the Loader that the data has changed:
                                            Main.this.getSupportLoaderManager().
                                                    getLoader(ToursLoader.TOUR_LOADER_ID).onContentChanged();
                                        }
                                    }

                                    @Override public void onAnimationStart(Animation animation) {}
                                    @Override public void onAnimationRepeat(Animation animation) {}
                                });
                                animate_me.startAnimation(animation);
                            }
                            if (deleted == tours.size()) {
                                Toast.makeText(Main.this, getString(R.string.main_toast_deleteSuccess, deleted),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        if (tours.size() > 1){
            builder.setMessage(getString(R.string.main_dialog_deleteTours, tours.size()));
        } else {
            builder.setMessage(getString(R.string.main_dialog_deleteTour, tours.get(0).toString()));
        }
        builder.create().show();
    }

}
