package com.minekraft.engine;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.ui.Picture;

public class HotbarManager {
    private Node guiNode;
    private Node hotbarNode;       // NEW: groups all hotbar children
    private Geometry highlightBox;
    private final int SLOT_SIZE = 48;
    private final int SPACING = 8;
    private int startX;
    private int startY = 20;
    private String[] iconPaths = {
        "Textures/HotbarGrass_side.png",
        "Textures/HotbarDirt.png",
        "Textures/HotbarStone.png",
        "Textures/HotbarPlanks.png",
        "Textures/HotbarGlass.png",
        "Textures/HotbarBricks.png",
        "Textures/HotbarLog_side.png",
        "Textures/HotBarSnow.png",
        "Textures/HotbarLeaves.png"
    };

    public HotbarManager(Node guiNode, AssetManager assetManager, int screenWidth) {
        this.guiNode = guiNode;
        this.hotbarNode = new Node("HotbarNode");  // NEW

        int totalWidth = (SLOT_SIZE * 9) + (SPACING * 8);
        this.startX = (screenWidth / 2) - (totalWidth / 2);

        Quad quad = new Quad(SLOT_SIZE + 8, SLOT_SIZE + 8);
        highlightBox = new Geometry("Highlight", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        highlightBox.setMaterial(mat);
        hotbarNode.attachChild(highlightBox);      // attach to hotbarNode, not guiNode

        for (int i = 0; i < 9; i++) {
            Picture pic = new Picture("Slot_" + i);
            try {
                pic.setImage(assetManager, iconPaths[i], true);
            } catch (Exception e) {
                System.err.println("Could not load icon: " + iconPaths[i]);
            }
            pic.setWidth(SLOT_SIZE);
            pic.setHeight(SLOT_SIZE);
            float xPos = startX + (i * (SLOT_SIZE + SPACING));
            pic.setPosition(xPos, startY);
            hotbarNode.attachChild(pic);           // attach to hotbarNode, not guiNode
        }

        guiNode.attachChild(hotbarNode);           // attach the group to guiNode once
        updateHighlight(0);
    }
    public void cleanup() {
    hotbarNode.removeFromParent();
}

    public void updateHighlight(int index) {
        float xPos = startX + (index * (SLOT_SIZE + SPACING)) - 4;
        float yPos = startY - 4;
        highlightBox.setLocalTranslation(xPos, yPos, -1);
    }

    // NEW
    public void setVisible(boolean visible) {
        hotbarNode.setCullHint(visible
            ? Spatial.CullHint.Inherit
            : Spatial.CullHint.Always);
    }
}