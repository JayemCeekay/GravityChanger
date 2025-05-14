package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AABB.class)
public abstract class AABBMixin {

    /**
     * Injects into the 'intersects' method to modify collision detection for off-axis gravity.
     * This transforms both AABBs to gravity-aligned space, checks intersection, and returns the result.
     */
    @Inject(method = "intersects(Lnet/minecraft/world/phys/AABB;)Z", at = @At("HEAD"), cancellable = true)
    private void onIntersects(AABB other, CallbackInfoReturnable<Boolean> cir) {
        // Only apply custom collision for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom collision calculation
        // This prevents recursion when OrientedBoundingBox.intersects calls AABB.intersects
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
            // Apply custom collision logic
            cir.setReturnValue(calculateCustomIntersection((AABB)(Object)this, other, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates intersection between two AABBs using OrientedBoundingBoxes.
     * 
     * @param thisBox The first AABB
     * @param otherBox The second AABB
     * @param gravityDir The gravity direction vector
     * @return True if the boxes intersect, false otherwise
     */
    private boolean calculateCustomIntersection(AABB thisBox, AABB otherBox, Vec3 gravityDir) {
        // Use the OrientedBoundingBoxTransformer to check for intersection
        return OrientedBoundingBoxTransformer.intersects(thisBox, otherBox, gravityDir);
    }
}
