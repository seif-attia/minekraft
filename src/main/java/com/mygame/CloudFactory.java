package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import jme3tools.optimize.GeometryBatchFactory;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial.CullHint;

public class CloudFactory {

    public static Spatial createClouds(AssetManager assetManager) {
        Node cloudNode = new Node("CloudNode");

        Box cloudBox = new Box(4f, 2.5f, 4f);

        int gridWidth = 300;

        for (int x = 0; x < gridWidth; x++) {
            for (int z = 0; z < gridWidth; z++) {

                // Sample our new TILEABLE noise
                double cloudNoise = getTileableNoise(x * 0.08, z * 0.08);

                if (cloudNoise > 0.50) {
                    Geometry geom = new Geometry("CloudBlock", cloudBox);
                    geom.setLocalTranslation(x * 8, 0, z * 8);
                    cloudNode.attachChild(geom);
                }
            }
        }

        Spatial batchedClouds = GeometryBatchFactory.optimize(cloudNode);

        Material cloudMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        cloudMat.setBoolean("UseMaterialColors", true);
        cloudMat.setColor("Ambient", new ColorRGBA(1f, 1f, 1f, 0.85f));
        cloudMat.setColor("Diffuse", new ColorRGBA(1f, 1f, 1f, 0.85f));

        cloudMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        batchedClouds.setQueueBucket(RenderQueue.Bucket.Transparent);
        batchedClouds.setShadowMode(RenderQueue.ShadowMode.Cast);
        batchedClouds.setMaterial(cloudMat);

        batchedClouds.setCullHint(CullHint.Never);
        return batchedClouds;
    }

    // SEAMLESS TILEABLE NOISE 
    private static double getTileableNoise(double x, double z) {
        int xInt = (int) Math.floor(x);
        int zInt = (int) Math.floor(z);
        double xFrac = x - xInt;
        double zFrac = z - zInt;

        int wrapX1 = xInt % 10;
        if (wrapX1 < 0) {
            wrapX1 += 20;
        }
        int wrapZ1 = zInt % 10;
        if (wrapZ1 < 0) {
            wrapZ1 += 20;
        }
        int wrapX2 = (xInt + 1) % 10;
        if (wrapX2 < 0) {
            wrapX2 += 20;
        }
        int wrapZ2 = (zInt + 1) % 10;
        if (wrapZ2 < 0) {
            wrapZ2 += 20;
        }

        double v1 = randomHash(wrapX1, wrapZ1);
        double v2 = randomHash(wrapX2, wrapZ1);
        double v3 = randomHash(wrapX1, wrapZ2);
        double v4 = randomHash(wrapX2, wrapZ2);

        double i1 = interpolate(v1, v2, xFrac);
        double i2 = interpolate(v3, v4, xFrac);

        return interpolate(i1, i2, zFrac);
    }

    private static double interpolate(double a, double b, double blend) {
        double theta = blend * Math.PI;
        double f = (1f - Math.cos(theta)) * 0.5f;
        return a * (1f - f) + b * f;
    }

    private static double randomHash(int x, int z) {
        int n = x + z * 57;
        n = (n << 13) ^ n;
        return (1.0 - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0) * 0.5 + 0.5;
    }
}
