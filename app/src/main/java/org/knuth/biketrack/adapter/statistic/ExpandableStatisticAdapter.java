package org.knuth.biketrack.adapter.statistic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import org.knuth.biketrack.R;

import java.util.List;

/**
 * The data-binding adapter for the statistics list on the {@code TourActivity}.
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class ExpandableStatisticAdapter extends BaseExpandableListAdapter{

    public enum ItemType{
        TEXT_VIEW(0), LINE_GRAPH(1), BAR_GRAPH(2), PIE_GRAPH(3);

        private final int id;
        private ItemType(int id){
            this.id = id;
        }
    }

    private final List<StatisticGroup> data;
    private final Context context;

    private final LayoutInflater inflater;

    public ExpandableStatisticAdapter(Context context, List<StatisticGroup> data){
        this.data = data;
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getGroupCount() {
        return data.size();
    }

    @Override
    public int getChildrenCount(int group_pos) {
        return data.get(group_pos).size();
    }

    @Override
    public Object getGroup(int group_pos) {
        return data.get(group_pos);
    }

    @Override
    public Object getChild(int group_pos, int child_pos) {
        return data.get(group_pos).get(child_pos);
    }

    @Override
    public long getGroupId(int group_pos) {
        return group_pos;
    }

    @Override
    public long getChildId(int group_pos, int child_pos) {
        return child_pos;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(
            int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View v;
        if (convertView != null){
            v = convertView;
        } else {
            v = inflater.inflate(R.layout.statistic_group_view, parent, false);
        }
        // Bind the data:
        TextView value = (TextView)v.findViewById(R.id.statistic_group_view_name);
        value.setText( data.get(groupPosition).getName() );
        return  v;
    }

    @Override
    public View getChildView(
            int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        View v;
        Statistic stat = data.get(groupPosition).get(childPosition);

        if (convertView != null){
            v = convertView;
        } else {
            v = inflater.inflate(stat.getResourceId(), parent, false);
        }
        // Bind the data:
        return stat.getView(v, isLastChild);
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        return data.get(groupPosition).get(childPosition).getItemType().id;
    }

    @Override
    public int getChildTypeCount() {
        return ItemType.values().length;
    }

    @Override
    public boolean isChildSelectable(int group_pos, int child_pos) {
        return false;
    }
}
