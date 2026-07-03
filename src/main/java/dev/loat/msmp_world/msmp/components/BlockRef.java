package dev.loat.msmp_world.msmp.components;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

import java.util.Map;
import java.util.Optional;


/**
 * Represents a block reference: its id, (optional) block states, and (optional) block-entity
 * data ({@code components}).
 *
 * <p>{@code states} is only present for blocks that have distinguishing state properties
 * (e.g. {@code facing}, {@code waterlogged}, ...). Simple blocks like {@code minecraft:stone}
 * omit it entirely. {@code components} is only present for blocks with a block entity
 * (Chest, Sign, Furnace, ...).</p>
 *
 * <p>Example JSON representations:</p>
 * <pre>{@code
 * { "id": "minecraft:stone" }
 * { "id": "minecraft:chest", "states": { "facing": "north" } }
 * {
 *   "id": "minecraft:chest",
 *   "states": { "facing": "north" },
 *   "components": { "Items": [ { "Slot": 0, "id": "minecraft:diamond", "count": 5 } ] }
 * }
 * }</pre>
 *
 * @param id The block's resource location as a string (e.g. {@code minecraft:chest})
 * @param states The block's states as key-value string pairs, if any
 * @param components The block-entity data, generically passed through as JSON, if any
 */
public record BlockRef(String id, Optional<Map<String, String>> states, Optional<JsonElement> components) {

    /**
     * Codec for passing {@link JsonElement} through the serialization pipeline without modification.
     * Same pattern as {@code ItemsResponse.JSON_ELEMENT_CODEC} in {@code msmp-entity-mod}.
     */
    public static final Codec<JsonElement> JSON_ELEMENT_CODEC = Codec.PASSTHROUGH.xmap(
        dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
        json -> new Dynamic<>(JsonOps.INSTANCE, json)
    );

    private static final Schema<Map<String, String>> STATES_SCHEMA =
        Schema.ofType("object", Codec.unboundedMap(Codec.STRING, Codec.STRING));

    private static final Schema<JsonElement> COMPONENTS_SCHEMA =
        Schema.ofType("object", JSON_ELEMENT_CODEC);

    /**
     * Codec for serializing and deserializing {@link BlockRef} instances.
     */
    public static final Codec<BlockRef> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("id").forGetter(BlockRef::id),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("states").forGetter(BlockRef::states),
        JSON_ELEMENT_CODEC.optionalFieldOf("components").forGetter(BlockRef::components)
    ).apply(i, BlockRef::new));

    /**
     * MSMP schema for {@link BlockRef}, used for protocol discovery.
     */
    public static final Schema<BlockRef> SCHEMA = Schema.record(CODEC)
        .withField("id", Schema.STRING_SCHEMA)
        .withField("states", STATES_SCHEMA)
        .withField("components", COMPONENTS_SCHEMA);
}
