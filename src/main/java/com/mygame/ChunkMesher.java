package com.mygame;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkMesher {

    // Helper class to hold mesh data for EACH specific block type
    private class MeshBuilder {

        List<Float> positions = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        int vertexOffset = 0;
    }

    private boolean shouldDrawFace(byte currentBlock, byte neighborBlock) {
        if (neighborBlock == 0) {
            return true;
        }
        if (currentBlock == neighborBlock) {
            return false;
        }
        // Draw against transparent blocks (Assuming 4 is Water, 8 is Leaves)
        return neighborBlock == 5 || neighborBlock == 9 || neighborBlock == 10;
    }

    /**
     * Builds the Greedy Mesh and returns a Node containing one Geometry per
     * Block ID.
     */
    public Node createMesh(Chunk chunk, WorldManager world, int chunkX, int chunkZ) {
        Node chunkNode = new Node("ChunkNode");
        Map<Byte, MeshBuilder> builders = new HashMap<>();

        // ====================================================================
        // 1. Y-AXIS (TOP AND BOTTOM FACES)
        // ====================================================================
        for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
            byte[][] maskTop = new byte[16][16];
            byte[][] maskBot = new byte[16][16];

            // Build the 2D visual mask for this slice
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    byte b = chunk.getBlock(x, y, z);
                    if (b == 0 || b == 10) {
                        continue;
                    }
                    int globalX = (chunkX * 16) + x;
                    int globalZ = (chunkZ * 16) + z;

                    if (shouldDrawFace(b, world.getBlockGlobal(globalX, y + 1, globalZ))) {
                        maskTop[x][z] = getFaceTexture(b, "TOP");
                    }
                    if (shouldDrawFace(b, world.getBlockGlobal(globalX, y - 1, globalZ))) {
                        maskBot[x][z] = getFaceTexture(b, "BOTTOM");
                    }
                }
            }
            // Sweep the masks and generate the giant rectangles
            sweepY(maskTop, builders, y, true);
            sweepY(maskBot, builders, y, false);
        }

        // ====================================================================
        // 2. Z-AXIS (FRONT AND BACK FACES)
        // ====================================================================
        for (int z = 0; z < 16; z++) {
            byte[][] maskFront = new byte[16][Chunk.CHUNK_HEIGHT];
            byte[][] maskBack = new byte[16][Chunk.CHUNK_HEIGHT];

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    byte b = chunk.getBlock(x, y, z);
                    if (b == 0 || b == 10) {
                        continue;
                    }
                    int globalX = (chunkX * 16) + x;
                    int globalZ = (chunkZ * 16) + z;

                    if (shouldDrawFace(b, world.getBlockGlobal(globalX, y, globalZ + 1))) {
                        maskFront[x][y] = getFaceTexture(b, "SIDE");
                    }
                    if (shouldDrawFace(b, world.getBlockGlobal(globalX, y, globalZ - 1))) {
                        maskBack[x][y] = getFaceTexture(b, "SIDE");
                    }
                }
            }
            sweepZ(maskFront, builders, z, true);
            sweepZ(maskBack, builders, z, false);
        }

        // ====================================================================
        // 3. X-AXIS (LEFT AND RIGHT FACES)
        // ====================================================================
        for (int x = 0; x < 16; x++) {
            byte[][] maskRight = new byte[16][Chunk.CHUNK_HEIGHT];
            byte[][] maskLeft = new byte[16][Chunk.CHUNK_HEIGHT];

            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    byte b = chunk.getBlock(x, y, z);
                    if (b == 0 || b == 10) {
                        continue;
                    }
                    int globalX = (chunkX * 16) + x;
                    int globalZ = (chunkZ * 16) + z;

                    if (shouldDrawFace(b, world.getBlockGlobal(globalX + 1, y, globalZ))) {
                        maskRight[z][y] = getFaceTexture(b, "SIDE");
                    }
                    if (shouldDrawFace(b, world.getBlockGlobal(globalX - 1, y, globalZ))) {
                        maskLeft[z][y] = getFaceTexture(b, "SIDE");
                    }
                }
            }
            sweepX(maskRight, builders, x, true);
            sweepX(maskLeft, builders, x, false);
        }

        // ====================================================================
        // 4. CROSS MESHES (Tall Grass, Flowers, Saplings)
        // ====================================================================
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < 16; z++) {
                    byte b = chunk.getBlock(x, y, z);
                    if (b == 10) { // If it is Tall Grass
                        MeshBuilder bld = builders.computeIfAbsent(b, k -> new MeshBuilder());
                        buildCrossMesh(bld, x, y, z);
                    }
                }
            }
        }

        // ====================================================================
        // COMPILE INTO GEOMETRIES
        // ====================================================================
        for (Map.Entry<Byte, MeshBuilder> entry : builders.entrySet()) {
            byte blockId = entry.getKey();
            MeshBuilder builder = entry.getValue();

            Mesh mesh = new Mesh();
            mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(toFloatArray(builder.positions)));
            mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(toIntArray(builder.indices)));
            mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(toFloatArray(builder.texCoords)));
            mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(toFloatArray(builder.normals)));
            mesh.updateBound();

            Geometry geom = new Geometry("Blocks_" + blockId, mesh);
            // Grab the repeating material we made in WorldManager!
            geom.setMaterial(world.getMaterialForBlock(blockId));
            // --- THE WATER FIX ---
            if (blockId == 5) { // If this geometry is the Water mesh
                // 1. Draw it LAST so we can see the solid blocks behind it
                geom.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);

                // 2. Stop water from casting solid pitch-black shadows onto the ocean floor
                geom.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Receive);
            } else {
                // All other solid blocks cast and receive shadows normally
                geom.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.CastAndReceive);
            }
            chunkNode.attachChild(geom);
        }

        return chunkNode;
    }

    // ========================================================================
    // GREEDY SWEEPERS
    // ========================================================================
    private void sweepY(byte[][] mask, Map<Byte, MeshBuilder> builders, int y, boolean isTop) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (mask[x][z] != 0) {
                    byte id = mask[x][z];
                    int w = 1, h = 1;

                    // 1. Expand Width (X)
                    while (x + w < 16 && mask[x + w][z] == id) {
                        w++;
                    }

                    // 2. Expand Height (Z)
                    boolean done = false;
                    while (z + h < 16) {
                        for (int k = 0; k < w; k++) {
                            if (mask[x + k][z + h] != id) {
                                done = true;
                                break;
                            }
                        }
                        if (done) {
                            break;
                        }
                        h++;
                    }

                    // 3. Generate Quad
                    MeshBuilder b = builders.computeIfAbsent(id, k -> new MeshBuilder());
                    float yPos = isTop ? y + 1f : y;

                    b.positions.add((float) x);
                    b.positions.add(yPos);
                    b.positions.add((float) z + h); // V0
                    b.positions.add((float) x + w);
                    b.positions.add(yPos);
                    b.positions.add((float) z + h); // V1
                    b.positions.add((float) x);
                    b.positions.add(yPos);
                    b.positions.add((float) z);   // V2
                    b.positions.add((float) x + w);
                    b.positions.add(yPos);
                    b.positions.add((float) z);   // V3

                    for (int i = 0; i < 4; i++) {
                        b.normals.add(0f);
                        b.normals.add(isTop ? 1f : -1f);
                        b.normals.add(0f);
                    }

                    // CRITICAL: We pass the width and height to the UVs so the texture repeats perfectly!
                    b.texCoords.add(0f);
                    b.texCoords.add(0f);
                    b.texCoords.add((float) w);
                    b.texCoords.add(0f);
                    b.texCoords.add(0f);
                    b.texCoords.add((float) h);
                    b.texCoords.add((float) w);
                    b.texCoords.add((float) h);

                    addIndices(b.indices, b.vertexOffset, isTop);
                    b.vertexOffset += 4;

                    // 4. Erase the rectangle from the mask
                    for (int i = 0; i < w; i++) {
                        for (int j = 0; j < h; j++) {
                            mask[x + i][z + j] = 0;
                        }
                    }
                }
            }
        }
    }

    private void sweepZ(byte[][] mask, Map<Byte, MeshBuilder> builders, int z, boolean isFront) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                if (mask[x][y] != 0) {
                    byte id = mask[x][y];
                    int w = 1, h = 1;
                    while (x + w < 16 && mask[x + w][y] == id) {
                        w++;
                    }
                    boolean done = false;
                    while (y + h < Chunk.CHUNK_HEIGHT) {
                        for (int k = 0; k < w; k++) {
                            if (mask[x + k][y + h] != id) {
                                done = true;
                                break;
                            }
                        }
                        if (done) {
                            break;
                        }
                        h++;
                    }

                    MeshBuilder b = builders.computeIfAbsent(id, k -> new MeshBuilder());
                    float zPos = isFront ? z + 1f : z;

                    b.positions.add((float) x);
                    b.positions.add((float) y);
                    b.positions.add(zPos);
                    b.positions.add((float) x + w);
                    b.positions.add((float) y);
                    b.positions.add(zPos);
                    b.positions.add((float) x);
                    b.positions.add((float) y + h);
                    b.positions.add(zPos);
                    b.positions.add((float) x + w);
                    b.positions.add((float) y + h);
                    b.positions.add(zPos);

                    for (int i = 0; i < 4; i++) {
                        b.normals.add(0f);
                        b.normals.add(0f);
                        b.normals.add(isFront ? 1f : -1f);
                    }

                    b.texCoords.add(0f);
                    b.texCoords.add(0f);
                    b.texCoords.add((float) w);
                    b.texCoords.add(0f);
                    b.texCoords.add(0f);
                    b.texCoords.add((float) h);
                    b.texCoords.add((float) w);
                    b.texCoords.add((float) h);

                    addIndices(b.indices, b.vertexOffset, isFront);
                    b.vertexOffset += 4;

                    for (int i = 0; i < w; i++) {
                        for (int j = 0; j < h; j++) {
                            mask[x + i][y + j] = 0;
                        }
                    }
                }
            }
        }
    }

    private void sweepX(byte[][] mask, Map<Byte, MeshBuilder> builders, int x, boolean isRight) {
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                if (mask[z][y] != 0) {
                    byte id = mask[z][y];
                    int w = 1, h = 1;
                    while (z + w < 16 && mask[z + w][y] == id) {
                        w++;
                    }
                    boolean done = false;
                    while (y + h < Chunk.CHUNK_HEIGHT) {
                        for (int k = 0; k < w; k++) {
                            if (mask[z + k][y + h] != id) {
                                done = true;
                                break;
                            }
                        }
                        if (done) {
                            break;
                        }
                        h++;
                    }

                    MeshBuilder b = builders.computeIfAbsent(id, k -> new MeshBuilder());
                    float xPos = isRight ? x + 1f : x;

                    b.positions.add(xPos);
                    b.positions.add((float) y);
                    b.positions.add((float) z);
                    b.positions.add(xPos);
                    b.positions.add((float) y);
                    b.positions.add((float) z + w);
                    b.positions.add(xPos);
                    b.positions.add((float) y + h);
                    b.positions.add((float) z);
                    b.positions.add(xPos);
                    b.positions.add((float) y + h);
                    b.positions.add((float) z + w);

                    for (int i = 0; i < 4; i++) {
                        b.normals.add(isRight ? 1f : -1f);
                        b.normals.add(0f);
                        b.normals.add(0f);
                    }

                    b.texCoords.add(0f);
                    b.texCoords.add(0f);
                    b.texCoords.add((float) w);
                    b.texCoords.add(0f);
                    b.texCoords.add(0f);
                    b.texCoords.add((float) h);
                    b.texCoords.add((float) w);
                    b.texCoords.add((float) h);

                    // Reversing indices for the left side
                    addIndices(b.indices, b.vertexOffset, !isRight);
                    b.vertexOffset += 4;

                    for (int i = 0; i < w; i++) {
                        for (int j = 0; j < h; j++) {
                            mask[z + i][y + j] = 0;
                        }
                    }
                }
            }
        }
    }

    private void addIndices(List<Integer> indices, int offset, boolean frontFacing) {
        if (frontFacing) {
            indices.add(offset + 0);
            indices.add(offset + 1);
            indices.add(offset + 2);
            indices.add(offset + 1);
            indices.add(offset + 3);
            indices.add(offset + 2);
        } else {
            indices.add(offset + 2);
            indices.add(offset + 1);
            indices.add(offset + 0);
            indices.add(offset + 2);
            indices.add(offset + 3);
            indices.add(offset + 1);
        }
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private byte getFaceTexture(byte originalBlockId, String faceType) {
        // --- GRASS BLOCK LOGIC ---
        if (originalBlockId == 1) {
            if (faceType.equals("TOP")) {
                return 1;    // Grass Top
            }
            if (faceType.equals("BOTTOM")) {
                return 3; // Dirt Bottom
            }
            return 2;                                // Grass Side
        }

        // --- WOOD LOGIC ---
        if (originalBlockId == 7) {
            if (faceType.equals("TOP") || faceType.equals("BOTTOM")) {
                return 8; // Wood Inside
            }
            return 7; // Wood Side
        }

        return originalBlockId;
    }

    /**
     * Builds two intersecting diagonal quads for foliage (X shape)
     */
    private void buildCrossMesh(MeshBuilder b, int x, int y, int z) {
        // Quad 1: Bottom-Left to Top-Right diagonal (/)
        addCrossQuad(b, x, y, z, x + 1, y, z + 1, x, y + 1, z, x + 1, y + 1, z + 1);
        // Quad 1 Backwards (So you can see it from behind)
        addCrossQuad(b, x + 1, y, z + 1, x, y, z, x + 1, y + 1, z + 1, x, y + 1, z);

        // Quad 2: Bottom-Right to Top-Left diagonal (\)
        addCrossQuad(b, x + 1, y, z, x, y, z + 1, x + 1, y + 1, z, x, y + 1, z + 1);
        // Quad 2 Backwards
        addCrossQuad(b, x, y, z + 1, x + 1, y, z, x, y + 1, z + 1, x + 1, y + 1, z);
    }

    private void addCrossQuad(MeshBuilder b, float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3) {
        // Vertices
        b.positions.add(x0);
        b.positions.add(y0);
        b.positions.add(z0); // V0: Bottom-Left
        b.positions.add(x1);
        b.positions.add(y1);
        b.positions.add(z1); // V1: Bottom-Right
        b.positions.add(x2);
        b.positions.add(y2);
        b.positions.add(z2); // V2: Top-Left
        b.positions.add(x3);
        b.positions.add(y3);
        b.positions.add(z3); // V3: Top-Right

        // Point normals straight up so the lighting perfectly matches the grass block underneath it!
        for (int i = 0; i < 4; i++) {
            b.normals.add(0f);
            b.normals.add(1f);
            b.normals.add(0f);
        }

        // UV Mapping (Full image)
        b.texCoords.add(0f);
        b.texCoords.add(0f);
        b.texCoords.add(1f);
        b.texCoords.add(0f);
        b.texCoords.add(0f);
        b.texCoords.add(1f);
        b.texCoords.add(1f);
        b.texCoords.add(1f);

        // Triangles
        int off = b.vertexOffset;
        b.indices.add(off + 0);
        b.indices.add(off + 1);
        b.indices.add(off + 2);
        b.indices.add(off + 1);
        b.indices.add(off + 3);
        b.indices.add(off + 2);
        b.vertexOffset += 4;
    }
}
