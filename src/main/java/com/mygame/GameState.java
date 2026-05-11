package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterBoxShape;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
import java.util.HashSet;

public class GameState extends BaseAppState {

    private SimpleApplication app;
    private WorldManager myWorld;
    private Camera cam;
    private Node rootNode;
    private MinimapManager minimap;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private AssetManager assetManager;
    private InputManager inputManager;

    // Sun
    private Geometry sunGeom;
    private Vector3f lightDir = new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal();

    // Clouds
    private Spatial cloudLayer;
    private float cloudTimer = 0;

    // Dust and god rays
    private ParticleEmitter ambientDust;
    private LightScatteringFilter godRays;
    private DirectionalLight sun;

    @Override
    protected void initialize(Application app) {
        // Cast to SimpleApplication to access rootNode, assetManager, etc.
        this.app = (SimpleApplication) app;
        this.cam = this.app.getCamera();
        this.rootNode = this.app.getRootNode();
        this.renderManager = this.app.getRenderManager();
        this.viewPort = this.app.getViewPort();
        this.assetManager = this.app.getAssetManager();
        this.inputManager = this.app.getInputManager();

        // Initialize the world logic moved from Main.simpleInitApp
        myWorld = new WorldManager(this.app, rootNode, this.app.getAssetManager());

        minimap = new MinimapManager(renderManager, cam, myWorld.getWorldNode());

        // Setup camera
        cam.setLocation(new Vector3f(-10, 200, -10));
        cam.lookAt(new Vector3f(24, 0, 24), Vector3f.UNIT_Y);
        this.app.getFlyByCamera().setMoveSpeed(70f);

        // Sun 
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(al);

        // THE SUN (Directional Light)
        sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White.mult(1.2f)); // Slightly brighter than white
        // Pointing down and slightly to the side to cast cool angled shadows
        sun.setDirection(new Vector3f(-0.8f, -0.4f, -0.3f).normalizeLocal());
        rootNode.addLight(sun);

        // Setup sun object
        Sphere sunBox = new Sphere(32, 32, 30f);
        sunGeom = new Geometry("Sun", sunBox);
        Material sunMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        sunMat.setColor("Color", new ColorRGBA(4.0f, 3.8f, 2.5f, 1.0f)); // Warm yellow/white
        sunGeom.setMaterial(sunMat);

        sunGeom.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);

        rootNode.attachChild(sunGeom);

        float sunDistance = 800f;
        Vector3f sunOrigin = sun.getDirection().mult(-sunDistance);
        sunGeom.setLocalTranslation(sunOrigin);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        // 2. THE SHADOW FILTER
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 2048, 3);
        dlsf.setLight(sun);
        dlsf.setShadowIntensity(0.35f);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
        dlsf.setShadowZExtend(250f);
        dlsf.setEdgesThickness(10);
        fpp.addFilter(dlsf);

        // 2. Create the God Rays filter
        // 1. ADD BLOOM (Makes the Sun physically glow like a star)
        BloomFilter bloom = new BloomFilter();
        bloom.setBloomIntensity(1.5f); // How bright the glow is
        bloom.setExposurePower(4.0f);
        bloom.setBlurScale(1.2f);      // How far the glow spreads
        fpp.addFilter(bloom);
        // THE GOD RAYS
        LightScatteringFilter lsf = new LightScatteringFilter(sunOrigin);

        // Push the density even higher to force the light to stretch further
        lsf.setLightDensity(1.8f);

        // Keep the shafts fat
        lsf.setBlurWidth(0.8f);

        // --- THE ACTUAL jME QUALITY MULTIPLIER ---
        // By default, jME only samples the light 50 times. 
        // Doubling this to 100 or 150 makes the rays drastically denser, smoother, 
        // and much more noticeable as they drag across the screen.
        lsf.setNbSamples(120);

        fpp.addFilter(lsf);
        this.godRays = lsf;

        ////////////////////////////////////////////////
        // 3. THE FOG FILTER
        FogFilter fog = new FogFilter();
        fog.setFogColor(new ColorRGBA(0.5f, 0.6f, 0.8f, 1.0f));
        fog.setFogDistance(150);
        fog.setFogDensity(1f);

        fpp.addFilter(fog);

        // 4. ATTACH TO VIEWPORT
        viewPort.addProcessor(fpp);

        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.6f, 0.8f, 1.0f));

        // init clouds
        cloudLayer = CloudFactory.createClouds(assetManager);
        cloudLayer.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        rootNode.attachChild(cloudLayer);

        // dust particles
        // 1. Create the Emitter (Allows up to 400 dust motes on screen at once)
        ParticleEmitter dust = new ParticleEmitter("AmbientDust", ParticleMesh.Type.Triangle, 800);

        // 2. Set the Material
        Material dustMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        dustMat.setTexture("Texture", assetManager.loadTexture("Textures/dust.png"));
        dust.setMaterial(dustMat);

        // 3. Make them spawn in a massive 30x30x30 box
        dust.setShape(new EmitterBoxShape(new Vector3f(-15f, -15f, -15f), new Vector3f(15f, 15f, 15f)));
        dust.setImagesX(1);
        dust.setImagesY(1);

        // 4. The Physics of Dust
        dust.setGravity(0, -0.05f, 0); // Barely any gravity, so they float
        dust.setLowLife(4f);  // Dust lives for at least 4 seconds
        dust.setHighLife(8f); // Dust lives for up to 8 seconds

        // Give them a tiny bit of random drifting speed
        dust.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 0.2f, 0));
        dust.getParticleInfluencer().setVelocityVariation(1f);

        // Make them tiny, and fade them out smoothly before they die
        dust.setStartSize(0.12f);
        dust.setEndSize(0.06f);
        dust.setStartColor(new ColorRGBA(1f, 1f, 1f, 0.8f)); // 40% transparent white
        dust.setEndColor(new ColorRGBA(1f, 1f, 1f, 0f));     // Fades to invisible

        // 5. Attach to the world
        rootNode.attachChild(dust);

        // Note: We have to make this a global variable so the update loop can see it!
        this.ambientDust = dust;

        // wireframe toggle
        inputManager.addMapping("ToggleWireframe", new KeyTrigger(KeyInput.KEY_X));

        // 2. Tell the InputManager what to do when "ToggleWireframe" is triggered
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                // We only want it to trigger once when the key is PRESSED, not when released
                if (name.equals("ToggleWireframe") && isPressed) {
                    myWorld.toggleWireframe();
                }
            }
        }, "ToggleWireframe");

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

        if (ambientDust != null) {
            ambientDust.setLocalTranslation(cam.getLocation());
        }

        // Make the Sun follow the player
        if (sunGeom != null && godRays != null) {
            float sunDistance = 800f;

            // Camera Position + (Reversed Light Direction * Distance)
            Vector3f newSunPos = cam.getLocation().add(sun.getDirection().mult(-sunDistance));

            // Move the glowing sphere
            sunGeom.setLocalTranslation(newSunPos);

            // Move the God Rays origin so they follow the sphere perfectly
            godRays.setLightPosition(newSunPos);
        }

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
