package org.knuth.biketrack.adapter.statistic;

import android.view.View;
import android.widget.TextView;
import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;
import org.knuth.biketrack.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Description
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class BarGraphStatistic extends Statistic<Bar> {

    private final ArrayList<Bar> bars;
    private final String unit;

    public BarGraphStatistic(String description, String unit, Bar ...bars) {
        super(null, null, description);
        this.unit = unit;
        this.bars = new ArrayList<Bar>(Arrays.asList(bars));
    }

    @Override
    public int getResourceId() {
        return R.layout.statistic_item_bargraph;
    }

    @Override
    public ExpandableStatisticAdapter.ItemType getItemType() {
        return ExpandableStatisticAdapter.ItemType.BAR_GRAPH;
    }

    @Override
    public View getView(View v, boolean isLastChild) {
        TextView desc = (TextView)v.findViewById(R.id.statistic_item_view_description);
        BarGraph graph = (BarGraph)v.findViewById(R.id.statistic_item_bargraph);
        desc.setText(getDescription());
        if (graph.getBars().size() != this.bars.size()){
            graph.setBars(this.bars);
            graph.setUnit(this.unit);
            graph.appendUnit(true);
        }
        return v;
    }
}
