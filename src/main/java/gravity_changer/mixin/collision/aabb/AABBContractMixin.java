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
 * Mixin for the AABB class to handle contracting bounding boxes with custom gravity directions.
 * This ensures that OrientedBoundingBoxes are contracted correctly in their local space.
 */
@Mixin(AABB.class)
public abstract class AABBContractMixin {

    /**
     * Injects into the 'contract' method to handle contracting OrientedBoundingBoxes.
     * This ensures that the box is contracted correctly in its local space.
     */
    @Inject(method = "contract(DDD)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onContract(double x, double y, double z, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom contraction for entities with custom gravity
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
            // Apply custom contraction logic
            cir.setReturnValue(calculateCustomContract((OrientedBoundingBox)(Object)this, x, y, z, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates the result of contracting an OrientedBoundingBox.
     * 
     * @param obb The OrientedBoundingBox to contract
     * @param x The amount to contract in the x direction
     * @param y The amount to contract in the y direction
     * @param z The amount to contract in the z direction
     * @param gravityDir The gravity direction vector
     * @return The contracted OrientedBoundingBox
     */
    private AABB calculateCustomContract(OrientedBoundingBox obb, double x, double y, double z, Vec3 gravityDir) {
        // Create a new AABB with the contracted coordinates
        AABB contractedBox = new AABB(
            obb.minX + x, obb.minY + y, obb.minZ + z,
            obb.maxX - x, obb.maxY - y, obb.maxZ - z
        );
        
        // Transform the contracted box to an OrientedBoundingBox
        return OrientedBoundingBoxTransformer.transformToOBB(contractedBox, gravityDir);
    }
}