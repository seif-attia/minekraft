package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterBoxShape;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
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

public class GameState extends BaseAppState implements ActionListener, AnalogListener {

    private SimpleApplication app;
    private WorldManager myWorld;
    private Camera cam;
    private Node rootNode;
    private MinimapManager minimap;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private AssetManager assetManager;
    private InputManager inputManager;
    private PhysicsEngine physicsEngine;
    private SelectionManager selectionManager;
    private Node guiNode;

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

    private Player player;
    private MovementManager movementManager;
    private RaycastManager raycastManager;
    private FlyByCamera flyCam;

    private HotbarManager hotbarManager;

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
        this.flyCam = this.app.getFlyByCamera();
        this.guiNode = this.app.getGuiNode();

        // Initialize the world logic moved from Main.simpleInitApp
        myWorld = new WorldManager(this.app, rootNode, this.app.getAssetManager());

        minimap = new MinimapManager(renderManager, cam, myWorld.getWorldNode());

        //physics and raycast manager init
        // 1. Initialize Managers
        player = new Player();
        movementManager = new MovementManager(player);
        physicsEngine = new PhysicsEngine(player, myWorld, movementManager);
        raycastManager = new RaycastManager(cam, myWorld);
        selectionManager = new SelectionManager(rootNode, assetManager, raycastManager, player);

        // 2. Setup Inputs 
        initKeys();

        initCrosshair();

        hotbarManager = new HotbarManager(this.guiNode, assetManager, cam.getWidth());

        // Disable the default flycam so our Player.java rotation math takes over
        flyCam.setEnabled(false);
        app.getInputManager().setCursorVisible(false);
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
        fog.setFogDistance(300);
        fog.setFogDensity(0.7f);

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

        // --- 1. THE MISSING PHYSICS AND MOVEMENT UPDATES ---
        physicsEngine.update(tpf);
        raycastManager.update(tpf);
        selectionManager.update();

        // --- 2. SYNC CAMERA TO PLAYER ---
        // Move the camera to the player's body position + 1.6f units up for eye level
        cam.setLocation(player.position.add(0, 1.6f, 0));

        // Rotate the camera based on mouse movement (pitch and yaw)
        Quaternion q = new Quaternion();
        q.fromAngles(player.pitch, player.yaw, 0);
        cam.setRotation(q);

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
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("Forward")) {
            movementManager.setForward(isPressed);
        } else if (name.equals("Back")) {
            movementManager.setBack(isPressed);
        } else if (name.equals("Left")) {
            movementManager.setLeft(isPressed);
        } else if (name.equals("Right")) {
            movementManager.setRight(isPressed);
        } else if (name.equals("Jump")) {
            player.wantsToJump = isPressed;
        } else if (isPressed && name.startsWith("Slot")) {
            int slotNumber = Integer.parseInt(name.replace("Slot", ""));
            int slotIndex = slotNumber - 1; // 0 to 8 for the UI

            // Map the slot number to your specific Block IDs
            switch (slotNumber) {
                case 1:
                    player.selectedBlockId = 1;
                    break;  // Grass
                case 2:
                    player.selectedBlockId = 3;
                    break;  // Dirt
                case 3:
                    player.selectedBlockId = 4;
                    break;  // Stone
                case 4:
                    player.selectedBlockId = 13;
                    break; // Planks
                case 5:
                    player.selectedBlockId = 12;
                    break; // Glass
                case 6:
                    player.selectedBlockId = 11;
                    break; // Bricks
                case 7:
                    player.selectedBlockId = 7;
                    break;  // Wood
                case 8:
                    player.selectedBlockId = 6;
                    break;  // Snow
                case 9:
                    player.selectedBlockId = 9;
                    break;  // Leaves
            }

            // Move the visual highlight
            hotbarManager.updateHighlight(slotIndex);
        } else if (name.equals("ToggleGhost") && isPressed) {
            player.toggleGhostMode();
            System.out.println("Ghost Mode: " + (player.isGhostMode ? "ON" : "OFF"));
        } // Raycasting Actions
        else if (name.equals("Delete") && isPressed) {
            if (!player.isGhostMode) {
                RaycastResult res = raycastManager.currentResult;
                if (res != null) {
                    myWorld.setBlockGlobal((int) res.blockPos.x, (int) res.blockPos.y, (int) res.blockPos.z, (byte) 0);
                }
            }
        } else if (name.equals("Place") && isPressed) {
            if (!player.isGhostMode) {
                RaycastResult res = raycastManager.currentResult;
                if (res != null) {
                    int targetX = (int) res.adjacent.x;
                    int targetY = (int) res.adjacent.y;
                    int targetZ = (int) res.adjacent.z;

                    if (!player.intersectsVoxel(targetX, targetY, targetZ)) {
                        // THE FIX: Use the selected ID instead of a hardcoded 1
                        myWorld.setBlockGlobal(targetX, targetY, targetZ, player.selectedBlockId);
                    }
                }
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (name.equals("MouseRight")) {
            player.rotate(-value, 0);
        } else if (name.equals("MouseLeft")) {
            player.rotate(value, 0);
        } else if (name.equals("MouseUp")) {
            player.rotate(0, value);
        } else if (name.equals("MouseDown")) {
            player.rotate(0, -value);
        } else if (name.equals("SpeedUp")) {
            //player.adjustSpeed(1.0f);
        } else if (name.equals("SpeedDown")) {
            //player.adjustSpeed(-1.0f);
        }
    }

    @Override
    protected void cleanup(Application app) {
        // Clean up the world when this state is detached
        if (myWorld != null) {
            myWorld.destroy();
        }

        // Remove listeners so they don't leak into other game states
        if (inputManager != null) {
            inputManager.removeListener(this);
        }

    }

    @Override
    protected void onEnable() {
        SimpleApplication app = (SimpleApplication) getApplication();
        app.setDisplayFps(Main.fpsflag);
        app.setDisplayStatView(Main.statsflag);
    }

    @Override
    protected void onDisable() {
        SimpleApplication app = (SimpleApplication) getApplication();
        app.setDisplayFps(false);
        app.setDisplayStatView(false);
    }

    private void initCrosshair() {
        // Load the default font from the asset manager
        com.jme3.font.BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        com.jme3.font.BitmapText ch = new com.jme3.font.BitmapText(guiFont, false);

        // Scale it up slightly and set the classic + symbol
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");

        // Calculate the exact center of the screen based on current resolution
        float width = app.getContext().getSettings().getWidth();
        float height = app.getContext().getSettings().getHeight();

        float x = (width / 2) - (ch.getLineWidth() / 2);
        float y = (height / 2) + (ch.getLineHeight() / 2);
        ch.setLocalTranslation(x, y, 0);

        // Attach to the GUI node so it renders on top of everything
        app.getGuiNode().attachChild(ch);
    }

    private void initKeys() {

        for (int i = 1; i <= 9; i++) {
            int keyCode = KeyInput.KEY_1 + (i - 1);
            inputManager.addMapping("Slot" + i, new KeyTrigger(keyCode));
            inputManager.addListener(this, "Slot" + i);
        }

        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("ToggleGhost", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping("Delete", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Place", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addMapping("MouseLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("MouseRight", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("MouseUp", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("MouseDown", new MouseAxisTrigger(MouseInput.AXIS_Y, false));

        inputManager.addMapping("SpeedUp", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("SpeedDown", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addListener(this, "Place", "Delete", "Forward", "Back", "Left", "Right", "Jump", "ToggleGhost");
        inputManager.addListener(this, "MouseLeft", "MouseRight", "MouseUp", "MouseDown", "SpeedUp", "SpeedDown");
    }
}
