package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.GuiGlobals;

public class PauseState extends BaseAppState implements ActionListener {

    private SimpleApplication app;
    private Container pauseWindow;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        float screenWidth = app.getContext().getSettings().getWidth();
        float screenHeight = app.getContext().getSettings().getHeight();

        pauseWindow = new Container();
        Vector3f buttonSize = new Vector3f(500, 60, 0);
        Insets3f spaceBetween = new Insets3f(10, 10, 10, 10);

        // 1. Settings Buttons (Mirrors SettingsState)
        Checkbox hudbtn = pauseWindow.addChild(new Checkbox("Hide HUD"));
        configureSetting(hudbtn, buttonSize, spaceBetween);
        hudbtn.getModel().setChecked(Main.hideHud);
        hudbtn.addClickCommands(source -> {
            Main.hideHud = hudbtn.getModel().isChecked();
            GameState gs = getStateManager().getState(GameState.class);
            if (gs != null) {
                gs.setHudVisible(!Main.hideHud);
            }
        });
        
        Checkbox minimapbtn = pauseWindow.addChild(new Checkbox("Hide Minimap"));
        configureSetting(minimapbtn, buttonSize, spaceBetween);
        minimapbtn.getModel().setChecked(Main.hideMinimap);
        minimapbtn.addClickCommands(source -> {
            Main.hideMinimap = minimapbtn.getModel().isChecked();
            GameState gs = getStateManager().getState(GameState.class);
            if (gs != null) {
                gs.setMinimapEnabled(!Main.hideMinimap);
            }
        });
        
        Checkbox fpsbtn = pauseWindow.addChild(new Checkbox("Show FPS"));
        configureSetting(fpsbtn, buttonSize, spaceBetween);
        fpsbtn.getModel().setChecked(Main.fpsflag);
        fpsbtn.addClickCommands(source -> Main.fpsflag = fpsbtn.getModel().isChecked());
        
        Checkbox statsbtn = pauseWindow.addChild(new Checkbox("Show Stats"));
        configureSetting(statsbtn, buttonSize, spaceBetween);
        statsbtn.getModel().setChecked(Main.statsflag);
        statsbtn.addClickCommands(source -> Main.statsflag = statsbtn.getModel().isChecked());

        // 2. Resume & Exit Buttons
        Button resumeBtn = pauseWindow.addChild(new Button("Resume Game"));
        configureSetting(resumeBtn, buttonSize, spaceBetween);
        resumeBtn.addClickCommands(source -> resumeGame());

        Button exitBtn = pauseWindow.addChild(new Button("Exit to Menu"));
        configureSetting(exitBtn, buttonSize, spaceBetween);
        exitBtn.addClickCommands(source -> {
            // Detach the game, detach the pause menu, and load the main menu
            getStateManager().detach(getStateManager().getState(GameState.class));
            getStateManager().detach(this);
            getStateManager().attach(new MenuState()); 
        });

        // Center window
        Vector3f size = pauseWindow.getPreferredSize();
        pauseWindow.setLocalTranslation(screenWidth / 2 - size.x / 2, screenHeight / 2 + size.y / 2, 0);

        // Map ESC to unpause while this menu is open
        app.getInputManager().addMapping("UnpauseGame", new KeyTrigger(KeyInput.KEY_ESCAPE));
    }

    private void configureSetting(com.simsilica.lemur.Panel p, Vector3f size, Insets3f insets) {
        p.setPreferredSize(size);
        p.setInsets(insets);
        if (p instanceof Checkbox) {
            ((Checkbox)p).setTextHAlignment(HAlignment.Center); 
            ((Checkbox)p).setTextVAlignment(VAlignment.Center);
        } else if (p instanceof Button) {
            ((Button)p).setTextHAlignment(HAlignment.Center); 
            ((Button)p).setTextVAlignment(VAlignment.Center);
        }
    }

    @Override
    protected void onEnable() {
        app.getGuiNode().attachChild(pauseWindow);
        
        // Show cursor so we can click buttons
        app.getInputManager().setCursorVisible(true);
        
        // Pause the GameState (this stops its update loop)
        GameState gameState = getStateManager().getState(GameState.class);
        if (gameState != null) {
            gameState.setEnabled(false);
        }

        app.getInputManager().addListener(this, "UnpauseGame");
    }

    @Override
    protected void onDisable() {
        pauseWindow.removeFromParent();
        GuiGlobals.getInstance().getFocusManagerState().setFocus(null);
        app.getInputManager().removeListener(this);
    }

    @Override
    protected void cleanup(Application app) {
        app.getInputManager().deleteMapping("UnpauseGame");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("UnpauseGame") && isPressed && isEnabled()) {
            resumeGame();
        }
    }

    private void resumeGame() {
        // Re-enable GameState
        GameState gameState = getStateManager().getState(GameState.class);
        if (gameState != null) {
            gameState.setEnabled(true);
            // Hide the cursor again
            app.getInputManager().setCursorVisible(false);
        }
        // Destroy the pause menu
        getStateManager().detach(this);
    }
}