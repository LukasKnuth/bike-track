package org.knuth.biketrack.adapter.statistic;

import java.util.ArrayList;
import java.util.List;

/**
 * Description
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class StatisticGroup {

    private final List<Statistic> statistics;
    private final String group_name;

    /**
     * A new group of {@code Statistic}s.
     * @param group_name the name for the group.
     * @throws NullPointerException if {@code group_name} is null.
     * @throws IllegalArgumentException if {@code group_name} is empty.
     */
    public StatisticGroup(String group_name){
        if (group_name == null)
            throw new NullPointerException("[group_name] can't be null!");
        if (group_name.length() <= 0)
            throw new IllegalArgumentException("[group_name] can't be empty!");
        // Okay:
        this.group_name = group_name;
        this.statistics = new ArrayList<Statistic>(5);
    }

    public void add(Statistic stat){
        if (stat == null)
            throw new NullPointerException("Statistic can't be null!");
        // Okay:
        statistics.add(stat);
    }

    public Statistic get(int index){
        return statistics.get(index);
    }

    public String getName(){
        return this.group_name;
    }

    public int size(){
        return statistics.size();
    }
}
