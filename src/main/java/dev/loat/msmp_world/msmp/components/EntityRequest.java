package dev.loat.msmp_world.msmp.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.Optional;


/**
 * Common request payload for methods that look up an entity by UUID or player name.
 *
 * <p>At least one of {@code id} or {@code name} must be present.
 * UUID lookup works for all entity types; name lookup is limited to online players.</p>
 *
 * <p>Example JSON representations:</p>
 * <pre>{@code
 * { "id": "069a79f4-44e9-4726-a5be-fca90e38aaf5" } // any entity by UUID
 * { "name": "Steve" } // player by name
 * }</pre>
 *
 * @param id   The entity's UUID as a string, if provided
 * @param name The player's in-game name, if provided (only works for online players)
 */
public record EntityRequest(Optional<String> id, Optional<String> name) implements EntityLookup {

    /**
     * Codec for serializing and deserializing {@link EntityRequest} instances.
     * Both fields are optional.
     */
    public static final Codec<EntityRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("id").forGetter(EntityRequest::id),
        Codec.STRING.optionalFieldOf("name").forGetter(EntityRequest::name)
    ).apply(i, EntityRequest::new));

    /**
     * MSMP schema for {@link EntityRequest}, used for protocol discovery.
     */
    public static final Schema<EntityRequest> SCHEMA = Schema.record(CODEC)
        .withField("id", Schema.STRING_SCHEMA)
        .withField("name", Schema.STRING_SCHEMA);
}
