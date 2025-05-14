package gravity_changer.mixin.world.phys.shapes.VoxelShape;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.VoxelShapeTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Mixin for the VoxelShape class to handle finding the closest point with custom gravity directions.
 * This transforms the VoxelShape and point to gravity-aligned space, finds the closest point,
 * and transforms the result back to world space.
 */
@Mixin(VoxelShape.class)
public abstract class VoxelShapeClosestPointToMixin {

    /**
     * Injects into the 'closestPointTo' method to modify finding the closest point for off-axis gravity.
     * This transforms the VoxelShape and point to gravity-aligned space, finds the closest point,
     * and transforms the result back to world space.
     */
    @Inject(method = "closestPointTo(Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void onClosestPointTo(Vec3 point, CallbackInfoReturnable<Optional<Vec3>> cir) {
        // Only apply custom closest point finding for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom calculation
        // This prevents recursion when transformed shapes call closestPointTo
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                // Entity doesn't have a gravity component yet, use default closest point finding
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

        // Set the flag to indicate we're inside a custom calculation
        CollisionContext.setInCustomCollision(true);
        try {
            // Transform the VoxelShape to gravity-aligned space
            VoxelShape thisShape = (VoxelShape)(Object)this;
            VoxelShape transformedShape = VoxelShapeTransformer.transformVoxelShape(thisShape, gravityDirectionVec);
            
            // Transform the point to gravity-aligned space
            Vec3 transformedPoint = RotationUtil.vecWorldToPlayerVec(point, gravityDirectionVec);
            
            // Find the closest point in gravity-aligned space
            Optional<Vec3> transformedResult = transformedShape.closestPointTo(transformedPoint);
            
            // Transform the result back to world space
            if (transformedResult.isPresent()) {
                Vec3 worldResult = RotationUtil.vecPlayerToWorldVec(transformedResult.get(), gravityDirectionVec);
                cir.setReturnValue(Optional.of(worldResult));
            } else {
                cir.setReturnValue(Optional.empty());
            }
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}