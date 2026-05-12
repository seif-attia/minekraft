package com.minekraft.engine;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class RaycastManager {

    private Camera cam;
    private WorldManager world;
    private float reach = 5.0f; // How many blocks away can you reach?

    public RaycastResult currentResult;

    public RaycastManager(Camera cam, WorldManager world) {
        this.cam = cam;
        this.world = world;
    }

    public void update(float tpf) {
        currentResult = raycast();
    }

    private RaycastResult raycast() {
        Vector3f pos = cam.getLocation();
        Vector3f dir = cam.getDirection();

        // Current voxel coordinates
        int x = (int) Math.floor(pos.x);
        int y = (int) Math.floor(pos.y);
        int z = (int) Math.floor(pos.z);

        // Step direction
        int stepX = (dir.x > 0) ? 1 : -1;
        int stepY = (dir.y > 0) ? 1 : -1;
        int stepZ = (dir.z > 0) ? 1 : -1;

        // Distance to next voxel boundary
        float tMaxX = (float) (stepX > 0 ? (Math.floor(pos.x) + 1 - pos.x) : (pos.x - Math.floor(pos.x))) / Math.abs(dir.x);
        float tMaxY = (float) (stepY > 0 ? (Math.floor(pos.y) + 1 - pos.y) : (pos.y - Math.floor(pos.y))) / Math.abs(dir.y);
        float tMaxZ = (float) (stepZ > 0 ? (Math.floor(pos.z) + 1 - pos.z) : (pos.z - Math.floor(pos.z))) / Math.abs(dir.z);

        // How far t increases for one full voxel step
        float tDeltaX = Math.abs(1f / dir.x);
        float tDeltaY = Math.abs(1f / dir.y);
        float tDeltaZ = Math.abs(1f / dir.z);

        Vector3f lastPos = new Vector3f(x, y, z);
        float dist = 0;

        while (dist < reach) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    dist = tMaxX;
                    tMaxX += tDeltaX;
                    x += stepX;
                } else {
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                    z += stepZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    dist = tMaxY;
                    tMaxY += tDeltaY;
                    y += stepY;
                } else {
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                    z += stepZ;
                }
            }

            byte block = world.getBlockGlobal(x, y, z);
            if (block != 0 && block != 5) { // Stop if we hit a solid block (not air/water)
                return new RaycastResult(new Vector3f(x, y, z), lastPos);
            }
            lastPos.set(x, y, z);
        }
        return null;
    }
}

// Simple data class to hold the hit block and the empty space before it
class RaycastResult {

    public Vector3f blockPos;  // The block you are looking at
    public Vector3f adjacent;  // The empty space next to it (for placing blocks)

    public RaycastResult(Vector3f blockPos, Vector3f adjacent) {
        this.blockPos = blockPos;
        this.adjacent = adjacent;
    }
}
