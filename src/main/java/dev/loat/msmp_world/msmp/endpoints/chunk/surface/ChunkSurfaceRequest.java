package dev.loat.msmp_world.msmp.endpoints.chunk.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;
import java.util.Optional;


/**
 * Request payload for the {@code world:chunk/surface} method.
 *
 * <p>Example JSON representation:</p>
 * <pre>{@code
 * { "dimension": "minecraft:overworld", "chunk": [6, -2], "heightmap": "MOTION_BLOCKING_NO_LEAVES" }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string
 * @param chunk The chunk coordinates as {@code [chunkX, chunkZ]}
 * @param heightMap Which heightmap definition to use - one of {@code WORLD_SURFACE},
 * {@code MOTION_BLOCKING}, {@code MOTION_BLOCKING_NO_LEAVES} (default if omitted),
 * {@code OCEAN_FLOOR}
 */
public record ChunkSurfaceRequest(String dimension, List<Integer> chunk, Optional<String> heightMap) {

    private static final Schema<List<Integer>> CHUNK_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    /**
     * Codec for serializing and deserializing {@link ChunkSurfaceRequest} instances.
     */
    public static final Codec<ChunkSurfaceRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(ChunkSurfaceRequest::dimension),
        Codec.INT.listOf().fieldOf("chunk").forGetter(ChunkSurfaceRequest::chunk),
        Codec.STRING.optionalFieldOf("heightMap").forGetter(ChunkSurfaceRequest::heightMap)
    ).apply(i, ChunkSurfaceRequest::new));

    /**
     * MSMP schema for {@link ChunkSurfaceRequest}, used for protocol discovery.
     */
    public static final Schema<ChunkSurfaceRequest> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("chunk", CHUNK_SCHEMA)
        .withField("heightMap", Schema.STRING_SCHEMA);
}
