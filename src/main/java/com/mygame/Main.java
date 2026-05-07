package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication {

        public static void main(String[] args) {
                Main app = new Main();
                app.start();
        }

        @Override
        public void simpleInitApp() {
                Chunk myFirstChunk = new Chunk();
                ChunkMesher mesher = new ChunkMesher();

                Mesh chunkMesh = mesher.createMesh(myFirstChunk);

                Geometry chunkGeo = new Geometry("ChunkGeo", chunkMesh);
                Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

                mat.getAdditionalRenderState().setWireframe(true);

                mat.setColor("Color", ColorRGBA.Green);
                chunkGeo.setMaterial(mat);

                rootNode.attachChild(chunkGeo);
                // 3. Move the Camera back and up to see the whole chunk
                cam.setLocation(new com.jme3.math.Vector3f(-10, 20, -10));
                cam.lookAt(new com.jme3.math.Vector3f(8, 0, 8), com.jme3.math.Vector3f.UNIT_Y);

                // Optional: Speed up the fly camera so you can move around faster
                flyCam.setMoveSpeed(30f);
        }

        @Override
        public void simpleUpdate(float tpf) {
                //TODO: add update code
        }

        @Override
        public void simpleRender(RenderManager rm) {
                //TODO: add render code
        }
}
