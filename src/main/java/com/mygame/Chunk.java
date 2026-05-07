/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame;

/**
 *
 * @author EyonMiner
 */
public class Chunk {

    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 128;

    // blocks 
    /*
                air ->                      0
                dirt  ->                   1
                grass->                 2
                bedrock->          3
     */
    private byte[][][] blocks;

    public Chunk() {
        blocks = new byte[CHUNK_SIZE][CHUNK_HEIGHT][CHUNK_SIZE];
        generateSuperFlat();

    }

    private void generateSuperFlat() {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    if (y == 0) {
                        blocks[x][y][z] = 3;     // bedrock
                    } else if (y < 5) {
                        blocks[x][y][z] = 1;     // dirt
                    } else if (y == 5) {
                        blocks[x][y][z] = 2;     //grass
                    } else {
                        blocks[x][y][z] = 0;    //air
                    }

                }
            }
        }
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return 0; // Treat out of bounds as Air for now
        }
        return blocks[x][y][z];
    }

}
