/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 *
 * @author EyonMiner
 */
public class MovementManager {

    private Player player;

    // Input states (W, A, S, D)
    private boolean forward = false;
    private boolean back = false;
    private boolean left = false;
    private boolean right = false;

    // This variable "remembers" the direction for the PhysicsEngine to use
    private Vector3f lastMoveDir = new Vector3f(0, 0, 0);

    public MovementManager(Player player) {
        this.player = player;
    }

    /**
     * This method is used by the PhysicsEngine to check for wall collisions. It
     * tells the engine exactly where the player is TRYING to go.
     */
    public Vector3f getCurrentMoveDirection() {
        return lastMoveDir;
    }

    public void updateMovement(float tpf) {
        // 1. Create a rotation based on where the player is looking
        Quaternion q = new Quaternion();
        q.fromAngles(player.pitch, player.yaw, 0);

        // 2. Get the "Look" direction and "Side" direction
        Vector3f dir = q.getRotationColumn(2);     // Forward
        Vector3f leftVec = q.getRotationColumn(0); // Left

        // 3. Check if we are flying or walking
        if (player.isGhostMode()) {
            handleGhostMovement(dir, leftVec, tpf);
        } else {
            handlePhysicsMovement(dir, leftVec, tpf);
        }
    }

    private void handleGhostMovement(Vector3f dir, Vector3f leftVec, float tpf) {
        float speed = player.getMoveSpeed() * tpf;
        // In Ghost mode, we move exactly where the camera points (can fly up/down)
        if (forward) {
            player.position.addLocal(dir.mult(speed));
        }
        if (back) {
            player.position.subtractLocal(dir.mult(speed));
        }
        if (left) {
            player.position.addLocal(leftVec.mult(speed));
        }
        if (right) {
            player.position.subtractLocal(leftVec.mult(speed));
        }
    }

    private void handlePhysicsMovement(Vector3f dir, Vector3f leftVec, float tpf) {
        float speed = player.getMoveSpeed() * tpf;

        // "Flatten" the directions so W and S don't move us into the sky/ground
        Vector3f walkDir = new Vector3f(dir.x, 0, dir.z).normalizeLocal();
        Vector3f walkLeft = new Vector3f(leftVec.x, 0, leftVec.z).normalizeLocal();

        // Reset the direction for this frame's calculation
        lastMoveDir.set(0, 0, 0);

        if (forward) {
            lastMoveDir.addLocal(walkDir);
        }
        if (back) {
            lastMoveDir.subtractLocal(walkDir);
        }
        if (left) {
            lastMoveDir.addLocal(walkLeft);
        }
        if (right) {
            lastMoveDir.subtractLocal(walkLeft);
        }

        // Normalize prevents "Diagonal Speeding" (moving faster when pressing W+A)
        if (lastMoveDir.lengthSquared() > 0) {
            lastMoveDir.normalizeLocal();
        }
    }

    // These setters are called by the ActionListener in Main.java
    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void setBack(boolean back) {
        this.back = back;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public void setRight(boolean right) {
        this.right = right;
    }
}
