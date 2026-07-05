package dev.loat.msmp_world.msmp.endpoints.chunk.surface;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp_world.logging.Logger;
import dev.loat.msmp_world.msmp.components.BlockEntityRef;
import dev.loat.msmp_world.msmp.components.BlockResolver;
import dev.loat.msmp_world.msmp.components.BlockTypeRef;
import dev.loat.msmp_world.msmp.components.ChunkResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Registers the {@code world:chunk/surface/set} MSMP method.
 *
 * <p>Replaces the surface block of each column in a chunk. The server determines the
 * placement Y from the current terrain automatically (via the specified or default
 * height map) - there is no need to supply heights in the request. To place blocks at a
 * specific Y-coordinate use {@code world:block/set} instead.</p>
 *
 * <p>The response includes the resolved {@code heights} as confirmation of where each
 * block was actually placed.</p>
 *
 * <p><b>v0 scope</b> - deferred to a later iteration: every touched position always gets a
 * full neighbor/physics update ({@code Block.UPDATE_ALL}). For 256 positions in a tight
 * loop this could be noticeably more expensive than for a single block (e.g. cascading
 * redstone updates) - suppressing updates during the bulk pass and doing a single final
 * update afterward would be a natural follow-up optimization. Only already-loaded chunks
 * are properly handled, same caveat as the other block methods.</p>
 *
 * <p>Validation happens in two passes before any world mutation: all palette entries are
 * resolved upfront, and all {@code blockEntities} references are checked against
 * {@code blocks} - so a bad entry fails the whole request rather than leaving a
 * half-applied chunk.</p>
 *
 * <p>Example request:</p>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "world:chunk/surface/set",
 *   "params": [{
 *     "dimension": "minecraft:overworld",
 *     "chunk": [6, -2],
 *     "palette": [{ "id": "minecraft:sand" }],
 *     "blocks": [0, 0, 0, ... 256 entries total ]
 *   }]
 * }
 * }</pre>
 */
public class ChunkSurfaceSet {

    private ChunkSurfaceSet() {}

    /**
     * Registers the {@code world:chunk/surface/set} method on the given {@link MSMPNamespace}.
     *
     * @param namespace The namespace to register this method under
     */
    public static void register(MSMPNamespace namespace) {

        namespace.method("chunk/surface/set")
            .description("Bulk-places blocks across all 256 columns of a chunk using a compact palette format")
            .requestSchema(ChunkSurfaceSetRequest.SCHEMA)
            .responseSchema(ChunkSurfaceResponse.SCHEMA)
            .register((server, client, params) -> {
                try {
                    ServerLevel level = BlockResolver.resolveLevel(server, params.dimension());
                    int[] chunkCoords = ChunkResolver.resolveChunkCoords(params.chunk());
                    int chunkX = chunkCoords[0];
                    int chunkZ = chunkCoords[1];

                    List<Integer> blocks = params.blocks();

                    if (blocks.size() != 256) {
                        throw new IllegalArgumentException(
                            "'blocks' must contain exactly 256 entries, got " + blocks.size()
                        );
                    }

                    // Derive placement heights from the current terrain surface.
                    String heightmapName = params.heightMap().orElse(ChunkSurface.DEFAULT_HEIGHTMAP);
                    Heightmap.Types heightmapType = ChunkSurface.resolveHeightmapType(heightmapName);
                    LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                    int minY = level.dimensionType().minY();

                    List<Integer> heights = new ArrayList<>(256);
                    for (int index = 0; index < 256; index++) {
                        if (blocks.get(index) == ChunkSurface.NO_PALETTE_INDEX) {
                            heights.add(ChunkSurface.NO_HEIGHT);
                            continue;
                        }
                        int localX = ChunkResolver.localX(index);
                        int localZ = ChunkResolver.localZ(index);
                        int rawHeight = chunk.getHeight(heightmapType, localX, localZ);
                        heights.add(rawHeight <= minY ? ChunkSurface.NO_HEIGHT : rawHeight - 1);
                    }

                    List<BlockTypeRef> palette = params.palette();

                    // Pass 1: resolve every palette entry upfront, before touching the world,
                    // so a bad entry fails the whole request rather than leaving a half-applied chunk.
                    List<BlockState> resolvedPalette = new ArrayList<>(palette.size());
                    for (BlockTypeRef typeRef : palette) {
                        resolvedPalette.add(BlockResolver.resolveBlockState(typeRef));
                    }

                    List<BlockEntityRef> blockEntities = params.blockEntities().orElse(List.of());
                    for (BlockEntityRef ber : blockEntities) {
                        if (ber.index() < 0 || ber.index() >= 256) {
                            throw new IllegalArgumentException(
                                "blockEntities references invalid index " + ber.index()
                            );
                        }
                        if (blocks.get(ber.index()) == ChunkSurface.NO_PALETTE_INDEX) {
                            throw new IllegalArgumentException(
                                "blockEntities references index %d, but that column is marked as skipped"
                                    .formatted(ber.index())
                            );
                        }
                    }

                    // Pass 2: place all non-skipped blocks.
                    for (int index = 0; index < 256; index++) {
                        int height = heights.get(index);
                        int paletteIndex = blocks.get(index);

                        if (height == ChunkSurface.NO_HEIGHT || paletteIndex == ChunkSurface.NO_PALETTE_INDEX) {
                            continue;
                        }
                        if (paletteIndex < 0 || paletteIndex >= resolvedPalette.size()) {
                            throw new IllegalArgumentException(
                                "blocks[%d] references invalid palette index %d".formatted(index, paletteIndex)
                            );
                        }

                        int localX = ChunkResolver.localX(index);
                        int localZ = ChunkResolver.localZ(index);
                        BlockPos pos = new BlockPos(chunkX * 16 + localX, height, chunkZ * 16 + localZ);

                        level.setBlock(pos, resolvedPalette.get(paletteIndex), Block.UPDATE_ALL);
                    }

                    // Pass 3: apply block-entity data, now that every block is placed.
                    for (BlockEntityRef ber : blockEntities) {
                        int localX = ChunkResolver.localX(ber.index());
                        int localZ = ChunkResolver.localZ(ber.index());
                        int height = heights.get(ber.index());
                        BlockPos pos = new BlockPos(chunkX * 16 + localX, height, chunkZ * 16 + localZ);

                        String blockId = palette.get(blocks.get(ber.index())).id();
                        BlockResolver.applyComponents(level, pos, blockId, ber.components());
                    }

                    return new ChunkSurfaceResponse(
                        params.dimension(),
                        params.chunk(),
                        Optional.empty(),
                        palette,
                        heights,
                        blocks,
                        blockEntities
                    );
                } catch (IllegalArgumentException e) {
                    Logger.warning("world:chunk/surface/set - " + e.getMessage());
                    throw e;
                }
            });
    }
}
