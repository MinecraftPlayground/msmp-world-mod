package dev.loat.msmp_world.msmp.components;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Utility class for resolving dimensions, positions, and block states for MSMP block methods.
 *
 * <p><b>v0 simplification:</b> works only with whatever {@link ServerLevel#getBlockState}/
 * {@link ServerLevel#setBlock} do by default - an unloaded chunk is force-loaded (and, if
 * necessary, generated) as Vanilla's normal behavior. The more careful three-state chunk
 * handling (loaded / generated-but-inactive / not generated) described in the design spec
 * is not yet implemented here.</p>
 */
public final class BlockResolver {

    private BlockResolver() {}

    /**
     * Resolves a {@link ServerLevel} from its dimension resource key string.
     *
     * @param server The running {@link MinecraftServer} instance
     * @param dimension The dimension's resource key as a string (e.g. {@code minecraft:overworld})
     * @return The resolved {@link ServerLevel}
     * @throws IllegalArgumentException if the dimension identifier is malformed or unknown
     */
    public static ServerLevel resolveLevel(MinecraftServer server, String dimension) {
        Identifier id;
        try {
            id = Identifier.parse(dimension);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid dimension: " + dimension);
        }

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel level = server.getLevel(key);
        if (level == null) {
            throw new IllegalArgumentException("Unknown dimension: " + dimension);
        }
        return level;
    }

    /**
     * Converts a {@code [x, y, z]} list into a {@link BlockPos}.
     *
     * @param position The position as a 3-element list
     * @return The corresponding {@link BlockPos}
     * @throws IllegalArgumentException if the list does not contain exactly 3 elements
     */
    public static BlockPos resolvePosition(List<Integer> position) {
        if (position.size() != 3) {
            throw new IllegalArgumentException(
                "'position' must contain exactly 3 elements [x, y, z], got " + position.size()
            );
        }
        return new BlockPos(position.get(0), position.get(1), position.get(2));
    }

    /**
     * Converts a {@link BlockState} at a given position into a {@link BlockRef}
     * (id + states + block-entity data as strings/JSON).
     *
     * @param level The level the position belongs to (needed to look up the block entity, if any)
     * @param pos The position of the block
     * @param state The block state at that position
     * @return The corresponding {@link BlockRef}
     */
    public static BlockRef toBlockRef(ServerLevel level, BlockPos pos, BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return new BlockRef(id.toString(), extractStates(state), readComponents(level, pos));
    }

    /**
     * Reads the block-entity data at a given position, if any, as JSON.
     *
     * <p><b>Uncertain API:</b> uses {@code BlockEntity#saveWithoutMetadata(HolderLookup.Provider)}
     * - this is my best guess at the current method name/signature post Data-Components
     * migration. If this doesn't compile, this is the first place to check against the real
     * API for your version.</p>
     *
     * @param level The level the position belongs to
     * @param pos The position to check
     * @return The block-entity data as JSON, or {@link Optional#empty()} if there is none
     */
    public static Optional<JsonElement> readComponents(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return Optional.empty();

        // UNCERTAIN: exact method name for the current Minecraft version - see javadoc above.
        CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
        JsonElement json = new Dynamic<>(NbtOps.INSTANCE, tag).convert(JsonOps.INSTANCE).getValue();
        return Optional.of(json);
    }

    /**
     * Converts a {@link BlockState} into a "pure" {@link BlockTypeRef} (id + states only, no
     * block-entity data). Used for palette entries in bulk block methods.
     *
     * @param state The block state to convert
     * @return The corresponding {@link BlockTypeRef}
     */
    public static BlockTypeRef toBlockTypeRef(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return new BlockTypeRef(id.toString(), extractStates(state));
    }

    /**
     * Extracts a {@link BlockState}'s properties into a states map, shared by
     * {@link #toBlockRef} and {@link #toBlockTypeRef}.
     *
     * @param state The block state to extract states from
     * @return The states map, or {@link Optional#empty()} if the block has no states
     */
    private static Optional<Map<String, String>> extractStates(BlockState state) {
        Map<String, String> states = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            states.put(property.getName(), nameOf(state, property));
        }
        return states.isEmpty() ? Optional.empty() : Optional.of(states);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String nameOf(BlockState state, Property property) {
        return property.getName(state.getValue(property));
    }

    /**
     * Resolves a {@link BlockState} from a {@link BlockRef} (id + optional states).
     *
     * <p>Missing states fall back to the block's default state values, matching Vanilla
     * {@code /setblock} behavior.</p>
     *
     * @param ref The block reference to resolve
     * @return The resolved {@link BlockState}
     * @throws IllegalArgumentException if the block id is unknown, or a state name/value is invalid
     */
    public static BlockState resolveBlockState(BlockRef ref) {
        return resolveBlockState(ref.id(), ref.states());
    }

    /**
     * Resolves a {@link BlockState} from a {@link BlockTypeRef} (id + optional states).
     * Same resolution logic as {@link #resolveBlockState(BlockRef)}, for palette entries.
     *
     * @param ref The block type reference to resolve
     * @return The resolved {@link BlockState}
     * @throws IllegalArgumentException if the block id is unknown, or a state name/value is invalid
     */
    public static BlockState resolveBlockState(BlockTypeRef ref) {
        return resolveBlockState(ref.id(), ref.states());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState resolveBlockState(String blockId, Optional<Map<String, String>> states) {
        Identifier id;
        try {
            id = Identifier.parse(blockId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid block id: " + blockId);
        }

        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(() ->
            new IllegalArgumentException("Unknown block: " + blockId)
        );

        BlockState state = block.defaultBlockState();

        if (states.isPresent()) {
            for (Map.Entry<String, String> entry : states.get().entrySet()) {
                Property property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property == null) {
                    throw new IllegalArgumentException(
                        "Block '%s' has no state '%s'".formatted(blockId, entry.getKey())
                    );
                }

                Optional<?> value = property.getValue(entry.getValue());
                if (value.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Invalid value '%s' for state '%s' on block '%s'"
                            .formatted(entry.getValue(), entry.getKey(), blockId)
                    );
                }

                state = state.setValue(property, (Comparable) value.get());
            }
        }

        return state;
    }

    /**
     * Applies {@code components} (block-entity data) from a {@link BlockRef} to the block
     * entity already placed at the given position. No-op if {@code ref} has no
     * {@code components}.
     *
     * @param level The level the position belongs to
     * @param pos The position of the already-placed block
     * @param ref The block reference whose {@code components} should be applied
     * @throws IllegalArgumentException if {@code components} is provided but the block has no
     * block entity, or if the data is not a valid JSON object / does not load successfully
     */
    public static void applyComponents(ServerLevel level, BlockPos pos, BlockRef ref) {
        if (ref.components().isEmpty()) return;
        applyComponents(level, pos, ref.id(), ref.components().get());
    }

    /**
     * Applies {@code components} (block-entity data) to the block entity already placed at
     * the given position. Shared implementation used both for single-block
     * {@code components} ({@link #applyComponents(ServerLevel, BlockPos, BlockRef)}) and for
     * the sparse {@code blockEntities} list in bulk methods like {@code world:chunk/surface/set}.
     *
     * <p><b>Uncertain API:</b> loading the data uses {@code BlockEntity#loadWithComponents
     * (ValueInput)} via a best-guess {@code TagValueInput} construction - see chat history
     * for details, this may need adjustment for your version.</p>
     *
     * @param level The level the position belongs to
     * @param pos The position of the already-placed block
     * @param blockId The block's id, used only for error messages
     * @param components The block-entity data to apply, as JSON
     * @throws IllegalArgumentException if the block at {@code pos} has no block entity, or if
     * the data is not a valid JSON object / does not load successfully
     */
    public static void applyComponents(ServerLevel level, BlockPos pos, String blockId, JsonElement components) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            throw new IllegalArgumentException(
                "Block '%s' has no block entity, but 'components' was provided".formatted(blockId)
            );
        }

        Tag nbt;
        try {
            nbt = new Dynamic<>(JsonOps.INSTANCE, components).convert(NbtOps.INSTANCE).getValue();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid 'components' for block '%s': %s".formatted(blockId, e.getMessage())
            );
        }

        if (!(nbt instanceof CompoundTag compound)) {
            throw new IllegalArgumentException("'components' for block '%s' must be a JSON object".formatted(blockId));
        }

        try {
            // UNCERTAIN: best-guess at constructing a ValueInput from a CompoundTag - the
            // exact factory class/method name is not reliably known. See chat for details.
            net.minecraft.world.level.storage.ValueInput input =
                net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    compound
                );
            blockEntity.loadWithComponents(input);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid 'components' for block '%s': %s".formatted(blockId, e.getMessage())
            );
        }

        blockEntity.setChanged();
        BlockState current = level.getBlockState(pos);
        level.sendBlockUpdated(pos, current, current, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }
}
