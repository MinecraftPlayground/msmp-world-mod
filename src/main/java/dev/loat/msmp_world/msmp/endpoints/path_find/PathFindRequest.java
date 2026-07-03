package dev.loat.msmp_world.msmp.endpoints.path_find;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;


/**
 * Request payload for the {@code world:path_find} method.
 *
 * <p>v0: path finding always simulates a generic, player-like ground walker (no flying,
 * no swimming) - there is no way yet to specify a different mover profile or reference
 * an existing entity.</p>
 *
 * <p>Example JSON representation:</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "start": [100, 64, 200],
 *   "end": [150, 70, 230]
 * }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string
 * @param start The starting position as {@code [x, y, z]}
 * @param end The target position as {@code [x, y, z]}
 */
public record PathFindRequest(String dimension, List<Integer> start, List<Integer> end) {

    private static final Schema<List<Integer>> POSITION_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    /**
     * Codec for serializing and deserializing {@link PathFindRequest} instances.
     */
    public static final Codec<PathFindRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(PathFindRequest::dimension),
        Codec.INT.listOf().fieldOf("start").forGetter(PathFindRequest::start),
        Codec.INT.listOf().fieldOf("end").forGetter(PathFindRequest::end)
    ).apply(i, PathFindRequest::new));

    /**
     * MSMP schema for {@link PathFindRequest}, used for protocol discovery.
     */
    public static final Schema<PathFindRequest> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("start", POSITION_SCHEMA)
        .withField("end", POSITION_SCHEMA);
}
