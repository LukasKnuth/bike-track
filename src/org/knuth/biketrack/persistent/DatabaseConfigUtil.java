package org.knuth.biketrack.persistent;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

/**
 * <p>The db-config util, used to write configs for faster DEO creation on
 *  Android.</p>
 * <p>See <a href="http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html#SEC41">
 *     The ORMlite documentation</a></p>
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class DatabaseConfigUtil extends OrmLiteConfigUtil {

    private static final Class<?>[] classes = new Class[] {
            LocationStamp.class,
    };

    public static void main(String[] args) throws Exception {
        writeConfigFile("ormlite_config.txt", classes);
    }
}
