package gravity_changer.mixin.world.level;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Mixin for the CollisionGetter interface to handle collision detection with custom gravity.
 * This ensures entities with altered gravity properly interact with the world geometry.
 */
@Mixin(CollisionGetter.class)
public interface CollisionGetterMixin {

    /**
     * Injects into the findSupportingBlock method to adjust for custom gravity orientation.
     * This method is critical for determining what block an entity is standing on.
     */
    @Inject(method = "findSupportingBlock", at = @At("HEAD"), cancellable = true)
    default void onFindSupportingBlock(Entity entity, AABB box, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        // Check if the entity has a gravity component
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            // Entity's component container might not be initialized yet
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
        if (isDefaultGravity) return;

        // Set the current entity context for other mixins to use
        CollisionContext.setCurrentEntity(entity);
        try {
            // Get the current position and bounding box
            Vec3 position = entity.position();
            AABB entityBox = entity.getBoundingBox();

            // Offset the box in the direction of gravity to find the supporting block
            Vec3 gravityOffset = gravityDirection.scale(0.1); // Small offset in gravity direction
            AABB searchBox = entityBox.move(gravityOffset);

            // Find the closest block in the direction of gravity
            BlockPos entityBlockPos = entity.blockPosition();
            BlockPos supportingBlockPos = null;
            double closestDistance = Double.MAX_VALUE;

            // Check nearby blocks in the gravity direction
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos checkPos = entityBlockPos.offset(x, y, z);

                        // Skip air blocks
                        if (((CollisionGetter) this).getBlockState(checkPos).isAir()) {
                            continue;
                        }

                        // Get the block's collision shape
                        VoxelShape blockShape = ((CollisionGetter) this).getBlockState(checkPos)
                                .getCollisionShape((CollisionGetter) this, checkPos);

                        if (blockShape.isEmpty()) continue;

                        // Check if there's a collision with the entity's box
                        if (Shapes.joinIsNotEmpty(blockShape, Shapes.create(searchBox), BooleanOp.AND)) {
                            double distance = checkPos.distToCenterSqr(position);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                supportingBlockPos = checkPos;
                            }
                        }
                    }
                }
            }

            if (supportingBlockPos != null) {
                cir.setReturnValue(Optional.of(supportingBlockPos.immutable()));
            }
        } finally {
            // Clear the entity context
            CollisionContext.clearCurrentEntity();
        }
    }

    /**
     * Injects into the findFreePosition method to adjust for custom gravity orientation.
     * This helps entities find appropriate spaces to move into when affected by gravity changes.
     */
    @Inject(method = "findFreePosition", at = @At("HEAD"), cancellable = true)
    default void onFindFreePosition(Entity entity, VoxelShape shape, Vec3 pos, double x, double y, double z,
                                    CallbackInfoReturnable<Optional<Vec3>> cir) {
        if (entity == null) return;

        // Check if the entity has a gravity component
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
        if (isDefaultGravity) return;

        if (shape.isEmpty()) {
            cir.setReturnValue(Optional.empty());
            return;
        }

        // Set the entity context
        CollisionContext.setCurrentEntity(entity);
        try {
            // Transform search dimensions based on gravity direction
            Vec3 dimensions = new Vec3(x, y, z);
            Vec3 adjustedDimensions = RotationUtil.vecWorldToPlayerVec(dimensions, gravityDirection);

            // Adjust search position for gravity orientation
            Vec3 adjustedPos = RotationUtil.vecWorldToPlayerVec(pos, gravityDirection);

            // Call vanilla implementation with adjusted parameters
            Optional<Vec3> result = ((CollisionGetter) this).findFreePosition(
                    entity, shape, adjustedPos, adjustedDimensions.x, adjustedDimensions.y, adjustedDimensions.z);

            // Transform the result back to world space if found
            if (result.isPresent()) {
                Vec3 freePos = result.get();
                Vec3 worldFreePos = RotationUtil.vecPlayerToWorldVec(freePos, gravityDirection);
                cir.setReturnValue(Optional.of(worldFreePos));
            }
        } finally {
            // Clear the entity context
            CollisionContext.clearCurrentEntity();
        }
    }

    /**
     * Injects into the noCollision method to properly detect collisions with custom gravity.
     */
    @Inject(method = "noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z",
            at = @At("HEAD"), cancellable = true)
    default void onNoCollision(Entity entity, AABB box, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) return;

        // Check if the entity has a gravity component
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
        if (isDefaultGravity) return;

        // For OrientedBoundingBox, the standard collision detection should work
        // since we've already modified the AABB methods
        if (box instanceof OrientedBoundingBox) {
            return;
        }

        // Set the entity context for other mixins
        CollisionContext.setCurrentEntity(entity);
        CollisionContext.setInCustomCollision(true);

        try {
            // Transform the AABB to an OrientedBoundingBox for proper collision detection
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDirection);

            // Check if there are any collisions with the transformed box
            boolean hasCollision = !((CollisionGetter) this).getBlockCollisions(entity, obb.getLocalBox()).iterator().hasNext();

            // Set the result
            cir.setReturnValue(hasCollision);
        } finally {
            // Always clear the custom collision flag
            CollisionContext.setInCustomCollision(false);
            CollisionContext.clearCurrentEntity();
        }
    }
}
