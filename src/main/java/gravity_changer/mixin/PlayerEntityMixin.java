package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
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

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    @Shadow public double xCloak;

    @Shadow public double yCloakO;

    @Shadow public double zCloakO;

    @Shadow public double yCloak;

    @Shadow public double zCloak;

    @Shadow public abstract @Nullable ItemEntity drop(ItemStack droppedItem, boolean dropAround, boolean includeThrowerName);

    @Shadow public double xCloakO;

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
        // Get Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(playerEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(playerEntity);
        }


        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(original.call(playerEntity), gravityDirectionVec);

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

        // Get Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;

        // For arbitrary directions, use the Vec3-based method
        rotate = RotationUtil.vecPlayerToWorldVec(rotate, gravityDirectionVec);

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
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return new ItemEntity(world, x, y, z, stack);
        }

        Vec3 vec3d;

            // For arbitrary directions, use the Vec3-based method
            vec3d = this.getEyePosition().subtract(RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, 0.30000001192092896D, 0.0D), gravityDirectionVec));


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
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            original.call(itemEntity, x, y, z);
            return;
        }

        Vec3 world;

            // For arbitrary directions, use the Vec3-based method
            world = RotationUtil.vecPlayerToWorldVec(new Vec3(x, y, z), gravityDirectionVec);


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
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this_);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        Vec3 playerMovement;

            // For arbitrary directions, use the Vec3-based method
            playerMovement = RotationUtil.vecWorldToPlayerVec(movement, gravityDirectionVec);


        if (!this.abilities.flying && (type == MoverType.SELF || type == MoverType.PLAYER) && this.isStayingOnGroundSurface() && this.isAboveGround()) {
            double d = playerMovement.x;
            double e = playerMovement.z;
            double var7 = 0.05D;

            Vec3 moveVec;
            // Check collision with appropriate transformation based on gravity type
            while (d != 0.0D) {

                    moveVec = RotationUtil.vecPlayerToWorldVec(new Vec3(d, (double) (-this.maxUpStep()), 0.0D), gravityDirectionVec);


                if (this_.level().noCollision(this, this.getBoundingBox().move(moveVec))) {
                    if (d < 0.05D && d >= -0.05D) {
                        d = 0.0D;
                    } else if (d > 0.0D) {
                        d -= 0.05D;
                    } else {
                        d += 0.05D;
                    }
                } else {
                    break;
                }
            }

            while (e != 0.0D) {

                    moveVec = RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, (double) (-this.maxUpStep()), e), gravityDirectionVec);


                if (this_.level().noCollision(this, this.getBoundingBox().move(moveVec))) {
                    if (e < 0.05D && e >= -0.05D) {
                        e = 0.0D;
                    } else if (e > 0.0D) {
                        e -= 0.05D;
                    } else {
                        e += 0.05D;
                    }
                } else {
                    break;
                }
            }

            while (d != 0.0D && e != 0.0D) {

                    moveVec = RotationUtil.vecPlayerToWorldVec(new Vec3(d, (double) (-this.maxUpStep()), e), gravityDirectionVec);


                if (this_.level().noCollision(this, this.getBoundingBox().move(moveVec))) {
                    if (d < 0.05D && d >= -0.05D) {
                        d = 0.0D;
                    } else if (d > 0.0D) {
                        d -= 0.05D;
                    } else {
                        d += 0.05D;
                    }

                    if (e < 0.05D && e >= -0.05D) {
                        e = 0.0D;
                    } else if (e > 0.0D) {
                        e -= 0.05D;
                    } else {
                        e += 0.05D;
                    }
                } else {
                    break;
                }
            }

            // Transform back to world coordinates based on gravity type
                cir.setReturnValue(RotationUtil.vecPlayerToWorldVec(new Vec3(d, playerMovement.y, e), gravityDirectionVec));

        } else {
            cir.setReturnValue(movement);
        }
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void inject_fixFlyingWhileSneaking(Vec3 travelVector, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (!this.level().isClientSide) { // Server-side check only
            if (this.abilities.flying && !this.abilities.mayfly) {
                if (shouldDisableFlying(player)) {
                    this.abilities.flying = false;
                }
            }
        }
    }

    /**
     * Custom flying disable logic under arbitrary gravity
     */
    private boolean shouldDisableFlying(Player player) {
        Vec3 gravityVec = GravityChangerAPI.getGravityDirectionVec(player);
        boolean isDefaultGravity = gravityVec.y < -0.99 && gravityVec.x == 0 && gravityVec.z == 0;

        if (isDefaultGravity) {
            // Use vanilla behavior if gravity is normal
            return this.onGround();
        }

        // Custom check: are we actually hitting terrain below us (in gravity direction)?
        Vec3 checkOffset = gravityVec.normalize().scale(-0.1); // Small nudge downward
        AABB testBox = player.getBoundingBox().move(checkOffset);

        return !player.level().noCollision(player, testBox);
    }

    @ModifyVariable(
            method = "travel",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Vec3 modifyTravelInput(Vec3 travelVector) {
        Player player = (Player)(Object)this;

        Vec3 gravityVec = GravityChangerAPI.getGravityDirectionVec(player);

        boolean isDefaultGravity = gravityVec.y < -0.99 && gravityVec.x == 0 && gravityVec.z == 0;
        if (!isDefaultGravity) {
            travelVector = rotateMovementInput(travelVector, gravityVec);
        }

        return travelVector;
    }


    private Vec3 rotateMovementInput(Vec3 input, Vec3 gravityVec) {
        Vec3 gravity = gravityVec.normalize();

        // Pick an arbitrary vector not parallel to gravity
        Vec3 arbitrary = (Math.abs(gravity.x) < 0.5) ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);

        // Strafe axis (left/right)
        Vec3 left = gravity.cross(arbitrary).normalize();

        // Forward axis (forward/back)
        Vec3 forward = left.cross(gravity).normalize();

        // Compose world movement vector
        // input.x = strafe left/right
        // input.y = jump/sneak
        // input.z = forward/backward
        return left.scale(input.x)
                .add(gravity.scale(-input.y)) // Jump is against gravity
                .add(forward.scale(input.z));
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
        Entity entity = (Entity) (Object) this;
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(box, x, y, z);
        }

        // Transform the AABB to an OrientedBoundingBox
        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDirectionVec, entity);

        // Create a movement vector in player space
        Vec3 movement = new Vec3(x, y, z);

        // Transform the movement vector to world space
        Vec3 worldMovement = RotationUtil.vecPlayerToWorldVec(movement, gravityDirectionVec);

        // Apply the movement to the OBB
        // Since OrientedBoundingBox extends AABB, we can use the move method directly
        return obb.move(worldMovement.x, worldMovement.y, worldMovement.z);
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
        // Get Vec3 gravity directions for target and attacker
        Vec3 targetGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);
        Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

        // Check if both entities have the same gravity direction
        boolean sameGravity = targetGravityDirectionVec.distanceTo(attackerGravityDirectionVec) < 0.01;

        if (sameGravity) {
            return original.call(attacker);
        }

        // Use Vec3-based rotation transform methods for arbitrary gravity directions
        return RotationUtil.rotWorldToPlayerVec(
                RotationUtil.rotPlayerToWorldVec(original.call(attacker), attacker.getXRot(), attackerGravityDirectionVec),
                targetGravityDirectionVec
        ).x;
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
        // Get Vec3 gravity directions for target and attacker
        Vec3 targetGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(target);
        Vec3 attackerGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

        // Check if both entities have the same gravity direction
        boolean sameGravity = targetGravityDirectionVec.distanceTo(attackerGravityDirectionVec) < 0.01;

        if (sameGravity) {
            return original.call(attacker);
        }

        // Use Vec3-based methods for arbitrary gravity directions
        return RotationUtil.rotWorldToPlayerVec(
                RotationUtil.rotPlayerToWorldVec(original.call(attacker), attacker.getXRot(), attackerGravityDirectionVec),
                targetGravityDirectionVec
        ).x;
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
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

        // Check if we're using the default gravity direction`
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(attacker);
        }


            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.rotPlayerToWorldVec(original.call(attacker), attacker.getXRot(), gravityDirectionVec).x;

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
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(attacker);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(attacker);
        }


            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.rotPlayerToWorldVec(original.call(attacker), attacker.getXRot(), gravityDirectionVec).x;

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
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"
            )
    )
    private void modify_tickMovement_expand_0(Args args) {
        // Get both Direction and Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        Vec3 vec3d;

        // For arbitrary directions, we need a new approach to maintain WASD controls
        // 1. Create a basis aligned with the gravity direction
        Vec3 gravityNormalized = gravityDirectionVec.normalize();

        // 2. Find perpendicular vectors to form a coordinate system
        // First perpendicular vector - prioritize keeping it in the xz plane if possible
        Vec3 perpVec1;
        if (Math.abs(gravityNormalized.x) < 0.99 && Math.abs(gravityNormalized.z) < 0.99) {
            // Use a vector in the xz plane as a first perpendicular vector
            perpVec1 = new Vec3(-gravityNormalized.z, 0, gravityNormalized.x).normalize();
        } else {
            // If gravity is nearly aligned with x or z axis, use a vector in the xy plane
            perpVec1 = new Vec3(-gravityNormalized.y, gravityNormalized.x, 0).normalize();
        }

        // Second perpendicular vector through cross product
        Vec3 perpVec2 = gravityNormalized.cross(perpVec1).normalize();

        // 3. Transform the input vector using this basis
        // This approach preserves WASD movement relative to the gravity orientation
        double x = args.get(0);
        double y = args.get(1);
        double z = args.get(2);

        // In our new basis:
        // - y (up/down) corresponds to the gravity direction
        // - x (left/right) corresponds to the first perpendicular vector
        // - z (forward/backward) corresponds to the second perpendicular vector
        vec3d = perpVec1.scale(x)
                .add(gravityNormalized.scale(y))
                .add(perpVec2.scale(z));

        args.set(0, vec3d.x);
        args.set(1, vec3d.y);
        args.set(2, vec3d.z);
    }

    /**
     * Properly handles the movement of the player's cloak in altered gravity
     */
    @Inject(method = "moveCloak", at = @At("HEAD"), cancellable = true)
    private void injectMoveCloak(CallbackInfo ci) {
        // Get Vec3 gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 && gravityDirectionVec.x() == 0 && gravityDirectionVec.z() == 0;
        if (isDefaultGravity) return; // Default gravity, let vanilla handle it

        // Cancel vanilla cloak movement
        ci.cancel();

        // Store old cloak positions
        this.xCloakO = this.xCloak;
        this.yCloakO = this.yCloak;
        this.zCloakO = this.zCloak;

        // Calculate cloak movement based on player movement in gravity-aware space
        double xDiff = this.getX() - this.xCloak;
        double yDiff = this.getY() - this.yCloak;
        double zDiff = this.getZ() - this.zCloak;

        // Transform the diff to player space
        Vec3 diffVec = RotationUtil.vecWorldToPlayerVec(new Vec3(xDiff, yDiff, zDiff),
                gravityDirectionVec);

        // Apply gravity-aware physics to the cloak
        // The cloak should always hang downward in the gravity direction
        double maxMove = 10.0;
        if (diffVec.lengthSqr() > maxMove * maxMove) {
            // Cap the movement speed
            diffVec = diffVec.normalize().scale(maxMove);
        }

        // Transform back to world space
        Vec3 newDiffVec = RotationUtil.vecPlayerToWorldVec(diffVec,
                gravityDirectionVec);

        // Update cloak position
        this.xCloak = this.getX() - newDiffVec.x();
        this.yCloak = this.getY() - newDiffVec.y();
        this.zCloak = this.getZ() - newDiffVec.z();
    }

    /**
     * Improves inventory item handling with altered gravity
     */
    @WrapOperation(
            method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"
            )
    )
    private ItemEntity wrapOperation_inventory_drop(
            Player player,
            ItemStack stack,
            boolean includeOffset,
            boolean thisCopy,
            Operation<ItemEntity> original
    ) {
        // Get Vec3 gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 && gravityDirectionVec.x() == 0 && gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return original.call(player, stack, includeOffset, thisCopy);
        }

        // For non-default gravity, use our custom item drop implementation
        // that's already modified to handle altered gravity correctly
        return this.drop(stack, includeOffset, thisCopy);
    }



}
