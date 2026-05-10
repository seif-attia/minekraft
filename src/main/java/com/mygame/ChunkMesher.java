package com.mygame;

import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils; // CRITICAL: Use the jME one, not LWJGL!
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the generation of 3D geometry from raw voxel chunk data.
 *
 * @author EyonMiner
 */
public class ChunkMesher {

    /**
     * Iterates through chunk data and builds an optimized jME Mesh using Face
     * Culling.
     */
    private static final float TEX_RES = 16f;

    public Mesh createMesh(Chunk chunk, WorldManager world, int chunkX, int chunkZ) {
        List<Float> positions = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vertexOffset = 0; // Keeps track of the index for triangles
        List<Float> texCoords = new ArrayList<>();

        for (int localX = 0; localX < Chunk.CHUNK_SIZE; localX++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int localZ = 0; localZ < Chunk.CHUNK_SIZE; localZ++) {

                    byte blockId = chunk.getBlock(localX, y, localZ);
                    if (blockId == 0) {
                        continue; // Skip air 
                    }

                    int globalX = (chunkX * Chunk.CHUNK_SIZE) + localX;
                    int globalZ = (chunkZ * Chunk.CHUNK_SIZE) + localZ;

                    // --- FACE CULLING ---
                    // Top Face Check (+Y)
                    // checking blocks at global coords relative to all neighboring chunks
                    if (world.getBlockGlobal(globalX, y + 1, globalZ) == 0) {
                        // keep draw coords local to the chunk itself
                        addTopFaceVertices(positions, localX, y, localZ);
                        //add textures
                        assignTextureForBlock(texCoords, blockId, "TOP");

                        addIndices(indices, vertexOffset);
                        vertexOffset += 4;
                    }
                    // Bottom Face Check (-Y)
                    if (world.getBlockGlobal(globalX, y - 1, globalZ) == 0) {
                        addBottomFaceVertices(positions, localX, y, localZ);
                        //add textures
                        assignTextureForBlock(texCoords, blockId, "BOTTOM");
                        addIndices(indices, vertexOffset);
                        vertexOffset += 4;
                    }
                    // Front Face Check (+Z)
                    if (world.getBlockGlobal(globalX, y, globalZ + 1) == 0) {
                        addFrontFaceVertices(positions, localX, y, localZ);
                        //add textures
                        assignTextureForBlock(texCoords, blockId, "FRONT");
                        addIndices(indices, vertexOffset);
                        vertexOffset += 4;
                    }
                    // Back Face Check (-Z)
                    if (world.getBlockGlobal(globalX, y, globalZ - 1) == 0) {
                        addBackFaceVertices(positions, localX, y, localZ);
                        //add textures
                        assignTextureForBlock(texCoords, blockId, "BACK");
                        addIndices(indices, vertexOffset);
                        vertexOffset += 4;
                    }
                    // Right Face Check (+X)
                    if (world.getBlockGlobal(globalX + 1, y, globalZ) == 0) {
                        addRightFaceVertices(positions, localX, y, localZ);
                        //add textures
                        assignTextureForBlock(texCoords, blockId, "RIGHT");
                        addIndices(indices, vertexOffset);
                        vertexOffset += 4;
                    }
                    // Left Face Check (-X)
                    if (world.getBlockGlobal(globalX - 1, y, globalZ) == 0) {
                        addLeftFaceVertices(positions, localX, y, localZ);
                        //add textures
                        assignTextureForBlock(texCoords, blockId, "LEFT");
                        addIndices(indices, vertexOffset);
                        vertexOffset += 4;
                    }
                }
            }
        }

        // Convert raw lists into a compiled jME Mesh
        return buildJmeMesh(positions, indices, texCoords);
    }

    private void assignTextureForBlock(List<Float> texCoords, byte blockId, String faceDirection) {
        int spriteX = 0;
        int spriteY = 0;

        switch (blockId) {
            case 1: // Dirt
                spriteX = 0;
                spriteY = 0;
                break;

            case 2: // Grass
                switch (faceDirection) {
                    case "TOP":
                        spriteX = 2; // Grass Top texture
                        spriteY = 0;
                        break;
                    case "BOTTOM":
                        spriteX = 0; // The bottom of a grass block (dirt)
                        spriteY = 0;
                        break;
                    default: // SIDES ("FRONT", "BACK", "LEFT", "RIGHT")
                        spriteX = 1; // Grass Side texture
                        spriteY = 0;
                        break;
                }
                break;

            case 3: // Bedrock
                spriteX = 3;
                spriteY = 0;
                break;
            case 4: // Water
                spriteX = 4;
                spriteY = 0;
        }

        // Send the chosen sprite coordinates to your existing UV math method
        addTexCoords(texCoords, spriteX, spriteY);
    }

    private void addTexCoords(List<Float> texCoords, int spriteX, int spriteY) {

        // to prevent texture bleed and highlighted block outlines
        float offset = 0.001f;

        // Calculate the UV boundaries for this specific sprite
        float u1 = spriteX / TEX_RES + offset;
        float u2 = (spriteX + 1) / TEX_RES - offset;

        // jME's V axis starts at the BOTTOM of the image. 
        // We invert it so spriteY=0 is the top row of your image file.
        float v1 = (TEX_RES - spriteY - 1) / TEX_RES + offset;
        float v2 = (TEX_RES - spriteY) / TEX_RES - offset;

        // V0: Bottom-Left
        texCoords.add(u1);
        texCoords.add(v1);
        // V1: Bottom-Right
        texCoords.add(u2);
        texCoords.add(v1);
        // V2: Top-Left
        texCoords.add(u1);
        texCoords.add(v2);
        // V3: Top-Right
        texCoords.add(u2);
        texCoords.add(v2);
    }

    private Mesh buildJmeMesh(List<Float> posList, List<Integer> indexList, List<Float> texCoords) {
        // 1. Convert List<Float> to float[]
        float[] posArray = new float[posList.size()];
        for (int i = 0; i < posList.size(); i++) {
            posArray[i] = posList.get(i);
        }

        // 2. Convert List<Integer> to int[]
        int[] indexArray = new int[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) {
            indexArray[i] = indexList.get(i);
        }

        //convert texture list into an arrayfloat[] texArray = new float[texCoords.size()];
        float[] texArray = new float[texCoords.size()];
        for (int i = 0; i < texCoords.size(); i++) {
            texArray[i] = texCoords.get(i);
        }

        // 3. Build the jME Mesh
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(posArray));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indexArray));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texArray));
        mesh.updateBound(); // Crucial for jME frustum culling

        return mesh; // Return the mesh to the caller!
    }

    // --- TRIANGLE INDICES ---
    private void addIndices(List<Integer> indices, int offset) {
        // Triangle 1
        indices.add(offset + 0);
        indices.add(offset + 1);
        indices.add(offset + 2);
        // Triangle 2
        indices.add(offset + 1);
        indices.add(offset + 3);
        indices.add(offset + 2);
    }

    // --- (+Y) TOP FACE ---
    private void addTopFaceVertices(List<Float> pos, int x, int y, int z) {
        pos.add((float) x);
        pos.add((float) y + 1);
        pos.add((float) z + 1); // V0: Bottom-Left (from top view)
        pos.add((float) x + 1);
        pos.add((float) y + 1);
        pos.add((float) z + 1); // V1: Bottom-Right
        pos.add((float) x);
        pos.add((float) y + 1);
        pos.add((float) z);     // V2: Top-Left
        pos.add((float) x + 1);
        pos.add((float) y + 1);
        pos.add((float) z);     // V3: Top-Right
    }

    // --- (-Y) BOTTOM FACE ---
    private void addBottomFaceVertices(List<Float> pos, int x, int y, int z) {
        pos.add((float) x);
        pos.add((float) y);
        pos.add((float) z);     // V0: Bottom-Left (from bottom view)
        pos.add((float) x + 1);
        pos.add((float) y);
        pos.add((float) z);     // V1: Bottom-Right
        pos.add((float) x);
        pos.add((float) y);
        pos.add((float) z + 1); // V2: Top-Left
        pos.add((float) x + 1);
        pos.add((float) y);
        pos.add((float) z + 1); // V3: Top-Right
    }

    // --- (+Z) FRONT FACE ---
    private void addFrontFaceVertices(List<Float> pos, int x, int y, int z) {
        pos.add((float) x);
        pos.add((float) y);
        pos.add((float) z + 1); // V0
        pos.add((float) x + 1);
        pos.add((float) y);
        pos.add((float) z + 1); // V1
        pos.add((float) x);
        pos.add((float) y + 1);
        pos.add((float) z + 1); // V2
        pos.add((float) x + 1);
        pos.add((float) y + 1);
        pos.add((float) z + 1); // V3
    }

    // --- (-Z) BACK FACE ---
    private void addBackFaceVertices(List<Float> pos, int x, int y, int z) {
        pos.add((float) x + 1);
        pos.add((float) y);
        pos.add((float) z); // V0
        pos.add((float) x);
        pos.add((float) y);
        pos.add((float) z); // V1
        pos.add((float) x + 1);
        pos.add((float) y + 1);
        pos.add((float) z); // V2
        pos.add((float) x);
        pos.add((float) y + 1);
        pos.add((float) z); // V3
    }

    // --- (+X) RIGHT FACE ---
    private void addRightFaceVertices(List<Float> pos, int x, int y, int z) {
        pos.add((float) x + 1);
        pos.add((float) y);
        pos.add((float) z + 1); // V0
        pos.add((float) x + 1);
        pos.add((float) y);
        pos.add((float) z);     // V1
        pos.add((float) x + 1);
        pos.add((float) y + 1);
        pos.add((float) z + 1); // V2
        pos.add((float) x + 1);
        pos.add((float) y + 1);
        pos.add((float) z);     // V3
    }

    // --- (-X) LEFT FACE ---
    private void addLeftFaceVertices(List<Float> pos, int x, int y, int z) {
        pos.add((float) x);
        pos.add((float) y);
        pos.add((float) z);     // V0
        pos.add((float) x);
        pos.add((float) y);
        pos.add((float) z + 1); // V1
        pos.add((float) x);
        pos.add((float) y + 1);
        pos.add((float) z);     // V2
        pos.add((float) x);
        pos.add((float) y + 1);
        pos.add((float) z + 1); // V3
    }
}
