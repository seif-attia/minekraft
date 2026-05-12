/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

/**
 *
 * @author EyonMiner
 */
public class MinimapManager {

    private Camera minimapCam;
    private ViewPort minimapView;
    private RenderManager renderManager;

    public MinimapManager(RenderManager renderManager, Camera mainCam, Node worldNode) {

        // Clone the main camera
        minimapCam = mainCam.clone();
        this.renderManager = renderManager;

        //  Set the screen region (Top Right Corner)
        minimapCam.setViewPort(0.78f, 0.98f, 0.78f, 0.98f);

        // Make it flat/orthographic
        minimapCam.setParallelProjection(true);
        float zoom = 48f; // The view radius in blocks
        minimapCam.setFrustum(-100, 1000, -zoom, zoom, zoom, -zoom);

        // Point it straight down. UNIT_Z keeps North facing UP on the screen.
        minimapCam.lookAtDirection(new Vector3f(0, -1, 0), Vector3f.UNIT_Z);

        // Create the ViewPort overlay
        minimapView = renderManager.createMainView("Minimap", minimapCam);
        minimapView.setClearFlags(true, true, true);
        minimapView.setBackgroundColor(ColorRGBA.DarkGray); // The background color of the map

        //Tells it to only draw the blocks 
        minimapView.attachScene(worldNode);
    }
    
    public void cleanup() {
        if (minimapView != null) {
            minimapView.clearScenes();
            renderManager.removeMainView(minimapView);
            // Optionally remove from post-processors if you added any
        }
    }

    /**
     * Gets called every frame to keep the minimap centered over the player.
     */
    public void update(Vector3f playerLocation) {
        minimapCam.setLocation(new Vector3f(playerLocation.x, 150f, playerLocation.z));
    }
}
