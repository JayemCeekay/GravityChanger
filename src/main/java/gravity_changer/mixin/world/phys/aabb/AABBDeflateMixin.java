package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the AABB class to handle deflating bounding boxes with custom gravity directions.
 * This ensures that OrientedBoundingBoxes are deflated correctly in their local space.
 */
@Mixin(AABB.class)
public abstract class AABBDeflateMixin {

    /**
     * Injects into the 'deflate(double)' method to handle deflating OrientedBoundingBoxes.
     * This ensures that the box is deflated correctly in its local space.
     */
    @Inject(method = "deflate(D)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onDeflate(double value, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom deflation for entities with custom gravity
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
            // Apply custom deflation logic
            cir.setReturnValue(calculateCustomDeflate((OrientedBoundingBox)(Object)this, value, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Injects into the 'deflate(double, double, double)' method to handle deflating OrientedBoundingBoxes.
     * This ensures that the box is deflated correctly in its local space.
     */
    @Inject(method = "deflate(DDD)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onDeflateXYZ(double x, double y, double z, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom deflation for entities with custom gravity
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
            // Apply custom deflation logic
            cir.setReturnValue(calculateCustomDeflateXYZ((OrientedBoundingBox)(Object)this, x, y, z, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates the result of deflating an OrientedBoundingBox by a uniform amount.
     * 
     * @param obb The OrientedBoundingBox to deflate
     * @param value The amount to deflate by
     * @param gravityDir The gravity direction vector
     * @return The deflated OrientedBoundingBox
     */
    private AABB calculateCustomDeflate(OrientedBoundingBox obb, double value, Vec3 gravityDir) {
        // Create a new AABB with the deflated coordinates
        AABB deflatedBox = new AABB(
            obb.minX + value, obb.minY + value, obb.minZ + value,
            obb.maxX - value, obb.maxY - value, obb.maxZ - value
        );
        
        // Transform the deflated box to an OrientedBoundingBox
        return OrientedBoundingBoxTransformer.transformToOBB(deflatedBox, gravityDir);
    }

    /**
     * Calculates the result of deflating an OrientedBoundingBox by different amounts in each direction.
     * 
     * @param obb The OrientedBoundingBox to deflate
     * @param x The amount to deflate in the x direction
     * @param y The amount to deflate in the y direction
     * @param z The amount to deflate in the z direction
     * @param gravityDir The gravity direction vector
     * @return The deflated OrientedBoundingBox
     */
    private AABB calculateCustomDeflateXYZ(OrientedBoundingBox obb, double x, double y, double z, Vec3 gravityDir) {
        // Create a new AABB with the deflated coordinates
        AABB deflatedBox = new AABB(
            obb.minX + x, obb.minY + y, obb.minZ + z,
            obb.maxX - x, obb.maxY - y, obb.maxZ - z
        );
        
        // Transform the deflated box to an OrientedBoundingBox
        return OrientedBoundingBoxTransformer.transformToOBB(deflatedBox, gravityDir);
    }
}