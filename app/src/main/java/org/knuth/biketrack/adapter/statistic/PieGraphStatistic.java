package org.knuth.biketrack.adapter.statistic;

import android.view.View;
import android.widget.TextView;
import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieSlice;
import org.knuth.biketrack.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Description
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class PieGraphStatistic extends Statistic<PieSlice> {

    private final ArrayList<PieSlice> slices;

    public PieGraphStatistic(String description, PieSlice ...slices) {
        super(null, null, description);
        this.slices = new ArrayList<PieSlice>(Arrays.asList(slices));
    }

    @Override
    public int getResourceId() {
        return R.layout.statistic_item_piegraph;
    }

    @Override
    public ExpandableStatisticAdapter.ItemType getItemType() {
        return ExpandableStatisticAdapter.ItemType.PIE_GRAPH;
    }

    @Override
    public View getView(View v, boolean isLastChild) {
        TextView desc = (TextView)v.findViewById(R.id.statistic_item_view_description);
        PieGraph graph = (PieGraph)v.findViewById(R.id.statistic_item_piegraph);
        desc.setText(getDescription());
        if (graph.getSlices().size() != this.slices.size()){
            graph.setSlices(this.slices);
        }
        return v;
    }
}
