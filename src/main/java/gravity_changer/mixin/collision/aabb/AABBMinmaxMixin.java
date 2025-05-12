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
 * Mixin for the AABB class to handle minmax operation with custom gravity directions.
 * This ensures that OrientedBoundingBoxes are combined correctly in their local space.
 */
@Mixin(AABB.class)
public abstract class AABBMinmaxMixin {

    /**
     * Injects into the 'minmax' method to handle combining OrientedBoundingBoxes.
     * This ensures that the boxes are combined correctly in their local space.
     */
    @Inject(method = "minmax(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onMinmax(AABB other, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom minmax for entities with custom gravity
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
            // Apply custom minmax logic
            cir.setReturnValue(calculateCustomMinmax((OrientedBoundingBox)(Object)this, other, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates the result of combining an OrientedBoundingBox with another AABB.
     * 
     * @param obb The OrientedBoundingBox to combine
     * @param other The other AABB to combine with
     * @param gravityDir The gravity direction vector
     * @return The combined OrientedBoundingBox
     */
    private AABB calculateCustomMinmax(OrientedBoundingBox obb, AABB other, Vec3 gravityDir) {
        // Create a new AABB that encompasses both boxes
        AABB minmaxBox = new AABB(
            Math.min(obb.minX, other.minX),
            Math.min(obb.minY, other.minY),
            Math.min(obb.minZ, other.minZ),
            Math.max(obb.maxX, other.maxX),
            Math.max(obb.maxY, other.maxY),
            Math.max(obb.maxZ, other.maxZ)
        );
        
        // Transform the combined box to an OrientedBoundingBox
        return OrientedBoundingBoxTransformer.transformToOBB(minmaxBox, gravityDir);
    }
}