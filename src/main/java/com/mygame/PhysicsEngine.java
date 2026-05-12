package com.mygame;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;

public class PhysicsEngine {

    private CharacterControl playerControl;
    private MovementManager movementManager;

    // Minecraft walking speed is roughly 4.3 m/s, sprinting is ~5.6 m/s. 
    // Voxel scales usually feel better around 8 to 10.
    private float moveSpeed = 10.0f;

    public PhysicsEngine(CharacterControl playerControl, MovementManager movementManager) {
        this.playerControl = playerControl;
        this.movementManager = movementManager;
    }

    public void updatePhysics(float tpf) {
        // Get the W/A/S/D direction
        Vector3f moveDir = movementManager.getCurrentMoveDirection();

        // CRITICAL: For Kinematic CharacterControl, walkDirection is the 
        // offset to move *per tick*, so we MUST multiply the speed by tpf!
        Vector3f walkOffset = moveDir.mult(moveSpeed * tpf);

        // The engine handles swept collisions and wall sliding automatically
        playerControl.setWalkDirection(walkOffset);
    }
}
