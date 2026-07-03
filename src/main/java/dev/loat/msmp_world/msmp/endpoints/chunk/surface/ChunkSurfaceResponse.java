package dev.loat.msmp_world.msmp.endpoints.chunk.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.loat.msmp_world.msmp.components.BlockEntityRef;
import dev.loat.msmp_world.msmp.components.BlockTypeRef;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;
import java.util.Optional;


/**
 * Response payload shared between {@code world:chunk/surface} and
 * {@code world:chunk/surface/set}.
 *
 * <p>{@code heightMap} is only present for {@code world:chunk/surface} (echoes which
 * heightMap type was used) - it's absent for {@code world:chunk/surface/set}, since that
 * operation places blocks at explicitly given heights rather than deriving them from a
 * heightMap. For the same reason, the {@code world:chunk/surface/set} response mirrors the
 * validated request rather than re-reading the world via a heightMap scan, to avoid a
 * confusing mismatch if a taller block happens to already exist above a position that was
 * just set (see the design discussion).</p>
 *
 * <p>{@code palette} entries are deliberately "pure" (id + optional states only) - per-position
 * block-entity data lives separately in {@code blockEntities}, keyed by the same flat column
 * index used in {@code heights}/{@code blocks} ({@code index = localX * 16 + localZ}).</p>
 *
 * <p><b>Sentinel values</b> (Mojang's Codec system can't represent {@code null} inline in a
 * JSON array): {@code heights[i] == Integer.MIN_VALUE} or {@code blocks[i] == -1} means
 * "no block at this column" (GET) or "this column was left untouched" (SET).</p>
 *
 * <p>Example JSON representation (excerpt, 256 entries in reality):</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "chunk": [6, -2],
 *   "heightMap": "MOTION_BLOCKING_NO_LEAVES",
 *   "palette": [{ "id": "minecraft:grass_block" }, { "id": "minecraft:chest", "states": { "facing": "north" } }],
 *   "heights": [71, 77],
 *   "blocks": [0, 1],
 *   "blockEntities": [{ "index": 1, "components": { "Items": [] } }]
 * }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string
 * @param chunk The chunk coordinates as {@code [chunkX, chunkZ]}
 * @param heightMap Which heightMap type was used, if applicable ({@code world:chunk/surface} only)
 * @param palette The unique block type references referenced by {@code blocks}
 * @param heights The Y-coordinate per column (256 entries, flat index order)
 * @param blocks The palette index per column (256 entries, flat index order)
 * @param blockEntities Sparse per-column block-entity data, keyed by flat index
 */
public record ChunkSurfaceResponse(
    String dimension,
    List<Integer> chunk,
    Optional<String> heightMap,
    List<BlockTypeRef> palette,
    List<Integer> heights,
    List<Integer> blocks,
    List<BlockEntityRef> blockEntities
) {

    private static final Schema<List<Integer>> CHUNK_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    private static final Schema<List<BlockTypeRef>> PALETTE_SCHEMA =
        Schema.ofType("array", BlockTypeRef.CODEC.listOf());

    private static final Schema<List<Integer>> HEIGHTS_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    private static final Schema<List<Integer>> BLOCKS_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    private static final Schema<List<BlockEntityRef>> BLOCK_ENTITIES_SCHEMA =
        Schema.ofType("array", BlockEntityRef.CODEC.listOf());

    /**
     * Codec for serializing and deserializing {@link ChunkSurfaceResponse} instances.
     */
    public static final Codec<ChunkSurfaceResponse> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(ChunkSurfaceResponse::dimension),
        Codec.INT.listOf().fieldOf("chunk").forGetter(ChunkSurfaceResponse::chunk),
        Codec.STRING.optionalFieldOf("heightMap").forGetter(ChunkSurfaceResponse::heightMap),
        BlockTypeRef.CODEC.listOf().fieldOf("palette").forGetter(ChunkSurfaceResponse::palette),
        Codec.INT.listOf().fieldOf("heights").forGetter(ChunkSurfaceResponse::heights),
        Codec.INT.listOf().fieldOf("blocks").forGetter(ChunkSurfaceResponse::blocks),
        BlockEntityRef.CODEC.listOf().fieldOf("blockEntities").forGetter(ChunkSurfaceResponse::blockEntities)
    ).apply(i, ChunkSurfaceResponse::new));

    /**
     * MSMP schema for {@link ChunkSurfaceResponse}, used for protocol discovery.
     */
    public static final Schema<ChunkSurfaceResponse> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("chunk", CHUNK_SCHEMA)
        .withField("heightMap", Schema.STRING_SCHEMA)
        .withField("palette", PALETTE_SCHEMA)
        .withField("heights", HEIGHTS_SCHEMA)
        .withField("blocks", BLOCKS_SCHEMA)
        .withField("blockEntities", BLOCK_ENTITIES_SCHEMA);
}
