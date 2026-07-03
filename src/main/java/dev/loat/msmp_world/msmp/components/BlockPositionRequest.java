package dev.loat.msmp_world.msmp.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.List;


/**
 * Common request payload for block methods that look up a position by dimension + coordinates.
 *
 * <p>Example JSON representation:</p>
 * <pre>{@code
 * { "dimension": "minecraft:overworld", "position": [100, 64, 200] }
 * }</pre>
 *
 * @param dimension The dimension's resource key as a string (e.g. {@code minecraft:overworld})
 * @param position The block position as {@code [x, y, z]}
 */
public record BlockPositionRequest(String dimension, List<Integer> position) {

    private static final Schema<List<Integer>> POSITION_SCHEMA =
        Schema.ofType("array", Codec.INT.listOf());

    /**
     * Codec for serializing and deserializing {@link BlockPositionRequest} instances.
     */
    public static final Codec<BlockPositionRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("dimension").forGetter(BlockPositionRequest::dimension),
        Codec.INT.listOf().fieldOf("position").forGetter(BlockPositionRequest::position)
    ).apply(i, BlockPositionRequest::new));

    /**
     * MSMP schema for {@link BlockPositionRequest}, used for protocol discovery.
     */
    public static final Schema<BlockPositionRequest> SCHEMA = Schema.record(CODEC)
        .withField("dimension", Schema.STRING_SCHEMA)
        .withField("position", POSITION_SCHEMA);
}
