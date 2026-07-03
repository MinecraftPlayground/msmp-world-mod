package dev.loat.msmp_world.msmp.endpoints.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.loat.msmp_world.msmp.components.BlockRef;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;


/**
 * Request payload for the {@code world:block/set} method.
 *
 * <p><b>v0 simplification:</b> always behaves like {@code mode: "replace"} from the full
 * design spec, with neighbor/physics updates always enabled. No placement {@code mode},
 * no {@code updateNeighbors} toggle, and no {@code components} (block-entity data) yet -
 * these are deferred to a later iteration.</p>
 *
 * <p>Example JSON representation:</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "position": [100, 64, 200],
 *   "block": { "id": "minecraft:chest", "states": { "facing": "north" } }
 * }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string
 * @param position The target block position as {@code [x, y, z]}
 * @param block The block to set (id + optional states)
 */
public record BlockSetRequest(String dimension, List<Integer> position, BlockRef block) {

    private static final Schema<List<Integer>> POSITION_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    /**
     * Codec for serializing and deserializing {@link BlockSetRequest} instances.
     */
    public static final Codec<BlockSetRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(BlockSetRequest::dimension),
        Codec.INT.listOf().fieldOf("position").forGetter(BlockSetRequest::position),
        BlockRef.CODEC.fieldOf("block").forGetter(BlockSetRequest::block)
    ).apply(i, BlockSetRequest::new));

    /**
     * MSMP schema for {@link BlockSetRequest}, used for protocol discovery.
     */
    public static final Schema<BlockSetRequest> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("position", POSITION_SCHEMA)
        .withField("block", BlockRef.SCHEMA);
}
