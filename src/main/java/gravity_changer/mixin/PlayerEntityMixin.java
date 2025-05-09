package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = Player.class, priority = 1001)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow
    @Final
    private Abilities abilities;

    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);

    @Shadow
    protected abstract boolean isStayingOnGroundSurface();

    @Shadow
    protected abstract boolean isAboveGround();

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @WrapOperation(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 wrapOperation_travel_getRotationVector_0(Player playerEntity, Operation<Vec3> original) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(playerEntity);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(playerEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(playerEntity);
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(playerEntity)) {
            return RotationUtil.vecWorldToPlayer(original.call(playerEntity), gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.vecWorldToPlayerVec(original.call(playerEntity), gravityDirectionVec);
        }
    }


    @ModifyArgs(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;"
        )
    )
    private void modify_move_multiply_0(Args args) {
        Vec3 rotate = new Vec3(0.0D, 1.0D - 0.1D, 0.0D);

        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;

        // For cardinal directions, use the existing code path for backward compatibility
        if (isDefaultGravity || !GravityChangerAPI.isUsingVec3Gravity(this)) {
            rotate = RotationUtil.vecPlayerToWorld(rotate, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            rotate = RotationUtil.vecPlayerToWorldVec(rotate, gravityDirectionVec);
        }

        args.set(0, (double) args.get(0) - rotate.x);
        args.set(1, (double) args.get(1) - rotate.y + (1.0D - 0.1D));
        args.set(2, (double) args.get(2) - rotate.z);
    }
    //@Redirect(
    //        method = "travel",
    //        at = @At(
    //                value = "NEW",
    //                target = "Lnet/minecraft/util/math/BlockPos;<init>(DDD)V",
    //                ordinal = 0
    //        )
    //)
    //private BlockPos redirect_travel_new_0(double x, double y, double z) {
    //    Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity)(Object)this);
    //    if(gravityDirection == Direction.DOWN) {
    //        return new BlockPos(x, y, z);
    //    }
//
    //    return new BlockPos(this.getPos().add(RotationUtil.vecPlayerToWorld(0.0D, 1.0D - 0.1D, 0.0D, gravityDirection)));
    //}

    @Redirect(
        method = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;",
            ordinal = 0
        )
    )
    private ItemEntity redirect_dropItem_new_0(Level world, double x, double y, double z, ItemStack stack) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return new ItemEntity(world, x, y, z, stack);
        }

        Vec3 vec3d;
        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            vec3d = this.getEyePosition().subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.30000001192092896D, 0.0D, gravityDirection));
        } else {
            // For arbitrary directions, use the Vec3-based method
            vec3d = this.getEyePosition().subtract(RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, 0.30000001192092896D, 0.0D), gravityDirectionVec));
        }

        return new ItemEntity(world, vec3d.x, vec3d.y, vec3d.z, stack);
    }

    @WrapOperation(
        method = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(DDD)V"
        )
    )
    private void wrapOperation_dropItem_setVelocity(ItemEntity itemEntity, double x, double y, double z, Operation<Void> original) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            original.call(itemEntity, x, y, z);
            return;
        }

        Vec3 world;
        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity((Entity) (Object) this)) {
            world = RotationUtil.vecPlayerToWorld(x, y, z, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            world = RotationUtil.vecPlayerToWorldVec(new Vec3(x, y, z), gravityDirectionVec);
        }

        original.call(itemEntity, world.x, world.y, world.z);
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/player/Player;maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject_adjustMovementForSneaking(Vec3 movement, MoverType type, CallbackInfoReturnable<Vec3> cir) {
        Entity this_ = (Entity) (Object) this;

        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(this_);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this_);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        Vec3 playerMovement;
        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(this_)) {
            playerMovement = RotationUtil.vecWorldToPlayer(movement, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            playerMovement = RotationUtil.vecWorldToPlayerVec(movement, gravityDirectionVec);
        }

        if (!this.abilities.flying && (type == MoverType.SELF || type == MoverType.PLAYER) && this.isStayingOnGroundSurface() && this.isAboveGround()) {
            double d = playerMovement.x;
            double e = playerMovement.z;
            double var7 = 0.05D;

            Vec3 moveVec;
            // Check collision with appropriate transformation based on gravity type
            while (d != 0.0D) {
                if (!GravityChangerAPI.isUsingVec3Gravity(this_)) {
                    moveVec = RotationUtil.vecPlayerToWorld(d, (double) (-this.maxUpStep()), 0.0D, gravityDirection);
                } else {
                    moveVec = RotationUtil.vecPlayerToWorldVec(new Vec3(d, (double) (-this.maxUpStep()), 0.0D), gravityDirectionVec);
                }

                if (this_.level().noCollision(this, this.getBoundingBox().move(moveVec))) {
                    if (d < 0.05D && d >= -0.05D) {
                        d = 0.0D;
                    }
                    else if (d > 0.0D) {
                        d -= 0.05D;
                    }
                    else {
                        d += 0.05D;
                    }
                } else {
                    break;
                }
            }

            while (e != 0.0D) {
                if (!GravityChangerAPI.isUsingVec3Gravity(this_)) {
                    moveVec = RotationUtil.vecPlayerToWorld(0.0D, (double) (-this.maxUpStep()), e, gravityDirection);
                } else {
                    moveVec = RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, (double) (-this.maxUpStep()), e), gravityDirectionVec);
                }

                if (this_.level().noCollision(this, this.getBoundingBox().move(moveVec))) {
                    if (e < 0.05D && e >= -0.05D) {
                        e = 0.0D;
                    }
                    else if (e > 0.0D) {
                        e -= 0.05D;
                    }
                    else {
                        e += 0.05D;
                    }
                } else {
                    break;
                }
            }

            while (d != 0.0D && e != 0.0D) {
                if (!GravityChangerAPI.isUsingVec3Gravity(this_)) {
                    moveVec = RotationUtil.vecPlayerToWorld(d, (double) (-this.maxUpStep()), e, gravityDirection);
                } else {
                    moveVec = RotationUtil.vecPlayerToWorldVec(new Vec3(d, (double) (-this.maxUpStep()), e), gravityDirectionVec);
                }

                if (this_.level().noCollision(this, this.getBoundingBox().move(moveVec))) {
                    if (d < 0.05D && d >= -0.05D) {
                        d = 0.0D;
                    }
                    else if (d > 0.0D) {
                        d -= 0.05D;
                    }
                    else {
                        d += 0.05D;
                    }

                    if (e < 0.05D && e >= -0.05D) {
                        e = 0.0D;
                    }
                    else if (e > 0.0D) {
                        e -= 0.05D;
                    }
                    else {
                        e += 0.05D;
                    }
                } else {
                    break;
                }
            }

            // Transform back to world coordinates based on gravity type
            if (!GravityChangerAPI.isUsingVec3Gravity(this)) {
                cir.setReturnValue(RotationUtil.vecPlayerToWorld(d, playerMovement.y, e, gravityDirection));
            } else {
                cir.setReturnValue(RotationUtil.vecPlayerToWorldVec(new Vec3(d, playerMovement.y, e), gravityDirectionVec));
            }
        }
        else {
            cir.setReturnValue(movement);
        }
    }

    @WrapOperation(
        method = "isAboveGround",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;"
        )
    )
    private AABB wrapOperation_method_30263_offset_0(AABB box, double x, double y, double z, Operation<AABB> original) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(box, x, y, z);
        }

        Vec3 world;

        // For arbitrary directions, use the Vec3-based method
        world = RotationUtil.vecPlayerToWorldVec(new Vec3(x, y, z), gravityDirectionVec);


        return original.call(box, world.x, world.y, world.z);
    }

    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getYRot()F",
            ordinal = 0
        )
    )
    private float wrapOperation_attack_getYaw_0(Player attacker, Operation<Float> original, Entity target) {
        // Get both Direction and Vec3 gravity directions for target and attacker
        Direction targetGravityDirection = GravityChangerAPI.getGravityDirection(target);
        Direction attackerGravityDirection = GravityChangerAPI.getGravityDirection(attacker);
        Vec3 targetGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);
        Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

        // Check if both entities have the same gravity direction
        boolean sameGravity = targetGravityDirection == attackerGravityDirection;
        if (!sameGravity) {
            // For more precise comparison with Vec3-based gravity
            sameGravity = targetGravityDirectionVec.distanceTo(attackerGravityDirectionVec) < 0.01;
        }

        if (sameGravity) {
            return original.call(attacker);
        }

        // Check if we're using cardinal directions for both entities
        boolean useCardinal = targetGravityDirection.getAxis() != null && attackerGravityDirection.getAxis() != null;

        if (!GravityChangerAPI.isUsingVec3Gravity(attacker) && !GravityChangerAPI.isUsingVec3Gravity(target)) {
            // Use Direction-based methods for backward compatibility
            return RotationUtil.rotWorldToPlayer(
                RotationUtil.rotPlayerToWorld(original.call(attacker), attacker.getXRot(), attackerGravityDirection), 
                targetGravityDirection
            ).x;
        } else {
            // Use Vec3-based methods for arbitrary gravity directions
            return RotationUtil.rotWorldToPlayerVec(
                RotationUtil.rotPlayerToWorldVec(original.call(attacker), attacker.getXRot(), attackerGravityDirectionVec), 
                targetGravityDirectionVec
            ).x;
        }
    }

    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getYRot()F",
            ordinal = 1
        )
    )
    private float wrapOperation_attack_getYaw_1(Player attacker, Operation<Float> original, Entity target) {
        // Get both Direction and Vec3 gravity directions for target and attacker
        Direction targetGravityDirection = GravityChangerAPI.getGravityDirection(target);
        Direction attackerGravityDirection = GravityChangerAPI.getGravityDirection(attacker);
        Vec3 targetGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);
        Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

        // Check if both entities have the same gravity direction
        boolean sameGravity = targetGravityDirection == attackerGravityDirection;
        if (!sameGravity) {
            // For more precise comparison with Vec3-based gravity
            sameGravity = targetGravityDirectionVec.distanceTo(attackerGravityDirectionVec) < 0.01;
        }

        if (sameGravity) {
            return original.call(attacker);
        }

        // Check if we're using cardinal directions for both entities
        boolean useCardinal = targetGravityDirection.getAxis() != null && attackerGravityDirection.getAxis() != null;

        if (!GravityChangerAPI.isUsingVec3Gravity(attacker) && !GravityChangerAPI.isUsingVec3Gravity(target)) {
            // Use Direction-based methods for backward compatibility
            return RotationUtil.rotWorldToPlayer(
                RotationUtil.rotPlayerToWorld(original.call(attacker), attacker.getXRot(), attackerGravityDirection), 
                targetGravityDirection
            ).x;
        } else {
            // Use Vec3-based methods for arbitrary gravity directions
            return RotationUtil.rotWorldToPlayerVec(
                RotationUtil.rotPlayerToWorldVec(original.call(attacker), attacker.getXRot(), attackerGravityDirectionVec), 
                targetGravityDirectionVec
            ).x;
        }
    }

    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getYRot()F",
            ordinal = 2
        )
    )
    private float wrapOperation_attack_getYaw_2(Player attacker, Operation<Float> original) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(attacker);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(attacker);
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(attacker)) {
            return RotationUtil.rotPlayerToWorld(original.call(attacker), attacker.getXRot(), gravityDirection).x;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.rotPlayerToWorldVec(original.call(attacker), attacker.getXRot(), gravityDirectionVec).x;
        }
    }

    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getYRot()F",
            ordinal = 3
        )
    )
    private float wrapOperation_attack_getYaw_3(Player attacker, Operation<Float> original) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(attacker);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(attacker);
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(attacker)) {
            return RotationUtil.rotPlayerToWorld(original.call(attacker), attacker.getXRot(), gravityDirection).x;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.rotPlayerToWorldVec(original.call(attacker), attacker.getXRot(), gravityDirectionVec).x;
        }
    }

    @ModifyArgs(
        method = "addParticlesAroundSelf",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
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

    @ModifyArgs(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"
        )
    )
    private void modify_tickMovement_expand_0(Args args) {
        // Get Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        Vec3 vec3d;

            // For arbitrary directions, we need to use a different approach
            // Since maskPlayerToWorld is for cardinal directions only, we'll use vecPlayerToWorldVec
            vec3d = RotationUtil.vecPlayerToWorldVec(new Vec3(args.get(0), args.get(1), args.get(2)), gravityDirectionVec);


        args.set(0, vec3d.x);
        args.set(1, vec3d.y);
        args.set(2, vec3d.z);
    }
}
