package dev.loat.msmp_world.msmp.exceptions;

/**
 * Base exception for all MSMP world errors.
 *
 * <p>All method-specific exceptions extend this class, allowing handlers
 * to catch all MSMP errors with a single {@code catch (MSMPException e)} block.</p>
 */
public class MSMPException extends RuntimeException {

    /**
     * @param message A human-readable description of the error
     */
    public MSMPException(String message) {
        super(message);
    }
}
