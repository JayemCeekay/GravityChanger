package gravity_changer.mixin.world.entity.Entity;

import gravity_changer.collision.CollisionContext;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityGravityCollisionMixin {
    @Shadow
    public boolean horizontalCollision;
    @Shadow public boolean verticalCollision;
    @Shadow protected abstract Vec3 getDeltaMovement();
    @Shadow public abstract void setDeltaMovement(Vec3 vec3);
    @Shadow protected abstract boolean isHorizontalCollisionMinor(Vec3 motion);

    // --- verticalCollision redirect ---
    @Redirect(method = "move",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Entity;verticalCollision:Z",
                    opcode = Opcodes.PUTFIELD))
    private void redirectVerticalCollision(Entity instance, boolean original) {
        Vec3 oldMotion = CollisionContext.getLastMotion(instance);
        Vec3 newMotion = instance.getDeltaMovement();
        Vec3 gravity  = CollisionContext.getGravityDirection(instance);

        // project both onto the gravity axis
        double beforeAlong = oldMotion.dot(gravity);
        double afterAlong  = newMotion.dot(gravity);

        // vertical collision = we tried moving along gravity but ended up with zero (or reduced) motion
        boolean collidedVertically =
                beforeAlong != 0.0 &&
                        Math.abs(afterAlong) < 1e-6;

        instance.verticalCollision = collidedVertically;
    }

    // --- horizontalCollision redirect ---
    @Redirect(method = "move",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Entity;horizontalCollision:Z",
                    opcode = Opcodes.PUTFIELD))
    private void redirectHorizontalCollision(Entity instance, boolean original) {
        Vec3 oldMotion = CollisionContext.getLastMotion(instance);
        Vec3 newMotion = instance.getDeltaMovement();
        Vec3 gravity   = CollisionContext.getGravityDirection(instance);

        // remove the gravity‐axis component to get the “horizontal” part
        Vec3 oldHoriz = oldMotion.subtract(gravity.scale(oldMotion.dot(gravity)));
        Vec3 newHoriz = newMotion.subtract(gravity.scale(newMotion.dot(gravity)));

        // collision happened if the horizontal component changed
        boolean collidedHorizontally = !oldHoriz.equals(newHoriz);

        instance.horizontalCollision = collidedHorizontally;
    }

    @Redirect(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/world/entity/Entity;verticalCollisionBelow:Z"
            )
    )
    private void redirectSetVerticalCollisionBelow(Entity instance, boolean originalValue) {
        // compute your gravity‐aware “below” test
        Vec3 motion   = instance.getDeltaMovement();
        Vec3 gravity  = CollisionContext.getGravityDirection(instance);
        double along  = motion.dot(gravity);
        boolean below = instance.verticalCollision && along < 0.0;

        // write back your custom value
        instance.verticalCollisionBelow = below;
    }

    /**
     * Redirect the call
     *   this.isHorizontalCollisionMinor(vec3)
     * inside
     *   this.minorHorizontalCollision = this.horizontalCollision ? ... : false;
     */
    @Redirect(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isHorizontalCollisionMinor(Lnet/minecraft/world/phys/Vec3;)Z"
            )
    )
    private boolean redirectMinorHorizontal(Entity instance, Vec3 motion) {
        // 1) grab your custom gravity
        Vec3 gravity = CollisionContext.getGravityDirection(instance);
        // 2) rotate the motion into world-coordinates
        Vec3 worldMotion = RotationUtil.vecPlayerToWorldVec(motion, gravity);
        // 3) only test if we actually had a horizontal collision
        if (!instance.horizontalCollision) return false;
        // 4) hand off to vanilla minor-collision logic, but with our rotated vector
        return ((EntityAccessor)instance).invokeIsHorizontalCollisionMinor(worldMotion);
    }
}
