package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonEntityMixin {
    @Redirect(
            method = "Lnet/minecraft/world/entity/monster/AbstractSkeleton;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
                    ordinal = 0
            )
    )
    private double redirect_attack_getX_0(LivingEntity target) {
        // Get Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return target.getX();
        }

        // For arbitrary directions, use the Vec3-based method
        return target.position().add(RotationUtil.vecPlayerToWorldVec(new net.minecraft.world.phys.Vec3(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D), gravityDirectionVec)).x;

    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/monster/AbstractSkeleton;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getY(D)D",
                    ordinal = 0
            )
    )
    private double redirect_attack_getBodyY_0(LivingEntity target, double heightScale) {
        // Get Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return target.getY(heightScale);
        }

        // For arbitrary directions, use the Vec3-based method
        return target.position().add(RotationUtil.vecPlayerToWorldVec(new net.minecraft.world.phys.Vec3(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D), gravityDirectionVec)).y;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/monster/AbstractSkeleton;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
                    ordinal = 0
            )
    )
    private double redirect_attack_getZ_0(LivingEntity target) {
        // Get Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return target.getZ();
        }

        // For arbitrary directions, use the Vec3-based method
        return target.position().add(RotationUtil.vecPlayerToWorldVec(new net.minecraft.world.phys.Vec3(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D), gravityDirectionVec)).z;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/monster/AbstractSkeleton;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;sqrt(D)D"
            )
    )
    private double redirect_attack_sqrt_0(double value, LivingEntity target, float pullProgress) {
        // Get Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return Math.sqrt(value);
        }

        // For both cardinal and arbitrary directions, use the same approach
        // This is a special case where the calculation doesn't depend on the specific gravity direction
        return Math.sqrt(Math.sqrt(value));
    }
}
