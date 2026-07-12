package dev.loat.msmp_world.msmp.exceptions;

import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;


/**
 * Base exception for all MSMP world errors.
 *
 * <p>Extends {@link InvalidParameterJsonRpcException} so that Minecraft's JSON-RPC layer
 * forwards the error message directly to the client instead of swallowing it as a generic
 * internal error. All subclasses automatically benefit from this propagation.</p>
 *
 * <p>All method-specific exceptions extend this class, allowing handlers
 * to catch all MSMP errors with a single {@code catch (MSMPException e)} block.</p>
 */
public class MSMPException extends InvalidParameterJsonRpcException {

    /**
     * @param message A human-readable description of the error
     */
    public MSMPException(String message) {
        super(message);
    }
}
