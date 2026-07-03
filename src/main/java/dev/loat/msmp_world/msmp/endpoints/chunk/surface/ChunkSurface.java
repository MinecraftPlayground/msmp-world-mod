package dev.loat.msmp_world.msmp.endpoints.chunk.surface;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp_world.logging.Logger;
import dev.loat.msmp_world.msmp.components.BlockEntityRef;
import dev.loat.msmp_world.msmp.components.BlockResolver;
import dev.loat.msmp_world.msmp.components.BlockTypeRef;
import dev.loat.msmp_world.msmp.components.ChunkResolver;

import com.google.gson.JsonElement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Registers the {@code world:chunk/surface} MSMP method.
 *
 * <p>Returns the surface blocks of a chunk (all 256 columns) using the compact
 * palette/index format documented in the design spec, instead of 256 verbose per-column
 * objects.</p>
 *
 * <p><b>v0 scope</b> - deferred to a later iteration: only already-loaded chunks are
 * properly handled - an unloaded chunk is force-loaded (and, if necessary, generated) by
 * Vanilla's normal {@link ServerLevel#getChunk(int, int)} behavior rather than the more
 * careful three-state handling described in the spec.</p>
 *
 * <p>Example request:</p>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "world:chunk/surface",
 *   "params": [{ "dimension": "minecraft:overworld", "chunk": [6, -2] }]
 * }
 * }</pre>
 *
 * <p>Example response (excerpt, 256 entries in {@code heights}/{@code blocks} in reality):</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "chunk": [6, -2],
 *   "heightMap": "MOTION_BLOCKING_NO_LEAVES",
 *   "palette": [{ "id": "minecraft:grass_block" }, { "id": "minecraft:water" }],
 *   "heights": [71, 68],
 *   "blocks": [0, 1],
 *   "blockEntities": []
 * }
 * }</pre>
 */
public class ChunkSurface {

    /**
     * Default heightMap type used when the request omits {@code heightMap}.
     */
    public static final String DEFAULT_HEIGHTMAP = "MOTION_BLOCKING_NO_LEAVES";

    /**
     * Sentinel value for {@code heights[i]} meaning "no block found in this column".
     */
    public static final int NO_HEIGHT = Integer.MIN_VALUE;

    /**
     * Sentinel value for {@code blocks[i]} meaning "no block found in this column".
     */
    public static final int NO_PALETTE_INDEX = -1;

    private ChunkSurface() {}

    /**
     * Registers the {@code world:chunk/surface} method on the given {@link MSMPNamespace}.
     *
     * @param namespace The namespace to register this method under
     */
    public static void register(MSMPNamespace namespace) {

        namespace.method("chunk/surface")
            .description("Returns the surface blocks of a chunk (256 columns) using a compact palette format")
            .requestSchema(ChunkSurfaceRequest.SCHEMA)
            .responseSchema(ChunkSurfaceResponse.SCHEMA)
            .register((server, client, params) -> {
                try {
                    ServerLevel level = BlockResolver.resolveLevel(server, params.dimension());
                    int[] chunkCoords = ChunkResolver.resolveChunkCoords(params.chunk());
                    int chunkX = chunkCoords[0];
                    int chunkZ = chunkCoords[1];

                    String heightMapName = params.heightMap().orElse(DEFAULT_HEIGHTMAP);
                    Heightmap.Types heightMapType = resolveHeightmapType(heightMapName);

                    LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                    int minBuildHeight = level.dimensionType().minY();

                    List<BlockTypeRef> palette = new ArrayList<>();
                    List<Integer> heights = new ArrayList<>(256);
                    List<Integer> blocks = new ArrayList<>(256);
                    List<BlockEntityRef> blockEntities = new ArrayList<>();

                    for (int index = 0; index < 256; index++) {
                        int localX = ChunkResolver.localX(index);
                        int localZ = ChunkResolver.localZ(index);

                        // Heightmap values are "one above the surface" by convention.
                        int rawHeight = chunk.getHeight(heightMapType, localX, localZ);
                        if (rawHeight <= minBuildHeight) {
                            heights.add(NO_HEIGHT);
                            blocks.add(NO_PALETTE_INDEX);
                            continue;
                        }

                        int blockY = rawHeight;
                        BlockPos pos = new BlockPos(chunkX * 16 + localX, blockY, chunkZ * 16 + localZ);
                        BlockState state = level.getBlockState(pos);
                        BlockTypeRef typeRef = BlockResolver.toBlockTypeRef(state);

                        int paletteIndex = palette.indexOf(typeRef);
                        if (paletteIndex == -1) {
                            palette.add(typeRef);
                            paletteIndex = palette.size() - 1;
                        }

                        heights.add(blockY);
                        blocks.add(paletteIndex);

                        Optional<JsonElement> components = BlockResolver.readComponents(level, pos);
                        if (components.isPresent()) {
                            blockEntities.add(new BlockEntityRef(index, components.get()));
                        }
                    }

                    return new ChunkSurfaceResponse(
                        params.dimension(),
                        params.chunk(),
                        Optional.of(heightMapName),
                        palette,
                        heights,
                        blocks,
                        blockEntities
                    );
                } catch (IllegalArgumentException e) {
                    Logger.warning("world:chunk/surface - " + e.getMessage());
                    throw e;
                }
            });
    }

    /**
     * Resolves the requested heightMap name into a {@link Heightmap.Types}, restricted to
     * the 4 values documented in the design spec (rejecting e.g. the {@code _WG}
     * world-generation-only variants even though they exist on the real enum).
     *
     * @param name The requested heightMap name
     * @return The corresponding {@link Heightmap.Types}
     * @throws IllegalArgumentException if {@code name} is not one of the 4 allowed values
     */
    static Heightmap.Types resolveHeightmapType(String name) {
        return switch (name) {
            case "WORLD_SURFACE" -> Heightmap.Types.WORLD_SURFACE;
            case "MOTION_BLOCKING" -> Heightmap.Types.MOTION_BLOCKING;
            case "MOTION_BLOCKING_NO_LEAVES" -> Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            case "OCEAN_FLOOR" -> Heightmap.Types.OCEAN_FLOOR;
            default -> throw new IllegalArgumentException(
                "Invalid 'heightMap': '%s' - must be one of WORLD_SURFACE, MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES, OCEAN_FLOOR"
                    .formatted(name)
            );
        };
    }
}
