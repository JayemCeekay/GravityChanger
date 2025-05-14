package gravity_changer.mixin.world.entity.boss.wither;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherBoss.class)
public abstract class WitherEntityMixin {
    // Height factor used to target halfway up the entity's height
    private static final double HEIGHT_FACTOR = 0.5D;

    @Redirect(
            method = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
                    ordinal = 0
            )
    )
    private double redirect_shootSkullAt_getX_0(LivingEntity target) {
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
                        new Vec3(0.0D, target.getEyeHeight() * HEIGHT_FACTOR, 0.0D),
                        gravityDirectionVec
                )
        ).x();
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getY()D",
                    ordinal = 0
            )
    )
    private double redirect_shootSkullAt_getY_0(LivingEntity target) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return target.getY();  // Fixed from getX() to getY()
        }

        // Calculate adjusted Y position based on gravity direction
        // We subtract the height factor to shoot at the middle of the entity
        Vec3 position = target.position().add(
                RotationUtil.vecPlayerToWorldVec(
                        new Vec3(0.0D, target.getEyeHeight() * HEIGHT_FACTOR, 0.0D),
                        gravityDirectionVec
                )
        );
        return position.y() - target.getEyeHeight() * HEIGHT_FACTOR;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
                    ordinal = 0
            )
    )
    private double redirect_shootSkullAt_getZ_0(LivingEntity target) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return target.getZ();  // Fixed from getX() to getZ()
        }

        // Calculate adjusted Z position based on gravity direction
        return target.position().add(
                RotationUtil.vecPlayerToWorldVec(
                        new Vec3(0.0D, target.getEyeHeight() * HEIGHT_FACTOR, 0.0D),
                        gravityDirectionVec
                )
        ).z();
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;aiStep()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getEyeY()D",
                    ordinal = 0
            )
    )
    private double redirect_tickMovement_getEyeY_0(Entity entity) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return entity.getEyeY();
        }

        // Return the properly adjusted eye Y position
        return entity.getEyePosition().y();
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;aiStep()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getX()D",
                    ordinal = 0
            )
    )
    private double redirect_tickMovement_getX_0(Entity entity) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return entity.getX();
        }

        // Return the properly adjusted eye X position
        return entity.getEyePosition().x();
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;aiStep()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getZ()D",
                    ordinal = 0
            )
    )
    private double redirect_tickMovement_getZ_0(Entity entity) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return entity.getZ();
        }

        // Return the properly adjusted eye Z position
        return entity.getEyePosition().z();
    }
}