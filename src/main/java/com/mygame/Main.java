package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;

public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("MineKraft");
        settings.setSamples(16);
        settings.setResolution(1280, 768);
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
