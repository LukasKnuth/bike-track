package org.knuth.biketrack;

import com.actionbarsherlock.app.SherlockActivity;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import org.knuth.biketrack.persistent.DatabaseHelper;

/**
 * <p>This {@code Activity}-implementation should be used through the whole project.</p>
 * <p>It offers the functionality of the following libraries:</p>
 * <ul>
 *     <li>extend {@code SherlockActivity} (from ActionBarSherlock)</li>
 *     <li>copies functionality from {@code OrmLiteBaseActivity} (from OrmLite)</li>
 * </ul>
 * @author Lukas Knuth
 * @version 1.0
 */
public class BaseActivity extends SherlockActivity {

    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    /**
     * <p>Get the Database-Helper to operate on the SQLite Database.</p>
     * <p>This functionality is copied from {@code OrmLiteBaseActivity}</p>
     * @return the {@link DatabaseHelper} to operate on the SQLite Database.
     * @see com.j256.ormlite.android.apptools.OrmLiteBaseActivity
     * @see <a href="http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html#SEC40>
     *     ORMLite Docuemntation, giving this code as the minimum requirement (point 4).</a>
     */
    protected DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }
}
