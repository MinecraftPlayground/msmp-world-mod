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
 * <p>Mirrors the shape of the {@code world:chunk/surface} response, so a GET result can be
 * modified and sent straight back (round-trip friendly): {@code palette} entries are pure
 * (id + optional states, no {@code components}); {@code heights}/{@code blocks} must each
 * contain exactly 256 entries (flat index order, {@code index = localX * 16 + localZ}).</p>
 *
 * <p><b>Sentinel values</b> (Mojang's Codec system can't represent {@code null} inline in a
 * JSON array): {@code heights[i] == Integer.MIN_VALUE} or {@code blocks[i] == -1} means
 * "leave this column untouched" - both should normally be set together.</p>
 *
 * <p>Example JSON representation (excerpt, 256 entries in reality):</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "chunk": [6, -2],
 *   "palette": [{ "id": "minecraft:grass_block" }, { "id": "minecraft:chest", "states": { "facing": "north" } }],
 *   "heights": [71, 77],
 *   "blocks": [0, 1],
 *   "blockEntities": [{ "index": 1, "components": { "Items": [] } }]
 * }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string
 * @param chunk The chunk coordinates as {@code [chunkX, chunkZ]}
 * @param palette The unique block type references referenced by {@code blocks}
 * @param heights The target Y-coordinate per column (256 entries, flat index order)
 * @param blocks The palette index per column (256 entries, flat index order)
 * @param blockEntities Sparse per-column block-entity data to apply, keyed by flat index
 */
public record ChunkSurfaceSetRequest(
    String dimension,
    List<Integer> chunk,
    List<BlockTypeRef> palette,
    List<Integer> heights,
    List<Integer> blocks,
    Optional<List<BlockEntityRef>> blockEntities
) {

    private static final Schema<List<Integer>> CHUNK_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    private static final Schema<List<BlockTypeRef>> PALETTE_SCHEMA =
        Schema.ofType("array", BlockTypeRef.CODEC.listOf());

    private static final Schema<List<Integer>> COLUMNS_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    private static final Schema<List<BlockEntityRef>> BLOCK_ENTITIES_SCHEMA =
        Schema.ofType("array", BlockEntityRef.CODEC.listOf());

    /**
     * Codec for serializing and deserializing {@link ChunkSurfaceSetRequest} instances.
     */
    public static final Codec<ChunkSurfaceSetRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(ChunkSurfaceSetRequest::dimension),
        Codec.INT.listOf().fieldOf("chunk").forGetter(ChunkSurfaceSetRequest::chunk),
        BlockTypeRef.CODEC.listOf().fieldOf("palette").forGetter(ChunkSurfaceSetRequest::palette),
        Codec.INT.listOf().fieldOf("heights").forGetter(ChunkSurfaceSetRequest::heights),
        Codec.INT.listOf().fieldOf("blocks").forGetter(ChunkSurfaceSetRequest::blocks),
        BlockEntityRef.CODEC.listOf().optionalFieldOf("blockEntities").forGetter(ChunkSurfaceSetRequest::blockEntities)
    ).apply(i, ChunkSurfaceSetRequest::new));

    /**
     * MSMP schema for {@link ChunkSurfaceSetRequest}, used for protocol discovery.
     */
    public static final Schema<ChunkSurfaceSetRequest> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("chunk", CHUNK_SCHEMA)
        .withField("palette", PALETTE_SCHEMA)
        .withField("heights", COLUMNS_SCHEMA)
        .withField("blocks", COLUMNS_SCHEMA)
        .withField("blockEntities", BLOCK_ENTITIES_SCHEMA);
}
