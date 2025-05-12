package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Projectile.class)
public abstract class ProjectileEntityMixin {
    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            at = @At("HEAD"),
            ordinal = 0
    )
    private float modify_setProperties_pitch(float value, Entity user, float yaw, float roll, float speed, float divergence) {
        // Get Vec3 gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(user);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 && gravityDirectionVec.x() == 0 && gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return value;
        }

        // Use rotPlayerToWorldVec for arbitrary gravity directions
        return RotationUtil.rotPlayerToWorldVec(user.getYRot(), user.getXRot(), gravityDirectionVec).y;
    }

    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            at = @At("HEAD"),
            ordinal = 1
    )
    private float modify_setProperties_yaw(float value, Entity user, float pitch, float roll, float speed, float divergence) {
        // Get Vec3 gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(user);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 && gravityDirectionVec.x() == 0 && gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return value;
        }

        // Use rotPlayerToWorldVec for arbitrary gravity directions
        return RotationUtil.rotPlayerToWorldVec(user.getYRot(), user.getXRot(), gravityDirectionVec).x;
    }
}