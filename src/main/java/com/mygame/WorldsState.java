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

import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;
import java.io.File;
import com.jme3.math.ColorRGBA;

import com.simsilica.lemur.GuiGlobals;

public class WorldsState extends BaseAppState {

    private SimpleApplication app;
    private Container worldsWindow;
    private Container scrollContainer;
    private TextField nameInput;
   

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        float screenWidth = app.getContext().getSettings().getWidth();
        float screenHeight = app.getContext().getSettings().getHeight();

        worldsWindow = new Container();
       
        worldsWindow.setLayout(new SpringGridLayout(Axis.Y, Axis.X));

        Label title = worldsWindow.addChild(new Label("Worlds Manager"));
        title.setTextHAlignment(HAlignment.Center);
        title.setFontSize(40f);

        
        Container createContainer = worldsWindow.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        createContainer.addChild(new Label("World Name: "));
        nameInput = createContainer.addChild(new TextField("New_World"));
        nameInput.setPreferredSize(new Vector3f(200, 30, 0));

        Button enterGameBtn = worldsWindow.addChild(new Button("Create new world"));
        enterGameBtn.setTextHAlignment(HAlignment.Center);
        
        enterGameBtn.addClickCommands(source -> {
             String inputName = nameInput.getText().trim();
            
            if (inputName.isEmpty() || inputName.equals("New_World")) {
                inputName = "World_" + System.currentTimeMillis();
            }
            
            startTheGame(inputName);
        });

     
        scrollContainer = worldsWindow.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X)));
        refreshWorldList();

      
        Button backBtn = worldsWindow.addChild(new Button("Back"));
        backBtn.addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuState());
        });

        
        Vector3f size = worldsWindow.getPreferredSize();
        worldsWindow.setLocalTranslation(screenWidth / 2 - size.x / 2, screenHeight / 2 + size.y / 2, 0);
    }

    private void refreshWorldList() {
        scrollContainer.clearChildren();
        File saveDir = new File("saves");
        if (!saveDir.exists()) saveDir.mkdirs();
        
        File[] folders = saveDir.listFiles(File::isDirectory);

        if (folders != null) {
            for (File worldFolder : folders) {
                String name = worldFolder.getName();
                Container row = scrollContainer.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
                
                Button worldBtn = row.addChild(new Button(name));
                worldBtn.setPreferredSize(new Vector3f(250, 40, 0));
                worldBtn.addClickCommands(source -> startTheGame(name));

                Button deleteBtn = row.addChild(new Button(" X "));
                deleteBtn.setColor(ColorRGBA.Red); 
                deleteBtn.addClickCommands(source -> {
                    deleteWorldData(worldFolder);
                    refreshWorldList();
                });
            }
        }
    }

    private void deleteWorldData(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) deleteWorldData(f);
        }
        folder.delete();
        System.out.println("AAA: Deleted " + folder.getName());
    }

    private void startTheGame(String worldName) {
        getStateManager().detach(this);
        getStateManager().attach(new GameState(worldName));
    }

    @Override
    protected void onEnable() {
        this.app.getGuiNode().attachChild(worldsWindow);
        this.app.getFlyByCamera().setDragToRotate(true);
        this.app.getInputManager().setCursorVisible(true);
        
      
        app.enqueue(() -> {
            if (nameInput != null) {
                GuiGlobals.getInstance().requestFocus(nameInput);
            }
        });
    }

    @Override
    protected void onDisable() {
        worldsWindow.removeFromParent();
        this.app.getFlyByCamera().setDragToRotate(false);
        this.app.getInputManager().setCursorVisible(false);
        GuiGlobals.getInstance().getFocusManagerState().setFocus(null);
    }

    @Override
    protected void cleanup(Application app) {}
}
