package gravity_changer.mixin.world.entity.LivingEntity;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    public abstract void readAdditionalSaveData(CompoundTag nbt);

    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);

    @Shadow
    public abstract float getViewYRot(float tickDelta);

    @Shadow
    protected abstract void jumpFromGround();
    @Shadow
    protected abstract float getJumpPower();

    @Shadow
    protected boolean jumping;

    @Shadow
    public float yya;  // Vertical movement input

    @Shadow
    public float xxa;  // Horizontal movement input (strafe)

    @Shadow
    public float zza;  // Forward/backward movement input

    @Shadow public abstract boolean hasEffect(MobEffect effect);

    @Shadow public abstract @Nullable MobEffectInstance getEffect(MobEffect effect);

    @Shadow protected abstract void playBlockFallSound();

    // UUID for gravity-specific movement modifiers
    private static final UUID GRAVITY_MOVEMENT_MODIFIER_UUID = UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635");

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyArg(
            method = "playBlockFallSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            index = 0
    )
    private BlockPos modify_playBlockFallSound_getBlockState_0(BlockPos blockPos) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return blockPos;
        }


        // For arbitrary directions, use the Vec3-based method
        return BlockPos.containing(this.position().add(RotationUtil.vecPlayerToWorldVec(new Vec3(0, -0.20000000298023224D, 0), gravityDirectionVec)));

    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(
                    value = "NEW",
                    target = "(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            )
    )
    private Vec3 redirect_canSee_new_0(double x, double y, double z) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return new Vec3(x, y, z);
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return this.getEyePosition();
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(
                    value = "NEW",
                    target = "(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1
            )
    )
    private Vec3 redirect_canSee_new_1(double x, double y, double z, Entity entity) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return new Vec3(x, y, z);
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return entity.getEyePosition();
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/LivingEntity;getLocalBoundsForPose(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/phys/AABB;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void inject_getBoundingBox(Pose pose, CallbackInfoReturnable<AABB> cir) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return;
        }

        AABB box = cir.getReturnValue();

        // Use OrientedBoundingBoxTransformer to transform the box
        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDirectionVec);

        cir.setReturnValue(obb);
    }

