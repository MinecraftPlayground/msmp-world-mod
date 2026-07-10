package dev.loat.msmp_world.msmp.exceptions;

/**
 * Thrown when a method receives invalid or incomplete parameters.
 *
 * <p>Covers structural validation errors such as wrong array lengths,
 * missing required fields, or out-of-range index references.</p>
 */
public class InvalidParamsException extends MSMPException {

    /**
     * @param message A description of which parameter is invalid and why
     */
    public InvalidParamsException(String message) {
        super(message);
    }
}
