package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.WireBox;
import com.jme3.math.Vector3f;
import com.mygame.RaycastManager;
import com.mygame.RaycastResult;

public class SelectionManager {

    private Geometry selectionObject;
    private Node rootNode;
    private RaycastManager raycastManager;
    private Player player;

    public SelectionManager(Node rootNode, AssetManager assetManager, RaycastManager raycastManager, Player player) {
        this.rootNode = rootNode;
        this.raycastManager = raycastManager;
        this.player = player;

        // Create a wireframe box slightly larger than 1x1x1 to prevent z-fighting
        WireBox wireBox = new WireBox(0.505f, 0.505f, 0.505f);
        selectionObject = new Geometry("SelectionBox", wireBox);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Black);
        mat.getAdditionalRenderState().setLineWidth(2f);
        selectionObject.setMaterial(mat);
    }

    public void update() {
        RaycastResult result = raycastManager.currentResult;

        if (result != null && !player.isGhostMode) {
            // If the ray hit a block, show the box and move it to the block center
            if (selectionObject.getParent() == null) {
                rootNode.attachChild(selectionObject);
            }

            // Offset by 0.5 because the WireBox center is 0,0,0
            Vector3f pos = result.blockPos;
            selectionObject.setLocalTranslation(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f);
        } else {
            // Hide the box if we aren't looking at anything
            if (selectionObject.getParent() != null) {
                selectionObject.removeFromParent();
            }
        }
    }
}
