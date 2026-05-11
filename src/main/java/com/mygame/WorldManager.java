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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import com.jme3.app.Application;
import com.jme3.asset.TextureKey;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Manages the infinite voxel world using a sliding window algorithm.
 *
 * @author EyonMiner
 */
public class WorldManager {

    private int renderDistance = 15; // Loads a grid of chunks around the player, so its nxn + 1

    private Map<ChunkPos, Chunk> activeChunks = new ConcurrentHashMap<>();
    private Map<ChunkPos, Geometry> activeGeometries = new HashMap<>();

    // MultiThreading Variables
    private Application app;
    private ExecutorService executor;
    // A set to save chunks that are queued for loading to avoid reloading them and to save compute
    private Set<ChunkPos> loadingChunks = ConcurrentHashMap.newKeySet();

    // Optimizations
    private int lastPlayerChunkX = Integer.MAX_VALUE;
    private int lastPlayerChunkZ = Integer.MAX_VALUE;

    private List<ChunkPos> chunkLoadQueue = new LinkedList<>();

    private Node worldNode;
    private ChunkMesher mesher;
    private AssetManager assetManager;
    private Material masterMaterial; // Cached material so we don't reload textures constantly

    public WorldManager(Application app, Node rootNode, AssetManager assetManager) {
        this.app = app;

        // Create a thread pool with all available threads in the computer
        int cores = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(Math.max(2, cores - 1));

        this.worldNode = new Node("WorldNode");
        rootNode.attachChild(this.worldNode);

        this.assetManager = assetManager;
        this.mesher = new ChunkMesher();

        initMaterial();
    }

    private void initMaterial() {
        masterMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        // Create a texture key for the mipmap
        //        TextureKey key = new TextureKey("Textures/atlas.png", false);
        //        key.setGenerateMips(true);
        // Texture tex = assetManager.loadTexture(key);
        Texture tex = assetManager.loadTexture("Textures/atlas.png");
        // remove blur for pixel art
        tex.setMagFilter(Texture.MagFilter.Nearest);

        // activate mipmaps for blocks far away
        tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        //masterMaterial.getAdditionalRenderState().setWireframe(true);

        tex.setAnisotropicFilter(8);

        masterMaterial.setTexture("ColorMap", tex);
    }

    public void update(Vector3f playerLocation) {
        int playerChunkX = (int) Math.floor(playerLocation.x / Chunk.CHUNK_SIZE);
        int playerChunkZ = (int) Math.floor(playerLocation.z / Chunk.CHUNK_SIZE);

        // 1. BOUNDARY CHECK: Did the player actually move to a new chunk?
        if (playerChunkX != lastPlayerChunkX || playerChunkZ != lastPlayerChunkZ) {

            // Update the tracker
            lastPlayerChunkX = playerChunkX;
            lastPlayerChunkZ = playerChunkZ;

            // Clear the old queue because the player moved, priorities changed!
            chunkLoadQueue.clear();

            // 2. SCAN FOR MISSING CHUNKS
            for (int x = -renderDistance; x <= renderDistance; x++) {
                for (int z = -renderDistance; z <= renderDistance; z++) {
                    ChunkPos pos = new ChunkPos(playerChunkX + x, playerChunkZ + z);

                    if (!activeChunks.containsKey(pos) && !loadingChunks.contains(pos)) {
                        // Don't build it yet! Just add it to the waitlist.
                        chunkLoadQueue.add(pos);
                        chunkLoadQueue.sort((p1, p2) -> {
                            double dist1 = Math.pow(p1.x() - playerChunkX, 2) + Math.pow(p1.z() - playerChunkZ, 2);
                            double dist2 = Math.pow(p2.x() - playerChunkX, 2) + Math.pow(p2.z() - playerChunkZ, 2);
                            return Double.compare(dist1, dist2);
                        });
                    }
                }
            }

            // 3. UNLOAD FAR CHUNKS
            List<ChunkPos> chunksToUnload = new ArrayList<>();
            for (ChunkPos pos : activeChunks.keySet()) {
                if (Math.abs(pos.x() - playerChunkX) > renderDistance
                        || Math.abs(pos.z() - playerChunkZ) > renderDistance) {
                    chunksToUnload.add(pos);
                }
            }
            for (ChunkPos pos : chunksToUnload) {
                unloadChunk(pos);
            }
        }

        // 4. TIME-SLICING: Only pop a few chunks off the queue per frame
        // Processing 2 chunks per frame keeps 60FPS while loading terrain incredibly fast
        int chunksToProcessThisFrame = 2;

        while (!chunkLoadQueue.isEmpty() && chunksToProcessThisFrame > 0) {
            ChunkPos pos = chunkLoadQueue.remove(0);

            // Double check it wasn't loaded by a neighbor notification while waiting in queue
            if (!activeChunks.containsKey(pos) && !loadingChunks.contains(pos)) {
                loadChunk(pos);
                chunksToProcessThisFrame--;
            }
        }
    }

