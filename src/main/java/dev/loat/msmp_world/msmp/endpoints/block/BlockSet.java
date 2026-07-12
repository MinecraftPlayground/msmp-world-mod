package dev.loat.msmp_world.msmp.endpoints.block;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp_world.logging.Logger;
import dev.loat.msmp_world.msmp.components.BlockResolver;
import dev.loat.msmp_world.msmp.exceptions.MSMPException;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;


/**
 * Registers the {@code world:block/set} MSMP method.
 *
 * <p>Sets the block at a given position in a given dimension.</p>
 *
 * <p><b>v0 scope</b> - deferred to a later iteration (see the design spec): no placement
 * {@code mode} (always behaves like the spec's {@code "replace"} default, with neighbor/
 * physics updates always enabled, i.e. always {@code updateNeighbors: true}), and only
 * already-loaded chunks are properly handled - an unloaded chunk is force-loaded (and, if
 * necessary, generated) by Vanilla's normal {@link ServerLevel#setBlock} behavior rather
 * than the more careful three-state handling described in the spec.</p>
 *
 * <p>Example request:</p>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "world:block/set",
 *   "params": [{
 *     "dimension": "minecraft:overworld",
 *     "position": [100, 64, 200],
 *     "block": {
 *       "id": "minecraft:chest",
 *       "states": { "facing": "north" },
 *       "components": { "Items": [ { "Slot": 0, "id": "minecraft:diamond", "count": 5 } ] }
 *     }
 *   }]
 * }
 * }</pre>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "position": [100, 64, 200],
 *   "block": {
 *     "id": "minecraft:chest",
 *     "states": { "facing": "north" },
 *     "components": { "Items": [ { "Slot": 0, "id": "minecraft:diamond", "count": 5 } ] }
 *   }
 * }
 * }</pre>
 */
public class BlockSet {

    private BlockSet() {}

    /**
     * Registers the {@code world:block/set} method on the given {@link MSMPNamespace}.
     *
     * <p>After setting, the block at the position is re-read and returned, confirming the
     * actual resulting state rather than echoing back the request - mirrors the pattern
     * used by {@code PositionSet}/{@code RotationSet} in {@code msmp-entity-mod}. If
     * {@code components} were provided, they are applied to the block entity right after
     * placement, before re-reading.</p>
     *
     * @param namespace The namespace to register this method under
     */
    public static void register(MSMPNamespace namespace) {

        namespace.method("block/set")
            .description("Sets the block at a given position in a given dimension")
            .requestSchema(BlockSetRequest.SCHEMA)
            .responseSchema(BlockResponse.SCHEMA)
            .register((server, client, params) -> {
                try {
                    ServerLevel level = BlockResolver.resolveLevel(server, params.dimension());
                    BlockPos pos = BlockResolver.resolvePosition(params.position());
                    BlockState state = BlockResolver.resolveBlockState(params.block());

                    // Fully-qualified: this package already has its own "Block" class (the
                    // world:block endpoint), so net.minecraft...Block cannot be imported here.
                    level.setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);

                    BlockResolver.applyComponents(level, pos, params.block());

                    BlockState result = level.getBlockState(pos);
                    return new BlockResponse(params.dimension(), params.position(), BlockResolver.toBlockRef(level, pos, result));
                } catch (MSMPException e) {
                    Logger.warning("world:block/set - " + e.getMessage());
                    throw e;
                }
            });
    }
}
