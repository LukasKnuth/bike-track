package org.knuth.biketrack.adapter.statistic;

import android.view.View;
import android.widget.TextView;
import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import org.knuth.biketrack.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Description
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class LineGraphStatistic extends Statistic<Line> {

    private final float y_max;
    private final ArrayList<Line> lines;

    public LineGraphStatistic(String description, float y_max, Line ...lines) {
        super(null, null, description);
        this.y_max = y_max;
        this.lines = new ArrayList<Line>(Arrays.asList(lines));
    }

    @Override
    public int getResourceId() {
        return R.layout.statistic_item_linegraph;
    }

    @Override
    public ExpandableStatisticAdapter.ItemType getItemType() {
        return ExpandableStatisticAdapter.ItemType.LINE_GRAPH;
    }

    @Override
    public View getView(View v, boolean isLastChild) {
        TextView desc = (TextView)v.findViewById(R.id.statistic_item_view_description);
        LineGraph graph = (LineGraph)v.findViewById(R.id.statistic_item_linegraph);
        desc.setText(getDescription());
        // Show a line-graph:
        if (graph.getLines().size() != this.lines.size()){
            graph.setLines(this.lines);
        }
        graph.setRangeY(0, y_max);
        graph.setLineToFill(0);
        graph.setLineToFill(1);
        return v;
    }
}
