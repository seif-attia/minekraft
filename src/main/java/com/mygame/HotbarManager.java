/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.ui.Picture;

/**
 *
 * @author EyonMiner
 */
public class HotbarManager {

    private Node guiNode;
    private Geometry highlightBox;

    private final int SLOT_SIZE = 64;
    private final int SPACING = 8;
    private int startX;
    private int startY = 20; // 20 pixels from the bottom of the screen

    // Expected paths for your PNGs in the assets/Textures/Hotbar folder
    private String[] iconPaths = {
        "Textures/grass_side.png",
        "Textures/dirt.png",
        "Textures/stone.png",
        "Textures/planks.png",
        "Textures/glass.png",
        "Textures/bricks.png",
        "Textures/wood_side.png",
        "Textures/snow.png",
        "Textures/leaves.png"
    };

    public HotbarManager(Node guiNode, AssetManager assetManager, int screenWidth) {
        this.guiNode = guiNode;

        // Calculate the starting X so the hotbar is perfectly centered
        int totalWidth = (SLOT_SIZE * 9) + (SPACING * 8);
        this.startX = (screenWidth / 2) - (totalWidth / 2);

        // 1. Create the Highlight Box (Yellow square slightly larger than the slot)
        Quad quad = new Quad(SLOT_SIZE + 8, SLOT_SIZE + 8);
        highlightBox = new Geometry("Highlight", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        highlightBox.setMaterial(mat);
        guiNode.attachChild(highlightBox);

        // 2. Load and position the 9 PNGs
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

            guiNode.attachChild(pic);
        }

        // Initialize the highlight on slot 1 (Index 0)
        updateHighlight(0);
    }

    /**
     * Moves the yellow highlight box behind the selected slot.
     *
     * @param index 0 to 8
     */
    public void updateHighlight(int index) {
        float xPos = startX + (index * (SLOT_SIZE + SPACING)) - 4; // -4 to center the larger box
        float yPos = startY - 4;
        highlightBox.setLocalTranslation(xPos, yPos, -1); // Z=-1 keeps it behind the PNGs
    }
}
