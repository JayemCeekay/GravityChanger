package gravity_changer.mixin.world.phys.shapes.VoxelShape;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.VoxelShapeTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the VoxelShape class to handle projecting shapes onto faces with custom gravity directions.
 * This transforms the direction to gravity-aligned space, projects the shape onto the face,
 * and transforms the result back to world space.
 */
@Mixin(VoxelShape.class)
public abstract class VoxelShapeGetFaceShapeMixin {

    /**
     * Injects into the 'getFaceShape' method to modify projecting shapes onto faces for off-axis gravity.
     * This transforms the direction to gravity-aligned space, projects the shape onto the face,
     * and transforms the result back to world space.
     */
    @Inject(method = "getFaceShape(Lnet/minecraft/core/Direction;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void onGetFaceShape(Direction side, CallbackInfoReturnable<VoxelShape> cir) {
        // Only apply custom face shape projection for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom calculation
        // This prevents recursion when transformed shapes call getFaceShape
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                // Entity doesn't have a gravity component yet, use default face shape projection
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
            
            // Transform the direction to gravity-aligned space
            Direction cardinalGravity = RotationUtil.vec3ToDirection(gravityDirectionVec);
            Direction transformedSide = RotationUtil.dirWorldToPlayer(side, cardinalGravity);
            
            // Project the shape onto the face in gravity-aligned space
            VoxelShape transformedFaceShape = transformedShape.getFaceShape(transformedSide);
            
            // Transform the result back to world space
            VoxelShape worldFaceShape = VoxelShapeTransformer.inverseTransformVoxelShape(transformedFaceShape, gravityDirectionVec);
            
            cir.setReturnValue(worldFaceShape);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}