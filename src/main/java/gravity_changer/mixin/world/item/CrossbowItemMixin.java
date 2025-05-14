package gravity_changer.mixin.world.item;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @Redirect(
        method = "Lnet/minecraft/world/item/CrossbowItem;shootProjectile(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;FZFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
            ordinal = 0
        )
    )
    private static double redirect_shoot_getX_0(LivingEntity livingEntity) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(livingEntity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return livingEntity.getX();
        }
        
        return livingEntity.getEyePosition().subtract(RotationUtil.vecPlayerToWorldVec(0.0D, 0.15000000596046448D, 0.0D, gravityDirection)).x;
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/item/CrossbowItem;shootProjectile(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;FZFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D",
            ordinal = 0
        )
    )
    private static double redirect_shoot_getEyeY_0(LivingEntity livingEntity) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(livingEntity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return livingEntity.getEyeY();
        }
        
        return livingEntity.getEyePosition().subtract(RotationUtil.vecPlayerToWorldVec(0.0D, 0.15000000596046448D, 0.0D, gravityDirection)).y + 0.15000000596046448D;
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/item/CrossbowItem;shootProjectile(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;FZFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
            ordinal = 0
        )
    )
    private static double redirect_shoot_getZ_0(LivingEntity livingEntity) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(livingEntity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return livingEntity.getZ();
        }
        
        return livingEntity.getEyePosition().subtract(RotationUtil.vecPlayerToWorldVec(0.0D, 0.15000000596046448D, 0.0D, gravityDirection)).z;
    }
}
