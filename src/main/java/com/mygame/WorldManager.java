/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;

/**
 *
 * @author EyonMiner
 */
public class WorldManager {

    public static final int WORLD_SIZE_CHUNKS = 100; // A 3x3 grid of chunks
    private Chunk[][] chunks;

    private Node worldNode;
    private ChunkMesher mesher;
    private AssetManager assetManager;

    public WorldManager(Node rootNode, AssetManager assetManager) {
        this.worldNode = new Node("WorldNode");
        rootNode.attachChild(this.worldNode);

        this.assetManager = assetManager;
        this.chunks = new Chunk[WORLD_SIZE_CHUNKS][WORLD_SIZE_CHUNKS];
        this.mesher = new ChunkMesher();

        generateWorld();
    }

    private void generateWorld() {
        // 1. Create the data for all chunks FIRST
        for (int chunkX = 0; chunkX < WORLD_SIZE_CHUNKS; chunkX++) {
            for (int chunkZ = 0; chunkZ < WORLD_SIZE_CHUNKS; chunkZ++) {
                chunks[chunkX][chunkZ] = new Chunk();
            }
        }

        // Define materials for the blocks
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        //set texture
        Texture tex = assetManager.loadTexture("Textures/atlas.png");
        tex.setMagFilter(Texture.MagFilter.Nearest);            //removes blur
        tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        mat.setTexture("ColorMap", tex);
        //mat.getAdditionalRenderState().setWireframe(true);  // for testing

        // 2. Mesh them SECOND (so they can see their neighbors' data)
        for (int chunkX = 0; chunkX < WORLD_SIZE_CHUNKS; chunkX++) {
            for (int chunkZ = 0; chunkZ < WORLD_SIZE_CHUNKS; chunkZ++) {

                // get raw math from mesher
                Mesh chunkMesh = mesher.createMesh(chunks[chunkX][chunkZ], this, chunkX, chunkZ);

                // creating physical objects
                Geometry chunkGeo = new Geometry("Chunk_" + chunkX + "_" + chunkZ, chunkMesh);
                chunkGeo.setMaterial(mat);

                // move the objects to their glocal position
                chunkGeo.setLocalTranslation(chunkX * Chunk.CHUNK_SIZE, 0, chunkZ * Chunk.CHUNK_SIZE);

                worldNode.attachChild(chunkGeo);

            }
        }
    }

    public byte getBlockGlobal(int globalX, int globalY, int globalZ) {
        // 1. Check if the request is entirely outside the world bounds
        if (globalX < 0 || globalX >= WORLD_SIZE_CHUNKS * Chunk.CHUNK_SIZE
                || globalY < 0 || globalY >= Chunk.CHUNK_HEIGHT
                || globalZ < 0 || globalZ >= WORLD_SIZE_CHUNKS * Chunk.CHUNK_SIZE) {
            return 0; // Return Air if outside the world map
        }

        // 2. Math to find WHICH chunk holds this block
        int chunkX = globalX / Chunk.CHUNK_SIZE;
        int chunkZ = globalZ / Chunk.CHUNK_SIZE;

        // 3. Math to find the LOCAL coordinates inside that specific chunk
        int localX = globalX % Chunk.CHUNK_SIZE;
        int localZ = globalZ % Chunk.CHUNK_SIZE;

        // 4. Ask that specific chunk for the block
        return chunks[chunkX][chunkZ].getBlock(localX, globalY, localZ);
    }
}
