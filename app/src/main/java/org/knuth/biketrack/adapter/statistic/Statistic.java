package org.knuth.biketrack.adapter.statistic;

import android.view.View;
import android.widget.TextView;
import org.knuth.biketrack.R;

/**
 * A single statistic with value and unit.
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class Statistic<T> {

    private final T value;
    private final String unit;
    private final String description;

    public Statistic(T value, String unit, String description){
        this.value = value;
        this.unit = unit;
        this.description = description;
    }

    public T getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    public int getResourceId(){
        return R.layout.statistic_item_view;
    }

    public ExpandableStatisticAdapter.ItemType getItemType(){
        return ExpandableStatisticAdapter.ItemType.TEXT_VIEW;
    }

    public View getView(View v, boolean isLastChild){
        TextView value = (TextView)v.findViewById(R.id.statistic_item_view_value);
        TextView unit = (TextView)v.findViewById(R.id.statistic_item_view_unit);
        TextView desc = (TextView)v.findViewById(R.id.statistic_item_view_description);
        value.setText(getValue().toString());
        unit.setText(getUnit());
        desc.setText(getDescription());
        return v;
    }
}
