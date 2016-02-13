package org.knuth.biketrack.service;

import org.knuth.biketrack.persistent.LocationStamp;

/**
 * The callback-interface for the {@code TracingBinder}.
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public interface TrackingListener {

    /**
     * This method will be called with the generated data provided by the GPS-
     *  module, as soon as it is available.
     * @param data the data from the GPS-module as a {@link LocationStamp}.
     */
    public void update(LocationStamp data);
}
