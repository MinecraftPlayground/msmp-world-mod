package dev.loat.msmp_world.msmp.endpoints.path_find;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp_world.config.Config;
import dev.loat.msmp_world.config.files.path_find.PathFindConfig.BoundingBoyType;
import dev.loat.msmp_world.logging.Logger;
import dev.loat.msmp_world.msmp.components.BlockResolver;
import dev.loat.msmp_world.msmp.components.ChunkResolver;
import dev.loat.msmp_world.msmp.components.EntityRequest;
import dev.loat.msmp_world.msmp.exceptions.DistanceExceededException;
import dev.loat.msmp_world.msmp.exceptions.EntityNotFoundException;
import dev.loat.msmp_world.msmp.exceptions.InvalidParamsException;
import dev.loat.msmp_world.msmp.exceptions.InvalidUUIDException;
import dev.loat.msmp_world.msmp.exceptions.MSMPException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Registers the {@code world:path_find} MSMP method.
 *
 * <p>Computes a path between two positions in a dimension, simulating a generic,
 * player-like ground walker (no flying, no swimming). Both {@code start} and {@code end}
 * accept either a fixed position or an entity reference (by UUID or player name).</p>
 *
 * <p><b>Implementation approach:</b> Minecraft's pathfinding ({@code PathNavigation}/
 * {@code NodeEvaluator}) is inherently tied to a {@link net.minecraft.world.entity.Mob}
 * instance. This works around that by creating a temporary, invisible, silent,
 * invulnerable {@link Villager} with {@code NoAI} purely to carry the mob profile.
 * The entity is added to the world, the path computed, and it is immediately discarded
 * before any tick logic runs. Villager was chosen because it doesn't burn in daylight
 * and matches player width (0.6) exactly.</p>
 *
 * <p>Example requests:</p>
 * <pre>{@code
 * // Position to position
 * { "dimension": "minecraft:overworld", "start": { "position": [100, 64, 200] }, "end": { "position": [150, 70, 230] } }
 *
 * // Entity to position
 * { "dimension": "minecraft:overworld", "start": { "name": "Steve" }, "end": { "position": [150, 70, 230] } }
 *
 * // Entity to entity
 * { "dimension": "minecraft:overworld", "start": { "name": "Steve" }, "end": { "id": "069a79f4-44e9-4726-a5be-fca90e38aaf5" } }
 * }</pre>
 */
public class PathFind {

    private PathFind() {}

    /**
     * Registers the {@code world:path_find} method on the given {@link MSMPNamespace}.
     *
     * @param namespace The namespace to register this method under
     */
    public static void register(MSMPNamespace namespace) {

        namespace.method("path_find")
            .description("Computes a path between two positions or entities, simulating a generic player-like ground walker")
            .requestSchema(PathFindRequest.SCHEMA)
            .responseSchema(PathFindResponse.SCHEMA)
            .register((server, client, params) -> {
                try {
                    ServerLevel level = BlockResolver.resolveLevel(server, params.dimension());
                    BlockPos startPos = resolveTargetPos(server, params.start());
                    BlockPos endPos = resolveTargetPos(server, params.end());

                    double maxDistance = Config.getConfig().pathFind.maxDistance;
                    double distance = Math.sqrt(startPos.distSqr(endPos));
                    if (distance > maxDistance) {
                        throw new DistanceExceededException(distance, maxDistance);
                    }

                    Path path = computePath(level, startPos, endPos);

                    if (path == null || !path.canReach()) {
                        return new PathFindResponse(params.dimension(), false, List.of());
                    }

                    List<List<Integer>> waypoints = toWaypoints(path, level);

                    // Normalize start: the Path's node list doesn't necessarily include the
                    // exact start position as its first entry.
                    List<Integer> startList = List.of(startPos.getX(), startPos.getY(), startPos.getZ());
                    if (waypoints.isEmpty() || !waypoints.get(0).equals(startList)) {
                        waypoints.add(0, startList);
                    }

                    // Normalize end: the pathfinder stops one node short of the target
                    // (the final approach is implicit in Minecraft's AI), so we append the
                    // requested end position explicitly if it isn't already the last node.
                    // Only done after canReach() confirmed the path actually gets there.
                    List<Integer> endList = List.of(endPos.getX(), endPos.getY(), endPos.getZ());
                    if (waypoints.isEmpty() || !waypoints.get(waypoints.size() - 1).equals(endList)) {
                        waypoints.add(endList);
                    }

                    return new PathFindResponse(params.dimension(), true, waypoints);
                } catch (MSMPException e) {
                    Logger.warning("world:path_find - " + e.getMessage());
                    throw e;
                }
            });
    }

