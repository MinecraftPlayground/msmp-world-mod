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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Registers the {@code world:chunk/surface/set} MSMP method.
 *
 * <p>Bulk-places blocks across all 256 columns of a chunk, using the same compact
 * palette/index format as {@code world:chunk/surface} - sentinel entries
 * ({@link ChunkSurface#NO_HEIGHT}/{@link ChunkSurface#NO_PALETTE_INDEX}) mean "leave this
 * column untouched". Much more efficient than 256 individual {@code world:block/set} calls.</p>
 *
 * <p>The response mirrors the validated request rather than re-reading the world via a
 * heightMap scan - see {@link ChunkSurfaceResponse} for why.</p>
 *
 * <p><b>v0 scope</b> - deferred to a later iteration: every touched position always gets a
 * full neighbor/physics update ({@code Block.UPDATE_ALL}), same as {@code world:block/set}.
 * For 256 positions in a tight loop this could be noticeably more expensive than for a
 * single block (e.g. cascading redstone updates) - suppressing updates during the bulk pass
 * and doing a single final update afterward would be a natural follow-up optimization. Only
 * already-loaded chunks are properly handled, same caveat as the other block methods.</p>
 *
 * <p>Validation happens in two passes before any world mutation: all palette entries are
 * resolved upfront, and all {@code blockEntities} references are checked against
 * {@code blocks} - so a bad entry fails the whole request rather than leaving a
 * half-applied chunk.</p>
 *
 * <p>Example request (excerpt - {@code heights}/{@code blocks} have 256 entries in reality;
 * {@code Integer.MIN_VALUE} / {@code -1} mark a skipped column):</p>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "world:chunk/surface/set",
 *   "params": [{
 *     "dimension": "minecraft:overworld",
 *     "chunk": [6, -2],
 *     "palette": [{ "id": "minecraft:grass_block" }],
 *     "heights": [71, -2147483648],
 *     "blocks": [0, -1]
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

                    List<Integer> heights = params.heights();
                    List<Integer> blocks = params.blocks();

                    if (heights.size() != 256) {
                        throw new IllegalArgumentException(
                            "'heights' must contain exactly 256 entries, got " + heights.size()
                        );
                    }
                    if (blocks.size() != 256) {
                        throw new IllegalArgumentException(
                            "'blocks' must contain exactly 256 entries, got " + blocks.size()
                        );
                    }

                    List<BlockTypeRef> palette = params.palette();

                    // Pass 1: resolve every palette entry upfront, before touching the world,
                    // so a bad entry fails the whole request rather than leaving a half-applied chunk.
                    List<BlockState> resolvedPalette = new ArrayList<>(palette.size());
                    for (BlockTypeRef typeRef : palette) {
                        resolvedPalette.add(BlockResolver.resolveBlockState(typeRef));
                    }

                    List<BlockEntityRef> blockEntities = params.blockEntities().orElse(List.of());
                    for (BlockEntityRef blockEntity : blockEntities) {
                        if (blockEntity.index() < 0 || blockEntity.index() >= 256) {
                            throw new IllegalArgumentException(
                                "blockEntities references invalid index " + blockEntity.index()
                            );
                        }
                        if (blocks.get(blockEntity.index()) == ChunkSurface.NO_PALETTE_INDEX) {
                            throw new IllegalArgumentException(
                                "blockEntities references index %d, but that column is marked as skipped"
                                    .formatted(blockEntity.index())
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
                    for (BlockEntityRef blockEntity : blockEntities) {
                        int localX = ChunkResolver.localX(blockEntity.index());
                        int localZ = ChunkResolver.localZ(blockEntity.index());
                        int height = heights.get(blockEntity.index());
                        BlockPos pos = new BlockPos(chunkX * 16 + localX, height, chunkZ * 16 + localZ);

                        String blockId = palette.get(blocks.get(blockEntity.index())).id();
                        BlockResolver.applyComponents(level, pos, blockId, blockEntity.components());
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
