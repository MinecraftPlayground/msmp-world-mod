package dev.loat.msmp_world.msmp.exceptions;

/**
 * Thrown when the straight-line distance between two path finding targets exceeds
 * the configured maximum (see {@code path-find.max-distance} in the config file).
 */
public class DistanceExceededException extends MSMPException {

    /**
     * @param distance  The actual straight-line distance between start and end
     * @param maxDistance The configured maximum distance
     */
    public DistanceExceededException(double distance, double maxDistance) {
        super("Distance between 'start' and 'end' (%.1f) exceeds the configured maximum of %.0f blocks"
            .formatted(distance, maxDistance));
    }
}
