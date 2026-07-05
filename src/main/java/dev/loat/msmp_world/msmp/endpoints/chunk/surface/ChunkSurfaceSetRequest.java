package dev.loat.msmp_world.msmp.endpoints.chunk.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.loat.msmp_world.msmp.components.BlockEntityRef;
import dev.loat.msmp_world.msmp.components.BlockTypeRef;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;
import java.util.Optional;


/**
 * Request payload for the {@code world:chunk/surface/set} method.
 *
 * <p>Replaces the surface block of each column in a chunk. The server determines the
 * placement Y automatically from the current terrain (using the height map specified in
 * {@code heightMap}, defaulting to {@code MOTION_BLOCKING_NO_LEAVES}) – there is no need
 * to supply heights explicitly. To place blocks at a specific Y-coordinate, use
 * {@code world:block/set} instead.</p>
 *
 * <p>{@code palette} entries are pure (id + optional states, no {@code components});
 * {@code blocks} must contain exactly 256 entries (flat index order,
 * {@code index = localX * 16 + localZ}).</p>
 *
 * <p><b>Sentinel value</b> for {@code blocks[i] == -1}: leave this column untouched.</p>
 *
 * <p>Example – paint all surface blocks of a chunk with sand:</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "chunk": [6, -2],
 *   "palette": [{ "id": "minecraft:sand" }],
 *   "blocks": [0, 0, 0, ... 256 entries total ]
 * }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string
 * @param chunk The chunk coordinates as {@code [chunkX, chunkZ]}
 * @param heightMap Which height map to use when deriving placement Y (optional,
 * defaults to {@code MOTION_BLOCKING_NO_LEAVES})
 * @param palette The unique block type references referenced by {@code blocks}
 * @param blocks The palette index per column (256 entries, flat index order; -1 = skip)
 * @param blockEntities Sparse per-column block-entity data to apply, keyed by flat index
 */
public record ChunkSurfaceSetRequest(
    String dimension,
    List<Integer> chunk,
    Optional<String> heightMap,
    List<BlockTypeRef> palette,
    List<Integer> blocks,
    Optional<List<BlockEntityRef>> blockEntities
) {

    private static final Schema<List<Integer>> CHUNK_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    private static final Schema<List<BlockTypeRef>> PALETTE_SCHEMA =
        Schema.ofType("array", BlockTypeRef.CODEC.listOf());

    private static final Schema<List<Integer>> BLOCKS_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    private static final Schema<List<BlockEntityRef>> BLOCK_ENTITIES_SCHEMA =
        Schema.ofType("array", BlockEntityRef.CODEC.listOf());

    /**
     * Codec for serializing and deserializing {@link ChunkSurfaceSetRequest} instances.
     */
    public static final Codec<ChunkSurfaceSetRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(ChunkSurfaceSetRequest::dimension),
        Codec.INT.listOf().fieldOf("chunk").forGetter(ChunkSurfaceSetRequest::chunk),
        Codec.STRING.optionalFieldOf("heightMap").forGetter(ChunkSurfaceSetRequest::heightMap),
        BlockTypeRef.CODEC.listOf().fieldOf("palette").forGetter(ChunkSurfaceSetRequest::palette),
        Codec.INT.listOf().fieldOf("blocks").forGetter(ChunkSurfaceSetRequest::blocks),
        BlockEntityRef.CODEC.listOf().optionalFieldOf("blockEntities").forGetter(ChunkSurfaceSetRequest::blockEntities)
    ).apply(i, ChunkSurfaceSetRequest::new));

    /**
     * MSMP schema for {@link ChunkSurfaceSetRequest}, used for protocol discovery.
     */
    public static final Schema<ChunkSurfaceSetRequest> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("chunk", CHUNK_SCHEMA)
        .withField("heightMap", Schema.STRING_SCHEMA)
        .withField("palette", PALETTE_SCHEMA)
        .withField("blocks", BLOCKS_SCHEMA)
        .withField("blockEntities", BLOCK_ENTITIES_SCHEMA);
}
