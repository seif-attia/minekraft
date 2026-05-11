package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.post.filters.FogFilter;

public class GameState extends BaseAppState {

    private SimpleApplication app;
    private WorldManager myWorld;
    private Camera cam;
    private Node rootNode;
    private MinimapManager minimap;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private AssetManager assetManager;

    @Override
    protected void initialize(Application app) {
        // Cast to SimpleApplication to access rootNode, assetManager, etc.
        this.app = (SimpleApplication) app;
        this.cam = this.app.getCamera();
        this.rootNode = this.app.getRootNode();
        this.renderManager = this.app.getRenderManager();
        this.viewPort = this.app.getViewPort();
        this.assetManager = this.app.getAssetManager();

        // Initialize the world logic moved from Main.simpleInitApp
        myWorld = new WorldManager(this.app, rootNode, this.app.getAssetManager());

        minimap = new MinimapManager(renderManager, cam, myWorld.getWorldNode());

        // Setup camera
        cam.setLocation(new Vector3f(-10, 200, -10));
        cam.lookAt(new Vector3f(24, 0, 24), Vector3f.UNIT_Y);
        this.app.getFlyByCamera().setMoveSpeed(70f);

        // Setup Fog
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        FogFilter fog = new FogFilter();

        // Match the fog color to a light blue sky
        fog.setFogColor(new ColorRGBA(0.5f, 0.6f, 0.8f, 1.0f));

        // How far away the fog starts getting thick
        fog.setFogDistance(150);
        // How dense the fog is (higher = harder to see through)
        fog.setFogDensity(1f);

        fpp.addFilter(fog);
        viewPort.addProcessor(fpp);

        // Also change your background color so the sky matches the fog!
        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.6f, 0.8f, 1.0f));
    }

    @Override
    public void update(float tpf) {
        // The game loop logic moved from Main.simpleUpdate
        if (myWorld != null) {
            myWorld.update(cam.getLocation());
        }

        if (minimap != null) {
            minimap.update(cam.getLocation());
        }
    }

    @Override
    protected void cleanup(Application app) {
        // Clean up the world when this state is detached
        if (myWorld != null) {
            myWorld.destroy();
        }
    }

    @Override
    protected void onEnable() {
        // Logic for when the game is unpaused or shown
    }

    @Override
    protected void onDisable() {
        // Logic for when the game is paused or hidden
    }
}
