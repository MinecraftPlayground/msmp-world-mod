package dev.loat.msmp_world.msmp.endpoints.block;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp_world.logging.Logger;
import dev.loat.msmp_world.msmp.components.BlockPositionRequest;
import dev.loat.msmp_world.msmp.components.BlockResolver;
import dev.loat.msmp_world.msmp.exceptions.MSMPException;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;


/**
 * Registers the {@code world:block} MSMP method.
 *
 * <p>Returns the block at a given position in a given dimension.</p>
 *
 * <p><b>v0 scope</b> - deferred to a later iteration (see the design spec): only
 * already-loaded chunks are properly handled - an unloaded chunk is force-loaded (and, if
 * necessary, generated) by Vanilla's normal {@link ServerLevel#getBlockState} behavior
 * rather than the more careful three-state handling described in the spec.</p>
 *
 * <p>Example request:</p>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "world:block",
 *   "params": [{ "dimension": "minecraft:overworld", "position": [100, 64, 200] }]
 * }
 * }</pre>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "position": [100, 64, 200],
 *   "block": { "id": "minecraft:chest", "states": { "facing": "north" } }
 * }
 * }</pre>
 */
public class Block {

    private Block() {}

    /**
     * Registers the {@code world:block} method on the given {@link MSMPNamespace}.
     *
     * @param namespace The namespace to register this method under
     */
    public static void register(MSMPNamespace namespace) {

        namespace.method("block")
            .description("Returns the block at a given position in a given dimension")
            .requestSchema(BlockPositionRequest.SCHEMA)
            .responseSchema(BlockResponse.SCHEMA)
            .register((server, client, params) -> {
                try {
                    ServerLevel level = BlockResolver.resolveLevel(server, params.dimension());
                    BlockPos pos = BlockResolver.resolvePosition(params.position());
                    BlockState state = level.getBlockState(pos);

                    return new BlockResponse(params.dimension(), params.position(), BlockResolver.toBlockRef(level, pos, state));
                } catch (MSMPException e) {
                    Logger.warning("world:block - " + e.getMessage());
                    throw e;
                }
            });
    }
}
