package dev.loat.msmp_world.msmp.exceptions;

/**
 * Thrown when a provided UUID string cannot be parsed.
 */
public class InvalidUUIDException extends MSMPException {

    /**
     * @param raw The malformed UUID string
     */
    public InvalidUUIDException(String raw) {
        super("Invalid UUID: " + raw);
    }
}
