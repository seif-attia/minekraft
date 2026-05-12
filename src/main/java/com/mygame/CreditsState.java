package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.Label;

public class CreditsState extends BaseAppState {

    private SimpleApplication app;
    private Container creditsWindow;
    private Container backgroundWindow;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        float screenWidth = app.getContext().getSettings().getWidth();
        float screenHeight = app.getContext().getSettings().getHeight();

        // Background image (full screen)
        backgroundWindow = new Container();
        backgroundWindow.setBackground(null);
        Label bg = backgroundWindow.addChild(new Label(""));
        IconComponent bgIcon = new IconComponent("Textures/Credits.png");
        bgIcon.setIconScale(1.06f); // Adjust scale to fit your image
        bg.setIcon(bgIcon);
        backgroundWindow.setLocalTranslation(0, screenHeight, 0);

        // Buttons container
        creditsWindow = new Container();
        Vector3f buttonSize = new Vector3f(200, 60, 0);
        Insets3f spacing = new Insets3f(10, 10, 10, 10);

        Button backBtn = creditsWindow.addChild(new Button("Back"));
        backBtn.setPreferredSize(buttonSize);
        backBtn.setInsets(spacing);
        backBtn.setTextHAlignment(HAlignment.Center);
        backBtn.setTextVAlignment(VAlignment.Center);
        backBtn.addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuState());
        });

        // Position the back button at the bottom center
        Vector3f size = creditsWindow.getPreferredSize();
        creditsWindow.setLocalTranslation(
            (screenWidth / 2 - size.x / 2) - 530,
            size.y + 700,
            0
        );
    }

    @Override
    protected void onEnable() {
        app.getGuiNode().attachChild(backgroundWindow);
        app.getGuiNode().attachChild(creditsWindow);
        app.getFlyByCamera().setDragToRotate(true);
        app.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        backgroundWindow.removeFromParent();
        creditsWindow.removeFromParent();
        app.getFlyByCamera().setDragToRotate(false);
        app.getInputManager().setCursorVisible(false);
        GuiGlobals.getInstance().getFocusManagerState().setFocus(null);
    }

    @Override
    protected void cleanup(Application app) {}
}