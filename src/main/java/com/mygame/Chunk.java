package com.mygame;

/**
 * @author EyonMiner
 */
public class Chunk {

    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 384;

    // blocks 
    /*
        IDs in worldmanager
     */
    private byte[][][] blocks;

    public Chunk() {
        // Just initialize the memory. Java automatically fills it with 0 (Air).
        // Terrain generator will handle the rest of the logic
        blocks = new byte[CHUNK_SIZE][CHUNK_HEIGHT][CHUNK_SIZE];
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return 0; // Treat out of bounds as Air
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, byte blockId) {
        // Always bounds check before setting to prevent crashes
        if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_HEIGHT && z >= 0 && z < CHUNK_SIZE) {
            blocks[x][y][z] = blockId;
        }
    }
}
