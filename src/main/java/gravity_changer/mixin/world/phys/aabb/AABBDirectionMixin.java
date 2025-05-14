package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the AABB class to handle direction determination with custom gravity directions.
 * This transforms the inputs to gravity-aligned space, determines the direction, and transforms the result back.
 */
@Mixin(AABB.class)
public abstract class AABBDirectionMixin {

    /**
     * Injects into the 'getDirection' method to modify direction determination for off-axis gravity.
     * This transforms the inputs to gravity-aligned space, determines the direction, and transforms the result back.
     */
    @Inject(method = "getDirection(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/phys/Vec3;[DLnet/minecraft/core/Direction;DDD)Lnet/minecraft/core/Direction;", at = @At("HEAD"), cancellable = true)
    private static void onGetDirection(AABB aabb, Vec3 start, double[] minDistance, Direction facing, double deltaX, double deltaY, double deltaZ, CallbackInfoReturnable<Direction> cir) {
        // Only apply custom direction determination for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom collision calculation
        // This prevents recursion when OrientedBoundingBox.getDirection calls AABB.getDirection
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                // Entity doesn't have a gravity component yet, use default direction determination
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

        // Set the flag to indicate we're inside a custom direction determination calculation
        CollisionContext.setInCustomCollision(true);
        try {
            // Apply custom direction determination logic
            cir.setReturnValue(calculateCustomDirection(aabb, start, minDistance, facing, deltaX, deltaY, deltaZ, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates the direction of intersection between a ray and an AABB using OrientedBoundingBoxes.
     * 
     * @param aabb The AABB to check
     * @param start The start point of the ray
     * @param minDistance The minimum distance array
     * @param facing The current facing direction
     * @param deltaX The x component of the ray direction
     * @param deltaY The y component of the ray direction
     * @param deltaZ The z component of the ray direction
     * @param gravityDir The gravity direction vector
     * @return The direction of intersection
     */
    private static Direction calculateCustomDirection(AABB aabb, Vec3 start, double[] minDistance, Direction facing, double deltaX, double deltaY, double deltaZ, Vec3 gravityDir) {
        // Transform the box to an OrientedBoundingBox
        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(aabb, gravityDir);

        // Transform the ray to local space
        Vec3 center = new Vec3(
            (aabb.minX + aabb.maxX) / 2,
            (aabb.minY + aabb.maxY) / 2,
            (aabb.minZ + aabb.maxZ) / 2
        );
        Vec3 localStart = RotationUtil.vecWorldToPlayerVec(start.subtract(center), gravityDir).add(center);
        Vec3 localDelta = RotationUtil.vecWorldToPlayerVec(new Vec3(deltaX, deltaY, deltaZ), gravityDir);

        // Get the local box
        AABB localBox = obb.getLocalBox();

        // Determine the direction in local space
        Direction localFacing = facing != null ? RotationUtil.dirWorldToPlayer(facing, RotationUtil.vec3ToDirection(gravityDir)) : null;
        Direction localDirection = AABBAccessor.invokeGetDirection(localBox, localStart, minDistance, localFacing, localDelta.x, localDelta.y, localDelta.z);

        // Transform the direction back to world space
        Direction worldDirection = RotationUtil.dirPlayerToWorld(localDirection, RotationUtil.vec3ToDirection(gravityDir));

        return worldDirection;
    }
}
