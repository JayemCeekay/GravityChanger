package gravity_changer.mixin.collision.aabb;

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
 * Mixin for the AABB class to handle intersect operation with custom gravity directions.
 * This ensures that OrientedBoundingBoxes are intersected correctly in their local space.
 */
@Mixin(AABB.class)
public abstract class AABBIntersectMixin {

    /**
     * Injects into the 'intersect' method to handle intersecting OrientedBoundingBoxes.
     * This ensures that the boxes are intersected correctly in their local space.
     */
    @Inject(method = "intersect(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onIntersect(AABB other, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom intersect for entities with custom gravity
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
            // Apply custom intersect logic
            cir.setReturnValue(calculateCustomIntersect((OrientedBoundingBox)(Object)this, other, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates the result of intersecting an OrientedBoundingBox with another AABB.
     * 
     * @param obb The OrientedBoundingBox to intersect
     * @param other The other AABB to intersect with
     * @param gravityDir The gravity direction vector
     * @return The intersected OrientedBoundingBox
     */
    private AABB calculateCustomIntersect(OrientedBoundingBox obb, AABB other, Vec3 gravityDir) {
        // Create a new AABB that is the intersection of both boxes
        AABB intersectBox = new AABB(
            Math.max(obb.minX, other.minX),
            Math.max(obb.minY, other.minY),
            Math.max(obb.minZ, other.minZ),
            Math.min(obb.maxX, other.maxX),
            Math.min(obb.maxY, other.maxY),
            Math.min(obb.maxZ, other.maxZ)
        );
        
        // Check if the intersection is valid (all min values must be less than or equal to max values)
        if (intersectBox.minX > intersectBox.maxX || 
            intersectBox.minY > intersectBox.maxY || 
            intersectBox.minZ > intersectBox.maxZ) {
            // No valid intersection, return an empty box
            return new AABB(0, 0, 0, 0, 0, 0);
        }
        
        // Transform the intersected box to an OrientedBoundingBox
        return OrientedBoundingBoxTransformer.transformToOBB(intersectBox, gravityDir);
    }
}