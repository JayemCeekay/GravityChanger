package gravity_changer.mixin.world.entity.LivingEntity;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for applying gravity in the custom direction.
 * This ensures that entities fall in the direction of their custom gravity.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin extends Entity {

    public LivingEntityTravelMixin(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    /**
     * Modifies the gravity constant to account for gravity strength and direction.
     * For custom gravity directions, we return 0 to prevent vanilla gravity application.
     */
    /*@ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.08))
    private double modifyGravityConstant(double constant) {
        // Get the gravity direction
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirection.y < -0.99 && 
                                  gravityDirection.x == 0 && 
                                  gravityDirection.z == 0;

        // Only modify the constant for default gravity
        if (isDefaultGravity) {
            return constant * GravityChangerAPI.getGravityStrength(this);
        }

        // For custom gravity, return 0 to prevent vanilla gravity application
        // We'll apply gravity ourselves in the correct direction
        return 0.0;
    }*/

    /**
     * Applies gravity in the custom direction after vanilla movement processing.
     * This ensures that entities fall in the direction of their custom gravity.
     */
    /*@Inject(method = "travel", at = @At("RETURN"))
    private void applyCustomGravity(Vec3 travelVector, CallbackInfo ci) {
        // Get the gravity direction
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirection.y < -0.99 && 
                                  gravityDirection.x == 0 && 
                                  gravityDirection.z == 0;

        // Only apply custom gravity for non-default directions
        if (!isDefaultGravity && !this.isNoGravity()) {

            // Set up collision context for gravity-aware collision detection
            CollisionContext.setCurrentEntity(this);

            try {
                // Get the gravity constant
                double gravityConstant = 0.08 * GravityChangerAPI.getGravityStrength(this);

                // Apply gravity in the custom direction
                Vec3 gravityVector = gravityDirection.scale(gravityConstant);
                Vec3 motion = this.getDeltaMovement();

                // Set the new motion with gravity applied in the custom direction
                this.setDeltaMovement(motion.add(gravityVector));
            } finally {
                // Always clear the collision context
                CollisionContext.clearCurrentEntity();
            }
        }
    }*/

    /**
     * Redirects the gravity application in lava to apply it in the custom gravity direction
     */
    @Redirect(
            method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 3
            )
    )
    private void redirectLavaGravity(LivingEntity entity, Vec3 motion) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;

        if (isDefaultGravity || entity.isNoGravity()) {
            // Use vanilla behavior for default gravity or no gravity
            entity.setDeltaMovement(motion);
            return;
        }

        // Get the gravity constant (already multiplied by strength)
        double gravityConstant = 0.08 * GravityChangerAPI.getGravityStrength(entity);

        // Apply gravity in the custom direction
        Vec3 gravityVector = gravityDirection.scale(gravityConstant / 4.0);
        Vec3 newMotion = entity.getDeltaMovement();

        entity.setDeltaMovement(newMotion.add(gravityVector));
    }
}
