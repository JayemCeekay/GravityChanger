package gravity_changer.mixin.collision.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the AABB class to handle expandTowards operation with custom gravity directions.
 * This ensures that OrientedBoundingBoxes are expanded correctly in their local space.
 */
@Mixin(AABB.class)
public abstract class AABBExpandTowardsMixin {

    /**
     * Injects into the 'expandTowards(Vec3)' method to handle expanding OrientedBoundingBoxes.
     * This ensures that the box is expanded correctly in its local space.
     */
    @Inject(method = "expandTowards(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onExpandTowardsVec3(Vec3 vector, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom expansion for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if this is an OrientedBoundingBox
        if (!((Object)this instanceof OrientedBoundingBox)) {
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
            // Apply custom expandTowards logic
            cir.setReturnValue(calculateCustomExpandTowardsVec3((OrientedBoundingBox)(Object)this, vector, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Injects into the 'expandTowards(double, double, double)' method to handle expanding OrientedBoundingBoxes.
     * This ensures that the box is expanded correctly in its local space.
     */
    @Inject(method = "expandTowards(DDD)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void onExpandTowardsXYZ(double x, double y, double z, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom expansion for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if this is an OrientedBoundingBox
        if (!((Object)this instanceof OrientedBoundingBox)) {
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
            // Apply custom expandTowards logic
            cir.setReturnValue(calculateCustomExpandTowardsXYZ((OrientedBoundingBox)(Object)this, x, y, z, gravityDirectionVec));
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Calculates the result of expanding an OrientedBoundingBox towards a vector.
     * 
     * @param obb The OrientedBoundingBox to expand
     * @param vector The vector to expand towards
     * @param gravityDir The gravity direction vector
     * @return The expanded OrientedBoundingBox
     */
    private AABB calculateCustomExpandTowardsVec3(OrientedBoundingBox obb, Vec3 vector, Vec3 gravityDir) {
        // Transform the vector to local space
        Vec3 localVector = RotationUtil.vecWorldToPlayerVec(vector, gravityDir);
        
        // Create a new AABB with the expanded coordinates
        AABB expandedBox = new AABB(
            localVector.x > 0.0 ? obb.minX : obb.minX + localVector.x,
            localVector.y > 0.0 ? obb.minY : obb.minY + localVector.y,
            localVector.z > 0.0 ? obb.minZ : obb.minZ + localVector.z,
            localVector.x < 0.0 ? obb.maxX : obb.maxX + localVector.x,
            localVector.y < 0.0 ? obb.maxY : obb.maxY + localVector.y,
            localVector.z < 0.0 ? obb.maxZ : obb.maxZ + localVector.z
        );
        
        // Transform the expanded box to an OrientedBoundingBox
        return OrientedBoundingBoxTransformer.transformToOBB(expandedBox, gravityDir);
    }

    /**
     * Calculates the result of expanding an OrientedBoundingBox towards specific coordinates.
     * 
     * @param obb The OrientedBoundingBox to expand
     * @param x The x coordinate to expand towards
     * @param y The y coordinate to expand towards
     * @param z The z coordinate to expand towards
     * @param gravityDir The gravity direction vector
     * @return The expanded OrientedBoundingBox
     */
    private AABB calculateCustomExpandTowardsXYZ(OrientedBoundingBox obb, double x, double y, double z, Vec3 gravityDir) {
        // Transform the coordinates to local space
        Vec3 localVector = RotationUtil.vecWorldToPlayerVec(new Vec3(x, y, z), gravityDir);
        
        // Create a new AABB with the expanded coordinates
        AABB expandedBox = new AABB(
            localVector.x > 0.0 ? obb.minX : obb.minX + localVector.x,
            localVector.y > 0.0 ? obb.minY : obb.minY + localVector.y,
            localVector.z > 0.0 ? obb.minZ : obb.minZ + localVector.z,
            localVector.x < 0.0 ? obb.maxX : obb.maxX + localVector.x,
            localVector.y < 0.0 ? obb.maxY : obb.maxY + localVector.y,
            localVector.z < 0.0 ? obb.maxZ : obb.maxZ + localVector.z
        );
        
        // Transform the expanded box to an OrientedBoundingBox
        return OrientedBoundingBoxTransformer.transformToOBB(expandedBox, gravityDir);
    }
}