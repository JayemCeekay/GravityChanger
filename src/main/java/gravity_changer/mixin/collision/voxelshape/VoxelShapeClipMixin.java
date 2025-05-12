package gravity_changer.mixin.collision.voxelshape;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.VoxelShapeTransformer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the VoxelShape class to handle ray tracing with custom gravity directions.
 * This transforms the VoxelShape to gravity-aligned space, performs ray tracing,
 * and transforms the result back to world space.
 */
@Mixin(VoxelShape.class)
public abstract class VoxelShapeClipMixin {

    /**
     * Injects into the 'clip' method to modify ray tracing for off-axis gravity.
     * This transforms the VoxelShape to gravity-aligned space, performs ray tracing,
     * and transforms the result back to world space.
     */
    @Inject(method = "clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;", at = @At("HEAD"), cancellable = true)
    private void onClip(Vec3 startVec, Vec3 endVec, BlockPos pos, CallbackInfoReturnable<BlockHitResult> cir) {
        // Only apply custom ray tracing for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom ray tracing calculation
        // This prevents recursion when transformed shapes call clip
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                // Entity doesn't have a gravity component yet, use default ray tracing
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

        // Set the flag to indicate we're inside a custom ray tracing calculation
        CollisionContext.setInCustomCollision(true);
        try {
            // Transform the VoxelShape to gravity-aligned space
            VoxelShape thisShape = (VoxelShape)(Object)this;
            VoxelShape transformedShape = VoxelShapeTransformer.transformVoxelShape(thisShape, gravityDirectionVec);
            
            // Perform ray tracing in gravity-aligned space
            // The AABBClipMixin will handle transforming the ray and result
            BlockHitResult result = transformedShape.clip(startVec, endVec, pos);
            
            cir.setReturnValue(result);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}