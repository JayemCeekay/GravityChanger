package gravity_changer.mixin.entity;

import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import gravity_changer.GravityChangerMod;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.RotationUtil;
import gravity_changer.util.VectorDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    private Vec3 position;

    @Shadow
    private EntityDimensions dimensions;

    @Shadow
    private float eyeHeight;

    @Shadow
    public double xo;

    @Shadow
    public double yo;

    @Shadow
    public double zo;

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract Vec3 getEyePosition();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public Level level;

    @Shadow
    public abstract int getBlockX();

    @Shadow
    public abstract int getBlockZ();

    @Shadow
    public boolean noPhysics;

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract boolean isVehicle();

    @Shadow
    public abstract AABB getBoundingBox();

    // Note: The getBoundingBox() injection has been moved to EntityBoundingBoxMixin.java

    @Shadow
    public static Vec3 collideWithShapes(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        return null;
    }

    @Shadow
    public abstract Vec3 position();


    @Shadow
    public abstract boolean isPassengerOfSameVehicle(Entity entity);

    @Shadow
    public abstract void push(double deltaX, double deltaY, double deltaZ);

    @Shadow
    protected abstract void onBelowWorld();

    @Shadow
    public abstract double getEyeY();

    @Shadow
    public abstract float getViewYRot(float tickDelta);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    @Shadow
    @Final
    protected RandomSource random;

    @Shadow
    public float fallDistance;

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;makeBoundingBox()Lnet/minecraft/world/phys/AABB;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void inject_calculateBoundingBox(CallbackInfoReturnable<AABB> cir) {
        Entity entity = ((Entity) (Object) this);
        if (entity instanceof Projectile) return;

        // cardinal components initializes the component container in the end of constructor
        // but bounding box calculation can happen inside constructor
        // see dev.onyxstudios.cca.mixin.entity.common.MixinEntity
        if (((ComponentProvider) entity).getComponentContainer() == null) {
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        AABB box = cir.getReturnValue().move(this.position.reverse());

        // Use VectorDirection instead of AxisDirection
        Vec3 upVector = new Vec3(0, 1, 0); // Reference vector
        VectorDirection gravityVectorDirection = VectorDirection.fromVector(gravityDirection, upVector);

        // Check if gravity is pointing upward (positive Y)
        if (gravityVectorDirection == VectorDirection.POSITIVE) {
            box = box.move(0.0D, -1.0E-6D, 0.0D);
        }

        // Use OrientedBoundingBoxTransformer to transform the box with dynamic shrink factors and offsets
        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDirection);

        // Move the OBB back to the entity's position before returning it
        // Return the OBB directly instead of getting its bounding AABB
        cir.setReturnValue(obb.move(this.position));
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;getBoundingBoxForPose(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/phys/AABB;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void inject_calculateBoundsForPose(Pose pos, CallbackInfoReturnable<AABB> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        AABB box = cir.getReturnValue().move(this.position.reverse());
        box = box.inflate(-0.01); // avoid entering crouching because of floating point inaccuracy

        // Use OrientedBoundingBoxTransformer to transform the box with entity-specific shrink factor
        Entity entity = (Entity) (Object) this;
        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDirection);

        cir.setReturnValue(obb.move(this.position));
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void inject_getRotationVector(CallbackInfoReturnable<Vec3> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        cir.setReturnValue(RotationUtil.vecPlayerToWorldVec(cir.getReturnValue(), gravityDirection));
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;getBlockPosBelowThatAffectsMyMovement()Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_getVelocityAffectingPos(CallbackInfoReturnable<BlockPos> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        // Scale normalized gravity direction by 0.5000001D for offset
        cir.setReturnValue(BlockPos.containing(this.position.add(gravityDirection.normalize().scale(0.5000001D))));
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_getEyePos(CallbackInfoReturnable<Vec3> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        cir.setReturnValue(RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, this.eyeHeight, 0.0D), gravityDirection).add(this.position));
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_getCameraPosVec(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        Vec3 vec3d = RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, this.eyeHeight, 0.0D), gravityDirection);

        double d = Mth.lerp((double) tickDelta, this.xo, this.getX()) + vec3d.x;
        double e = Mth.lerp((double) tickDelta, this.yo, this.getY()) + vec3d.y;
        double f = Mth.lerp((double) tickDelta, this.zo, this.getZ()) + vec3d.z;
        cir.setReturnValue(new Vec3(d, e, f));
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;getLightLevelDependentMagicValue()F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_getBrightnessAtFEyes(CallbackInfoReturnable<Float> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        cir.setReturnValue(this.level.hasChunkAt(this.getBlockX(), this.getBlockZ()) ? this.level.getLightLevelDependentMagicValue(BlockPos.containing(this.getEyePosition())) : 0.0F);
    }

    // transform move vector from local to world (the velocity is local)
    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private Vec3 modify_move_Vec3d_0_0(Vec3 vec3d) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return vec3d;
        }

        return RotationUtil.vecPlayerToWorldVec(vec3d, gravityDirection);
    }

    // looks like not useful
