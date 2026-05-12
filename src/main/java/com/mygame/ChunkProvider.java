package com.mygame;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author AAA
 */

//aaa > this class is responisble for turning the byte arrays into files on the harddisk
public class ChunkProvider {
    
    // aaa > the path which the world files would be saved in
    private static final String SAVE_PATH = "saves/world1/";
    private String worldName;
    private File worldFolder;

    public ChunkProvider(String worldName) {
        this.worldName = worldName;
        //aaa > create new folder in saves folder
        this.worldFolder = new File("saves/" + worldName);
        if (!worldFolder.exists()) {
            worldFolder.mkdirs();
        }
    }
    
    public void deleteWorldFolder() {
    File worldFolder = new File("saves/" + worldName);
    if (worldFolder.exists()) {
        recursiveDelete(worldFolder);
    }
}

private void recursiveDelete(File file) {
    File[] contents = file.listFiles();
    if (contents != null) {
        for (File f : contents) {
            recursiveDelete(f);
        }
    }
    file.delete();
}
    
    public void saveChunk(ChunkPos pos, byte[][][] blocks) {
        // aaa > create the file to save in if it has not been created before
       // new File(SAVE_PATH).mkdirs();// aaa > check that the file is in the saves or not
       // File chunkFile = new File(SAVE_PATH + pos.x() + "_" + pos.z() + ".dat");
        File chunkFile = new File(worldFolder, "chunk_" + pos.x() + "_" + pos.z() + ".dat");
        //aaa > GZIPOutputStream : comprissed the data to decrease files sizes  (binary serialization)
        //aaa > DataOutputStream : write files in bytes to provide high speed
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(chunkFile)))) {
            // aaa > it loops on each chunk and write its value in file
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                        out.writeByte(blocks[x][y][z]);
                    }
                }
            }
        } catch (IOException e) {
          throw new RuntimeException("Critical Error: Could not access game save files.", e);
        }
}
    

    public byte[][][] loadChunk(ChunkPos pos) {
        File chunkFile = new File(worldFolder, "chunk_" + pos.x() + "_" + pos.z() + ".dat");
        if (!chunkFile.exists()) return null; // aaa > here the WorldManager decide whether to use the chunk generation methods or not

        byte[][][] blocks = new byte[Chunk.CHUNK_SIZE][Chunk.CHUNK_HEIGHT][Chunk.CHUNK_SIZE];
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(chunkFile)))) {
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                        blocks[x][y][z] = in.readByte();
                    }
                }
            }
            return blocks;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    public long getOrGenerateSeed() {
    File folder = new File("saves/" + worldName);
    if (!folder.exists()) folder.mkdirs();
    
    File seedFile = new File(folder, "level.seed");
    if (seedFile.exists()) {
        try (java.util.Scanner s = new java.util.Scanner(seedFile)) {
            return s.nextLong();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    long newSeed = new java.util.Random().nextLong();
    try (java.io.PrintWriter w = new java.io.PrintWriter(seedFile)) {
        w.println(newSeed);
    } catch (Exception e) { e.printStackTrace(); }
    return newSeed;
}
    
}
    
