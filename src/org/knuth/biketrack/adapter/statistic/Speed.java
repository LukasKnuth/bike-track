package org.knuth.biketrack.adapter.statistic;

import android.content.Context;

import java.text.DecimalFormat;

/**
 * Static helper-class to simplify working with speeds (kilometers-per-hour or miles-per-hour).
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class Speed {

    private Speed(){}

    private static final double MS_TO_MPH = 2.23693629f;
    private static final double MS_TO_KMH = 3.6f;

    private static final DecimalFormat FORMAT = new DecimalFormat("#");

    public static String formatCurrentUnit(double meters, Context context){
        return formatCurrentUnit(meters, FORMAT, context);
    }
    public static String formatCurrentUnit(double meters, DecimalFormat format, Context context){
        return format.format(toCurrentUnit(meters, context));
    }
    public static double toCurrentUnit(double meters, Context context){
        if (Distance.isMetric(context)){
            return toKmh(meters);
        } else {
            return toMph(meters);
        }
    }

    public static double toKmh(double ms){
        return ms * MS_TO_KMH;
    }
    public static double toMph(double ms){
        return ms * MS_TO_MPH;
    }

    public static String formatKmhString(double ms){
        return formatKmhString(ms, FORMAT);
    }
    public static String formatKmhString(double ms, DecimalFormat format){
        return format.format(toKmh(ms));
    }

    public static String formatMphString(double ms){
        return formatMphString(ms, FORMAT);
    }
    public static String formatMphString(double ms, DecimalFormat format){
        return format.format(toMph(ms));
    }
}
