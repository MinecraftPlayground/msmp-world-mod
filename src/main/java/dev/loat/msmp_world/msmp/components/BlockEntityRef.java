package dev.loat.msmp_world.msmp.components;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.jsonrpc.api.Schema;


/**
 * A single sparse block-entity entry, referenced by its flat column index within a
 * {@code world:chunk/surface}/{@code world:chunk/surface/set} payload
 * ({@code index = localX * 16 + localZ}).
 *
 * <p>Kept separate from {@code palette} so that positions sharing the same block type
 * (id + states) can still share one palette entry even when their block-entity contents
 * differ - see the design discussion for the full reasoning.</p>
 *
 * @param index The flat column index ({@code localX * 16 + localZ})
 * @param components The block-entity data, generically passed through as JSON
 */
public record BlockEntityRef(int index, JsonElement components) {

    /**
     * Codec for serializing and deserializing {@link BlockEntityRef} instances.
     */
    public static final Codec<BlockEntityRef> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("index").forGetter(BlockEntityRef::index),
        BlockRef.JSON_ELEMENT_CODEC.fieldOf("components").forGetter(BlockEntityRef::components)
    ).apply(i, BlockEntityRef::new));

    /**
     * MSMP schema for {@link BlockEntityRef}, used for protocol discovery.
     */
    public static final Schema<BlockEntityRef> SCHEMA = Schema.record(CODEC)
        .withField("index", Schema.INT_SCHEMA)
        .withField("components", Schema.ofType("object", BlockRef.JSON_ELEMENT_CODEC));
}
