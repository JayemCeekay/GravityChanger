package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the AABB class to handle size calculation methods with custom gravity directions.
 * This ensures that size calculations work correctly for OrientedBoundingBoxes.
 */
@Mixin(AABB.class)
public abstract class AABBGetSizeMixin {

    /**
     * Injects into the 'getSize' method to handle calculating the average size of an OrientedBoundingBox.
     */
    @Inject(method = "getSize", at = @At("HEAD"), cancellable = true)
    private void onGetSize(CallbackInfoReturnable<Double> cir) {
        // Only apply custom calculation for OrientedBoundingBoxes
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Get the entity context
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

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
            // Get the OrientedBoundingBox
            OrientedBoundingBox obb = (OrientedBoundingBox)(Object)this;
            
            // Get the local box
            AABB localBox = obb.getLocalBox();
            
            // Calculate the average size using the local box
            double xSize = localBox.maxX - localBox.minX;
            double ySize = localBox.maxY - localBox.minY;
            double zSize = localBox.maxZ - localBox.minZ;
            double averageSize = (xSize + ySize + zSize) / 3.0;
            
            // Return the result
            cir.setReturnValue(averageSize);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Injects into the 'getXsize' method to handle calculating the X size of an OrientedBoundingBox.
     */
    @Inject(method = "getXsize", at = @At("HEAD"), cancellable = true)
    private void onGetXsize(CallbackInfoReturnable<Double> cir) {
        // Only apply custom calculation for OrientedBoundingBoxes
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Get the entity context
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

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
            // Get the OrientedBoundingBox
            OrientedBoundingBox obb = (OrientedBoundingBox)(Object)this;
            
            // Get the local box
            AABB localBox = obb.getLocalBox();
            
            // Calculate the X size using the local box
            double xSize = localBox.maxX - localBox.minX;
            
            // Return the result
            cir.setReturnValue(xSize);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Injects into the 'getYsize' method to handle calculating the Y size of an OrientedBoundingBox.
     */
    @Inject(method = "getYsize", at = @At("HEAD"), cancellable = true)
    private void onGetYsize(CallbackInfoReturnable<Double> cir) {
        // Only apply custom calculation for OrientedBoundingBoxes
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Get the entity context
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

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
            // Get the OrientedBoundingBox
            OrientedBoundingBox obb = (OrientedBoundingBox)(Object)this;
            
            // Get the local box
            AABB localBox = obb.getLocalBox();
            
            // Calculate the Y size using the local box
            double ySize = localBox.maxY - localBox.minY;
            
            // Return the result
            cir.setReturnValue(ySize);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }

    /**
     * Injects into the 'getZsize' method to handle calculating the Z size of an OrientedBoundingBox.
     */
    @Inject(method = "getZsize", at = @At("HEAD"), cancellable = true)
    private void onGetZsize(CallbackInfoReturnable<Double> cir) {
        // Only apply custom calculation for OrientedBoundingBoxes
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if we're already inside a custom collision calculation
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Get the entity context
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

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
            // Get the OrientedBoundingBox
            OrientedBoundingBox obb = (OrientedBoundingBox)(Object)this;
            
            // Get the local box
            AABB localBox = obb.getLocalBox();
            
            // Calculate the Z size using the local box
            double zSize = localBox.maxZ - localBox.minZ;
            
            // Return the result
            cir.setReturnValue(zSize);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}