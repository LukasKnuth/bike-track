package org.knuth.biketrack.persistent;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.knuth.biketrack.Main;
import org.knuth.biketrack.R;

import java.sql.SQLException;

/**
 * Description
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private final static String DB_NAME = "bike_track.db";
    private final static int DB_VERSION = 6;

    private Dao<LocationStamp, Void> location_dao;
    private Dao<Tour, Integer> tour_dao;

    public DatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION, R.raw.ormlite_config);
    }

    /**
     * Get a (cached) DAO-instance to work with {@code LocationStamp}-classes.
     * @return the (cached) DAO.
     */
    public Dao<LocationStamp, Void> getLocationStampDao() throws SQLException {
        if (location_dao == null) location_dao = getDao(LocationStamp.class);
        return location_dao;
    }

    /**
     * Get a (cached) DAO-instance to work with {@code Tour}-classes.
     * @return the (cached) DAO.
     */
    public Dao<Tour,Integer> getTourDao() throws SQLException{
        if (tour_dao == null) tour_dao = getDao(Tour.class);
        return tour_dao;
    }

    @Override
    public void close() {
        super.close();
        location_dao = null;
        tour_dao = null;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, LocationStamp.class);
            TableUtils.createTable(connectionSource, Tour.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource conSource, int i, int i1) {
        try {
            TableUtils.dropTable(conSource, LocationStamp.class, true);
            TableUtils.dropTable(conSource, Tour.class, true);
            onCreate(db, conSource);
        } catch (SQLException e){
            throw new RuntimeException(e);
        }
        Log.v(Main.LOG_TAG, "Recreated the DB");
    }
}
