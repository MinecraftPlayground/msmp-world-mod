package dev.loat.msmp_world.msmp.endpoints;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp.MSMPServer;

import java.util.function.Supplier;


/**
 * Central registration point for all {@code world} MSMP endpoints.
 * 
 * <p>Each endpoint is implemented in its own sub-package and registered here.</p>
 */
public class Endpoints {
    private Endpoints() {}

    /**
     * Registers all endpoints on the given {@link MSMPNamespace}.
     *
     * @param namespace The namespace to register all endpoints under
     * @param msmpServer A supplier for the MSMPServer instance, used by some endpoints to subscribe to server events
     */
    public static void register(MSMPNamespace namespace, Supplier<MSMPServer> msmpServer) {
        // MSMP world endpoint registers
    }
}
