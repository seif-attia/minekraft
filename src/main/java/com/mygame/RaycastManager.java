package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

public class RaycastManager {

    private Camera cam;
    private Node rootNode;
    private AssetManager assetManager;
    private WorldManager worldManager;
    private Geometry selectionOutline;

    // How far the player can reach
    private final float REACH = 6.0f;
    private final float STEP_SIZE = 0.05f;

    // Internal tracking for where we are aiming
    private Vector3f targetBlock = null;
    private Vector3f buildBlock = null;

    public RaycastManager(Camera cam, Node rootNode, AssetManager assetManager, WorldManager worldManager) {
        this.cam = cam;
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.worldManager = worldManager;
        initOutline();
    }

    private void initOutline() {
        // Perfectly wraps a 1x1 voxel block
        Box box = new Box(0.505f, 0.505f, 0.505f);
        selectionOutline = new Geometry("SelectionOutline", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Black);
        selectionOutline.setMaterial(mat);
    }

    public void update(float tpf) {
        calculateVoxelRaycast();

        if (targetBlock != null) {
            // Blocks are centered exactly on integers. We snap the outline there.
            selectionOutline.setLocalTranslation(targetBlock.x, targetBlock.y, targetBlock.z);
            if (selectionOutline.getParent() == null) {
                rootNode.attachChild(selectionOutline);
            }
        } else {
            selectionOutline.removeFromParent();
        }
    }

    private void calculateVoxelRaycast() {
        Vector3f currentPos = cam.getLocation().clone();
        Vector3f direction = cam.getDirection().normalize().mult(STEP_SIZE);
        Vector3f previousPos = currentPos.clone();

        targetBlock = null;
        buildBlock = null;

        // Step forward incrementally through the 3D grid
        int maxSteps = (int) (REACH / STEP_SIZE);
        for (int i = 0; i < maxSteps; i++) {
            currentPos.addLocal(direction);

            // Convert exact float to Block Integer coordinate
            int bx = Math.round(currentPos.x);
            int by = Math.round(currentPos.y);
            int bz = Math.round(currentPos.z);

            byte blockId = worldManager.getBlockGlobal(bx, by, bz);

            // Ignore Air (0) and Water (5)
            if (blockId != 0 && blockId != 5) {
                targetBlock = new Vector3f(bx, by, bz);
                buildBlock = new Vector3f(Math.round(previousPos.x), Math.round(previousPos.y), Math.round(previousPos.z));
                return; // Stop stepping, we hit a wall
            }

            previousPos.set(currentPos);
        }
    }

    public void deleteBlock() {
        if (targetBlock != null) {
            worldManager.setBlockGlobal((int) targetBlock.x, (int) targetBlock.y, (int) targetBlock.z, (byte) 0);
        }
    }

    public void placeBlock(byte blockId) {
        if (buildBlock != null) {
            // Prevent placing blocks inside the player's own body
            int px = Math.round(cam.getLocation().x);
            int py = Math.round(cam.getLocation().y - 1.6f); // Player feet
            int pz = Math.round(cam.getLocation().z);

            boolean isInsidePlayer = (buildBlock.x == px && buildBlock.z == pz)
                    && (buildBlock.y == py || buildBlock.y == py + 1);

            if (!isInsidePlayer) {
                worldManager.setBlockGlobal((int) buildBlock.x, (int) buildBlock.y, (int) buildBlock.z, blockId);
            }
        }
    }
}
