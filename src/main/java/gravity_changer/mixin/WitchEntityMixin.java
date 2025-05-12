package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Witch.class)
public abstract class WitchEntityMixin {
    // Eye offset value used in calculations
    private static final double EYE_OFFSET = 1.100000023841858D;

    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/monster/Witch;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            ),
            ordinal = 0
    )
    private Vec3 modify_attack_Vec3d_0(Vec3 value, LivingEntity target, float pullProgress) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return value;
        }

        // Apply rotation to target's movement vector based on gravity direction
        return RotationUtil.vecPlayerToWorldVec(value, gravityDirectionVec);
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/monster/Witch;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
                    ordinal = 0
            )
    )
    private double redirect_attack_getX_0(LivingEntity target) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return target.getX();
        }

        // Calculate adjusted X position based on gravity direction
        return target.position().add(
                RotationUtil.vecPlayerToWorldVec(
                        new Vec3(0.0D, target.getEyeHeight() - EYE_OFFSET, 0.0D),
                        gravityDirectionVec
                )
        ).x();
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/monster/Witch;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D",
                    ordinal = 0
            )
    )
    private double redirect_attack_getEyeY_0(LivingEntity target) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return target.getEyeY();
        }

        // Calculate adjusted eye Y position based on gravity direction
        return target.position().add(
                RotationUtil.vecPlayerToWorldVec(
                        new Vec3(0.0D, target.getEyeHeight() - EYE_OFFSET, 0.0D),
                        gravityDirectionVec
                )
        ).y() + EYE_OFFSET;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/monster/Witch;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
                    ordinal = 0
            )
    )
    private double redirect_attack_getZ_0(LivingEntity target) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return target.getZ();
        }

        // Calculate adjusted Z position based on gravity direction
        return target.position().add(
                RotationUtil.vecPlayerToWorldVec(
                        new Vec3(0.0D, target.getEyeHeight() - EYE_OFFSET, 0.0D),
                        gravityDirectionVec
                )
        ).z();
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/monster/Witch;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;sqrt(D)D"
            )
    )
    private double redirect_attack_sqrt_0(double value, LivingEntity target, float pullProgress) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return Math.sqrt(value);
        }

        // Apply additional sqrt for non-standard gravity directions
        // This adjusts the travel distance calculation for projectiles
        return Math.sqrt(Math.sqrt(value));
    }
}