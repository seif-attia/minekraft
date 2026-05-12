package com.mygame;

import com.jme3.math.Vector3f;

public class PhysicsEngine {

    private Player player;
    private WorldManager world;
    private MovementManager movement;

    public PhysicsEngine(Player player, WorldManager world, MovementManager movement) {
        this.player = player;
        this.world = world;
        this.movement = movement;
    }

    public void update(float tpf) {
        if (player.isGhostMode) {
            handleGhostUpdate(tpf);
            return;
        }

        if (player.wantsToJump) {
            player.jump();
        }

        // 1. Apply Gravity to Y velocity
        player.velocity.y += player.gravity * tpf;

        // 2. Get the walking velocity from inputs
        Vector3f walkVel = movement.getDesiredMoveVelocity();

        // 3. Move Axis-by-Axis (The secret to no clipping)
        // Check X
        move(walkVel.x * tpf, 0, 0);
        // Check Z
        move(0, 0, walkVel.z * tpf);

        // Check Y (Gravity/Jumping)
        player.onGround = false; // Assume in air until Y collision proves otherwise
        move(0, player.velocity.y * tpf, 0);
    }

    private void handleGhostUpdate(float tpf) {
        // Get desired direction (includes vertical tilt)
        Vector3f moveDir = movement.getGhostMoveDirection();

        // In ghost mode, we move the position directly with no collision checks
        player.position.addLocal(moveDir.mult(player.moveSpeed * 3.5f * tpf));
    }

    private void move(float dx, float dy, float dz) {
        float nextX = player.position.x + dx;
        float nextY = player.position.y + dy;
        float nextZ = player.position.z + dz;

        // Check if the new position would result in a collision
        if (!isColliding(nextX, nextY, nextZ)) {
            player.position.set(nextX, nextY, nextZ);
        } else {
            if (dx != 0) {
                player.velocity.x = 0;
            }
            if (dz != 0) {
                player.velocity.z = 0;
            }
            if (dy < 0) {
                player.onGround = true;
                player.velocity.y = 0;
            } else if (dy > 0) {
                // Hit a ceiling
                player.velocity.y = 0;
            }
        }
    }

    /**
     * Checks multiple points on the player's bounding box against the voxel
     * grid.
     */
    private boolean isColliding(float x, float y, float z) {
        float skin = 0.1f;
        float r = player.width / 2.0f + skin;

        // We check 8 points: Corners at feet level and corners at head level.
        // Also check a middle point if you find you're clipping through narrow gaps.
        float[] yLevels = {0.1f, player.height / 2f, player.height - 0.1f};
        float[] offsets = {-r, r};

        for (float ty : yLevels) {
            for (float tx : offsets) {
                for (float tz : offsets) {
                    if (isBlockSolid(x + tx, y + ty, z + tz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBlockSolid(float x, float y, float z) {
        // Floor the coordinates to get the integer voxel key
        int val = world.getBlockGlobal((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        // IDs: 0 is Air, 5 is Water, 10 is Grass (all non-solid)
        return val != 0 && val != 5 && val != 10;
    }

}
