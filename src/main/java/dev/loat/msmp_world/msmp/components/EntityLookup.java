package dev.loat.msmp_world.msmp.components;

import java.util.Optional;


/**
 * Common interface for request types that identify an entity by UUID or player name.
 *
 * <p>Implemented by all request records that need entity lookup via
 * {@link EntityResolver}. This allows {@link EntityResolver} to work with
 * any request type without duplicating the {@code id}/{@code name} fields.</p>
 */
public interface EntityLookup {

    /**
     * The entity's UUID as a string, if provided.
     *
     * @return The UUID string, or {@link Optional#empty()} if not provided
     */
    Optional<String> id();

    /**
     * The player's in-game name, if provided.
     * Only works for online players.
     *
     * @return The player name, or {@link Optional#empty()} if not provided
     */
    Optional<String> name();
}
