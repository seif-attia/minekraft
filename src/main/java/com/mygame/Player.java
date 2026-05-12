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

    public boolean intersectsVoxel(int bx, int by, int bz) {
        // Calculate Player Bounding Box
        // (Assuming position is at the exact bottom center of the feet)
        float pMinX = position.x - (width / 2.0f);
        float pMaxX = position.x + (width / 2.0f);
        float pMinY = position.y;
        float pMaxY = position.y + height;
        float pMinZ = position.z - (width / 2.0f);
        float pMaxZ = position.z + (width / 2.0f);

        // Calculate Block Bounding Box (A voxel is exactly 1x1x1)
        float bMinX = bx;
        float bMaxX = bx + 1.0f;
        float bMinY = by;
        float bMaxY = by + 1.0f;
        float bMinZ = bz;
        float bMaxZ = bz + 1.0f;

        // Standard 3D AABB Collision Check
        // If ALL of these are true, the boxes are overlapping
        return (pMinX < bMaxX && pMaxX > bMinX)
                && (pMinY < bMaxY && pMaxY > bMinY)
                && (pMinZ < bMaxZ && pMaxZ > bMinZ);
    }
}