    private void loadChunk(ChunkPos pos) {
        // Mark as loading immediately on the main thread
        loadingChunks.add(pos);

        // Add chunks immediately to avoid breaking face culling
        Chunk newChunk = new Chunk();
        activeChunks.put(pos, newChunk);

        //  Submit the heavy lifting to a background thread
        executor.submit(() -> {

            // --- BACKGROUND THREAD ---
            TerrainGenerator.generateTerrain(newChunk, pos.x(), pos.z());
            Mesh chunkMesh = mesher.createMesh(newChunk, WorldManager.this, pos.x(), pos.z());

            //  Send the finished Mesh back to the Main Thread safely
            app.enqueue(() -> {

                // Edge case: The player might have run far away while this thread was building.
                // If they did, we just throw the mesh away.
                int playerX = (int) Math.floor(app.getCamera().getLocation().x / Chunk.CHUNK_SIZE);
                int playerZ = (int) Math.floor(app.getCamera().getLocation().z / Chunk.CHUNK_SIZE);
                if (Math.abs(pos.x() - playerX) > renderDistance || Math.abs(pos.z() - playerZ) > renderDistance) {
                    loadingChunks.remove(pos);
                    activeChunks.remove(pos);
                    return;
                }

                // Cleanup loading state
                loadingChunks.remove(pos);

                // Create and attach the Geometry
                Geometry chunkGeo = new Geometry("Chunk_" + pos.x() + "_" + pos.z(), chunkMesh);
                chunkGeo.setMaterial(masterMaterial);
                chunkGeo.setLocalTranslation(pos.x() * Chunk.CHUNK_SIZE, 0, pos.z() * Chunk.CHUNK_SIZE);

                worldNode.attachChild(chunkGeo);
                activeGeometries.put(pos, chunkGeo);

                // Notifiy neighbors that a new chunk was created to recalculate their meshes and cull hidden faces
                ChunkPos north = new ChunkPos(pos.x(), pos.z() + 1);
                if (activeGeometries.containsKey(north)) {
                    rebuildChunk(north);
                }

                ChunkPos south = new ChunkPos(pos.x(), pos.z() - 1);
                if (activeGeometries.containsKey(south)) {
                    rebuildChunk(south);
                }

                ChunkPos east = new ChunkPos(pos.x() + 1, pos.z());
                if (activeGeometries.containsKey(east)) {
                    rebuildChunk(east);
                }

                ChunkPos west = new ChunkPos(pos.x() - 1, pos.z());
                if (activeGeometries.containsKey(west)) {
                    rebuildChunk(west);
                }
            });
        });
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

    // Chunk neighbor notification system to cull the meshes of the outer chunk borders after generating new chunks
    private void rebuildChunk(ChunkPos pos) {

        // Check if the chunk is already in queue or not
        if (loadingChunks.contains(pos)) {
            return;
        }

        loadingChunks.add(pos);
        Chunk existingData = activeChunks.get(pos);

        executor.submit(() -> {
            // Recalculate the math in the background
            Mesh updatedMesh = mesher.createMesh(existingData, WorldManager.this, pos.x(), pos.z());

            app.enqueue(() -> {
                // Remove from loading queue
                loadingChunks.remove(pos);

                // If the player ran faster before the chunk finishes throw the mesh and load nothing
                if (!activeChunks.containsKey(pos)) {
                    return;
                }

                // Find the OLD geometry and delete it from the screen
                Geometry oldGeo = activeGeometries.remove(pos);
                if (oldGeo != null) {
                    oldGeo.removeFromParent();
                }

                //  Attach the NEW geometry
                Geometry newGeo = new Geometry("Chunk_" + pos.x() + "_" + pos.z(), updatedMesh);
                newGeo.setMaterial(masterMaterial);
                newGeo.setLocalTranslation(pos.x() * Chunk.CHUNK_SIZE, 0, pos.z() * Chunk.CHUNK_SIZE);

                worldNode.attachChild(newGeo);
                activeGeometries.put(pos, newGeo);
            });
        });
    }

    public Node getWorldNode() {
        return worldNode;
    }

    // Destroys thread workers upon application shutdown
    public void destroy() {
        executor.shutdownNow();
    }
}
