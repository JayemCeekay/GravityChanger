package gravity_changer.mixin.world.level;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.VoxelShapeTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;

/**
 * Mixin for the BlockCollisions class to handle collision detection with custom gravity.
 * This ensures that OrientedBoundingBoxes are properly used for collision detection.
 */
@Mixin(BlockCollisions.class)
public class BlockCollisionsMixin<T> {

    @Shadow @Final private AABB box;
    @Shadow @Final private VoxelShape entityShape;

    // Store the entity locally since it's not a field in BlockCollisions
    private Entity capturedEntity;

    /**
     * Injects into the constructor to capture the entity and transform the AABB to an OrientedBoundingBox if needed.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;ZLjava/util/function/BiFunction;)V", at = @At("RETURN"))
    private void onInit(CollisionGetter collisionGetter, Entity entity, AABB aabb, boolean bl, BiFunction<?, ?, T> biFunction, CallbackInfo ci) {
        // Capture the entity for later use
        this.capturedEntity = entity;

        // Only apply for entities with custom gravity
        if (entity == null) return;

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
        if (isDefaultGravity) return;

        // Set the entity context for other mixins
        CollisionContext.setCurrentEntity(entity);
        CollisionContext.setInCustomCollision(true);
    }

    /**
     * Injects into the computeNext method to handle collision detection with custom gravity.
     */
    @Inject(method = "computeNext", at = @At("HEAD"), cancellable = true)
    private void onComputeNextStart(CallbackInfoReturnable<?> cir) {
        if (entityHasCustomGravity(this.capturedEntity)) {
            CollisionContext.setCurrentEntity(this.capturedEntity);
            CollisionContext.setInCustomCollision(true);  // Set this BEFORE any intersects calls
        }
    }

    // Helper method to check if entity has custom gravity
    private boolean entityHasCustomGravity(Entity entity) {
        if (entity == null) return false;

        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return false;
            }
        } catch (NullPointerException e) {
            return false;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        return !(gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0);
    }

    @Inject(method = "computeNext", at = @At("RETURN"))
    private void onComputeNextEnd(CallbackInfoReturnable<?> cir) {
        CollisionContext.clearInCustomCollision();
        CollisionContext.clearCurrentEntity();
    }

    @Inject(method = "computeNext",
            at = @At("HEAD"))
    private void beforeComputeNext(CallbackInfoReturnable<T> cir) {
        // store the “before” motion so our Entity mixin can grab it later
        if(capturedEntity != null) {
            CollisionContext.setLastMotion(this.capturedEntity, this.capturedEntity.getDeltaMovement());
        }
    }

    /**
     * Injects into the computeNext method to transform the VoxelShape based on gravity direction.
     * This ensures that block collisions are properly handled for entities with custom gravity.
     */
    @Inject(method = "computeNext", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void onGetCollision(CallbackInfoReturnable<T> cir) {
        // Only apply for entities with custom gravity
        if (!entityHasCustomGravity(this.capturedEntity)) {
            return;
        }

        // Get the result from the original method
        T result = cir.getReturnValue();

        // If the result is a VoxelShape, transform it based on gravity direction
        if (result instanceof VoxelShape) {
            Vec3 gravityDir = GravityChangerAPI.getGravityDirectionVec(this.capturedEntity);

            // Transform the box according to gravity
            AABB rotatedBox = RotationUtil.boxWorldToPlayerVec(this.box, gravityDir);

            // Transform the shape based on gravity direction
            VoxelShape transformedShape = VoxelShapeTransformer.transformVoxelShape((VoxelShape) result, gravityDir);

            // Set the transformed shape as the return value
            cir.setReturnValue((T) transformedShape);
        }
    }
}
