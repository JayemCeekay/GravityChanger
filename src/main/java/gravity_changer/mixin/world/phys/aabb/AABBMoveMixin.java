package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the AABB class to handle moving bounding boxes with custom gravity directions.
 * This ensures that OrientedBoundingBoxes are moved correctly in their local space.
 */
@Mixin(AABB.class)
public abstract class AABBMoveMixin {

    /**
     * Injects into the 'move' method to handle moving OrientedBoundingBoxes.
     * This ensures that the box is moved correctly in its local space.
     */
    @Inject(method = "move(DDD)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onMove(double x, double y, double z, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom movement for entities with custom gravity
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
            // Apply custom movement logic
            cir.setReturnValue(calculateCustomMove((OrientedBoundingBox)(Object)this, x, y, z, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Injects into the 'move(Vec3)' method to handle moving OrientedBoundingBoxes.
     * This ensures that the box is moved correctly in its local space.
     */
    @Inject(method = "move(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onMoveVec3(Vec3 vec, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom movement for entities with custom gravity
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
            // Apply custom movement logic
            cir.setReturnValue(calculateCustomMove((OrientedBoundingBox)(Object)this, vec.x, vec.y, vec.z, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates the result of moving an OrientedBoundingBox.
     * 
     * @param obb The OrientedBoundingBox to move
     * @param x The x component of the movement
     * @param y The y component of the movement
     * @param z The z component of the movement
     * @param gravityDir The gravity direction vector
     * @return The moved OrientedBoundingBox
     */
    private AABB calculateCustomMove(OrientedBoundingBox obb, double x, double y, double z, Vec3 gravityDir) {
        // Get the current entity from the collision context
        Entity entity = CollisionContext.getCurrentEntity();

        // Simply move the OBB by the specified amount
        // This preserves the orientation and other properties of the OBB
        return obb.move(x, y, z);
    }
}
