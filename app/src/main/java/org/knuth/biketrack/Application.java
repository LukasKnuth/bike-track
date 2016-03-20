package org.knuth.biketrack;

import com.facebook.stetho.Stetho;

/**
 * Created by lukas on 20.03.16.
 */
public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
    
}
