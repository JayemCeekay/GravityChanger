package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the AABB class to handle axis-related methods with custom gravity directions.
 * This ensures that min/max operations on axes are properly transformed for entities with custom gravity.
 */
@Mixin(AABB.class)
public abstract class AABBAxisMethodsMixin {

    /**
     * Injects into the 'min' method to handle getting the minimum coordinate along an axis with custom gravity directions.
     */
    @Inject(method = "min", at = @At("HEAD"), cancellable = true)
    private void onMin(Direction.Axis axis, CallbackInfoReturnable<Double> cir) {
        // Only apply custom transformation for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if this is an OrientedBoundingBox
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
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
            // Get the OrientedBoundingBox
            OrientedBoundingBox obb = (OrientedBoundingBox)(Object)this;
            
            // Get the local box
            AABB localBox = obb.getLocalBox();
            
            // Transform the axis to the local coordinate system
            Direction.Axis localAxis = transformAxis(axis, gravityDirectionVec);
            
            // Get the min value along the transformed axis
            double minValue;
            switch (localAxis) {
                case X:
                    minValue = localBox.minX;
                    break;
                case Y:
                    minValue = localBox.minY;
                    break;
                case Z:
                    minValue = localBox.minZ;
                    break;
                default:
                    return; // Should never happen
            }
            
            // Return the min value
            cir.setReturnValue(minValue);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Injects into the 'max' method to handle getting the maximum coordinate along an axis with custom gravity directions.
     */
    @Inject(method = "max", at = @At("HEAD"), cancellable = true)
    private void onMax(Direction.Axis axis, CallbackInfoReturnable<Double> cir) {
        // Only apply custom transformation for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if this is an OrientedBoundingBox
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
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
            // Get the OrientedBoundingBox
            OrientedBoundingBox obb = (OrientedBoundingBox)(Object)this;
            
            // Get the local box
            AABB localBox = obb.getLocalBox();
            
            // Transform the axis to the local coordinate system
            Direction.Axis localAxis = transformAxis(axis, gravityDirectionVec);
            
            // Get the max value along the transformed axis
            double maxValue;
            switch (localAxis) {
                case X:
                    maxValue = localBox.maxX;
                    break;
                case Y:
                    maxValue = localBox.maxY;
                    break;
                case Z:
                    maxValue = localBox.maxZ;
                    break;
                default:
                    return; // Should never happen
            }
            
            // Return the max value
            cir.setReturnValue(maxValue);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Helper method to transform an axis from world space to local space based on the gravity direction.
     * 
     * @param worldAxis The axis in world space
     * @param gravityDir The gravity direction vector
     * @return The transformed axis in local space
     */
    private Direction.Axis transformAxis(Direction.Axis worldAxis, Vec3 gravityDir) {
        // Get the cardinal direction corresponding to the axis
        Direction worldDir;
        switch (worldAxis) {
            case X:
                worldDir = Direction.EAST;
                break;
            case Y:
                worldDir = Direction.UP;
                break;
            case Z:
                worldDir = Direction.SOUTH;
                break;
            default:
                return worldAxis; // Should never happen
        }
        
        // Transform the direction to local space
        Direction cardinalGravity = RotationUtil.vec3ToDirection(gravityDir);
        Direction localDir = RotationUtil.dirWorldToPlayer(worldDir, cardinalGravity);
        
        // Return the axis of the transformed direction
        return localDir.getAxis();
    }
}