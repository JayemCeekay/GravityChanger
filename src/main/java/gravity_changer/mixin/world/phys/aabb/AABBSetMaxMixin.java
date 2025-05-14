package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the AABB class to handle the setMaxX/Y/Z methods with custom gravity directions.
 * This ensures that AABBs with modified max coordinates are properly transformed for entities with custom gravity.
 */
@Mixin(AABB.class)
public abstract class AABBSetMaxMixin {

    @Shadow public final double minX;
    @Shadow public final double minY;
    @Shadow public final double minZ;
    @Shadow public final double maxX;
    @Shadow public final double maxY;
    @Shadow public final double maxZ;

    public AABBSetMaxMixin(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Injects into the 'setMaxX' method to handle modifying max X coordinate with custom gravity directions.
     */
    @Inject(method = "setMaxX", at = @At("HEAD"), cancellable = true)
    private void onSetMaxX(double maxX, CallbackInfoReturnable<AABB> cir) {
        handleSetMax(maxX, this.maxY, this.maxZ, cir);
    }

    /**
     * Injects into the 'setMaxY' method to handle modifying max Y coordinate with custom gravity directions.
     */
    @Inject(method = "setMaxY", at = @At("HEAD"), cancellable = true)
    private void onSetMaxY(double maxY, CallbackInfoReturnable<AABB> cir) {
        handleSetMax(this.maxX, maxY, this.maxZ, cir);
    }

    /**
     * Injects into the 'setMaxZ' method to handle modifying max Z coordinate with custom gravity directions.
     */
    @Inject(method = "setMaxZ", at = @At("HEAD"), cancellable = true)
    private void onSetMaxZ(double maxZ, CallbackInfoReturnable<AABB> cir) {
        handleSetMax(this.maxX, this.maxY, maxZ, cir);
    }

    /**
     * Helper method to handle setting max coordinates with custom gravity directions.
     */
    private void handleSetMax(double maxX, double maxY, double maxZ, CallbackInfoReturnable<AABB> cir) {
        // Only apply custom transformation for entities with custom gravity
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
            // Create a new AABB with the modified max coordinates
            AABB modifiedBox = new AABB(
                this.minX, this.minY, this.minZ,
                maxX, maxY, maxZ
            );
            
            // Transform the modified box to an OrientedBoundingBox
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(modifiedBox, gravityDirectionVec);
            
            // Return the transformed box
            cir.setReturnValue(obb);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}