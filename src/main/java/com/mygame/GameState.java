package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.post.filters.FogFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;

public class GameState extends BaseAppState {

    private SimpleApplication app;
    private WorldManager myWorld;
    private Camera cam;
    private Node rootNode;
    private MinimapManager minimap;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private AssetManager assetManager;

    // Sun
    private Geometry sunGeom;
    private Vector3f lightDir = new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal();

    // Clouds
    private Spatial cloudLayer;
    private float cloudTimer = 0;

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

        // Sun and shadows
        // AMBIENT LIGHT (Prevents shadows from being 100% pitch black)
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(al);

        // THE SUN (Directional Light)
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White.mult(1.2f)); // Slightly brighter than white
        // Pointing down and slightly to the side to cast cool angled shadows
        sun.setDirection(new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal());
        rootNode.addLight(sun);

        // SHADOW RENDERER
        final int SHADOWMAP_SIZE = 2048; // Crisp shadow resolution
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.5f);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        dlsr.setShadowZExtend(150f);

        viewPort.addProcessor(dlsr);

        // Setup sun object
        Sphere sunBox = new Sphere(5, 10, 10);
        sunGeom = new Geometry("Sun", sunBox);
        Material sunMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        sunMat.setColor("Color", new ColorRGBA(1.0f, 0.9f, 0.6f, 1.0f)); // Warm yellow/white
        sunGeom.setMaterial(sunMat);

        sunGeom.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);

        rootNode.attachChild(sunGeom);

        // init clouds
        cloudLayer = CloudFactory.createClouds(assetManager);
        rootNode.attachChild(cloudLayer);

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

        // Make the Sun follow the player
        Vector3f sunPosition = lightDir.mult(-300f).add(cam.getLocation());
        sunGeom.setLocalTranslation(sunPosition);

        // Cloud moving / drifting logic
        cloudTimer += tpf * 0.5f;

        float camX = cam.getLocation().x;
        float camZ = cam.getLocation().z;

        float snapX = (float) Math.floor((camX - cloudTimer + 500f) / 1000f) * 1000f;
        float snapZ = (float) Math.floor((camZ + 500f) / 1000f) * 1000f;

        cloudLayer.setLocalTranslation(snapX - 1200 + cloudTimer, 350, snapZ - 1200);
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
