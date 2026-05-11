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
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Manages the infinite voxel world using a sliding window algorithm.
 *
 * @author EyonMiner
 */
public class WorldManager {

    private boolean isWireframe = false;

    private int renderDistance = 20; // Loads a grid of chunks around the player, so its nxn + 1

    private Map<ChunkPos, Chunk> activeChunks = new ConcurrentHashMap<>();
    private Map<ChunkPos, Node> activeGeometries = new HashMap<>();

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
    private Map<Byte, Material> blockMaterials = new HashMap<>();

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

        // ID 1: Grass Top
        blockMaterials.put((byte) 1, createRepeatingMaterial("Textures/grass_top.png", false));
        // ID 2: Grass Side
        blockMaterials.put((byte) 2, createRepeatingMaterial("Textures/grass_side.png", false));
        // ID 3: DIRT (Or grass side depending on your IDs)
        blockMaterials.put((byte) 3, createRepeatingMaterial("Textures/dirt.png", false));
        // ID 4: STONE
        blockMaterials.put((byte) 4, createRepeatingMaterial("Textures/stone.png", false));
        // --- ID 5: WATER ---
        Material waterMat = createRepeatingMaterial("Textures/water.png", true);

        // Enable custom coloring
        waterMat.setBoolean("UseMaterialColors", true);

        // The 4th number (0.6f) is the Alpha! Lower it to make water more invisible.
        // Keeping RGB at 1.0f preserves the original color of your water.png
        waterMat.setColor("Diffuse", new ColorRGBA(1.0f, 1.0f, 1.0f, 0.6f));
        waterMat.setColor("Ambient", new ColorRGBA(1.0f, 1.0f, 1.0f, 0.6f));

        // Force smooth glass-like transparency instead of harsh cutouts
        waterMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);

        // If your createRepeatingMaterial method adds a discard threshold, we MUST remove it for water!
        waterMat.clearParam("AlphaDiscardThreshold");

        blockMaterials.put((byte) 5, waterMat);
        // ID 6: snow
        blockMaterials.put((byte) 6, createRepeatingMaterial("Textures/snow.png", false));
        // ID 7: wood sides
        blockMaterials.put((byte) 7, createRepeatingMaterial("Textures/wood_side.png", false));
        // ID 8 : wood insides
        blockMaterials.put((byte) 8, createRepeatingMaterial("Textures/wood_inside.png", false));
        // ID 9: LEAVES 
        blockMaterials.put((byte) 9, createRepeatingMaterial("Textures/leaves.png", true));
        // ID 10: Tall Grass
        blockMaterials.put((byte) 10, createRepeatingMaterial("Textures/tall_grass.png", true));

    }

    private Material createRepeatingMaterial(String texturePath, boolean isTransparent) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

        Texture tex = assetManager.loadTexture(texturePath);
        tex.setMagFilter(Texture.MagFilter.Nearest);
        tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        tex.setAnisotropicFilter(8);

        // --- THE MAGIC GREEDY MESH SETTING ---
        // Tells the GPU to infinitely tile the image if a face is larger than 1 block!
        tex.setWrap(Texture.WrapMode.Repeat);

        mat.setTexture("DiffuseMap", tex);
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", new ColorRGBA(0.4f, 0.4f, 0.4f, 1.0f));
        mat.setColor("Diffuse", ColorRGBA.White);

        if (isTransparent) {
            mat.setFloat("AlphaDiscardThreshold", 0.5f);
            mat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        }

        return mat;
    }

    // Add a quick getter so the mesher can access these
    public Material getMaterialForBlock(byte blockId) {
        return blockMaterials.get(blockId);
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

        // Submit the heavy lifting to a background thread
        executor.submit(() -> {

            // --- BACKGROUND THREAD ---
            Chunk newChunk = new Chunk();
            TerrainGenerator.generateTerrain(newChunk, pos.x(), pos.z());

            activeChunks.put(pos, newChunk);

            // Create the Greedy Mesh Node
            Node chunkNode = mesher.createMesh(newChunk, WorldManager.this, pos.x(), pos.z());

            // Send the finished Node back to the Main Thread safely
            app.enqueue(() -> {

                // Edge case: The player might have run far away while this thread was building.
                int playerX = (int) Math.floor(app.getCamera().getLocation().x / Chunk.CHUNK_SIZE);
                int playerZ = (int) Math.floor(app.getCamera().getLocation().z / Chunk.CHUNK_SIZE);
                if (Math.abs(pos.x() - playerX) > renderDistance || Math.abs(pos.z() - playerZ) > renderDistance) {
                    loadingChunks.remove(pos);
                    activeChunks.remove(pos);
                    return;
                }

                // Cleanup loading state
                loadingChunks.remove(pos);

                // Create and attach the Node
                chunkNode.setLocalTranslation(pos.x() * Chunk.CHUNK_SIZE, 0, pos.z() * Chunk.CHUNK_SIZE);

                worldNode.attachChild(chunkNode);
                activeGeometries.put(pos, chunkNode);

                // Notify neighbors that a new chunk was created to recalculate their meshes and cull hidden faces
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
        // Remove the data
        activeChunks.remove(pos);

        // Find the visual object, detach it from the scene, and remove it from the map
        Node chunkNode = activeGeometries.remove(pos);
        if (chunkNode != null) {
            chunkNode.removeFromParent(); // This deletes it from the screen
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
            // Recalculate the Greedy Math in the background
            Node updatedMesh = mesher.createMesh(existingData, WorldManager.this, pos.x(), pos.z());

            app.enqueue(() -> {
                // Remove from loading queue
                loadingChunks.remove(pos);

                // If the player ran faster before the chunk finishes throw the mesh and load nothing
                if (!activeChunks.containsKey(pos)) {
                    return;
                }

                // Find the OLD Node and delete it from the screen
                Node oldNode = activeGeometries.remove(pos);
                if (oldNode != null) {
                    oldNode.removeFromParent();
                }

                // The Mesher already built the Geometries and attached them to updatedMesh.
                // We just need to position the Node and attach it to the world.
                updatedMesh.setLocalTranslation(pos.x() * Chunk.CHUNK_SIZE, 0, pos.z() * Chunk.CHUNK_SIZE);

                worldNode.attachChild(updatedMesh);
                activeGeometries.put(pos, updatedMesh);
            });
        });
    }

    public Node getWorldNode() {
        return worldNode;
    }

    public void toggleWireframe() {
        isWireframe = !isWireframe; // Flip the state

        // Loop through every material (Grass, Stone, Water, etc.)
        for (Material mat : blockMaterials.values()) {
            mat.getAdditionalRenderState().setWireframe(isWireframe);
        }

        System.out.println("Wireframe Mode: " + (isWireframe ? "ON" : "OFF"));
    }
    // Destroys thread workers upon application shutdown

    public void destroy() {
        executor.shutdownNow();
    }
}
