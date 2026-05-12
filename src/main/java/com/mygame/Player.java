package com.mygame;

import com.jme3.math.Vector3f;

public class Player {

    // Physical state
    public Vector3f position = new Vector3f(8, 150, 8);
    public Vector3f velocity = new Vector3f(0, 0, 0);

    // Hitbox dimensions (Voxel standard: 0.6 wide, 1.8 tall)
    public float width = 0.6f;
    public float height = 1.8f;

    public float yaw = 0;
    public float pitch = 0;

    public boolean onGround = false;
    public boolean isGhostMode = false;
    public boolean wantsToJump = false;

    // Physics constants
    public float moveSpeed = 10.0f;
    public float jumpForce = 8f;
    public float gravity = -25.0f;
    public float mouseSensitivity = 2.0f;

    public void jump() {
        if (onGround && !isGhostMode) {
            velocity.y = jumpForce;
            onGround = false;
            wantsToJump = false; // Reset once triggered
        }
    }

    public void toggleGhostMode() {
        this.isGhostMode = !this.isGhostMode;
        // Reset velocity so you stop moving physically the moment you enter ghost mode
        this.velocity.set(0, 0, 0);
        this.onGround = false;
    }

    public void rotate(float yawValue, float pitchValue) {
        yaw += yawValue * mouseSensitivity;
        pitch += pitchValue * mouseSensitivity;
        pitch = Math.max(-1.5f, Math.min(1.5f, pitch));
    }
}
