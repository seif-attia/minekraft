package com.mygame;

import com.jme3.math.Vector3f;

public class Player {

    // Treat the position as the FEET of the player
    public Vector3f position = new Vector3f(0, 150, 0);
    public float yaw = 0;
    public float pitch = 0;
    private float yVelocity = 0;

    private boolean onGround = false;
    private boolean isGhostMode = false;

    // Snappy Voxel Physics Constants
    private final float JUMP_FORCE = 9.0f; // Increased for a punchy jump
    private final float GRAVITY = -28.0f;  // Heavy gravity stops the "moon" feel

    private float moveSpeed = 4.0f;
    private float mouseSensitivity = 2.0f;

    public void rotate(float yawValue, float pitchValue) {
        yaw += yawValue * mouseSensitivity;
        pitch += pitchValue * mouseSensitivity;

        // Prevent flipping over backwards
        if (pitch > 1.5f) {
            pitch = 1.5f;
        }
        if (pitch < -1.5f) {
            pitch = -1.5f;
        }
    }

    public void jump() {
        if (onGround && !isGhostMode) {
            setyVelocity(JUMP_FORCE);
            onGround = false;
        }
    }

    public void toggleGhostMode() {
        isGhostMode = !isGhostMode;
        yVelocity = 0;
    }

    public void adjustSpeed(float delta) {
        moveSpeed += delta;
        if (moveSpeed < 2.0f) {
            moveSpeed = 2.0f;
        }
        if (moveSpeed > 80.0f) {
            moveSpeed = 80.0f;
        }
    }

    // Getters and Setters
    public float getMoveSpeed() {
        return moveSpeed;
    }

    public boolean isGhostMode() {
        return isGhostMode;
    }

    public float getyVelocity() {
        return yVelocity;
    }

    public void setyVelocity(float yVelocity) {
        this.yVelocity = yVelocity;
    }

    public float getGravity() {
        return GRAVITY;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }
}
