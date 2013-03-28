package org.knuth.biketrack;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.LocationStamp;
import org.knuth.biketrack.persistent.Tour;
import org.knuth.biketrack.service.TrackingService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Main extends BaseActivity implements LoaderManager.LoaderCallbacks<Collection<Tour>> {

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
        tour_list.setOnItemClickListener(tour_click);
        // We'll load the contextual menus, depending on the current APIs available:
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            tour_adapter = new ArrayAdapter<Tour>(this,
                    android.R.layout.simple_list_item_multiple_choice);
            makeActionbarContextMenu();
        } else {
            tour_adapter = new ArrayAdapter<Tour>(this, android.R.layout.simple_list_item_1);
            Main.this.registerForContextMenu(tour_list);
        }
        tour_list.setAdapter(tour_adapter);
        // Set the empty-view for the list:
        View empty_view = this.getLayoutInflater().inflate(R.layout.statistic_empty_view, null);
        ((ViewGroup) tour_list.getParent()).addView(empty_view); // See http://stackoverflow.com/q/3727063/717341
        tour_list.setEmptyView(empty_view);
        // Load the content a-sync:
        this.getSupportLoaderManager().initLoader(ToursLoader.TOUR_LOADER_ID, null, this);
    }

    /**
     * This will asynchronously load all current tours from the database.
     */
    private static class ToursLoader extends AsyncTaskLoader<Collection<Tour>> {

        private static final int TOUR_LOADER_ID = 2;
        private final Context context;

        public ToursLoader(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        protected void onStartLoading() {
            forceLoad(); // This seems to be a bug in the SupportLibrary.
            // See http://stackoverflow.com/q/8606048/717341
        }

        @Override
        public Collection<Tour> loadInBackground() {
            try {
                Dao<Tour, Integer> tour_dao = OpenHelperManager.getHelper(
                        context, DatabaseHelper.class
                ).getTourDao();
                QueryBuilder<Tour, Integer> builder = tour_dao.queryBuilder();
                builder.orderBy("date", false);
                return builder.query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Collections.emptyList();
        }
    }

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

    /**
     * Create a new Tour.
     */
    public void newTour(MenuItem item){
        // Simply start the TourActivity without adding a tour-extra:
        Intent tour_activity = new Intent(this, TourActivity.class);
        this.startActivity(tour_activity);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        if (v == tour_list){
            // Add the context menu for the tour-list:
            menu.add(R.string.main_contextmenu_delete)
                .setIcon(android.R.drawable.ic_menu_edit)
                .setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(android.view.MenuItem menuItem) {
                        final AdapterView.AdapterContextMenuInfo info =
                                (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
                        List<Tour> tours = new ArrayList<Tour>(1);
                        tours.add(tour_adapter.getItem(info.position));
                        showDeleteDialog(tours);
                        return true;
                    }
                });
        }
    }

    /**
     * <p>This method should only be called on pre Honeycomb devices.</p>
     * <p>It will use the original ActionBar API to create a context-menu with it.</p>
     */
    @TargetApi(11)
    private void makeActionbarContextMenu(){
        tour_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        tour_list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(
                    android.view.ActionMode actionMode, int position, long id, boolean checked) {
                // Do something when items are de-/ selected.
                actionMode.setTitle(tour_list.getCheckedItemCount()+" selected");
            }

            @Override
            public boolean onCreateActionMode(
                    android.view.ActionMode actionMode, android.view.Menu menu) {
                // Infalte the Menu for the contextual choice:
                actionMode.getMenuInflater().inflate(R.menu.main_context_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(
                    android.view.ActionMode actionMode, android.view.Menu menu) {
                // Used to refresh a menu. Not implemented as of now.
                return false;
            }

            @Override
            public boolean onActionItemClicked(
                    android.view.ActionMode actionMode, android.view.MenuItem item) {
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
                    default:
                        // Unsupported action:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode actionMode) {
                // Left contextual menu.
            }
        });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*
            NOT inflating menu currently.
            See https://github.com/JakeWharton/ActionBarSherlock/issues/562
        */
        //this.getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        menu.add(R.string.main_menu_newTour).
             setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT).
             setIcon(android.R.drawable.ic_menu_add).
             setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                newTour(item);
                return true;
            }
        });
        menu.add(R.string.main_menu_settings).
                setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW).
                setIcon(android.R.drawable.ic_menu_preferences).
                setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(){
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent i = new Intent(Main.this, SettingsActivity.class);
                        Main.this.startActivity(i);
                        return true;
                    }
                });
        return true;
    }

    /**
     * Show the dialog to delete one or multiple tours.
     * @param tours the tours which are selected to be deleted.
     */
    private void showDeleteDialog(final List<Tour> tours){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).setTitle("Delete Tours").
                setPositiveButton("Yes", new DialogInterface.OnClickListener() {
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
                                        }
                                    }

                                    @Override public void onAnimationStart(Animation animation) {}
                                    @Override public void onAnimationRepeat(Animation animation) {}
                                });
                                animate_me.startAnimation(animation);
                            }
                            if (deleted == tours.size()) {
                                Toast.makeText(Main.this, "Successfully deleted " + deleted + " tours",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        if (tours.size() > 1){
            builder.setMessage("Are you sure that you want to delete all " +
                    tours.size() + " selected tours?");
        } else {
            builder.setMessage("Are you sure that you want to delete '" +
                     tours.get(0).toString()+"' ?");
        }
        builder.create().show();
    }

}
