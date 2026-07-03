package dev.loat.msmp_world.msmp.components;

import java.util.List;


/**
 * Utility class for resolving chunk coordinates and the flat column index convention
 * ({@code index = localX * 16 + localZ}) shared by all chunk-bulk MSMP methods
 * (e.g. {@code world:chunk/surface}).
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
            throw new IllegalArgumentException(
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
}
