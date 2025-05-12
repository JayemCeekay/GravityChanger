package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ThrowableProjectile.class)
public abstract class ThrownEntityMixin {

    @Shadow
    protected abstract float getGravity();

    /**
     * Modifies velocity during tick to account for custom gravity directions
     */
    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;tick()V",
            at = @At(
                    value = "STORE"
            ),
            ordinal = 0
    )
    public Vec3 tick(Vec3 modify) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((ThrowableProjectile) (Object) this);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return modify; // Skip unnecessary calculations for default gravity
        }

        // Add gravity in world space
        modify = new Vec3(modify.x(), modify.y() + this.getGravity(), modify.z());

        // Transform to player space, adjust gravity, then back to world space
        modify = RotationUtil.vecWorldToPlayerVec(modify, gravityDirectionVec);
        modify = new Vec3(modify.x(), modify.y() - this.getGravity(), modify.z());
        modify = RotationUtil.vecPlayerToWorldVec(modify, gravityDirectionVec);

        return modify;
    }

    /**
     * Modifies spawn position for thrown projectiles to account for gravity direction
     */
    @ModifyArgs(
            method = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;<init>(Lnet/minecraft/world/entity/EntityType;DDDLnet/minecraft/world/level/Level;)V",
                    ordinal = 0
            )
    )
    private static void modifyargs_init_init_0(Args args, EntityType<? extends ThrowableProjectile> type, LivingEntity owner, Level world) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(owner);

        // Check if using default gravity (optimized path)
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 &&
                gravityDirectionVec.x() == 0 &&
                gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return; // Skip unnecessary calculations for default gravity
        }

        // Calculate spawn position based on owner's eye position and gravity direction
        final double SPAWN_OFFSET = 0.10000000149011612D;
        Vec3 spawnOffset = RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, SPAWN_OFFSET, 0.0D), gravityDirectionVec);
        Vec3 pos = owner.getEyePosition().subtract(spawnOffset);

        // Set the position in the args
        args.set(1, pos.x());
        args.set(2, pos.y());
        args.set(3, pos.z());
    }

    /**
     * Modifies gravity based on gravity strength component
     */
    @ModifyReturnValue(method = "getGravity", at = @At("RETURN"))
    private float multiplyGravity(float original) {
        return original * (float) GravityChangerAPI.getGravityStrength(((Entity) (Object) this));
    }
}