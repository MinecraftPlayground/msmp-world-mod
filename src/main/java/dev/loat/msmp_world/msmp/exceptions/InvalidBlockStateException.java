package dev.loat.msmp_world.msmp.exceptions;

/**
 * Thrown when a block state key does not exist on the given block,
 * or the provided value is not valid for that state.
 */
public class InvalidBlockStateException extends MSMPException {

    /**
     * @param blockId The block id the state was applied to
     * @param key     The state key that was invalid
     * @param value   The value that was rejected, or {@code null} if the key itself was unknown
     */
    public InvalidBlockStateException(String blockId, String key, String value) {
        super(value != null
            ? "Invalid value '%s' for state '%s' on block '%s'".formatted(value, key, blockId)
            : "Block '%s' has no state '%s'".formatted(blockId, key)
        );
    }
}
