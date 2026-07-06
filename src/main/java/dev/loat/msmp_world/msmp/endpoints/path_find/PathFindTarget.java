package dev.loat.msmp_world.msmp.endpoints.path_find;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.loat.msmp_world.msmp.components.EntityRequest;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;
import java.util.Optional;


/**
 * A path finding target – either a fixed position or an entity reference.
 *
 * <p>Exactly one of {@code position} or {@code entity} must be present per target.
 * Using an entity reference resolves to that entity's current
 * {@link net.minecraft.core.BlockPos} at the time of the call.</p>
 *
 * <p>Example JSON representations:</p>
 * <pre>{@code
 * // Fixed position
 * { "position": [100, 64, 200] }
 *
 * // Entity by UUID
 * { "entity": { "id": "069a79f4-44e9-4726-a5be-fca90e38aaf5" } }
 *
 * // Entity by player name
 * { "entity": { "name": "Steve" } }
 * }</pre>
 *
 * @param position The fixed target position as {@code [x, y, z]}, if provided
 * @param entity   The entity reference (id and/or name), if provided
 */
public record PathFindTarget(
    Optional<List<Integer>> position,
    Optional<EntityRequest> entity
) {

    private static final Schema<List<Integer>> POSITION_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    /**
     * Codec for serializing and deserializing {@link PathFindTarget} instances.
     */
    public static final Codec<PathFindTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.listOf().optionalFieldOf("position").forGetter(PathFindTarget::position),
        EntityRequest.CODEC.optionalFieldOf("entity").forGetter(PathFindTarget::entity)
    ).apply(i, PathFindTarget::new));

    /**
     * MSMP schema for {@link PathFindTarget}, used for protocol discovery.
     */
    public static final Schema<PathFindTarget> SCHEMA = Schema.record(CODEC)
        .withField("position", POSITION_SCHEMA)
        .withField("entity", EntityRequest.SCHEMA);
}
