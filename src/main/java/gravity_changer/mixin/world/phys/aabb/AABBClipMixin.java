package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Mixin for the AABB class to handle ray tracing with custom gravity directions.
 * This transforms the ray to gravity-aligned space, performs ray tracing, and transforms the result back.
 */
@Mixin(AABB.class)
public abstract class AABBClipMixin {

    /**
     * Injects into the 'clip' method to modify ray tracing for off-axis gravity.
     * This transforms the ray to gravity-aligned space, performs ray tracing, and transforms the result back.
     */
    @Inject(method = "clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void onClip(Vec3 start, Vec3 end, CallbackInfoReturnable<Optional<Vec3>> cir) {
        // Only apply custom ray tracing for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom ray tracing calculation
        // This prevents recursion when OrientedBoundingBox.clip calls AABB.clip
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
            // Apply custom ray tracing logic
            cir.setReturnValue(calculateCustomRayTrace((AABB)(Object)this, start, end, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates ray tracing between a start and end point using OrientedBoundingBoxes.
     * 
     * @param box The AABB to ray trace against
     * @param start The start point of the ray
     * @param end The end point of the ray
     * @param gravityDir The gravity direction vector
     * @return An Optional<Vec3> containing the intersection point, or empty if there is no intersection
     */
    private Optional<Vec3> calculateCustomRayTrace(AABB box, Vec3 start, Vec3 end, Vec3 gravityDir) {
        // Transform the box to an OrientedBoundingBox
        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir);
        
        // Transform the ray to local space
        Vec3 center = new Vec3(
            (box.minX + box.maxX) / 2,
            (box.minY + box.maxY) / 2,
            (box.minZ + box.maxZ) / 2
        );
        Vec3 localStart = RotationUtil.vecWorldToPlayerVec(start.subtract(center), gravityDir).add(center);
        Vec3 localEnd = RotationUtil.vecWorldToPlayerVec(end.subtract(center), gravityDir).add(center);
        
        // Perform ray tracing in local space
        AABB localBox = obb.getLocalBox();
        Optional<Vec3> localHit = localBox.clip(localStart, localEnd);
        
        // If there's no intersection, return empty
        if (localHit.isEmpty()) {
            return Optional.empty();
        }
        
        // Transform the hit point back to world space
        Vec3 localHitPos = localHit.get();
        Vec3 worldHitPos = RotationUtil.vecPlayerToWorldVec(localHitPos.subtract(center), gravityDir).add(center);
        
        return Optional.of(worldHitPos);
    }

    /**
     * Injects into the static 'clip' method with BlockPos to modify ray tracing for off-axis gravity.
     */
    @Inject(method = "clip(Ljava/lang/Iterable;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;", at = @At("HEAD"), cancellable = true)
    private static void onClipStatic(Iterable<AABB> boxes, Vec3 start, Vec3 end, BlockPos pos, CallbackInfoReturnable<BlockHitResult> cir) {
        // Only apply custom ray tracing for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom ray tracing calculation
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

        // Set the flag to indicate we're inside a custom ray tracing calculation
        CollisionContext.setInCustomCollision(true);
        try {
            // Apply custom ray tracing logic
            cir.setReturnValue(calculateCustomRayTraceStatic(boxes, start, end, pos, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates ray tracing between a start and end point against multiple AABBs.
     * 
     * @param boxes The AABBs to ray trace against
     * @param start The start point of the ray
     * @param end The end point of the ray
     * @param pos The block position
     * @param gravityDir The gravity direction vector
     * @return A BlockHitResult if the ray intersects any box, null otherwise
     */
    private static BlockHitResult calculateCustomRayTraceStatic(Iterable<AABB> boxes, Vec3 start, Vec3 end, BlockPos pos, Vec3 gravityDir) {
        double closestDistance = Double.MAX_VALUE;
        Vec3 closestHit = null;
        Direction closestDirection = null;
        boolean closestInside = false;

        for (AABB box : boxes) {
            // Move the box to the block position
            AABB movedBox = box.move(pos);
            
            // Transform the box to an OrientedBoundingBox
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(movedBox, gravityDir);
            
            // Transform the ray to local space
            Vec3 center = new Vec3(
                (movedBox.minX + movedBox.maxX) / 2,
                (movedBox.minY + movedBox.maxY) / 2,
                (movedBox.minZ + movedBox.maxZ) / 2
            );
            Vec3 localStart = RotationUtil.vecWorldToPlayerVec(start.subtract(center), gravityDir).add(center);
            Vec3 localEnd = RotationUtil.vecWorldToPlayerVec(end.subtract(center), gravityDir).add(center);
            
            // Perform ray tracing in local space
            AABB localBox = obb.getLocalBox();
            Optional<Vec3> localHit = localBox.clip(localStart, localEnd);
            
            if (localHit.isPresent()) {
                Vec3 localHitPos = localHit.get();
                Vec3 worldHitPos = RotationUtil.vecPlayerToWorldVec(localHitPos.subtract(center), gravityDir).add(center);
                
                double distance = start.distanceToSqr(worldHitPos);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHit = worldHitPos;
                    
                    // Determine the hit direction
                    // This is a simplification - in a real implementation, we would need to calculate
                    // the actual face that was hit
                    Direction hitDirection = Direction.getNearest(
                        worldHitPos.x - movedBox.getCenter().x,
                        worldHitPos.y - movedBox.getCenter().y,
                        worldHitPos.z - movedBox.getCenter().z
                    );
                    closestDirection = hitDirection;
                    
                    // Determine if the hit is inside the box
                    // This is a simplification - in a real implementation, we would need to calculate
                    // whether the hit is inside or outside the box
                    closestInside = movedBox.contains(start);
                }
            }
        }

        if (closestHit == null) {
            return null;
        }

        return new BlockHitResult(closestHit, closestDirection, pos, closestInside);
    }
}