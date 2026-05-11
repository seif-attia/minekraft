/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame;

import java.util.Random;

/**
 *
 * @author EyonMiner
 */
public class StructureGenerator {

    private static final byte WOOD = 6;
    private static final byte LEAVES = 7;
    private static final Random rand = new Random();

    public static void buildOakTree(Chunk chunk, int localX, int surfaceY, int localZ) {
        // 1. SAFE ZONE CHECK: Prevent the leaves from crossing chunk borders!
        // Leaves span a 5x5 area (radius of 2), so we need 2 blocks of padding on all sides.
        if (localX < 2 || localX > Chunk.CHUNK_SIZE - 3 || localZ < 2 || localZ > Chunk.CHUNK_SIZE - 3) {
            return;
        }

        int trunkHeight = 4 + rand.nextInt(3); // Trees will be 4, 5, or 6 blocks tall

        // Prevent building above the sky limit
        if (surfaceY + trunkHeight + 3 >= Chunk.CHUNK_HEIGHT) {
            return;
        }

        // 2. BUILD THE LEAVES (Do this first so the trunk overwrites leaves inside it)
        int leafStartY = surfaceY + trunkHeight - 2;
        int leafEndY = surfaceY + trunkHeight + 1;

        for (int y = leafStartY; y <= leafEndY; y++) {
            // As the leaves get higher, make the cluster smaller to round the top
            int radius = (y == leafEndY) ? 1 : 2;

            for (int x = localX - radius; x <= localX + radius; x++) {
                for (int z = localZ - radius; z <= localZ + radius; z++) {

                    // Chop off the corners to make it look organic (not a perfect square)
                    if (Math.abs(x - localX) == radius && Math.abs(z - localZ) == radius && radius == 2) {
                        if (rand.nextBoolean()) {
                            continue; // 50% chance to drop the corner leaf
                        }
                    }

                    // Only place a leaf if it's currently air
                    if (chunk.getBlock(x, y, z) == 0) {
                        chunk.setBlock(x, y, z, LEAVES);
                    }
                }
            }
        }

        // 3. BUILD THE TRUNK (Straight up from the surface)
        for (int y = surfaceY + 1; y <= surfaceY + trunkHeight; y++) {
            chunk.setBlock(localX, y, localZ, WOOD);
        }
    }

}
