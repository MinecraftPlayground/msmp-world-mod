package dev.loat.msmp_world.msmp.components;
 
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
 
import dev.loat.msmp_world.msmp.exceptions.InvalidParamsException;
 
import java.util.List;
 
 
/**
 * Utility class for resolving chunk coordinates, the flat column index convention
 * ({@code index = localX * 16 + localZ}), and synchronous chunk pre-loading shared by
 * all chunk-aware MSMP methods (e.g. {@code world:chunk/surface},
 * {@code world:block}, {@code world:path_find}).
 */
public final class ChunkResolver {
 
    private ChunkResolver() {}
 
    /**
     * Converts a {@code [chunkX, chunkZ]} list into a {@code {chunkX, chunkZ}} int pair.
     *
     * @param chunk The chunk coordinates as a 2-element list
     * @return A 2-element array {@code {chunkX, chunkZ}}
     * @throws IllegalArgumentException if the list does not contain exactly 2 elements
     */
    public static int[] resolveChunkCoords(List<Integer> chunk) {
        if (chunk.size() != 2) {
            throw new InvalidParamsException(
                "'chunk' must contain exactly 2 elements [chunkX, chunkZ], got " + chunk.size()
            );
        }
        return new int[] { chunk.get(0), chunk.get(1) };
    }
 
    /**
     * Computes the flat column index for a local position within a chunk.
     *
     * @param localX The local X coordinate, 0-15
     * @param localZ The local Z coordinate, 0-15
     * @return The flat index, {@code localX * 16 + localZ}
     */
    public static int flatIndex(int localX, int localZ) {
        return localX * 16 + localZ;
    }
 
    /**
     * Extracts the local X coordinate from a flat column index.
     *
     * @param index The flat index
     * @return The local X coordinate, 0-15
     */
    public static int localX(int index) {
        return index / 16;
    }
 
    /**
     * Extracts the local Z coordinate from a flat column index.
     *
     * @param index The flat index
     * @return The local Z coordinate, 0-15
     */
    public static int localZ(int index) {
        return index % 16;
    }
 
    // -------------------------------------------------------------------------
    // Chunk pre-loading
    // -------------------------------------------------------------------------
 
    /**
     * Synchronously loads the single chunk that contains {@code pos}.
     *
     * <p>Already-loaded chunks are returned from cache instantly. Chunks that exist on
     * disk but are not currently in memory are deserialized. Chunks that have never been
     * generated are generated – this is the v0 behavior, matching {@code world:block} and
     * {@code world:chunk/surface}.</p>
     *
     * @param level The level whose chunk to load
     * @param pos   Any block position inside the chunk to load
     */
    public static void preloadChunk(ServerLevel level, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
 
    /**
     * Synchronously loads the single chunk at the given chunk coordinates.
     *
     * @param level   The level whose chunk to load
     * @param chunkX  Chunk X coordinate
     * @param chunkZ  Chunk Z coordinate
     */
    public static void preloadChunk(ServerLevel level, int chunkX, int chunkZ) {
        level.getChunk(chunkX, chunkZ);
    }
 
    /**
     * Synchronously loads all chunks in the bounding box between {@code a} and {@code b}
     * (plus {@code buffer} extra chunks on each side).
     *
     * <p>Use this before any operation that reads block data across a range of positions
     * (e.g. pathfinding, region queries) to ensure the underlying chunk data is present
     * rather than appearing as all-air. Already-loaded chunks are returned from cache
     * and do not incur additional I/O cost.</p>
     *
     * @param level  The level whose chunks to load
     * @param a      One corner of the area
     * @param b      The opposite corner of the area
     * @param buffer Extra chunks to load beyond the bounding box on each side
     */
    public static void preloadChunks(ServerLevel level, BlockPos a, BlockPos b, int buffer) {
        int minChunkX = (Math.min(a.getX(), b.getX()) >> 4) - buffer;
        int maxChunkX = (Math.max(a.getX(), b.getX()) >> 4) + buffer;
        int minChunkZ = (Math.min(a.getZ(), b.getZ()) >> 4) - buffer;
        int maxChunkZ = (Math.max(a.getZ(), b.getZ()) >> 4) + buffer;
 
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }
 
    /**
     * Convenience overload of {@link #preloadChunks(ServerLevel, BlockPos, BlockPos, int)}
     * with a default buffer of 1 chunk on each side.
     *
     * @param level The level whose chunks to load
     * @param a     One corner of the area
     * @param b     The opposite corner of the area
     */
    public static void preloadChunks(ServerLevel level, BlockPos a, BlockPos b) {
        preloadChunks(level, a, b, 1);
    }
}
