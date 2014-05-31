package org.knuth.biketrack.adapter.statistic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.knuth.biketrack.R;

import java.text.DecimalFormat;

/**
 * Static helper-class to simplify working with distances (kilometers or miles)
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class Distance {

    // This is a static helper-class!
    private Distance(){}

    /**
     * Checks whether the app is set to use the metric system or not.
     * @return {@code true} if the metric system is used, {@code false} otherwise.
     */
    static boolean isMetric(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(
                context.getString(R.string.prefs_key_system_of_measurement),
                context.getString(R.string.prefs_value_measure_system_metric)
        ).equals(context.getString(R.string.prefs_value_measure_system_metric));
    }

    private static final double METER_TO_MILE = 0.000621371192;
    private static final double METER_TO_KILOMETER = 0.001;

    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");

    public static String formatCurrentUnit(double meters, Context context){
        return formatCurrentUnit(meters, FORMAT, context);
    }
    public static String formatCurrentUnit(double meters, DecimalFormat format, Context context){
        return format.format(toCurrentUnit(meters, context));
    }
    public static double toCurrentUnit(double meters, Context context){
        if (isMetric(context)){
            return toKilometers(meters);
        } else {
            return toMiles(meters);
        }
    }

    public static double toMiles(double meters){
        return meters * METER_TO_MILE;
    }
    public static double toKilometers(double meters){
        return meters * METER_TO_KILOMETER;
    }

    public static String formatMilesString(double meters){
        return formatMilesString(meters, FORMAT);
    }
    public static String formatMilesString(double meters, DecimalFormat format){
        return format.format(toMiles(meters));
    }

    public static String formatKilometersString(double meters){
        return formatKilometersString(meters, FORMAT);
    }
    public static String formatKilometersString(double meters, DecimalFormat format){
        return format.format(toKilometers(meters));
    }

}
