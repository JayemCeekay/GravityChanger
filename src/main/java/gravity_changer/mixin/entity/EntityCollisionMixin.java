package gravity_changer.mixin.entity;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for the Entity class to handle custom movement and collision for non-default gravity.
 * This mixin sets the entity context before processing collisions and clears it afterward,
 * which allows the AABB Collision Mixin to work correctly.
 */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    /**
     * Injects at the start of the move method to set the entity context.
     */
    @Inject(method = "move", at = @At("HEAD"))
    private void onMoveStart(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;

        // Set the current entity in the collision context
        CollisionContext.setCurrentEntity(entity);
        // Ensure the custom collision flag is reset at the start of movement
        CollisionContext.setInCustomCollision(false);
    }

    /**
     * Injects at the end of the move method to clear the entity context.
     */
    @Inject(method = "move", at = @At("RETURN"))
    private void onMoveEnd(MoverType type, Vec3 movement, CallbackInfo ci) {
        // Clear the current entity from the collision context
        CollisionContext.clearCurrentEntity();
        // Also clear the custom collision flag to prevent it from affecting future collision checks
        CollisionContext.clearInCustomCollision();
    }

    /**
     * Injects before collision detection to transform the movement vector for off-axis gravity.
     */
    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", shift = At.Shift.BEFORE))
    private void onMoveBeforeCollide(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                // Entity doesn't have a gravity component yet, use default movement
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

        // Note: The actual transformation of the movement vector is already handled
        // in the existing EntityMixin.java, which transforms vectors between world and player space.
        // This injection point is just to ensure the entity context is set before collision detection.
    }
}
