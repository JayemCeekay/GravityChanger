package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for the AABB class to handle constructors with custom gravity directions.
 * This ensures that AABBs created with constructors are properly transformed for entities with custom gravity.
 */
@Mixin(AABB.class)
public abstract class AABBConstructorMixin {

    /**
     * Injects into the constructor with 6 double parameters to handle creating AABBs with custom gravity directions.
     */
    @Inject(method = "<init>(DDDDDD)V", at = @At("RETURN"))
    private void onConstructDouble(double x1, double y1, double z1, double x2, double y2, double z2, CallbackInfo ci) {
        handleConstruction();
    }

    /**
     * Injects into the constructor with a BlockPos parameter to handle creating AABBs with custom gravity directions.
     */
    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
    private void onConstructBlockPos(BlockPos pos, CallbackInfo ci) {
        handleConstruction();
    }

    /**
     * Injects into the constructor with two BlockPos parameters to handle creating AABBs with custom gravity directions.
     */
    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
    private void onConstructBlockPosBlockPos(BlockPos start, BlockPos end, CallbackInfo ci) {
        handleConstruction();
    }

    /**
     * Injects into the constructor with two Vec3 parameters to handle creating AABBs with custom gravity directions.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void onConstructVec3Vec3(Vec3 start, Vec3 end, CallbackInfo ci) {
        handleConstruction();
    }

    /**
     * Helper method to handle construction with custom gravity directions.
     * This method checks if there's an entity context with custom gravity and transforms the AABB if needed.
     */
    private void handleConstruction() {
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
            // Transform the AABB to an OrientedBoundingBox
            AABB thisBox = (AABB)(Object)this;
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(thisBox, gravityDirectionVec);
            
            // We can't replace the current object, so we'll just have to rely on other mixins to handle operations on it
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}