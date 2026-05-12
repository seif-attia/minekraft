package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.GuiGlobals;

public class WorldsState extends BaseAppState {

    private SimpleApplication app;
    private Container worldsWindow;
    private BackgroundHelper background;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        float screenWidth = app.getContext().getSettings().getWidth();
        float screenHeight = app.getContext().getSettings().getHeight();

        worldsWindow = new Container();
        worldsWindow.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        
        background = new BackgroundHelper(
        app.getAssetManager(),
        "Textures/Background.jpeg",   
        app.getContext().getSettings().getWidth(),
        app.getContext().getSettings().getHeight()
    );

        // 1. Button: Enter Game (The one that starts the actual game)
        Button enterGameBtn = worldsWindow.addChild(new Button("Create new world"));
        enterGameBtn.setTextHAlignment(HAlignment.Center);
        enterGameBtn.setTextVAlignment(VAlignment.Center);
        
        enterGameBtn.addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new GameState());
        });

        // 2. Button: Back (To return to Main Menu)
        Button backBtn = worldsWindow.addChild(new Button("Back"));
        backBtn.setTextHAlignment(HAlignment.Center);
        backBtn.setTextVAlignment(VAlignment.Center);
        
        backBtn.addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuState());
        });

        // Position the container
        Vector3f size = worldsWindow.getPreferredSize();
        worldsWindow.setLocalTranslation(screenWidth / 2 - size.x / 2, (screenHeight / 2 + size.y / 2) - 300, 0);
    }

    @Override
    protected void onEnable() {
        background.attach(app.getGuiNode());
        this.app.getGuiNode().attachChild(worldsWindow);
        this.app.getFlyByCamera().setDragToRotate(true);
        this.app.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        worldsWindow.removeFromParent();
        this.app.getFlyByCamera().setDragToRotate(false);
        this.app.getInputManager().setCursorVisible(false);
        GuiGlobals.getInstance().getFocusManagerState().setFocus(null);
        background.detach();
    }

    @Override
    protected void cleanup(Application app) {}
}
