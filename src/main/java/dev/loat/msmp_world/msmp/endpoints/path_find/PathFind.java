package dev.loat.msmp_world.msmp.endpoints.path_find;

import dev.loat.msmp.MSMPNamespace;
import dev.loat.msmp_world.logging.Logger;
import dev.loat.msmp_world.msmp.components.BlockResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.List;


/**
 * Registers the {@code world:path_find} MSMP method.
 *
 * <p>Computes a path between two positions in a dimension, simulating a generic,
 * player-like ground walker (no flying, no swimming).</p>
 *
 * <p><b>Implementation approach:</b> Minecraft's pathfinding ({@code PathNavigation}/
 * {@code NodeEvaluator}) is inherently tied to a {@link net.minecraft.world.entity.Mob}
 * instance - there is no entity-free way to invoke it. This works around that by creating
 * a temporary, invisible, silent, invulnerable {@link Villager} purely to carry the mob
 * profile (collision box, movement capabilities) that the pathfinder needs. Crucially,
 * the entity is <b>never added to the world</b> - {@code PathNavigation.createPath()}
 * only reads the entity's profile plus a {@code PathNavigationRegion} built from the
 * level, not the entity's actual presence in it. Villager was chosen over e.g. Zombie
 * because it doesn't catch fire in daylight and matches player width (0.6) exactly.</p>
 *
 * <p>Example request:</p>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "world:path_find",
 *   "params": [{
 *     "dimension": "minecraft:overworld",
 *     "start": [100, 64, 200],
 *     "end": [150, 70, 230]
 *   }]
 * }
 * }</pre>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "dimension": "minecraft:overworld",
 *   "found": true,
 *   "path": [[100, 64, 200], [101, 64, 201], ...]
 * }
 * }</pre>
 */
public class PathFind {

    /**
     * Hard distance limit (straight-line, in blocks) beyond which path finding is rejected
     * outright, before even attempting the search. Minecraft's path finder is tuned for
     * short AI ranges, not long-distance route planning.
     */
    private static final double MAX_DISTANCE = 256.0;

    private PathFind() {}

    /**
     * Registers the {@code world:path_find} method on the given {@link MSMPNamespace}.
     *
     * @param namespace The namespace to register this method under
     */
    public static void register(MSMPNamespace namespace) {

        namespace.method("path_find")
            .description("Computes a path between two positions, simulating a generic player-like ground walker")
            .requestSchema(PathFindRequest.SCHEMA)
            .responseSchema(PathFindResponse.SCHEMA)
            .register((server, client, params) -> {
                try {
                    ServerLevel level = BlockResolver.resolveLevel(server, params.dimension());
                    BlockPos startPos = BlockResolver.resolvePosition(params.start());
                    BlockPos endPos = BlockResolver.resolvePosition(params.end());

                    double distance = Math.sqrt(startPos.distSqr(endPos));
                    if (distance > MAX_DISTANCE) {
                        throw new IllegalArgumentException(
                            "Distance between 'start' and 'end' (%.1f) exceeds the maximum of %.0f blocks"
                                .formatted(distance, MAX_DISTANCE)
                        );
                    }

                    Path path = computePath(level, startPos, endPos);

                    if (path == null) {
                        return new PathFindResponse(params.dimension(), false, List.of());
                    }

                    List<List<Integer>> waypoints = toWaypoints(path);

                    // Normalize: make sure the response always starts exactly at 'start',
                    // since the Path's node list doesn't necessarily include the start position.
                    List<Integer> startList = List.of(startPos.getX(), startPos.getY(), startPos.getZ());
                    if (waypoints.isEmpty() || !waypoints.get(0).equals(startList)) {
                        waypoints.add(0, startList);
                    }

                    return new PathFindResponse(params.dimension(), true, waypoints);
                } catch (IllegalArgumentException e) {
                    Logger.warning("world:path_find - " + e.getMessage());
                    throw e;
                }
            });
    }

    /**
     * Creates a temporary {@link Villager} as a mob profile for the path finder, computes
     * the path, then lets it be garbage-collected. The entity is <b>never added to the
     * world</b> - it is only used as a profile container. {@code PathNavigation.createPath}
     * reads the level via the entity's {@code level} reference (set during {@code create()}),
     * not via the entity being present in it.
     *
     * @param level The level to path find in
     * @param startPos The starting position
     * @param endPos The target position
     * @return The computed {@link Path}, or {@code null} if no path was found
     */
    private static Path computePath(ServerLevel level, BlockPos startPos, BlockPos endPos) {
        // UNCERTAIN: exact factory method/signature for creating an entity instance in your version.
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        if (villager == null) return null;

        villager.setInvisible(true);
        villager.setSilent(true);
        villager.setInvulnerable(true);

        // setPos() is the base Entity position setter, always available regardless of
        // world presence - unlike moveTo() which had different overloads across versions.
        villager.setPos(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);

        // UNCERTAIN: whether boosting FOLLOW_RANGE is actually what's needed to let
        // createPath search out to MAX_DISTANCE - if paths fail beyond a short range
        // (e.g. ~16-48 blocks), this is the first thing to check.
        AttributeInstance followRange = villager.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(MAX_DISTANCE + 16.0);
        }

        PathNavigation navigation = villager.getNavigation();
        // UNCERTAIN: createPath's exact overload/accuracy semantics for your version.
        return navigation.createPath(endPos, 0);
    }

    /**
     * Converts a {@link Path}'s nodes into a list of {@code [x, y, z]} waypoints.
     *
     * @param path The computed path
     * @return The waypoints in order from start to end
     */
    private static List<List<Integer>> toWaypoints(Path path) {
        List<List<Integer>> waypoints = new ArrayList<>();
        for (int i = 0; i < path.getNodeCount(); i++) {
            // UNCERTAIN: direct field access (node.x/y/z) vs. a possible accessor method
            // (e.g. node.asBlockPos()) for your version.
            Node node = path.getNode(i);
            waypoints.add(List.of(node.x, node.y, node.z));
        }
        return waypoints;
    }
}