//    @Inject(
//            method = "updateLimbs",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private void inject_updateLimbs(LivingEntity entity, boolean flutter, CallbackInfo ci) {
//        Direction gravityDirection = GravityChangerAPI.getGravityDirection(entity);
//        if(gravityDirection == Direction.DOWN) return;
//
//        ci.cancel();
//
//        Vec3d playerPosDelta = RotationUtil.vecWorldToPlayer(entity.getX() - entity.prevX, entity.getY() - entity.prevY, entity.getZ() - entity.prevZ, gravityDirection);
//
//        entity.lastLimbDistance = entity.limbDistance;
//        double d = playerPosDelta.x;
//        double e = flutter ? playerPosDelta.y : 0.0D;
//        double f = playerPosDelta.z;
//        float g = (float)Math.sqrt(d * d + e * e + f * f) * 4.0F;
//        if (g > 1.0F) {
//            g = 1.0F;
//        }
//
//        entity.limbDistance += (g - entity.limbDistance) * 0.4F;
//        entity.limbAngle += entity.limbDistance;
//    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
                    ordinal = 0
            )
    )
    private double wrapOperation_tick_getX_0(LivingEntity livingEntity, Operation<Double> original) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(livingEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(livingEntity);
        }

        // Calculate the delta movement vector
        Vec3 deltaMovement = new Vec3(
                original.call(livingEntity) - livingEntity.xo,
                livingEntity.getY() - livingEntity.yo,
                livingEntity.getZ() - livingEntity.zo
        );


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(deltaMovement, gravityDirectionVec).x + livingEntity.xo;

    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
                    ordinal = 0
            )
    )
    private double wrapOperation_tick_getZ_0(LivingEntity livingEntity, Operation<Double> original) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(livingEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(livingEntity);
        }

        // Calculate the delta movement vector
        Vec3 deltaMovement = new Vec3(
                livingEntity.getX() - livingEntity.xo,
                livingEntity.getY() - livingEntity.yo,
                original.call(livingEntity) - livingEntity.zo
        );


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(deltaMovement, gravityDirectionVec).z + livingEntity.zo;

    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getX()D",
                    ordinal = 0
            )
    )
    private double redirect_damage_getX_0(Entity attacker) {
        // Get both Direction and Vec3 gravity directions for this entity
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if this entity is using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            // Get attacker's gravity direction
            Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

            // Check if attacker is using the default gravity direction
            boolean isAttackerDefaultGravity = attackerGravityDirectionVec.y < -0.99 && attackerGravityDirectionVec.x == 0 && attackerGravityDirectionVec.z == 0;
            if (isAttackerDefaultGravity) {
                return attacker.getX();
            } else {
                return attacker.getEyePosition().x;
            }
        }


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(attacker.getEyePosition(), gravityDirectionVec).x;

    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getZ()D",
                    ordinal = 0
            )
    )
    private double redirect_damage_getZ_0(Entity attacker) {
        // Get both Direction and Vec3 gravity directions for this entity
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if this entity is using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            // Get attacker's gravity direction
            Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

            // Check if attacker is using the default gravity direction
            boolean isAttackerDefaultGravity = attackerGravityDirectionVec.y < -0.99 && attackerGravityDirectionVec.x == 0 && attackerGravityDirectionVec.z == 0;
            if (isAttackerDefaultGravity) {
                return attacker.getZ();
            } else {
                return attacker.getEyePosition().z;
            }
        }


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(attacker.getEyePosition(), gravityDirectionVec).z;

    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
                    ordinal = 0
            )
    )
    private double redirect_damage_getX_0(LivingEntity target) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(target);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return target.getX();
        }

        return RotationUtil.vecWorldToPlayerVec(target.position(), gravityDirection).x;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
                    ordinal = 0
            )
    )
    private double redirect_damage_getZ_0(LivingEntity target) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(target);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return target.getZ();
        }

        return RotationUtil.vecWorldToPlayerVec(target.position(), gravityDirection).z;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;blockedByShield(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
                    ordinal = 0
            )
    )
    private double redirect_knockback_getX_0(LivingEntity target) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return target.getX();
        }


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(target.position(), gravityDirectionVec).x;

    }


    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;blockedByShield(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
                    ordinal = 0
            )
    )
    private double redirect_knockback_getZ_0(LivingEntity target) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return target.getZ();
        }


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(target.position(), gravityDirectionVec).z;

    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;blockedByShield(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getX()D",
                    ordinal = 1
            )
    )
    private double redirect_knockback_getX_1(LivingEntity attacker, LivingEntity target) {
        // Get both Direction and Vec3 gravity directions for target
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if target is using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            // Get attacker's gravity direction
            Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

            // Check if attacker is using the default gravity direction
            boolean isAttackerDefaultGravity = attackerGravityDirectionVec.y < -0.99 && attackerGravityDirectionVec.x == 0 && attackerGravityDirectionVec.z == 0;
            if (isAttackerDefaultGravity) {
                return attacker.getX();
            } else {
                return attacker.getEyePosition().x;
            }
        }

        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(attacker.getEyePosition(), gravityDirectionVec).x;

    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;blockedByShield(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D",
                    ordinal = 1
            )
    )
    private double redirect_knockback_getZ_1(LivingEntity attacker, LivingEntity target) {
        // Get both Direction and Vec3 gravity directions for target
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if target is using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            // Get attacker's gravity direction
            Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

            // Check if attacker is using the default gravity direction
            boolean isAttackerDefaultGravity = attackerGravityDirectionVec.y < -0.99 && attackerGravityDirectionVec.x == 0 && attackerGravityDirectionVec.z == 0;
            if (isAttackerDefaultGravity) {
                return attacker.getZ();
            } else {
                return attacker.getEyePosition().z;
            }
        }


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(attacker.getEyePosition(), gravityDirectionVec).z;

    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/LivingEntity;baseTick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;",
                    ordinal = 0
            )
    )
    private BlockPos redirect_baseTick_new_0(double x, double y, double z) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return BlockPos.containing(x, y, z);
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return BlockPos.containing(this.getEyePosition());
    }

    @WrapOperation(
            method = "spawnItemParticles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            )
    )
    private Vec3 wrapOperation_spawnItemParticles_add_0(Vec3 vec3d, double x, double y, double z, Operation<Vec3> original) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(vec3d, x, y, z);
        }

        Vec3 rotated;

        // For arbitrary directions, use the Vec3-based method
        rotated = RotationUtil.vecPlayerToWorldVec(vec3d, gravityDirectionVec);


        return original.call(this.getEyePosition(), rotated.x, rotated.y, rotated.z);
    }

    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/LivingEntity;spawnItemParticles(Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/phys/Vec3;yRot(F)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            ),
            ordinal = 0
    )
    private Vec3 modify_spawnItemParticles_Vec3d_0(Vec3 vec3d) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return vec3d;
        }


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecPlayerToWorldVec(vec3d, gravityDirectionVec);
    }

    @ModifyArgs(
            method = "tickEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void modify_tickStatusEffects_addParticle_0(Args args) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        Vec3 particlePos = new Vec3(args.get(1), args.get(2), args.get(3));
        Vec3 vec3d;


        // For arbitrary directions, use the Vec3-based method
        vec3d = this.position().subtract(RotationUtil.vecPlayerToWorldVec(this.position().subtract(particlePos), gravityDirectionVec));

        args.set(1, vec3d.x);
        args.set(2, vec3d.y);
        args.set(3, vec3d.z);
    }

    @ModifyArgs(
            method = "makePoofParticles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
                    ordinal = 0
            )
    )
    private void modify_addDeathParticless_addParticle_0(Args args) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        Vec3 particlePos = new Vec3(args.get(1), args.get(2), args.get(3));
        Vec3 vec3d;


        // For arbitrary directions, use the Vec3-based method
        vec3d = this.position().subtract(RotationUtil.vecPlayerToWorldVec(this.position().subtract(particlePos), gravityDirectionVec));

        args.set(1, vec3d.x);
        args.set(2, vec3d.y);
        args.set(3, vec3d.z);
    }

    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            ),
            ordinal = 1
    )
    private Vec3 modify_blockedByShield_Vec3d_1(Vec3 vec3d) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return vec3d;
        }


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(vec3d, gravityDirectionVec);

    }

    // TODO shield knockback
