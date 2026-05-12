package com.minekraft.engine;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class MovementManager {

    private Player player;
    private boolean forward, back, left, right;

    public MovementManager(Player player) {
        this.player = player;
    }

    /**
     * Calculates the direction the player wants to walk based on rotation.
     */
    public Vector3f getDesiredMoveVelocity() {
        Vector3f moveDir = new Vector3f(0, 0, 0);
        // Only use Yaw for walking so looking up/down doesn't affect speed
        Quaternion rot = new Quaternion().fromAngles(0, player.yaw, 0);

        Vector3f fwdVec = rot.getRotationColumn(2).normalizeLocal();
        Vector3f leftVec = rot.getRotationColumn(0).normalizeLocal();

        if (forward) {
            moveDir.addLocal(fwdVec);
        }
        if (back) {
            moveDir.subtractLocal(fwdVec);
        }
        if (left) {
            moveDir.addLocal(leftVec);
        }
        if (right) {
            moveDir.subtractLocal(leftVec);
        }

        if (moveDir.lengthSquared() > 0) {
            moveDir.normalizeLocal().multLocal(player.moveSpeed);
        }
        return moveDir;
    }

    public Vector3f getGhostMoveDirection() {
        Vector3f moveDir = new Vector3f(0, 0, 0);
        // Use the full rotation (Pitch + Yaw) for 3D flight
        Quaternion rot = new Quaternion().fromAngles(player.pitch, player.yaw, 0);

        Vector3f forwardVec = rot.getRotationColumn(2).normalizeLocal();
        Vector3f leftVec = rot.getRotationColumn(0).normalizeLocal();

        if (forward) {
            moveDir.addLocal(forwardVec);
        }
        if (back) {
            moveDir.subtractLocal(forwardVec);
        }
        if (left) {
            moveDir.addLocal(leftVec);
        }
        if (right) {
            moveDir.subtractLocal(leftVec);
        }

        if (moveDir.lengthSquared() > 0) {
            moveDir.normalizeLocal();
        }
        return moveDir;
    }

    // Input Setters
    public void setForward(boolean v) {
        forward = v;
    }

    public void setBack(boolean v) {
        back = v;
    }

    public void setLeft(boolean v) {
        left = v;
    }

    public void setRight(boolean v) {
        right = v;
    }
}
