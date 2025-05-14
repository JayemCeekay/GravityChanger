package gravity_changer.mixin.world.level.block.state;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for BlockBehaviour.BlockStateBase to handle collision detection with custom gravity.
 * This ensures that OrientedBoundingBoxes are properly used for collision detection with blocks.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {

    /**
     * Injects into the calculateSolid method to handle custom gravity when determining if a block is solid.
     * This method uses AABB to determine if a block is solid, so we need to handle OrientedBoundingBoxes.
     */
    @ModifyVariable(method = "calculateSolid", at = @At(value = "STORE"), ordinal = 0)
    private AABB modifyAABBInCalculateSolid(AABB aabb) {
        // Check if we're in a custom collision calculation
        if (!gravity_changer.collision.CollisionContext.isInCustomCollision()) {
            return aabb;
        }

        // Get the current entity from the collision context
        Entity entity = gravity_changer.collision.CollisionContext.getCurrentEntity();
        if (entity == null) {
            return aabb;
        }

        // Check if the entity has a gravity component
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return aabb;
            }
        } catch (NullPointerException e) {
            return aabb;
        }

        // Get the gravity direction
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 && 
                                  gravityDirection.x == 0 && 
                                  gravityDirection.z == 0;
        if (isDefaultGravity) {
            return aabb;
        }

        // Transform the AABB to an OrientedBoundingBox
        return OrientedBoundingBox.fromAABB(aabb, gravityDirection);
    }

    /**
     * Injects into the getCollisionShape method to handle custom gravity when getting the collision shape.
     * This method returns the collision shape for a block state, which is used for collision detection.
     */
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", 
            at = @At("RETURN"), 
            cancellable = true)
    private void onGetCollisionShape(BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        // Check if we're in a custom collision calculation
        if (!gravity_changer.collision.CollisionContext.isInCustomCollision()) {
            return;
        }

        // Get the current entity from the collision context
        Entity entity = gravity_changer.collision.CollisionContext.getCurrentEntity();
        if (entity == null) {
            return;
        }

        // Check if the entity has a gravity component
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        // Get the gravity direction
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 && 
                                  gravityDirection.x == 0 && 
                                  gravityDirection.z == 0;
        if (isDefaultGravity) {
            return;
        }

        // The original method will return a VoxelShape, which we need to transform
        // to work with OrientedBoundingBoxes. However, this is handled by other mixins
        // that transform the AABB inside the VoxelShape, so we don't need to modify
        // the return value here.
    }

    /**
     * Injects into the getCollisionShape method (no context) to handle custom gravity when getting the collision shape.
     * This method returns the collision shape for a block state, which is used for collision detection.
     */
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;", 
            at = @At("RETURN"), 
            cancellable = true)
    private void onGetCollisionShapeNoContext(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        // Check if we're in a custom collision calculation
        if (!gravity_changer.collision.CollisionContext.isInCustomCollision()) {
            return;
        }

        // Get the current entity from the collision context
        Entity entity = gravity_changer.collision.CollisionContext.getCurrentEntity();
        if (entity == null) {
            return;
        }

        // Check if the entity has a gravity component
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        // Get the gravity direction
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 && 
                                  gravityDirection.x == 0 && 
                                  gravityDirection.z == 0;
        if (isDefaultGravity) {
            return;
        }

        // The original method will return a VoxelShape, which we need to transform
        // to work with OrientedBoundingBoxes. However, this is handled by other mixins
        // that transform the AABB inside the VoxelShape, so we don't need to modify
        // the return value here.
    }
}
