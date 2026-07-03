package dev.loat.msmp_world.msmp.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.Map;
import java.util.Optional;


/**
 * Represents a pure block type reference: its id and (optional) block states - no
 * block-entity data.
 *
 * <p>Used for palette entries in bulk block methods (e.g. {@code world:chunk/surface}),
 * which are deliberately kept structural-only. Per-instance block-entity data is
 * represented separately (see {@code BlockEntityRef}) so that positions sharing the same
 * id+states can still share one palette entry even if their block-entity contents differ -
 * see the design discussion for why this split matters.</p>
 *
 * <p>Example JSON representations:</p>
 * <pre>{@code
 * { "id": "minecraft:stone" }
 * { "id": "minecraft:chest", "states": { "facing": "north" } }
 * }</pre>
 *
 * @param id The block's resource location as a string (e.g. {@code minecraft:chest})
 * @param states The block's states as key-value string pairs, if any
 */
public record BlockTypeRef(String id, Optional<Map<String, String>> states) {

    private static final Schema<Map<String, String>> STATES_SCHEMA =
        Schema.ofType("object", Codec.unboundedMap(Codec.STRING, Codec.STRING));

    /**
     * Codec for serializing and deserializing {@link BlockTypeRef} instances.
     */
    public static final Codec<BlockTypeRef> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("id").forGetter(BlockTypeRef::id),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("states").forGetter(BlockTypeRef::states)
    ).apply(i, BlockTypeRef::new));

    /**
     * MSMP schema for {@link BlockTypeRef}, used for protocol discovery.
     */
    public static final Schema<BlockTypeRef> SCHEMA = Schema.record(CODEC)
        .withField("id", Schema.STRING_SCHEMA)
        .withField("states", STATES_SCHEMA);
}
