package com.mygame;

public class TerrainGenerator {

    public static final int SEA_LEVEL = 64;

 
    private final int seedOffsetX;
    private final int seedOffsetZ;

   
   public TerrainGenerator(long seed) {
    java.util.Random relRandom = new java.util.Random(seed);
    this.seedOffsetX = relRandom.nextInt(2000000) - 1000000;
    this.seedOffsetZ = relRandom.nextInt(2000000) - 1000000;
    
    System.out.println("AAA Debug: TerrainGenerator initialized with seed: " + seed);
    }

    
    public void generateTerrain(Chunk chunk, int chunkX, int chunkZ) {
        for (int localX = 0; localX < Chunk.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < Chunk.CHUNK_SIZE; localZ++) {

               
                int globalX = (chunkX * Chunk.CHUNK_SIZE) + localX + seedOffsetX;
                int globalZ = (chunkZ * Chunk.CHUNK_SIZE) + localZ + seedOffsetZ;

                int columnHeight = generateHeight(globalX, globalZ);
                int heightRight = generateHeight(globalX + 1, globalZ);
                int heightForward = generateHeight(globalX, globalZ + 1);

                boolean isSteep = Math.abs(columnHeight - heightRight) >= 3 || Math.abs(columnHeight - heightForward) >= 3;

                double temperatureNoise = noise(globalX * 0.05, globalZ * 0.05);
                int dynamicSnowLine = 140 + (int) (temperatureNoise * 15);
                int dynamicTreeLine = 115 + (int) (temperatureNoise * 10);

                int renderLimit = Math.max(columnHeight, SEA_LEVEL);
                if (renderLimit >= Chunk.CHUNK_HEIGHT) {
                    renderLimit = Chunk.CHUNK_HEIGHT - 1;
                }

                for (int y = 0; y <= renderLimit; y++) {
                    if (y == columnHeight) {
                        // طبقة السطح
                        if (isSteep) {
                            chunk.setBlock(localX, y, localZ, (byte) 4); // STONE
                        } else if (columnHeight > dynamicSnowLine) {
                            chunk.setBlock(localX, y, localZ, (byte) 6); // SNOW
                        } else if (columnHeight > dynamicTreeLine) {
                            chunk.setBlock(localX, y, localZ, (byte) 4); // STONE
                        } else if (columnHeight <= SEA_LEVEL + 1) {
                            chunk.setBlock(localX, y, localZ, (byte) 3); // SAND
                        } else {
                            chunk.setBlock(localX, y, localZ, (byte) 1); // GRASS
                        }
                    } else if (y > columnHeight - 4 && y < columnHeight) {
                        
                        if (columnHeight > dynamicSnowLine) {
                            chunk.setBlock(localX, y, localZ, (byte) 6);
                        } else if (columnHeight > dynamicTreeLine || isSteep) {
                            chunk.setBlock(localX, y, localZ, (byte) 4);
                        } else {
                            chunk.setBlock(localX, y, localZ, (byte) 3);
                        }
                    } else if (y <= columnHeight - 4) {
                        chunk.setBlock(localX, y, localZ, (byte) 3); // Deep stone
                    } else if (y > columnHeight && y <= SEA_LEVEL) {
                        chunk.setBlock(localX, y, localZ, (byte) 5); // WATER
                    }
                }

                
                if (chunk.getBlock(localX, columnHeight, localZ) == 1) {
                    double randomSeed = Math.random();
                    if (randomSeed < 0.005) {
                        StructureGenerator.buildOakTree(chunk, localX, columnHeight, localZ);
                    } else if (randomSeed < 0.05) {
                        if (chunk.getBlock(localX, columnHeight + 1, localZ) == 0) {
                            chunk.setBlock(localX, columnHeight + 1, localZ, (byte) 10);
                        }
                    }
                }
            }
        }
    }

  
    private int generateHeight(double x, double z) {
        double baseNoise = getLayeredNoise(x, z, 0.002f, 4, 0.5);
        double baseHeight = 75 + (baseNoise * 20);
        double m = getRidgedNoise(x + 1000, z + 1000, 0.003f, 4, 0.5);
        double mountainHeight = m * m * m * 160;
        double detail = getLayeredNoise(x, z, 0.03f, 2, 0.5) * 3;
        double totalHeight = baseHeight + mountainHeight + detail;
        return (int) Math.max(1, Math.min(totalHeight, Chunk.CHUNK_HEIGHT - 1));
    }

    private double getLayeredNoise(double x, double z, float freq, int oct, double pers) {
        double total = 0, amp = 1.0, max = 0;
        for (int i = 0; i < oct; i++) {
            total += noise(x * freq, z * freq) * amp;
            max += amp; freq *= 2; amp *= pers;
        }
        return total / max;
    }

    private double getRidgedNoise(double x, double z, float freq, int oct, double pers) {
        double total = 0, amp = 1.0, max = 0;
        for (int i = 0; i < oct; i++) {
            double v = 1.0 - Math.abs(noise(x * freq, z * freq));
            total += v * v * amp;
            max += amp; freq *= 2; amp *= pers;
        }
        return total / max;
    }

    private double noise(double x, double y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double u = fade(xf), v = fade(yf);
        return lerp(v, lerp(u, grad(hash(X, Y), xf, yf), grad(hash(X+1, Y), xf-1, yf)),
                      lerp(u, grad(hash(X, Y+1), xf, yf-1), grad(hash(X+1, Y+1), xf-1, yf-1)));
    }

    private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private double lerp(double t, double a, double b) { return a + t * (b - a); }
    private double grad(int hash, double x, double y) {
        switch (hash & 3) {
            case 0: return x + y; case 1: return -x + y;
            case 2: return x - y; case 3: return -x - y;
            default: return 0;
        }
    }
    private int hash(int x, int y) {
        int h = x * 31 + y;
        h ^= h >> 16; h *= 0x85ebca6b; h ^= h >> 13;
        h *= 0xc2b2ae35; h ^= h >> 16;
        return h & 255;
    }
}