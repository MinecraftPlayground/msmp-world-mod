package dev.loat.msmp_world.msmp.endpoints;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp.MSMPServer;
import dev.loat.msmp_world.msmp.endpoints.block.Block;
import dev.loat.msmp_world.msmp.endpoints.block.BlockSet;
import dev.loat.msmp_world.msmp.endpoints.chunk.surface.ChunkSurface;
import dev.loat.msmp_world.msmp.endpoints.chunk.surface.ChunkSurfaceSet;
import dev.loat.msmp_world.msmp.endpoints.path_find.PathFind;

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
        Block.register(namespace);
        BlockSet.register(namespace);

        ChunkSurface.register(namespace);
        ChunkSurfaceSet.register(namespace);

        PathFind.register(namespace);
    }
}
