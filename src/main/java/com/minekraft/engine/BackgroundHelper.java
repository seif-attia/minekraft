package com.minekraft.engine;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;

public class BackgroundHelper {
    private Picture background;

    public BackgroundHelper(AssetManager assetManager, String imagePath, float screenWidth, float screenHeight) {
        background = new Picture("Background");
        background.setImage(assetManager, imagePath, false);
        background.setWidth(screenWidth);
        background.setHeight(screenHeight);
        background.setPosition(0, 0);
        background.setLocalTranslation(0, 0, -1); // Z=-1 keeps it behind everything
    }

    public void attach(Node guiNode) {
        guiNode.attachChild(background);
    }

    public void detach() {
        background.removeFromParent();
    }
}