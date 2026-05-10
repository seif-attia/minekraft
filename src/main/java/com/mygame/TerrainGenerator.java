/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame;

/**
 *
 * @author EyonMiner
 */
public class TerrainGenerator {

    public static final int SEA_LEVEL = 64;

    /**
     * Fills an empty Chunk with terrain data based on global coordinates.
     */
    public static void generateTerrain(Chunk chunk, int chunkX, int chunkZ) {
        for (int localX = 0; localX < Chunk.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < Chunk.CHUNK_SIZE; localZ++) {

                int globalX = (chunkX * Chunk.CHUNK_SIZE) + localX;
                int globalZ = (chunkZ * Chunk.CHUNK_SIZE) + localZ;

                int columnHeight = generateHeight(globalX, globalZ);

                int heightRight = generateHeight(globalX + 1, globalZ);
                int heightForward = generateHeight(globalX, globalZ + 1);

                // If it drops 3 blocks instantly, it's a rocky cliff
                boolean isSteep = Math.abs(columnHeight - heightRight) >= 3 || Math.abs(columnHeight - heightForward) >= 3;

                int renderLimit = Math.max(columnHeight, SEA_LEVEL);
                if (renderLimit >= Chunk.CHUNK_HEIGHT) {
                    renderLimit = Chunk.CHUNK_HEIGHT - 1;
                }

                for (int y = 0; y <= renderLimit; y++) {
                    if (y == columnHeight) {

                        // --- SURFACE LAYER ---
                        if (columnHeight > 115 || isSteep) {
                            chunk.setBlock(localX, y, localZ, (byte) 3); // STONE Peaks/Cliffs
                        } else if (columnHeight <= SEA_LEVEL + 1) {
                            // Only place dirt if the surface is literally at the water's edge
                            chunk.setBlock(localX, y, localZ, (byte) 1);
                        } else {
                            chunk.setBlock(localX, y, localZ, (byte) 2); // GRASS everywhere else
                        }

                    } else if (y > columnHeight - 4 && y < columnHeight) {

                        // --- SUB-SURFACE ---
                        if (columnHeight > 115 || isSteep) {
                            chunk.setBlock(localX, y, localZ, (byte) 3); // Solid stone under peaks
                        } else {
                            chunk.setBlock(localX, y, localZ, (byte) 1); // Dirt under grass
                        }

                    } else if (y <= columnHeight - 4) {
                        chunk.setBlock(localX, y, localZ, (byte) 3); // Deep stone
                    } else if (y > columnHeight && y <= SEA_LEVEL) {
                        chunk.setBlock(localX, y, localZ, (byte) 4); // WATER
                    }
                }
            }
        }
    }

    // =========================================================================
    // ROLE 3's MATH FUNCTIONS (Preserved exactly, just made static)
    // =========================================================================
    private static int generateHeight(double x, double z) {

        // 1. BASE TERRAIN
        // Gentle, rolling hills.
        double baseNoise = getLayeredNoise(x, z, 0.002f, 4, 0.5);
        double baseHeight = 75 + (baseNoise * 20); // Base sits comfortably around Y=75

        // 2. TRUE MOUNTAINS
        // We use ridged noise, but we cube it (m * m * m). 
        // This forces 80% of the map to stay flat, but the remaining 20% violently spikes into mountains.
        double m = getRidgedNoise(x + 1000, z + 1000, 0.003f, 4, 0.5);
        double mountainHeight = m * m * m * 160; // Up to 160 blocks tall!

        // 3. MICRO-DETAILS
        // High frequency noise that adds +/- 3 blocks of random bumps everywhere to kill flat plateaus
        double detail = getLayeredNoise(x, z, 0.03f, 2, 0.5) * 3;

        // 4. RIVERS
        double riverNoise = getLayeredNoise(x - 500, z - 500, 0.002f, 3, 0.5);
        double riverPath = Math.abs(riverNoise);

        double riverCarve = 0;
        if (riverPath < 0.06) {
            double depth = 1.0 - (riverPath / 0.06);
            depth = depth * depth; // Smooth U-shape
            riverCarve = depth * 30; // Deep river valleys
        }

        // COMBINE IT ALL
        double totalHeight = baseHeight + mountainHeight + detail - riverCarve;

        if (totalHeight < 1) {
            totalHeight = 1;
        }
        if (totalHeight >= Chunk.CHUNK_HEIGHT) {
            totalHeight = Chunk.CHUNK_HEIGHT - 1;
        }

        return (int) totalHeight;
    }

    private static double getLayeredNoise(double x, double z, float freq, int oct, double pers) {
        double total = 0;
        double amp = 1.0;
        double max = 0;
        for (int i = 0; i < oct; i++) {
            double v = noise(x * freq, z * freq);
            total += v * amp;
            max += amp;
            freq *= 2;
            amp *= pers;
        }
        return total / max;
    }

    private static double getRidgedNoise(double x, double z, float freq, int oct, double pers) {
        double total = 0;
        double amp = 1.0;
        double max = 0;
        for (int i = 0; i < oct; i++) {
            double v = noise(x * freq, z * freq);
            v = 1.0 - Math.abs(v);
            v = v * v;
            total += v * amp;
            max += amp;
            freq *= 2;
            amp *= pers;
        }
        return total / max;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        switch (hash & 3) {
            case 0:
                return x + y;
            case 1:
                return -x + y;
            case 2:
                return x - y;
            case 3:
                return -x - y;
            default:
                return 0;
        }
    }

    private static double noise(double x, double y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double u = fade(xf);
        double v = fade(yf);
        double n00 = grad(hash(X, Y), xf, yf);
        double n01 = grad(hash(X, Y + 1), xf, yf - 1);
        double n10 = grad(hash(X + 1, Y), xf - 1, yf);
        double n11 = grad(hash(X + 1, Y + 1), xf - 1, yf - 1);
        return lerp(v, lerp(u, n00, n10), lerp(u, n01, n11));
    }

    private static int hash(int x, int y) {
        int h = x * 31 + y;
        h ^= h >> 16;
        h *= 0x85ebca6b;
        h ^= h >> 13;
        h *= 0xc2b2ae35;
        h ^= h >> 16;
        return h & 255;
    }
}
