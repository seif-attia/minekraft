package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.DefaultCheckboxModel;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.GuiGlobals;

public class SettingsState extends BaseAppState {

    private SimpleApplication app;
    private Container settingsWindow;
    public boolean fpsflag = false;
    public boolean statsflag = false;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        float screenWidth = app.getContext().getSettings().getWidth();
        float screenHeight = app.getContext().getSettings().getHeight();

        settingsWindow = new Container();
        Vector3f buttonSize = new Vector3f(500, 60, 0);
        com.simsilica.lemur.Insets3f spaceBetween = new com.simsilica.lemur.Insets3f(10, 10, 10, 10);

        // 1. Add 4 Checkbox buttons (Placeholders)
        // Checkboxes in Lemur automatically handle their own "checked" state
        
        Checkbox hudbtn = settingsWindow.addChild(new Checkbox("Hide HUD"));
        configureSetting(hudbtn, buttonSize, spaceBetween);
        
        Checkbox minimapbtn = settingsWindow.addChild(new Checkbox("Hide Minimap"));
        configureSetting(minimapbtn, buttonSize, spaceBetween);
        minimapbtn.getModel().setChecked(Main.hideMinimap);
        minimapbtn.addClickCommands(source -> Main.hideMinimap = minimapbtn.getModel().isChecked());
        
        Checkbox fpsbtn = settingsWindow.addChild(new Checkbox("Show FPS"));
        configureSetting(fpsbtn, buttonSize, spaceBetween);
        fpsbtn.getModel().setChecked(Main.fpsflag);
        fpsbtn.addClickCommands(source -> Main.fpsflag = fpsbtn.getModel().isChecked());
        
        Checkbox statsbtn = settingsWindow.addChild(new Checkbox("Show Stats"));
        configureSetting(statsbtn, buttonSize, spaceBetween);
        statsbtn.getModel().setChecked(Main.statsflag);
        statsbtn.addClickCommands(source -> Main.statsflag = statsbtn.getModel().isChecked());

        // 2. Add a normal Back button
        Button backBtn = settingsWindow.addChild(new Button("Back"));
        backBtn.setTextHAlignment(HAlignment.Center);
        backBtn.setTextVAlignment(VAlignment.Center);
        backBtn.addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuState());
        });

        // Position the container in the center
        Vector3f size = settingsWindow.getPreferredSize();
        settingsWindow.setLocalTranslation(screenWidth / 2 - size.x / 2, screenHeight / 2 + size.y / 2, 0);
    }
    
    private void configureSetting(Checkbox c, Vector3f size, Insets3f insets) {
        c.setPreferredSize(size); // Sets the button dimensions
        c.setInsets(insets);     // Sets the outer spacing
        c.setTextHAlignment(HAlignment.Center); 
        c.setTextVAlignment(VAlignment.Center);
    }

    @Override
    protected void onEnable() {
        this.app.getGuiNode().attachChild(settingsWindow);
        // Ensure the mouse is visible so you can click the checkboxes
        this.app.getFlyByCamera().setDragToRotate(true);
        this.app.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        settingsWindow.removeFromParent();
        this.app.getFlyByCamera().setDragToRotate(false);
        this.app.getInputManager().setCursorVisible(false);
        GuiGlobals.getInstance().getFocusManagerState().setFocus(null);
    }

    @Override
    protected void cleanup(Application app) {}
}

