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
 * Mixin for the AABB class to handle the unitCubeFromLowerCorner method with custom gravity directions.
 * This ensures that unit cube AABBs are properly transformed for entities with custom gravity.
 */
@Mixin(AABB.class)
public abstract class AABBUnitCubeFromLowerCornerMixin {

    /**
     * Injects into the 'unitCubeFromLowerCorner' static method to handle creating unit cube AABBs with custom gravity directions.
     * This transforms the lower corner point based on the gravity direction.
     */
    @Inject(method = "unitCubeFromLowerCorner", at = @At("HEAD"), cancellable = true)
    private static void onUnitCubeFromLowerCorner(Vec3 vector, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom transformation for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
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
            // Create a standard unit cube AABB with the given lower corner
            AABB standardBox = new AABB(
                vector.x, vector.y, vector.z,
                vector.x + 1.0, vector.y + 1.0, vector.z + 1.0
            );
            
            // Transform the standard box to an OrientedBoundingBox
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(standardBox, gravityDirectionVec);
            
            // Return the transformed box
            cir.setReturnValue(obb);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}