    /**
     * Resolves a {@link PathFindTarget} to a {@link BlockPos}.
     *
     * <p>If the target specifies a {@code position}, it is used directly. Otherwise the
     * entity is looked up via the nested {@code entity} reference (by {@code id} or
     * {@code name}) and its current block position is returned.</p>
     *
     * @param server The running {@link MinecraftServer} instance
     * @param target The target to resolve
     * @return The resolved {@link BlockPos}
     * @throws IllegalArgumentException if neither {@code position} nor {@code entity} is
     * set, the entity reference is missing both {@code id} and {@code name}, the UUID is
     * malformed, or no matching entity is found
     */
    private static BlockPos resolveTargetPos(MinecraftServer server, PathFindTarget target) {
        if (target.position().isEmpty() && target.entity().isEmpty()) {
            throw new InvalidParamsException("PathFind target must specify either 'position' or 'entity'");
        }
        
        if (target.position().isPresent()) {
            List<Integer> pos = target.position().get();
            if (pos.size() != 3) {
                throw new InvalidParamsException("'position' must contain exactly 3 elements [x, y, z], but got %d".formatted(pos.size()));
            }
            return new BlockPos(pos.get(0), pos.get(1), pos.get(2));
        }

        EntityRequest entityRef = target.entity().get();
        if (entityRef.id().isEmpty() && entityRef.name().isEmpty()) {
            throw new InvalidParamsException("'entity' must specify at least 'id' or 'name'");
        }

        Entity entity = null;

        if (entityRef.id().isPresent()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(entityRef.id().get());
            } catch (IllegalArgumentException e) {
                throw new InvalidUUIDException(entityRef.id().get());
            }
            entity = server.getPlayerList().getPlayer(uuid);
            if (entity == null) {
                for (ServerLevel level : server.getAllLevels()) {
                    entity = level.getEntity(uuid);
                    
                    if (entity != null) break;
                }
            }
        }

        if (entity == null && entityRef.name().isPresent()) {
            entity = server.getPlayerList().getPlayerByName(entityRef.name().get());
        }

        if (entity == null) {
            String identifier = entityRef.id().orElseGet(() -> entityRef.name().get());
            throw new EntityNotFoundException(identifier);
        }

        return entity.blockPosition();
    }

    /**
     * Creates a temporary {@link Zombie} as a mob profile for the path finder, computes
     * the path, then discards the entity immediately.
     *
     * @param level    The level to path find in
     * @param startPos The starting position
     * @param endPos   The target position
     * @return The computed {@link Path}, or {@code null} if no path was found
     */
    private static Path computePath(ServerLevel level, BlockPos startPos, BlockPos endPos) {
        ChunkResolver.preloadChunks(level, startPos, endPos);

        Zombie entityContainer = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        if (entityContainer == null) return null;

        if (Config.getConfig().pathFind.boundingBoxType == BoundingBoyType.SMALL) {
            entityContainer.setBaby(true);
        }
        entityContainer.setNoAi(true);
        entityContainer.setOnGround(true);
        entityContainer.setPos(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);

        try {
            double maxDistance = Config.getConfig().pathFind.maxDistance;
            AttributeInstance followRange = entityContainer.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) {
                followRange.setBaseValue(maxDistance + 16.0);
            }

            PathNavigation navigation = entityContainer.getNavigation();
            return navigation.createPath(endPos, 0);
        } finally {
            entityContainer.discard();
        }
    }

    /**
     * Converts a {@link Path}'s nodes into a list of {@code [x, y, z]} waypoints.
     *
     * @param path The computed path
     * @return The waypoints in order from start to end
     */
    private static List<List<Integer>> toWaypoints(Path path, ServerLevel level) {
        List<List<Integer>> waypoints = new ArrayList<>();

        

        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i);
            waypoints.add(List.of(node.x, node.y, node.z));

            if (Config.getConfig().pathFind.debug) {
                AreaEffectCloud cloud = EntityType.AREA_EFFECT_CLOUD.create(level, EntitySpawnReason.COMMAND);
                cloud.setRadius(0.5f);
                cloud.setDuration(200);
                cloud.setCustomParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0));
                cloud.setPos(node.x + 0.5, node.y, node.z + 0.5);

                if (i == 0 || i == path.getNodeCount() - 1) {
                    cloud.setYRot(90);
                }

                if (i > 0) {
                    Vec2 rotation = new Vec3(node.cameFrom.x - node.x, node.cameFrom.y - node.y, node.cameFrom.z - node.z).rotation();
                    
                    cloud.setXRot(rotation.x);
                    cloud.setYRot(rotation.y);
                }

                level.addFreshEntity(cloud);
            }
        }
        return waypoints;
    }
}
