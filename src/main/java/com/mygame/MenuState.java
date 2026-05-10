package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.style.BaseStyles;

public class MenuState extends BaseAppState {

    private SimpleApplication app;
    private Container myWindow;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;

        // 1. Initialize Lemur Globals
        GuiGlobals.initialize(app);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        // 2. Create the main container (a vertical stack)
        myWindow = new Container();
        
        // Position it in the center of the screen
        float screenWidth = app.getContext().getSettings().getWidth();
        float screenHeight = app.getContext().getSettings().getHeight();
        myWindow.setLocalTranslation(screenWidth / 2 - 100, screenHeight / 2 + 150, 0);

        // 3. Add Title Image (PNG)
        // Ensure "Textures/title_logo.png" exists in your assets folder
        Label title = myWindow.addChild(new Label(""));
        IconComponent titleIcon = new IconComponent("Textures/title_logo.png");
        titleIcon.setIconScale(0.5f);
        title.setIcon(titleIcon);

        // 4. Button: Start Game (Initializes GameState)
        Button startBtn = myWindow.addChild(new Button("Start Game"));
        startBtn.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                startGame();
            }
        });

        // 5. Button: Settings (Placeholder)
        Button settingsBtn = myWindow.addChild(new Button("Settings"));

        // 6. Button: Quit
        Button quitBtn = myWindow.addChild(new Button("Quit"));
        quitBtn.addClickCommands(source -> System.exit(0));
        myWindow.setLocalTranslation(screenWidth / 2, screenHeight / 2, 0);
        
        Vector3f size = myWindow.getPreferredSize();
    myWindow.setLocalTranslation(screenWidth / 2 - size.x / 2, screenHeight / 2 + size.y / 2, 0);
    }

    private void startGame() {
        // Switch states: Kill the menu, start the game
        getStateManager().detach(this);
        getStateManager().attach(new GameState());
    }

    @Override
    protected void onEnable() {
        this.app.getGuiNode().attachChild(myWindow);
        this.app.getFlyByCamera().setDragToRotate(true);
        this.app.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        myWindow.removeFromParent();
        this.app.getFlyByCamera().setDragToRotate(false);
        this.app.getInputManager().setCursorVisible(false);
    }

    @Override
    protected void cleanup(Application app) {}
}
