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
    private final static int DB_VERSION = 3;

    private Dao<LocationStamp, Void> cached_dao;

    public DatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION, R.raw.ormlite_config);
    }

    /**
     * Get a (cached) DAO-instance to work with {@code LocationStamp}-classes.
     * @return the (cached) DAO.
     */
    public Dao<LocationStamp, Void> getDao() throws SQLException {
        if (cached_dao == null) cached_dao = getDao(LocationStamp.class);
        return cached_dao;
    }

    @Override
    public void close() {
        super.close();
        cached_dao = null;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, LocationStamp.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource conSource, int i, int i1) {
        db.execSQL("DROP TABLE loc_stamp");
        onCreate(db, conSource);
        Log.v(Main.LOG_TAG, "Recreated the DB");
    }
}
