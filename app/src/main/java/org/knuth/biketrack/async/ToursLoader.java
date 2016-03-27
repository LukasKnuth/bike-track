package org.knuth.biketrack.async;

/**
 * Created by Lukas on 27.03.2016.
 */

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.knuth.biketrack.persistent.DatabaseHelper;
import org.knuth.biketrack.persistent.Tour;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

/**
 * This will asynchronously load all current tours from the database.
 */
public class ToursLoader extends AsyncTaskLoader<Collection<Tour>> {

    public static final int TOUR_LOADER_ID = 2;
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