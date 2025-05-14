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
 * Mixin for the AABB class to handle contains methods with custom gravity directions.
 * This ensures that contains checks work correctly for OrientedBoundingBoxes.
 */
@Mixin(AABB.class)
public abstract class AABBContainsMixin {

    /**
     * Injects into the 'contains(Vec3)' method to handle checking if an OrientedBoundingBox contains a point.
     */
    @Inject(method = "contains(Lnet/minecraft/world/phys/Vec3;)Z", at = @At("HEAD"), cancellable = true)
    private void onContainsVec3(Vec3 vec, CallbackInfoReturnable<Boolean> cir) {
        // Only apply custom check for OrientedBoundingBoxes
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Get the entity context
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

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
            
            // Get the local box and rotation
            AABB localBox = obb.getLocalBox();
            Vec3 center = obb.getCenter();
            
            // Transform the point to the local coordinate system
            Vec3 localVec = obb.getRotation().inverse().rotate(vec.subtract(center));
            
            // Check if the local box contains the transformed point
            boolean contains = localBox.contains(localVec);
            
            // Return the result
            cir.setReturnValue(contains);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Injects into the 'contains(double, double, double)' method to handle checking if an OrientedBoundingBox contains a point.
     */
    @Inject(method = "contains(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void onContainsXYZ(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        // Only apply custom check for OrientedBoundingBoxes
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Get the entity context
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

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
            
            // Get the local box and rotation
            AABB localBox = obb.getLocalBox();
            Vec3 center = obb.getCenter();
            
            // Transform the point to the local coordinate system
            Vec3 vec = new Vec3(x, y, z);
            Vec3 localVec = obb.getRotation().inverse().rotate(vec.subtract(center));
            
            // Check if the local box contains the transformed point
            boolean contains = localBox.contains(localVec);
            
            // Return the result
            cir.setReturnValue(contains);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}