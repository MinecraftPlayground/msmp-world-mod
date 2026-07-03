package dev.loat.msmp_world;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp.MSMPServer;
// import dev.loat.msmp_world.config.Config;
import dev.loat.msmp_world.logging.Logger;
import dev.loat.msmp_world.msmp.endpoints.Endpoints;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Main entrypoint for the MSMP World mod.
 *
 * <p>Initializes the {@code world} MSMP namespace, registers all methods
 * and notifications, and manages the server lifecycle binding.</p>
 */
public class MSMPWorld implements ModInitializer {

    /**
     * The shared {@code world} namespace used for all MSMP registrations.
     * Attached to the running server in {@code SERVER_STARTED} and detached in {@code SERVER_STOPPED}.
     */
    private static final MSMPNamespace NS = new MSMPNamespace("world");

    /**
     * Provides access to the {@link net.minecraft.server.jsonrpc.ManagementServer}
     * for broadcasting notifications. {@code null} when no server is running.
     */
    private static MSMPServer msmp;

    /**
     * Called by Fabric during mod initialization, before the server starts.
     *
     * <p>Registers all MSMP methods and notifications and sets up server
     * lifecycle hooks for attaching and detaching the namespace.</p>
     */
    @Override
    public void onInitialize() {
        Logger.setLoggerClass(MSMPWorld.class);

        // Config.register();

        Endpoints.register(NS, () -> msmp);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            NS.attach(server);
            msmp = new MSMPServer(server);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            NS.detach();
            msmp = null;
        });

        Logger.info("Mod initialized.");
    }
}
