package gravity_changer.mixin;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mob.class)
public abstract class MobEntityMixin {
    @WrapOperation(
        method = "doHurtTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;getYRot()F"
        )
    )
    private float wrapOperation_tryAttack_getYaw_0(Mob attacker, Operation<Float> original, Entity target) {
        // Get both Direction and Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(attacker);
        }


            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.rotWorldToPlayerVec(original.call(attacker), attacker.getXRot(), gravityDirectionVec).x;

    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/Mob;lookAt(Lnet/minecraft/world/entity/Entity;FF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D",
            ordinal = 0
        )
    )
    private double redirect_lookAtEntity_getEyeY_0(LivingEntity livingEntity) {
        // Get both Direction and Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(livingEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return livingEntity.getEyeY();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return livingEntity.getEyePosition().y;
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/Mob;lookAt(Lnet/minecraft/world/entity/Entity;FF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getX()D",
            ordinal = 0
        )
    )
    private double redirect_lookAtEntity_getX_0(Entity entity) {
        // Get both Direction and Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return entity.getX();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return entity.getEyePosition().x;
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/Mob;lookAt(Lnet/minecraft/world/entity/Entity;FF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getZ()D",
            ordinal = 0
        )
    )
    private double redirect_lookAtEntity_getZ_0(Entity entity) {
        // Get both Direction and Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return entity.getZ();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return entity.getEyePosition().z;
    }
}
