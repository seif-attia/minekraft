package com.minekraft.engine;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.component.IconComponent;

public class CreditsState extends BaseAppState {

    private SimpleApplication app;
    private Container creditsWindow;
    private Container backgroundWindow;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;

        // Grab the current actual screen resolution
        float screenWidth = app.getContext().getSettings().getWidth();
        float screenHeight = app.getContext().getSettings().getHeight();

        // ==========================================
        // 1. FULL SCREEN BACKGROUND FIX
        // ==========================================
        backgroundWindow = new Container();

        // Force the container itself to be exactly the size of the screen
        backgroundWindow.setPreferredSize(new Vector3f(screenWidth, screenHeight, 0));

        // Apply the image directly as the background and FORCE it to fit the screen dimensions.
        // This will automatically stretch or compress the image on 720p, 1080p, 4K, etc.
        IconComponent bgIcon = new IconComponent("Textures/Credits.png");
        bgIcon.setIconSize(new Vector2f(screenWidth, screenHeight));
        backgroundWindow.setBackground(bgIcon);

        // JME draws UI downwards from the origin point. 
        // We set the origin to the top-left of the screen and push it back slightly on the Z-axis.
        backgroundWindow.setLocalTranslation(0, screenHeight, -10);

        // ==========================================
        // 2. DYNAMIC BUTTON POSITIONING FIX
        // ==========================================
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

        // Get the final calculated size of the button
        Vector3f size = creditsWindow.getPreferredSize();

        // Instead of hardcoded pixels, we use margins based on 5% of the screen size.
        // This pins the button to the bottom-left corner regardless of resolution.
        float marginX = screenWidth * 0.05f;
        float marginY = screenHeight * 0.05f;

        creditsWindow.setLocalTranslation(
                marginX, // 5% inwards from the left side
                size.y + marginY, // 5% upwards from the bottom
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
    protected void cleanup(Application app) {
    }
}
