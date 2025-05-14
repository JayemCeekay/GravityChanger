package gravity_changer.mixin.world.entity.Entity;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.collision.VoxelShapeTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin for the Entity class to handle OBB (Oriented Bounding Box) collisions with arbitrary gravity.
 * This mixin directly handles the collide method to provide more accurate collision detection for
 * entities with non-default gravity.
 */
@Mixin(Entity.class)
public abstract class EntityOBBCollisionMixin {

    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract Level level();
    @Shadow protected abstract Vec3 collide(Vec3 movement);

    /**
     * Injects at the head of the collide method to handle OBB collisions for entities with non-default gravity.
     * This method will completely replace the vanilla collision detection for entities with custom gravity.
     */
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void onCollide(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity)(Object)this;

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                // Entity doesn't have a gravity component yet, use default collision
                return;
            }
        } catch (NullPointerException e) {
            // Entity's component container might not be initialized yet
            return;
        }

        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 &&
                                  gravityDirectionVec.x == 0 &&
                                  gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        // Set the flag to indicate we're inside a custom collision calculation
        CollisionContext.setInCustomCollision(true);
        try {
            // Get the entity's bounding box
            AABB entityBoundingBox = this.getBoundingBox();

            // Transform the entity bounding box to an OrientedBoundingBox
            OrientedBoundingBox entityOBB = OrientedBoundingBoxTransformer.transformToOBB(entityBoundingBox, gravityDirectionVec);

            // Check if the movement vector has already been transformed to player space
            // In EntityMixin.java, the movement vector is transformed from world space to player space
            // before it reaches this mixin, so we need to handle that case
            Vec3 playerMovement;

            // If we're coming from EntityMixin.modify_adjustMovementForCollisions_Vec3d_0,
            // the movement vector is already in player space
            if (CollisionContext.getCurrentEntity() == entity) {
                // The movement is already in player space, use it directly
                playerMovement = movement;
            } else {
                // Transform the movement vector to player space (gravity-aligned)
                playerMovement = RotationUtil.vecWorldToPlayerVec(movement, gravityDirectionVec);
            }

            // Get all potential collision shapes from the world
            List<VoxelShape> collisions = this.level().getEntityCollisions(entity, entityBoundingBox.expandTowards(movement));

            // Add block collisions
            Iterable<VoxelShape> blockCollisions = this.level().getBlockCollisions(entity, entityBoundingBox.expandTowards(movement));
            for (VoxelShape shape : blockCollisions) {
                collisions.add(VoxelShapeTransformer.transformVoxelShape(shape, gravityDirectionVec));
            }

            // If there are no collisions, return the original movement
            if (collisions.isEmpty()) {
                cir.setReturnValue(movement);
                return;
            }

            // Handle collisions in player space (gravity-aligned)
            double playerMovementX = playerMovement.x;
            double playerMovementY = playerMovement.y;
            double playerMovementZ = playerMovement.z;

            // Create a copy of the bounding box for collision testing
            AABB workingBox = entityOBB.getLocalBox();

            // For arbitrary directions, we need to handle collisions in all three axes
            // First handle Y axis (up/down in player space)
            if (playerMovementY != 0.0D) {
                // Get the direction for the Y axis in world space
                Vec3 yAxisDir = RotationUtil.vecPlayerToWorldVec(new Vec3(0, 1, 0), gravityDirectionVec).normalize();

                // Calculate the axis and direction for collision testing
                Direction.Axis yAxis = getClosestAxis(yAxisDir);
                int yStep = getAxisDirection(yAxisDir, yAxis);

                // Apply collision along Y axis
                playerMovementY = net.minecraft.world.phys.shapes.Shapes.collide(yAxis, workingBox, collisions, playerMovementY * yStep) * yStep;

                // Move the working box if there's remaining movement
                if (playerMovementY != 0.0D) {
                    workingBox = workingBox.move(0.0D, playerMovementY, 0.0D);
                }
            }

            // Determine which horizontal axis to handle first (X or Z)
            boolean isZLargerThanX = Math.abs(playerMovementX) < Math.abs(playerMovementZ);

            // Handle Z axis first if it has larger movement
            if (isZLargerThanX && playerMovementZ != 0.0D) {
                // Get the direction for the Z axis in world space
                Vec3 zAxisDir = RotationUtil.vecPlayerToWorldVec(new Vec3(0, 0, 1), gravityDirectionVec).normalize();

                // Calculate the axis and direction for collision testing
                Direction.Axis zAxis = getClosestAxis(zAxisDir);
                int zStep = getAxisDirection(zAxisDir, zAxis);

                // Apply collision along Z axis
                playerMovementZ = net.minecraft.world.phys.shapes.Shapes.collide(zAxis, workingBox, collisions, playerMovementZ * zStep) * zStep;

                // Move the working box if there's remaining movement
                if (playerMovementZ != 0.0D) {
                    workingBox = workingBox.move(0.0D, 0.0D, playerMovementZ);
                }
            }

            // Handle X axis
            if (playerMovementX != 0.0D) {
                // Get the direction for the X axis in world space
                Vec3 xAxisDir = RotationUtil.vecPlayerToWorldVec(new Vec3(1, 0, 0), gravityDirectionVec).normalize();

                // Calculate the axis and direction for collision testing
                Direction.Axis xAxis = getClosestAxis(xAxisDir);
                int xStep = getAxisDirection(xAxisDir, xAxis);

                // Apply collision along X axis
                playerMovementX = net.minecraft.world.phys.shapes.Shapes.collide(xAxis, workingBox, collisions, playerMovementX * xStep) * xStep;

                // Move the working box if there's remaining movement and X is handled first
                if (!isZLargerThanX && playerMovementX != 0.0D) {
                    workingBox = workingBox.move(playerMovementX, 0.0D, 0.0D);
                }
            }

            // Handle Z axis last if X had larger movement
            if (!isZLargerThanX && playerMovementZ != 0.0D) {
                // Get the direction for the Z axis in world space
                Vec3 zAxisDir = RotationUtil.vecPlayerToWorldVec(new Vec3(0, 0, 1), gravityDirectionVec).normalize();

                // Calculate the axis and direction for collision testing
                Direction.Axis zAxis = getClosestAxis(zAxisDir);
                int zStep = getAxisDirection(zAxisDir, zAxis);

                // Apply collision along Z axis
                playerMovementZ = net.minecraft.world.phys.shapes.Shapes.collide(zAxis, workingBox, collisions, playerMovementZ * zStep) * zStep;
            }

            // Transform the final movement back to world space
            Vec3 finalMovement = RotationUtil.vecPlayerToWorldVec(new Vec3(playerMovementX, playerMovementY, playerMovementZ), gravityDirectionVec);

            // Return the final movement vector
            cir.setReturnValue(finalMovement);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }

    }

    /**
     * Gets the closest cardinal axis to the given direction vector.
     */
    private Direction.Axis getClosestAxis(Vec3 dir) {
        double absX = Math.abs(dir.x);
        double absY = Math.abs(dir.y);
        double absZ = Math.abs(dir.z);

        if (absX > absY && absX > absZ) {
            return Direction.Axis.X;
        } else if (absY > absX && absY > absZ) {
            return Direction.Axis.Y;
        } else {
            return Direction.Axis.Z;
        }
    }

    /**
     * Gets the direction (positive or negative) along the given axis.
     */
    private int getAxisDirection(Vec3 dir, Direction.Axis axis) {
        switch (axis) {
            case X:
                return dir.x > 0 ? 1 : -1;
            case Y:
                return dir.y > 0 ? 1 : -1;
            case Z:
                return dir.z > 0 ? 1 : -1;
            default:
                return 1; // Should never happen
        }
    }
}
