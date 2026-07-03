package dev.loat.msmp_world.msmp.endpoints.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.loat.msmp_world.msmp.components.BlockRef;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;


/**
 * Response payload shared between {@code world:block} and {@code world:block/set}.
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
 * @param position The block position as {@code [x, y, z]}
 * @param block The block reference (id + optional states)
 */
public record BlockResponse(String dimension, List<Integer> position, BlockRef block) {

    private static final Schema<List<Integer>> POSITION_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    /**
     * Codec for serializing and deserializing {@link BlockResponse} instances.
     */
    public static final Codec<BlockResponse> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(BlockResponse::dimension),
        Codec.INT.listOf().fieldOf("position").forGetter(BlockResponse::position),
        BlockRef.CODEC.fieldOf("block").forGetter(BlockResponse::block)
    ).apply(i, BlockResponse::new));

    /**
     * MSMP schema for {@link BlockResponse}, used for protocol discovery.
     */
    public static final Schema<BlockResponse> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("position", POSITION_SCHEMA)
        .withField("block", BlockRef.SCHEMA);
}
