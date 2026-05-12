package com.minekraft.engine;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

public class Main extends SimpleApplication {

    public static boolean fpsflag = false;
    public static boolean statsflag = false;
    public static boolean hideHud = false;
    public static boolean hideMinimap = false;

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("MineKraft");
        settings.setSamples(16);
        try {
            settings.setIcons(new java.awt.image.BufferedImage[]{
                javax.imageio.ImageIO.read(new File("assets/Textures/App Icon 1.png"))
            });
        } catch (IOException e) {
            System.err.println("Could not load window icon: " + e.getMessage());
        }

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();

        DisplayMode displayMode = device.getDisplayMode();
        int width = displayMode.getWidth();
        int height = displayMode.getHeight();

        //settings.setResolution(1280, 768);
        settings.setResolution(width, height);
        settings.setFullscreen(true);
        settings.setVSync(false);
        settings.setFrameRate(-1);
        settings.setBitsPerPixel(32);
        settings.setGammaCorrection(false);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();

    }

    @Override
    public void simpleInitApp() {

        getInputManager().deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);

        cam.setFrustumPerspective(90f, (float) cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
        setDisplayFps(false);
        setDisplayStatView(false);
        stateManager.attach(new MenuState());

    }

    @Override
    public void simpleUpdate(float tpf) {
        // Keep this empty; logic is now in GameState.update()

    }

    @Override
    public void simpleRender(RenderManager rm) {
        // Custom rendering code if needed
    }
}
