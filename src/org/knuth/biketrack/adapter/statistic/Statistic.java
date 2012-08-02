package org.knuth.biketrack.adapter.statistic;

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
}
