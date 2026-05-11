package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;


public class MenuState extends BaseAppState {

    private SimpleApplication app;
    private Container myWindow;
    private Container logoWindow;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        
        GuiGlobals.initialize(app);
        BaseStyles.loadGlassStyle();

        // LOAD YOUR CUSTOM FONT
        // Ensure the path matches where you saved the Hiero files
        BitmapFont minekraftFont = app.getAssetManager().loadFont("Font/MineKraft.fnt");

        // APPLY TO ALL UI ELEMENTS (Labels and Buttons)
        GuiGlobals.getInstance().getStyles().getSelector("glass").set("font", minekraftFont);
        GuiGlobals.getInstance().getStyles().getSelector("glass").set("fontSize", 32f);
        GuiGlobals.getInstance().getStyles().getSelector("button", "glass").set("preferredSize", new Vector3f(400, 50, 0));

        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        // 2. Create the main container (a vertical stack)
        myWindow = new Container();
        
        // Position it in the center of the screen
        float screenWidth = app.getContext().getSettings().getWidth();
        float screenHeight = app.getContext().getSettings().getHeight();
        myWindow.setLocalTranslation(screenWidth / 2 - 100, screenHeight / 2 , 0);

        // Create a separate container just for the logo
        logoWindow = new Container();
        // THIS removes the window-shaped outline
        logoWindow.setBackground(null); 

        Label title = logoWindow.addChild(new Label(""));
        IconComponent titleIcon = new IconComponent("Textures/title_logo.png");
        // Tweak this float to change the logo size
        titleIcon.setIconScale(0.4f); 
        title.setIcon(titleIcon);

        // Position the logo wherever you want (e.g., higher up)
        logoWindow.setLocalTranslation((screenWidth / 2 - logoWindow.getPreferredSize().x / 2) - 10, screenHeight + 120, 0);

        // Don't forget to attach it in onEnable() and detach in onDisable()!

        // 3. Button: Start Game (Initializes GameState)
        Button WorldsBtn = myWindow.addChild(new Button("Worlds"));
        WorldsBtn.addClickCommands(source -> {
            // Detach main menu, attach the worlds menu
            getStateManager().detach(this);
            getStateManager().attach(new WorldsState());
        });
       
        // 4. Button: Settings (Placeholder)
        Button settingsBtn = myWindow.addChild(new Button("Settings"));
        
        // 5. Button: Credits (Placeholder)
        Button creditsBtn = myWindow.addChild(new Button("Credits"));

        // 6. Button: Quit
        Button quitBtn = myWindow.addChild(new Button("Quit"));
        quitBtn.addClickCommands(source -> System.exit(0));
        myWindow.setLocalTranslation(screenWidth / 2, screenHeight / 2, 0);
        // Change BitmapFont.Align.Center to HAlignment.Center
        WorldsBtn.setTextHAlignment(HAlignment.Center);
        settingsBtn.setTextHAlignment(HAlignment.Center);
        creditsBtn.setTextHAlignment(HAlignment.Center);
        quitBtn.setTextHAlignment(HAlignment.Center);

        // Change BitmapFont.VAlign.Center to VAlignment.Center
        WorldsBtn.setTextVAlignment(VAlignment.Center);
        settingsBtn.setTextVAlignment(VAlignment.Center);
        creditsBtn.setTextVAlignment(VAlignment.Center);
        quitBtn.setTextVAlignment(VAlignment.Center);
        
        Vector3f size = myWindow.getPreferredSize();
        myWindow.setLocalTranslation(screenWidth / 2 - size.x / 2, (screenHeight / 2 + size.y / 2) - 80, 0);
    }

    private void startGame() {
        // Switch states: Kill the menu, start the game
        getStateManager().detach(this);
        getStateManager().attach(new GameState());
    }

    @Override
    protected void onEnable() {
        this.app.getGuiNode().attachChild(logoWindow);
        this.app.getGuiNode().attachChild(myWindow);
        this.app.getFlyByCamera().setDragToRotate(true);
        this.app.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        logoWindow.removeFromParent();
        myWindow.removeFromParent();
        this.app.getFlyByCamera().setDragToRotate(false);
        this.app.getInputManager().setCursorVisible(false);
    }

    @Override
    protected void cleanup(Application app) {}
}
