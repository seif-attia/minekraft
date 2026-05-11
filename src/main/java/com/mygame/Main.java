package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import java.io.File;
import java.io.IOException;

public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("MineKraft");
        settings.setSamples(16);
        try {
        // This looks for your icon in the assets folder
        // Use a 256x256 PNG for the best result
        settings.setIcons(new java.awt.image.BufferedImage[]{
            javax.imageio.ImageIO.read(new File("assets/Textures/App Icon 1.png"))
        });
        } catch (IOException e) {
            System.err.println("Could not load window icon: " + e.getMessage());
        }
        settings.setResolution(1280, 768);
//        settings.setResolution(1920, 1080);
//        settings.setFullscreen(true);
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

        setDisplayFps(true);
        setDisplayStatView(true);
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
