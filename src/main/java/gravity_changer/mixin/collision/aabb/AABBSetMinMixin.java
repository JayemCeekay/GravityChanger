package gravity_changer.mixin.collision.aabb;

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
 * Mixin for the AABB class to handle the setMinX/Y/Z methods with custom gravity directions.
 * This ensures that AABBs with modified min coordinates are properly transformed for entities with custom gravity.
 */
@Mixin(AABB.class)
public abstract class AABBSetMinMixin {

    @Shadow public final double minX;
    @Shadow public final double minY;
    @Shadow public final double minZ;
    @Shadow public final double maxX;
    @Shadow public final double maxY;
    @Shadow public final double maxZ;

    public AABBSetMinMixin(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Injects into the 'setMinX' method to handle modifying min X coordinate with custom gravity directions.
     */
    @Inject(method = "setMinX", at = @At("HEAD"), cancellable = true)
    private void onSetMinX(double minX, CallbackInfoReturnable<AABB> cir) {
        handleSetMin(minX, this.minY, this.minZ, cir);
    }

    /**
     * Injects into the 'setMinY' method to handle modifying min Y coordinate with custom gravity directions.
     */
    @Inject(method = "setMinY", at = @At("HEAD"), cancellable = true)
    private void onSetMinY(double minY, CallbackInfoReturnable<AABB> cir) {
        handleSetMin(this.minX, minY, this.minZ, cir);
    }

    /**
     * Injects into the 'setMinZ' method to handle modifying min Z coordinate with custom gravity directions.
     */
    @Inject(method = "setMinZ", at = @At("HEAD"), cancellable = true)
    private void onSetMinZ(double minZ, CallbackInfoReturnable<AABB> cir) {
        handleSetMin(this.minX, this.minY, minZ, cir);
    }

    /**
     * Helper method to handle setting min coordinates with custom gravity directions.
     */
    private void handleSetMin(double minX, double minY, double minZ, CallbackInfoReturnable<AABB> cir) {
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
            // Create a new AABB with the modified min coordinates
            AABB modifiedBox = new AABB(
                minX, minY, minZ,
                this.maxX, this.maxY, this.maxZ
            );
            
            // Transform the modified box to an OrientedBoundingBox
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(modifiedBox, gravityDirectionVec, entity);
            
            // Return the transformed box
            cir.setReturnValue(obb);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}