//    @ModifyArg(
//        method = "move",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/phys/Vec3;multiply(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
//            ordinal = 0
//        ),
//        index = 0
//    )
//    private Vec3 modify_move_multiply_0(Vec3 vec3d) {
//        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
//        if (gravityDirection == Direction.DOWN) {
//            return vec3d;
//        }
//
//        return RotationUtil.maskPlayerToWorld(vec3d, gravityDirection);
//    }

    // transform the argument vector back to local coordinate
    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                    ordinal = 0
            ),
            ordinal = 0,
            argsOnly = true
    )
    private Vec3 modify_move_Vec3d_0_1(Vec3 vec3d) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return vec3d;
        }

        return RotationUtil.vecWorldToPlayerVec(vec3d, gravityDirection);
    }

    // transform the local variable (result from collide()) to local coordinate
    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                    ordinal = 0
            ),
            ordinal = 1
    )
    private Vec3 modify_move_Vec3d_1(Vec3 vec3d) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return vec3d;
        }

        return RotationUtil.vecWorldToPlayerVec(vec3d, gravityDirection);
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;getOnPosLegacy()Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_getLandingPos(CallbackInfoReturnable<BlockPos> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;
        BlockPos blockPos = BlockPos.containing(RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, -0.20000000298023224D, 0.0D), gravityDirection).add(this.position));
        cir.setReturnValue(blockPos);
    }

    // transform the argument to local coordinate
    @ModifyVariable(
            method = "collide",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    ordinal = 0
            ),
            ordinal = 0
    )
    private Vec3 modify_adjustMovementForCollisions_Vec3d_0(Vec3 vec3d) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return vec3d;
        }

        return RotationUtil.vecWorldToPlayerVec(vec3d, gravityDirection);
    }

    // transform the result to world coordinate
    // the input to Entity.collideBoundingBox will be in local coord
    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void inject_adjustMovementForCollisions(CallbackInfoReturnable<Vec3> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        cir.setReturnValue(RotationUtil.vecPlayerToWorldVec(cir.getReturnValue(), gravityDirection));
    }

    // the argument was transformed to local coord,
    // but bounding box stretch needs world coord
    @ModifyArgs(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"
            )
    )
    private void redirect_adjustMovementForCollisions_stretch_0(Args args) {
        Vec3 rotate = new Vec3(args.get(0), args.get(1), args.get(2));
        rotate = RotationUtil.vecPlayerToWorldVec(rotate, GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this));
        args.set(0, rotate.x);
        args.set(1, rotate.y);
        args.set(2, rotate.z);
    }

    // the argument was transformed to local coord,
    // but bounding box move needs world coord
    @ModifyArgs(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;move(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"
            )
    )
    private void redirect_adjustMovementForCollisions_offset_0(Args args) {
        Vec3 rotate = args.get(0);
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return;
        }

        rotate = RotationUtil.vecPlayerToWorldVec(rotate, gravityDirection);
        args.set(0, rotate);
    }

    // Entity.collideBoundingBox is inputed with local coord, transform it to world coord
    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static Vec3 modify_adjustMovementForCollisions_Vec3d_0(Vec3 vec3d, Entity entity) {
        if (entity == null) {
            return vec3d;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return vec3d;
        }

        return RotationUtil.vecPlayerToWorldVec(vec3d, gravityDirection);
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            )
    )
    private static Vec3 redirect_adjustMovementForCollisions_adjustMovementForCollisions_0(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions, Entity entity) {
        if (entity == null) {
            return collideWithShapes(movement, entityBoundingBox, collisions);
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return collideWithShapes(movement, entityBoundingBox, collisions);
        }

        // For better compatibility, get the cardinal directions for axis-aligned collision
        Direction gravityDirCardinal = Direction.getNearest(gravityDirection.x, gravityDirection.y, gravityDirection.z);

        Vec3 playerMovement = RotationUtil.vecWorldToPlayerVec(movement, gravityDirection);
        double playerMovementX = playerMovement.x;
        double playerMovementY = playerMovement.y;
        double playerMovementZ = playerMovement.z;

        // Get cardinal directions for collision axes
        // For arbitrary directions, we still need to work with cardinal axes for Shapes.collide
        Direction directionX = RotationUtil.dirPlayerToWorld(Direction.EAST, gravityDirCardinal);
        Direction directionY = RotationUtil.dirPlayerToWorld(Direction.UP, gravityDirCardinal);
        Direction directionZ = RotationUtil.dirPlayerToWorld(Direction.SOUTH, gravityDirCardinal);

        if (playerMovementY != 0.0D) {
            playerMovementY = Shapes.collide(directionY.getAxis(), entityBoundingBox, collisions, playerMovementY * directionY.getAxisDirection().getStep()) * directionY.getAxisDirection().getStep();
            if (playerMovementY != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, playerMovementY, 0.0D), gravityDirection));
            }
        }

        boolean isZLargerThanX = Math.abs(playerMovementX) < Math.abs(playerMovementZ);
        if (isZLargerThanX && playerMovementZ != 0.0D) {
            playerMovementZ = Shapes.collide(directionZ.getAxis(), entityBoundingBox, collisions, playerMovementZ * directionZ.getAxisDirection().getStep()) * directionZ.getAxisDirection().getStep();
            if (playerMovementZ != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, 0.0D, playerMovementZ), gravityDirection));
            }
        }

        if (playerMovementX != 0.0D) {
            playerMovementX = Shapes.collide(directionX.getAxis(), entityBoundingBox, collisions, playerMovementX * directionX.getAxisDirection().getStep()) * directionX.getAxisDirection().getStep();
            if (!isZLargerThanX && playerMovementX != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(RotationUtil.vecPlayerToWorldVec(new Vec3(playerMovementX, 0.0D, 0.0D), gravityDirection));
            }
        }

        if (!isZLargerThanX && playerMovementZ != 0.0D) {
            playerMovementZ = Shapes.collide(directionZ.getAxis(), entityBoundingBox, collisions, playerMovementZ * directionZ.getAxisDirection().getStep()) * directionZ.getAxisDirection().getStep();
        }

        return RotationUtil.vecPlayerToWorldVec(new Vec3(playerMovementX, playerMovementY, playerMovementZ), gravityDirection);
    }

    @ModifyArgs(
            method = "isInWall",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;",
                    ordinal = 0
            )
    )
    private void modify_isInsideWall_of_0(Args args) {
        Vec3 rotate = new Vec3(args.get(1), args.get(2), args.get(3));
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return;
        }

        rotate = RotationUtil.vecPlayerToWorldVec(rotate, gravityDirection);
        args.set(1, rotate.x);
        args.set(2, rotate.y);
        args.set(3, rotate.z);
    }

    @ModifyArg(
            method = "getDirection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/Direction;fromYRot(D)Lnet/minecraft/core/Direction;"
            )
    )
    private double redirect_getHorizontalFacing_getYaw_0(double rotation) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return rotation;
        }

        // Use the Vec3-based rotation method
        Vec2 worldRot = RotationUtil.rotPlayerToWorldVec((float) rotation, this.getXRot(), gravityDirection);
        return worldRot.x;
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;spawnSprintParticle()V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_spawnSprintingParticles(CallbackInfo ci) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        ci.cancel();

        Vec3 floorOffset = new Vec3(0.0D, 0.20000000298023224D, 0.0D);
        Vec3 floorPos = this.position().subtract(RotationUtil.vecPlayerToWorldVec(floorOffset, gravityDirection));

        BlockPos blockPos = BlockPos.containing(floorPos);
        BlockState blockState = this.level.getBlockState(blockPos);
        if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
            // Create particle position with random offset in width
            Vec3 randomOffset = new Vec3(
                (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width,
                0.1D,
                (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width
            );

            Vec3 particlePos = this.position().add(RotationUtil.vecPlayerToWorldVec(randomOffset, gravityDirection));
            Vec3 playerVelocity = this.getDeltaMovement();

            // Create particle velocity
            Vec3 particleVelocityLocal = new Vec3(playerVelocity.x * -4.0D, 1.5D, playerVelocity.z * -4.0D);
            Vec3 particleVelocity = RotationUtil.vecPlayerToWorldVec(particleVelocityLocal, gravityDirection);

            this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState), 
                particlePos.x, particlePos.y, particlePos.z, 
                particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }
    }

    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/Entity;updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            ),
            ordinal = 1
    )
    private Vec3 modify_updateMovementInFluid_Vec3d_0(Vec3 vec3d) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return vec3d;
        }

        return RotationUtil.vecPlayerToWorldVec(vec3d, gravityDirection);
    }

    @ModifyArg(
            method = "updateFluidHeightAndDoFluidPushing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1
            ),
            index = 0
    )
    private Vec3 modify_updateMovementInFluid_add_0(Vec3 vec3d) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return vec3d;
        }

        return RotationUtil.vecWorldToPlayerVec(vec3d, gravityDirection);
    }


    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_pushAwayFrom(Entity entity, CallbackInfo ci) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);
        Vec3 otherGravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if both entities have default gravity
        boolean bothDefaultGravity = gravityDirection.equals(new Vec3(0, -1, 0)) && 
                                     otherGravityDirection.equals(new Vec3(0, -1, 0));

        if (bothDefaultGravity) return;

        ci.cancel();

        if (!this.isPassengerOfSameVehicle(entity)) {
            if (!entity.noPhysics && !this.noPhysics) {
                Vec3 entityOffset = entity.getBoundingBox().getCenter().subtract(this.getBoundingBox().getCenter());

                {
                    Vec3 playerEntityOffset = RotationUtil.vecWorldToPlayerVec(entityOffset, gravityDirection);
                    double dx = playerEntityOffset.x;
                    double dz = playerEntityOffset.z;
                    double f = Mth.absMax(dx, dz);
                    if (f >= 0.009999999776482582D) {
                        f = Math.sqrt(f);
                        dx /= f;
                        dz /= f;
                        double g = 1.0D / f;
                        if (g > 1.0D) {
                            g = 1.0D;
                        }

                        dx *= g;
                        dz *= g;
                        dx *= 0.05000000074505806D;
                        dz *= 0.05000000074505806D;
                        if (!this.isVehicle()) {
                            this.push(-dx, 0.0D, -dz);
                        }
                    }
                }

                {
                    Vec3 entityEntityOffset = RotationUtil.vecWorldToPlayerVec(entityOffset, otherGravityDirection);
                    double dx = entityEntityOffset.x;
                    double dz = entityEntityOffset.z;
                    double f = Mth.absMax(dx, dz);
                    if (f >= 0.009999999776482582D) {
                        f = Math.sqrt(f);
                        dx /= f;
                        dz /= f;
                        double g = 1.0D / f;
                        if (g > 1.0D) {
                            g = 1.0D;
                        }

                        dx *= g;
                        dz *= g;
                        dx *= 0.05000000074505806D;
                        dz *= 0.05000000074505806D;
                        if (!entity.isVehicle()) {
                            entity.push(dx, 0.0D, dz);
                        }
                    }
                }
            }
        }
    }

    @Inject(
            method = "Lnet/minecraft/world/entity/Entity;checkBelowWorld()V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_attemptTickInVoid(CallbackInfo ci) {
        Entity this_ = (Entity) (Object) this;

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(this_);

        // Check for upward gravity (void damage above world)
        boolean isUpwardGravity = gravityDirection.y > 0.9 && Math.abs(gravityDirection.x) < 0.1 && Math.abs(gravityDirection.z) < 0.1;

        if (GravityChangerMod.config.voidDamageAboveWorld &&
                this.getY() > (double) (this.level.getMaxBuildHeight() + 256) &&
                isUpwardGravity
        ) {
            this.onBelowWorld();
            ci.cancel();
            return;
        }

        // Check for horizontal gravity (void damage on falling too far)
        boolean isHorizontalGravity = Math.abs(gravityDirection.y) < 0.1;

        if (GravityChangerMod.config.voidDamageOnHorizontalFallTooFar &&
                isHorizontalGravity &&
                fallDistance > 1024
        ) {
            this.onBelowWorld();
            ci.cancel();
            return;
        }
    }

    @ModifyArgs(
            method = "isFree(DDD)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;",
                    ordinal = 0
            )
    )
    private void redirect_doesNotCollide_offset_0(Args args) {
        Vec3 rotate = new Vec3(args.get(0), args.get(1), args.get(2));
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec((Entity) (Object) this);

        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return;
        }

        rotate = RotationUtil.vecPlayerToWorldVec(rotate, gravityDirection);
        args.set(0, rotate.x);
        args.set(1, rotate.y);
        args.set(2, rotate.z);
    }


    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/Entity;updateFluidOnEyes()V",
            at = @At(
                    value = "STORE"
            ),
            ordinal = 0
    )
    private double submergedInWaterEyeFix(double d) {
        d = this.getEyePosition().y();
        return d;
    }

    @ModifyVariable(
            method = "Lnet/minecraft/world/entity/Entity;updateFluidOnEyes()V",
            at = @At(
                    value = "STORE"
            ),
            ordinal = 0
    )
    private BlockPos submergedInWaterPosFix(BlockPos blockpos) {
        blockpos = BlockPos.containing(this.getEyePosition());
        return blockpos;
    }

    @Inject(
            method = "calculateUpVector",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_calculateUpVector(float xRot, float yRot, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity)(Object)this;
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        // Return the inverse of gravity direction as the up vector
        cir.setReturnValue(gravityDirection.scale(-1));
    }

    @Inject(
            method = "isOnFire",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_isOnFire(CallbackInfoReturnable<Boolean> cir) {
        // This is important for fire animation orientation
    }

    @Inject(
            method = "getBlockStateOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_getBlockStateOn(CallbackInfoReturnable<BlockState> cir) {
        Entity entity = (Entity)(Object)this;
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        // Get the block state in the direction of gravity
        BlockPos blockPos = getBlockPosInGravityDirection(entity.blockPosition(), gravityDirection);
        cir.setReturnValue(entity.level().getBlockState(blockPos));
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

}