//    @ModifyArg(
//        method = "blockedByShield",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/phys/Vec3;vectorTo(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
//            ordinal = 0
//        ),
//        index = 0
//    )
//    private Vec3 modify_blockedByShield_relativize_0(Vec3 vec3d) {
//        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity)(Object)this);
//        if(gravityDirection == Direction.DOWN) {
//            return vec3d;
//        }
//
//        return this.getEyePosition();
//    }

//    @ModifyVariable(
//        method = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z",
//        at = @At(
//            value = "INVOKE_ASSIGN",
//            target = "Lnet/minecraft/world/phys/Vec3;normalize()Lnet/minecraft/world/phys/Vec3;",
//            ordinal = 0
//        ),
//        ordinal = 2
//    )
//    private Vec3 modify_blockedByShield_Vec3d_2(Vec3 vec3d) {
//        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity)(Object)this);
//        if(gravityDirection == Direction.DOWN) {
//            return vec3d;
//        }
//
//        return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
//    }


    @ModifyVariable(method = "Lnet/minecraft/world/entity/LivingEntity;calculateFallDamage(FF)I", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float diminishFallDamage(float value) {
        return value * (float) Math.sqrt(GravityChangerAPI.getGravityStrength(this));
    }

    /**
     * Handles proper jumping mechanics in altered gravity directions
     */
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void injectJumpFromGround(CallbackInfo ci) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return; // Default gravity, let vanilla handle it

        // Cancel vanilla jump mechanics
        ci.cancel();

        if (!this.onGround()) return; // Only jump if on ground

        // Calculate jump velocity based on gravity direction
        float jumpPower = this.getJumpPower();

        // Apply jump boost effect if present
        if (this.hasEffect(MobEffects.JUMP)) {
            MobEffectInstance jumpBoost = this.getEffect(MobEffects.JUMP);
            if (jumpBoost != null) {
                jumpPower += 0.1F * (jumpBoost.getAmplifier() + 1);
            }
        }

        // Create jump vector in player space (up = +Y)
        Vec3 jumpVec = new Vec3(0, jumpPower, 0);

        // Transform to world space based on gravity direction
        Vec3 worldJumpVec = RotationUtil.vecPlayerToWorldVec(jumpVec, gravityDirection);

        // Apply jump velocity
        this.setDeltaMovement(this.getDeltaMovement().add(worldJumpVec));

        // Mark as having an impulse to apply
        this.hasImpulse = true;

        // Reset jumping state to avoid repeat jumps
        this.jumping = false;
    }

    /**
     * Modifies fall damage calculation for custom gravity directions
     */
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void injectCauseFallDamage(float fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return; // Default gravity, vanilla handling

        // For non-default gravity, we need to ensure the fall direction is considered correctly
        if (fallDistance <= 3.0F) {
            // No damage for small falls
            cir.setReturnValue(false);
            return;
        }

        // Setup collision context for gravity-aware collision detection
        CollisionContext.setCurrentEntity(this);

        try {
            // Calculate adjusted fall damage - similar to vanilla but respecting gravity direction
            float adjustedDamage = (float) Math.ceil(fallDistance - 3.0F);

            // Apply damage with the correct source
            boolean damaged = this.hurt(source, adjustedDamage * multiplier);

            if (damaged) {
                // Play fall sound based on gravity direction
                this.playBlockFallSound();
            }

            cir.setReturnValue(damaged);
        } finally {
            // Always clear the collision context
            CollisionContext.clearCurrentEntity();
        }
    }



    /**
     * Adjusts block placement sound positioning when a living entity falls
     */
    @ModifyArg(
            method = "playBlockFallSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            index = 0
    )
    private BlockPos modifyPlayBlockFallSound(BlockPos pos) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return pos; // Default gravity

        // Determine which direction is "down" for the entity and adjust the block position
        return getBlockPosInGravityDirection(pos, gravityDirection);
    }

    /**
     * Utility method to find the block position in the direction of gravity
     */
    private static BlockPos getBlockPosInGravityDirection(BlockPos pos, Vec3 gravityDirectionVec) {
        // Normalize the gravity vector
        Vec3 normalizedGravity = gravityDirectionVec.normalize();

        // Scale by 1 block distance
        Vec3 offset = normalizedGravity.scale(1.0);

        // Convert to BlockPos (round to nearest block)
        return BlockPos.containing(
                pos.getX() + offset.x,
                pos.getY() + offset.y,
                pos.getZ() + offset.z
        );
    }

    /* TODO */
    /**
     * Modifies fluid checks for swimming in altered gravity
     */
    /*@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void modifyUpdateSwimming(CallbackInfo ci) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(this);
        if (gravityDirection == Direction.DOWN) return; // Default gravity

        // Set up fluid collision context
        CollisionContext.setCurrentEntity(this);
        CollisionContext.setInFluidCheck(true);

        try {
            // Let vanilla handle swimming state updates with our collision context
            // This ensures fluid collision detection respects gravity direction
        } finally {
            // Always clean up
            CollisionContext.setInFluidCheck(false);
            CollisionContext.clearCurrentEntity();
        }
    }*/

    /**
     * Adjusts bounding box calculation for different poses in altered gravity
     */
    @Inject(
            method = "getLocalBoundsForPose",
            at = @At("RETURN"),
            cancellable = true
    )
    private void modifyBoundingBoxForPose(Pose pose, CallbackInfoReturnable<AABB> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return; // Default gravity

        // Get the original bounding box
        AABB originalBox = cir.getReturnValue();

        // Transform the bounding box to respect gravity direction
        // This rotates the box so dimensions like height are oriented correctly
        AABB transformedBox = RotationUtil.boxWorldToPlayerVec(originalBox, gravityDirection);

        cir.setReturnValue(transformedBox);
    }

    /**
     * Completely overrides the hasLineOfSight method to handle gravity changes
     */
    @Overwrite
    public boolean hasLineOfSight(Entity entity) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            // Use vanilla implementation for default gravity
            if (this.level().isClientSide) {
                return this.hasLineOfSight(entity);
            }
            Vec3 vec3 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
            Vec3 vec32 = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            return this.level().clip(new ClipContext(vec3, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
        } else {
            // Custom implementation for altered gravity
            if (this.level().isClientSide) {
                return this.hasLineOfSight(entity);
            }

            // Get eye positions in world space
            Vec3 myEyePos = new Vec3(this.getX(), this.getEyeY(), this.getZ());
            Vec3 targetEyePos = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());

            // Transform to player space to account for gravity
            Vec3 gravityVec = GravityChangerAPI.getGravityDirectionVec(this);
            Vec3 myEyePosPlayerSpace = RotationUtil.vecWorldToPlayerVec(myEyePos, gravityDirection);
            Vec3 targetEyePosPlayerSpace = RotationUtil.vecWorldToPlayerVec(targetEyePos, gravityDirection);

            // Create clip context in player space
            ClipContext context = new ClipContext(
                    myEyePosPlayerSpace,
                    targetEyePosPlayerSpace,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            );

            // Perform the collision check
            HitResult result = this.level().clip(context);
            return result.getType() == HitResult.Type.MISS;
        }
    }

    /**
     * Adjusts movement control for wall/ceiling movement
     */
    @Inject(method = "onChangedBlock", at = @At("TAIL"))
    private void onBlockChange(BlockPos pos, CallbackInfo ci) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            // Remove any gravity-specific modifiers if back to normal gravity
            clearGravityMovementModifiers();
            return;
        }

        // For non-default gravity, adjust movement attributes for better control
        LivingEntity entity = (LivingEntity) (Object) this;

        AttributeInstance movementAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute == null) return;

        // Remove existing modifier if present
        clearGravityMovementModifiers();

        // Add movement boost for wall/ceiling walking
        // This helps counteract the awkwardness of moving on non-floor surfaces
        double baseValue = movementAttribute.getBaseValue();
        AttributeModifier gravityModifier = new AttributeModifier(
                GRAVITY_MOVEMENT_MODIFIER_UUID,
                "Gravity direction movement adjustment",
                0.15 * baseValue, // 15% boost
                AttributeModifier.Operation.ADDITION
        );

        movementAttribute.addTransientModifier(gravityModifier);
    }

    /**
     * Helper method to clear gravity-specific movement modifiers
     */
    private void clearGravityMovementModifiers() {
        LivingEntity entity = (LivingEntity) (Object) this;
        AttributeInstance movementAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movementAttribute != null) {
            movementAttribute.removeModifier(GRAVITY_MOVEMENT_MODIFIER_UUID);
        }
    }

    /**
     * Handles tick logic for living entities in altered gravity
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return; // Default gravity

        // For non-default gravity, ensure the entity knows it's affected by gravity
        // This is important for various entity behaviors

        LivingEntity entity = (LivingEntity) (Object) this;

        // Handle automatic jumping when moving up walls in altered gravity
        // This makes wall climbing more intuitive
        if (this.onGround() && this.zza > 0 && !this.jumping) {
            // Check if gravity is horizontal (any direction where Y component is near zero)
            boolean isHorizontalGravity = Math.abs(gravityDirection.y) < 0.1 &&
                    (Math.abs(gravityDirection.x) > 0.9 ||
                            Math.abs(gravityDirection.z) > 0.9);

            if (isHorizontalGravity) {
                // Calculate the "up" direction relative to gravity
                // This is the opposite of gravity
                Vec3 upDirection = gravityDirection.reverse().normalize();

                // Convert the player's view rotation to a look vector
                float xRot = entity.getViewXRot(1.0F);
                float yRot = entity.getViewYRot(1.0F);
                Vec3 lookVec = new Vec3(
                        -Math.sin(Math.toRadians(yRot)) * Math.cos(Math.toRadians(xRot)),
                        -Math.sin(Math.toRadians(xRot)),
                        Math.cos(Math.toRadians(yRot)) * Math.cos(Math.toRadians(xRot))
                );

                // Calculate the dot product between the look vector and the up direction
                // If it's positive, the player is looking up relative to their gravity
                double dotProduct = lookVec.dot(upDirection);

                // If player is looking up (dot product > 0.5, which is about 30 degrees)
                if (dotProduct > 0.5) {
                    // Trigger a jump to help climb up walls when looking up
                    this.jumpFromGround();
                }
            }
        }
    }

    /**
     * Handles custom entity collision for entities with different gravity directions
     */
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity other, CallbackInfo ci) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this);
        Vec3 otherGravityDirection = GravityChangerAPI.getGravityDirectionVec(other);

        // Default gravity is (0, -1, 0)
        Vec3 defaultGravity = new Vec3(0, -1, 0);

        if (gravityDirection.equals(defaultGravity) && otherGravityDirection.equals(defaultGravity)) {
            return; // Default gravity for both entities, let vanilla handle it
        }

        // For entities with different gravity directions, we need custom collision handling
        ci.cancel();

        // Calculate the collision response in a gravity-aware manner
        Vec3 thisPos = this.position();
        Vec3 otherPos = other.position();

        // Transform vectors to a common coordinate system
        // Use the default gravity direction as our reference frame
        Vec3 thisLocalPos = RotationUtil.vecWorldToPlayerVec(thisPos, gravityDirection);
        Vec3 otherLocalPos = RotationUtil.vecWorldToPlayerVec(otherPos, otherGravityDirection);

        // Convert both positions to world space with default gravity orientation
        thisLocalPos = RotationUtil.vecPlayerToWorldVec(thisLocalPos, defaultGravity);
        otherLocalPos = RotationUtil.vecPlayerToWorldVec(otherLocalPos, defaultGravity);

        // Calculate the direction vector from other to this
        Vec3 pushDir = thisLocalPos.subtract(otherLocalPos).normalize();

        // Calculate push strength based on distance
        double distance = thisLocalPos.distanceTo(otherLocalPos);
        double strength = Math.max(0, 1.0 - distance / 2.0) * 0.1;

        // Apply push in the appropriate coordinate systems
        Vec3 thisPush = pushDir.scale(strength);
        Vec3 otherPush = pushDir.scale(-strength);

        // Transform back to respective entity coordinate systems
        Vec3 thisWorldPush = RotationUtil.vecWorldToPlayerVec(thisPush, defaultGravity);
        thisWorldPush = RotationUtil.vecPlayerToWorldVec(thisWorldPush, gravityDirection);

        Vec3 otherWorldPush = RotationUtil.vecWorldToPlayerVec(otherPush, defaultGravity);
        otherWorldPush = RotationUtil.vecPlayerToWorldVec(otherWorldPush, otherGravityDirection);

        // Apply the pushes
        this.setDeltaMovement(this.getDeltaMovement().add(thisWorldPush));
        other.setDeltaMovement(other.getDeltaMovement().add(otherWorldPush));

        // Mark that both entities have had an impulse
        this.hasImpulse = true;
        other.hasImpulse = true;
    }

}
