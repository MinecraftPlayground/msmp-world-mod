package dev.loat.msmp_world.msmp.endpoints.path_find;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;


/**
 * Response payload for the {@code world:path_find} method.
 *
 * <p>{@code found: false} is a normal, valid outcome (no path exists, or the target is
 * unreachable within the distance limit) - not an error.</p>
 *
 * <p>Example JSON representations:</p>
 * <pre>{@code
 * { "dimension": "minecraft:overworld", "found": true, "path": [[100, 64, 200], [101, 64, 201]] }
 * { "dimension": "minecraft:overworld", "found": false, "path": [] }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string
 * @param found Whether a path was found
 * @param path The waypoints from start to end as {@code [x, y, z]} entries, empty if not found
 */
public record PathFindResponse(String dimension, boolean found, List<List<Integer>> path) {

    private static final Schema<List<List<Integer>>> PATH_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf().listOf());

    /**
     * Codec for serializing and deserializing {@link PathFindResponse} instances.
     */
    public static final Codec<PathFindResponse> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(PathFindResponse::dimension),
        Codec.BOOL.fieldOf("found").forGetter(PathFindResponse::found),
        Codec.INT.listOf().listOf().fieldOf("path").forGetter(PathFindResponse::path)
    ).apply(i, PathFindResponse::new));

    /**
     * MSMP schema for {@link PathFindResponse}, used for protocol discovery.
     */
    public static final Schema<PathFindResponse> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("found", Schema.BOOL_SCHEMA)
        .withField("path", PATH_SCHEMA);
}
