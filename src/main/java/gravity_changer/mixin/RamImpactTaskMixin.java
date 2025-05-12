package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.RamTarget;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RamTarget.class, priority = 1001)
public abstract class RamImpactTaskMixin {
    @Shadow
    private Vec3 ramDirection;

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V",
                    ordinal = 0
            )
    )
    private void wrapOperation_keepRunning_takeKnockback_0(LivingEntity target, double strength, double x, double z, Operation<Void> original) {
        // Get Vec3 gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 && gravityDirectionVec.x() == 0 && gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            original.call(target, strength, x, z);
            return;
        }

        // Transform the direction vector for arbitrary gravity
        Vec3 direction = RotationUtil.vecWorldToPlayerVec(this.ramDirection, gravityDirectionVec);
        original.call(target, strength, direction.x(), direction.z());
    }
}