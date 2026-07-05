package dev.loat.msmp_world.msmp.endpoints.path_find;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;


/**
 * Request payload for the {@code world:path_find} method.
 *
 * <p>Both {@code start} and {@code end} accept either a fixed position or an entity
 * reference (by UUID or player name) – see {@link PathFindTarget} for details.</p>
 *
 * <p>Example JSON representations:</p>
 * <pre>{@code
 * // Position to position
 * { "dimension": "minecraft:overworld", "start": { "position": [100, 64, 200] }, "end": { "position": [150, 70, 230] } }
 *
 * // Entity to position
 * { "dimension": "minecraft:overworld", "start": { "name": "Steve" }, "end": { "position": [150, 70, 230] } }
 *
 * // Entity to entity
 * { "dimension": "minecraft:overworld", "start": { "name": "Steve" }, "end": { "id": "069a79f4-44e9-4726-a5be-fca90e38aaf5" } }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string
 * @param start The starting target (position or entity)
 * @param end The ending target (position or entity)
 */
public record PathFindRequest(String dimension, PathFindTarget start, PathFindTarget end) {

    /**
     * Codec for serializing and deserializing {@link PathFindRequest} instances.
     */
    public static final Codec<PathFindRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(PathFindRequest::dimension),
        PathFindTarget.CODEC.fieldOf("start").forGetter(PathFindRequest::start),
        PathFindTarget.CODEC.fieldOf("end").forGetter(PathFindRequest::end)
    ).apply(i, PathFindRequest::new));

    /**
     * MSMP schema for {@link PathFindRequest}, used for protocol discovery.
     */
    public static final Schema<PathFindRequest> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("start", PathFindTarget.SCHEMA)
        .withField("end", PathFindTarget.SCHEMA);
}
