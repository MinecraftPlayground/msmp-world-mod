package dev.loat.msmp_world.msmp.endpoints.path_find;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;
import java.util.Optional;


/**
 * A path finding target - either a fixed position or an entity (by UUID or player name).
 *
 * <p>Exactly one of the three fields must be present per target. Using an entity reference
 * resolves to that entity's current {@link net.minecraft.core.BlockPos} at the time of the
 * call.</p>
 *
 * <p>Example JSON representations:</p>
 * <pre>{@code
 * // Fixed position
 * { "position": [100, 64, 200] }
 *
 * // Any entity by UUID
 * { "id": "069a79f4-44e9-4726-a5be-fca90e38aaf5" }
 *
 * // Online player by name
 * { "name": "Steve" }
 * }</pre>
 *
 * @param position The fixed target position as {@code [x, y, z]}, if provided
 * @param id The entity's UUID as a string, if provided
 * @param name The player's in-game name, if provided (online players only)
 */
public record PathFindTarget(
    Optional<List<Integer>> position,
    Optional<String> id,
    Optional<String> name
) {

    private static final Schema<List<Integer>> POSITION_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    /**
     * Codec for serializing and deserializing {@link PathFindTarget} instances.
     */
    public static final Codec<PathFindTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.listOf().optionalFieldOf("position").forGetter(PathFindTarget::position),
        Codec.STRING.optionalFieldOf("id").forGetter(PathFindTarget::id),
        Codec.STRING.optionalFieldOf("name").forGetter(PathFindTarget::name)
    ).apply(i, PathFindTarget::new));

    /**
     * MSMP schema for {@link PathFindTarget}, used for protocol discovery.
     */
    public static final Schema<PathFindTarget> SCHEMA = Schema.record(CODEC)
        .withField("position", POSITION_SCHEMA)
        .withField("id", Schema.STRING_SCHEMA)
        .withField("name", Schema.STRING_SCHEMA);
}
