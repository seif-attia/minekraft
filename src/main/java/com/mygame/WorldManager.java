package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the infinite voxel world using a sliding window algorithm.
 *
 * @author EyonMiner
 */
public class WorldManager {

    private int renderDistance = 5; // Loads a grid of chunks around the player, so its nxn + 1

    private Map<ChunkPos, Chunk> activeChunks = new HashMap<>();
    private Map<ChunkPos, Geometry> activeGeometries = new HashMap<>();

    private Node worldNode;
    private ChunkMesher mesher;
    private AssetManager assetManager;
    private Material masterMaterial; // Cached material so we don't reload textures constantly

    public WorldManager(Node rootNode, AssetManager assetManager) {
        this.worldNode = new Node("WorldNode");
        rootNode.attachChild(this.worldNode);

        this.assetManager = assetManager;
        this.mesher = new ChunkMesher();

        initMaterial();
    }

    private void initMaterial() {
        masterMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture("Textures/atlas.png");
        tex.setMagFilter(Texture.MagFilter.Nearest); // Removes blur for pixel art
        tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        masterMaterial.setTexture("ColorMap", tex);
    }

    public void update(Vector3f playerLocation) {
        // Check player's postion within the chunk
        int playerChunkX = (int) Math.floor(playerLocation.x / Chunk.CHUNK_SIZE);
        int playerChunkZ = (int) Math.floor(playerLocation.z / Chunk.CHUNK_SIZE);

        // Load chunks within render distance
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                ChunkPos pos = new ChunkPos(playerChunkX + x, playerChunkZ + z);

                if (!activeChunks.containsKey(pos)) {
                    loadChunk(pos);
                }
            }
        }

        // Unload chunks outside render distance
        List<ChunkPos> chunksToUnload = new ArrayList<>();

        for (ChunkPos pos : activeChunks.keySet()) {
            // kill any chunk that is outside render distance
            if (Math.abs(pos.x() - playerChunkX) > renderDistance
                    || Math.abs(pos.z() - playerChunkZ) > renderDistance) {
                chunksToUnload.add(pos);
            }
        }

        for (ChunkPos pos : chunksToUnload) {
            unloadChunk(pos);
        }
    }

    private void loadChunk(ChunkPos pos) {
        //  Create chunk data
        Chunk newChunk = new Chunk();

        // Add to active map BEFORE meshing, so neighbors can see it
        activeChunks.put(pos, newChunk);

        //  Build the Mesh
        Mesh chunkMesh = mesher.createMesh(newChunk, this, pos.x(), pos.z());

        // Create the visual object
        Geometry chunkGeo = new Geometry("Chunk_" + pos.x() + "_" + pos.z(), chunkMesh);
        chunkGeo.setMaterial(masterMaterial);

        // Position it in the 3D world
        chunkGeo.setLocalTranslation(pos.x() * Chunk.CHUNK_SIZE, 0, pos.z() * Chunk.CHUNK_SIZE);

        // Attach to scene and save reference
        worldNode.attachChild(chunkGeo);
        activeGeometries.put(pos, chunkGeo);
    }

    private void unloadChunk(ChunkPos pos) {
        //  Remove the data
        activeChunks.remove(pos);

        // Find the visual object, detach it from the scene, and remove it from the map
        Geometry geo = activeGeometries.remove(pos);
        if (geo != null) {
            geo.removeFromParent(); // This deletes it from the screen
        }
    }

    public byte getBlockGlobal(int globalX, int globalY, int globalZ) {
        // Y-axis bounds check
        if (globalY < 0 || globalY >= Chunk.CHUNK_HEIGHT) {
            return 0;
        }

        int chunkX = Math.floorDiv(globalX, Chunk.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(globalZ, Chunk.CHUNK_SIZE);

        ChunkPos targetChunk = new ChunkPos(chunkX, chunkZ);

        // If the neighbor chunk isn't loaded yet, treat it as Air
        if (!activeChunks.containsKey(targetChunk)) {
            return 0;
        }

        int localX = Math.floorMod(globalX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(globalZ, Chunk.CHUNK_SIZE);

        return activeChunks.get(targetChunk).getBlock(localX, globalY, localZ);
    }
}
