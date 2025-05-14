package gravity_changer.mixin;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Drowned.class)
public abstract class DrownedEntityMixin {
    @Redirect(
        method = "Lnet/minecraft/world/entity/monster/Drowned;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
            ordinal = 0
        )
    )
    private double redirect_attack_getX_0(LivingEntity target) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(target);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return target.getX();
        }
        
        return target.position().add(RotationUtil.vecPlayerToWorldVec(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D, gravityDirection)).x;
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/entity/monster/Drowned;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getY(D)D",
            ordinal = 0
        )
    )
    private double redirect_attack_getBodyY_0(LivingEntity target, double heightScale) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(target);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return target.getY(heightScale);
        }
        
        return target.position().add(RotationUtil.vecPlayerToWorldVec(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D, gravityDirection)).y;
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/entity/monster/Drowned;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
            ordinal = 0
        )
    )
    private double redirect_attack_getZ_0(LivingEntity target) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(target);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return target.getZ();
        }
        
        return target.position().add(RotationUtil.vecPlayerToWorldVec(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D, gravityDirection)).z;
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/entity/monster/Drowned;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;sqrt(D)D"
        )
    )
    private double redirect_attack_sqrt_0(double value, LivingEntity target, float pullProgress) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(target);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return Math.sqrt(value);
        }
        
        return Math.sqrt(Math.sqrt(value));
    }
}
