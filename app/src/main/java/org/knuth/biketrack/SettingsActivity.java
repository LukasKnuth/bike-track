package org.knuth.biketrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.view.MenuItem;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * <p>This activity shows and allows to change the application-wide settings.</p>
 * @author Lukas Knuth
 * @version 1.0
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    // TODO Migrate this to use the new PreferenceFragment on 3.0+: http://stackoverflow.com/q/5501431/717341

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set up the Dialog for the OpenSource Software License
        this.findPreference(this.getString(R.string.prefs_key_show_opensource_license)).
                setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AlertDialog.Builder b = new AlertDialog.Builder(SettingsActivity.this);
                        b.setMessage(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(SettingsActivity.this));
                        b.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        b.create().show();
                        return true;
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            // If the Logo in the ActionBar is pressed, simulate a "BACK"-button press.
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

}