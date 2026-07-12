package dev.loat.msmp_world.msmp.exceptions;

/**
 * Thrown when a block id cannot be found in the block registry.
 */
public class UnknownBlockException extends MSMPException {

    /**
     * @param blockId The block id that could not be resolved
     */
    public UnknownBlockException(String blockId) {
        super("Unknown block: " + blockId);
    }
}
