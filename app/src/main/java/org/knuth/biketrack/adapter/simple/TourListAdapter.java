package org.knuth.biketrack.adapter.simple;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import org.knuth.biketrack.R;
import org.knuth.biketrack.persistent.Tour;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by Lukas on 27.03.2016.
 */
public class TourListAdapter extends ArrayAdapter<Tour> {

    private final LayoutInflater inflater;
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);

    public TourListAdapter(Context context) {
        super(context, -1);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View recycle, ViewGroup parent) {
        View v = recycle;
        if (v == null){
            v = inflater.inflate(R.layout.tour_item, parent, false);
            TextView name = (TextView) v.findViewById(R.id.tour_item_name);
            TextView date = (TextView) v.findViewById(R.id.tour_item_date);
            Holder holder = new Holder(name, date);
            v.setTag(holder);
        }
        // Populate data:
        Holder holder = (Holder) v.getTag();
        Tour tour = getItem(position);
        holder.name.setText(tour.getTitle());
        holder.date.setText(getContext().getString(R.string.main_listItem_tourDate,
                DATE_FORMAT.format(tour.getDate()), TIME_FORMAT.format(tour.getDate()))
        );
        return v;
    }

    private static class Holder{
        public final TextView name;
        public final TextView date;

        public Holder(TextView name, TextView date) {
            this.name = name;
            this.date = date;
        }
    }
}
