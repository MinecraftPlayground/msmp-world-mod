package dev.loat.msmp_world.msmp.exceptions;

/**
 * Thrown when a method receives invalid or incomplete parameters.
 *
 * <p>Used for method-specific validation errors, such as missing required
 * fields or out-of-range values.</p>
 */
public class InvalidParamsException extends MSMPException {

    /**
     * @param message A description of which parameter is invalid and why
     */
    public InvalidParamsException(String message) {
        super(message);
    }
}
