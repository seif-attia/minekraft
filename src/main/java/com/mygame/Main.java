package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("MineKraft");

        // 4x MSAA
        settings.setSamples(4);

        settings.setResolution(1280, 768);
        settings.setSamples(16);
        settings.setVSync(false);
        settings.setFrameRate(-1);
        settings.setBitsPerPixel(32);
        settings.setGammaCorrection(false);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    private WorldManager myWorld;

    @Override
    public void simpleInitApp() {
        myWorld = new WorldManager(this, rootNode, assetManager);

        cam.setLocation(new com.jme3.math.Vector3f(-10, 50, -10));
        cam.lookAt(new com.jme3.math.Vector3f(24, 0, 24), com.jme3.math.Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(70f);
    }

    @Override
    public void simpleUpdate(float tpf) {
        myWorld.update(cam.getLocation());
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    @Override
    public void destroy() {
        if (myWorld != null) {
            myWorld.destroy();
        }
        super.destroy(); // for jME clean-up
    }
}
