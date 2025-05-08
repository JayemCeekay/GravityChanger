package gravity_changer.mixin;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    public abstract void readAdditionalSaveData(CompoundTag nbt);

    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);

    @Shadow
    public abstract float getViewYRot(float tickDelta);


    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getY()D",
            ordinal = 0
        )
    )
    private double redirect_travel_getY_0(LivingEntity livingEntity) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(livingEntity);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(livingEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return livingEntity.getY();
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(livingEntity)) {
            return RotationUtil.vecWorldToPlayer(livingEntity.position(), gravityDirection).y;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(livingEntity.position(), gravityDirectionVec).y;
        }
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getY()D",
            ordinal = 1
        )
    )
    private double redirect_travel_getY_1(LivingEntity livingEntity) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(livingEntity);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(livingEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return livingEntity.getY();
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(livingEntity)) {
            return RotationUtil.vecWorldToPlayer(livingEntity.position(), gravityDirection).y;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(livingEntity.position(), gravityDirectionVec).y;
        }
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getY()D",
            ordinal = 2
        )
    )
    private double redirect_travel_getY_2(LivingEntity livingEntity) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(livingEntity);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(livingEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return livingEntity.getY();
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(livingEntity)) {
            return RotationUtil.vecWorldToPlayer(livingEntity.position(), gravityDirection).y;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(livingEntity.position(), gravityDirectionVec).y;
        }
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getY()D",
            ordinal = 3
        )
    )
    private double redirect_travel_getY_3(LivingEntity livingEntity) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(livingEntity);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(livingEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return livingEntity.getY();
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(livingEntity)) {
            return RotationUtil.vecWorldToPlayer(livingEntity.position(), gravityDirection).y;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(livingEntity.position(), gravityDirectionVec).y;
        }
    }

    @ModifyVariable(
        method = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;",
            ordinal = 0
        ),
        ordinal = 2
    )
    private Vec3 modify_travel_Vec3d_2(Vec3 vec3d) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return vec3d;
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(vec3d, gravityDirectionVec);
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return blockPos;
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            return BlockPos.containing(this.position().add(RotationUtil.vecPlayerToWorld(0, -0.20000000298023224D, 0, gravityDirection)));
        } else {
            // For arbitrary directions, use the Vec3-based method
            return BlockPos.containing(this.position().add(RotationUtil.vecPlayerToWorldVec(new Vec3(0, -0.20000000298023224D, 0), gravityDirectionVec)));
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(entity);
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        AABB box = cir.getReturnValue();
        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            box = box.move(0.0D, -1.0E-6D, 0.0D);
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            cir.setReturnValue(RotationUtil.boxPlayerToWorld(box, gravityDirection));
        } else {
            // For arbitrary directions, use the Vec3-based method
            cir.setReturnValue(RotationUtil.boxPlayerToWorldVec(box, gravityDirectionVec));
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(livingEntity);
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

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(livingEntity)) {
            return RotationUtil.vecWorldToPlayer(deltaMovement, gravityDirection).x + livingEntity.xo;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(deltaMovement, gravityDirectionVec).x + livingEntity.xo;
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(livingEntity);
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

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(livingEntity)) {
            return RotationUtil.vecWorldToPlayer(deltaMovement, gravityDirection).z + livingEntity.zo;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(deltaMovement, gravityDirectionVec).z + livingEntity.zo;
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if this entity is using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            // Get attacker's gravity direction
            Direction attackerGravityDirection = GravityChangerAPI.getGravityDirection(attacker);
            Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

            // Check if attacker is using the default gravity direction
            boolean isAttackerDefaultGravity = attackerGravityDirectionVec.y < -0.99 && attackerGravityDirectionVec.x == 0 && attackerGravityDirectionVec.z == 0;
            if (isAttackerDefaultGravity) {
                return attacker.getX();
            } else {
                return attacker.getEyePosition().x;
            }
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            return RotationUtil.vecWorldToPlayer(attacker.getEyePosition(), gravityDirection).x;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(attacker.getEyePosition(), gravityDirectionVec).x;
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if this entity is using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            // Get attacker's gravity direction
            Direction attackerGravityDirection = GravityChangerAPI.getGravityDirection(attacker);
            Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

            // Check if attacker is using the default gravity direction
            boolean isAttackerDefaultGravity = attackerGravityDirectionVec.y < -0.99 && attackerGravityDirectionVec.x == 0 && attackerGravityDirectionVec.z == 0;
            if (isAttackerDefaultGravity) {
                return attacker.getZ();
            } else {
                return attacker.getEyePosition().z;
            }
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            return RotationUtil.vecWorldToPlayer(attacker.getEyePosition(), gravityDirection).z;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(attacker.getEyePosition(), gravityDirectionVec).z;
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getX();
        }

        return RotationUtil.vecWorldToPlayer(target.position(), gravityDirection).x;
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getZ();
        }

        return RotationUtil.vecWorldToPlayer(target.position(), gravityDirection).z;
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(target);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return target.getX();
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(target)) {
            return RotationUtil.vecWorldToPlayer(target.position(), gravityDirection).x;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(target.position(), gravityDirectionVec).x;
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(target);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return target.getZ();
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(target)) {
            return RotationUtil.vecWorldToPlayer(target.position(), gravityDirection).z;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(target.position(), gravityDirectionVec).z;
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(target);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if target is using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            // Get attacker's gravity direction
            Direction attackerGravityDirection = GravityChangerAPI.getGravityDirection(attacker);
            Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

            // Check if attacker is using the default gravity direction
            boolean isAttackerDefaultGravity = attackerGravityDirectionVec.y < -0.99 && attackerGravityDirectionVec.x == 0 && attackerGravityDirectionVec.z == 0;
            if (isAttackerDefaultGravity) {
                return attacker.getX();
            } else {
                return attacker.getEyePosition().x;
            }
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(target)) {
            return RotationUtil.vecWorldToPlayer(attacker.getEyePosition(), gravityDirection).x;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(attacker.getEyePosition(), gravityDirectionVec).x;
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(target);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);

        // Check if target is using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            // Get attacker's gravity direction
            Direction attackerGravityDirection = GravityChangerAPI.getGravityDirection(attacker);
            Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

            // Check if attacker is using the default gravity direction
            boolean isAttackerDefaultGravity = attackerGravityDirectionVec.y < -0.99 && attackerGravityDirectionVec.x == 0 && attackerGravityDirectionVec.z == 0;
            if (isAttackerDefaultGravity) {
                return attacker.getZ();
            } else {
                return attacker.getEyePosition().z;
            }
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(target)) {
            return RotationUtil.vecWorldToPlayer(attacker.getEyePosition(), gravityDirection).z;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(attacker.getEyePosition(), gravityDirectionVec).z;
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(vec3d, x, y, z);
        }

        Vec3 rotated;
        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            rotated = RotationUtil.vecPlayerToWorld(vec3d, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            rotated = RotationUtil.vecPlayerToWorldVec(vec3d, gravityDirectionVec);
        }

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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return vec3d;
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            return RotationUtil.vecPlayerToWorld(vec3d, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecPlayerToWorldVec(vec3d, gravityDirectionVec);
        }
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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        Vec3 particlePos = new Vec3(args.get(1), args.get(2), args.get(3));
        Vec3 vec3d;

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            vec3d = this.position().subtract(RotationUtil.vecPlayerToWorld(this.position().subtract(particlePos), gravityDirection));
        } else {
            // For arbitrary directions, use the Vec3-based method
            vec3d = this.position().subtract(RotationUtil.vecPlayerToWorldVec(this.position().subtract(particlePos), gravityDirectionVec));
        }

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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        Vec3 particlePos = new Vec3(args.get(1), args.get(2), args.get(3));
        Vec3 vec3d;

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            vec3d = this.position().subtract(RotationUtil.vecPlayerToWorld(this.position().subtract(particlePos), gravityDirection));
        } else {
            // For arbitrary directions, use the Vec3-based method
            vec3d = this.position().subtract(RotationUtil.vecPlayerToWorldVec(this.position().subtract(particlePos), gravityDirectionVec));
        }

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
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return vec3d;
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(vec3d, gravityDirectionVec);
        }
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

    @ModifyConstant(method = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V", constant = @Constant(doubleValue = 0.08))
    private double multiplyGravity(double constant) {
        return constant * GravityChangerAPI.getGravityStrength(this);
    }

    @ModifyVariable(method = "Lnet/minecraft/world/entity/LivingEntity;calculateFallDamage(FF)I", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float diminishFallDamage(float value) {
        return value * (float) Math.sqrt(GravityChangerAPI.getGravityStrength(this));
    }
}
