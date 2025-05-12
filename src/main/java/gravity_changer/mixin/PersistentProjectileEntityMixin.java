package gravity_changer.mixin;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractArrow.class)
public abstract class PersistentProjectileEntityMixin extends Entity {
    
    public PersistentProjectileEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }
    
    
    @ModifyVariable(
        method = "Lnet/minecraft/world/entity/projectile/AbstractArrow;tick()V",
        at = @At(
            value = "STORE"
        )
        , ordinal = 0
    )
    public Vec3 tick(Vec3 modify) {
        modify = new Vec3(modify.x, modify.y + 0.05, modify.z);
        modify = RotationUtil.vecWorldToPlayerVec(modify, GravityChangerAPI.getGravityDirectionVec(this));
        modify = new Vec3(modify.x, modify.y - 0.05, modify.z);
        modify = RotationUtil.vecPlayerToWorldVec(modify, GravityChangerAPI.getGravityDirectionVec(this));
        return modify;
    }


    @ModifyArgs(
            method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;<init>(Lnet/minecraft/world/entity/EntityType;DDDLnet/minecraft/world/level/Level;)V"
            )
    )
    private static void modifyargs_init_init_0(Args args, EntityType<? extends ThrowableProjectile> type, LivingEntity owner, Level world) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(owner);

        // Check if we're using the default gravity direction (0, -1, 0)
        boolean isDefaultGravity = gravityDirection.equals(new Vec3(0, -1, 0));
        if (isDefaultGravity) return;

        // Create an offset vector in player's local space
        Vec3 localOffset = new Vec3(0.0D, 0.10000000149011612D, 0.0D);

        // Transform the offset to world space based on the gravity direction
        Vec3 worldOffset = RotationUtil.vecPlayerToWorldVec(localOffset, gravityDirection);

        // Calculate the adjusted position by subtracting the world offset from the eye position
        Vec3 pos = owner.getEyePosition().subtract(worldOffset);

        // Update the constructor arguments with the new position
        args.set(1, pos.x);
        args.set(2, pos.y);
        args.set(3, pos.z);
    }
    
    @ModifyConstant(method = "Lnet/minecraft/world/entity/projectile/AbstractArrow;tick()V", constant = @Constant(doubleValue = 0.05000000074505806))
    private double multiplyGravity(double constant) {
        return constant * GravityChangerAPI.getGravityStrength(this);
    }
}
