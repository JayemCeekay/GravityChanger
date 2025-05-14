package gravity_changer.mixin.world.entity.Entity;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

/**
 * Mixin for the Entity class to handle custom movement and collision for non-default gravity.
 * This mixin sets the entity context before processing collisions and clears it afterward,
 * which allows the AABB Collision Mixin to work correctly.
 */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public Level level;

    @Shadow
    private static Vec3 collideWithShapes(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        return null;
    }

    /**
     * Injects at the start of the move method to set the entity context.
     */
    @Inject(method = "move", at = @At("HEAD"))
    private void onMoveStart(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;

        // Set the current entity in the collision context
        CollisionContext.setCurrentEntity(entity);
        // Ensure the custom collision flag is reset at the start of movement
        CollisionContext.setInCustomCollision(false);
    }

    /**
     * Injects at the end of the move method to clear the entity context.
     */
    @Inject(method = "move", at = @At("RETURN"))
    private void onMoveEnd(MoverType type, Vec3 movement, CallbackInfo ci) {
        // Clear the current entity from the collision context
        CollisionContext.clearCurrentEntity();
        // Also clear the custom collision flag to prevent it from affecting future collision checks
        CollisionContext.clearInCustomCollision();
    }

    /**
     * Injects before collision detection to transform the movement vector for off-axis gravity.
     */
    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", shift = At.Shift.BEFORE))
    private void onMoveBeforeCollide(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                // Entity doesn't have a gravity component yet, use default movement
                return;
            }
        } catch (NullPointerException e) {
            // Entity's component container might not be initialized yet
            return;
        }

        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && 
                                  gravityDirectionVec.x == 0 && 
                                  gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        // Note: The actual transformation of the movement vector is already handled
        // in the existing EntityMixin.java, which transforms vectors between world and player space.
        // This injection point is just to ensure the entity context is set before collision detection.
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
            method = "collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
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
            method = "collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
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

        // Set up collision context to track the current entity
        CollisionContext.setCurrentEntity(entity);

        try {
            // Transform movement to player space
            Vec3 playerMovement = RotationUtil.vecWorldToPlayerVec(movement, gravityDirection);
            double playerMovementX = playerMovement.x;
            double playerMovementY = playerMovement.y;
            double playerMovementZ = playerMovement.z;

            // Create a copy of the bounding box for collision testing
            AABB workingBox = new AABB(
                entityBoundingBox.minX, entityBoundingBox.minY, entityBoundingBox.minZ,
                entityBoundingBox.maxX, entityBoundingBox.maxY, entityBoundingBox.maxZ
            );

            // For arbitrary directions, we need to handle collisions in all three axes
            // First handle Y axis (up/down in player space)
            if (playerMovementY != 0.0D) {
                // Get the direction for the Y axis in world space
                Vec3 yAxisDir = RotationUtil.vecPlayerToWorldVec(new Vec3(0, 1, 0), gravityDirection).normalize();

                // Calculate the axis and direction for collision testing
                Direction.Axis yAxis = getClosestAxis(yAxisDir);
                int yStep = getAxisDirection(yAxisDir, yAxis);

                // Apply collision along Y axis
                playerMovementY = Shapes.collide(yAxis, workingBox, collisions, playerMovementY * yStep) * yStep;

                // Move the working box if there's remaining movement
                if (playerMovementY != 0.0D) {
                    Vec3 yOffset = RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, playerMovementY, 0.0D), gravityDirection);
                    workingBox = workingBox.move(yOffset);
                }
            }

            // Determine which horizontal axis to handle first (X or Z)
            boolean isZLargerThanX = Math.abs(playerMovementX) < Math.abs(playerMovementZ);

            // Handle Z axis first if it has larger movement
            if (isZLargerThanX && playerMovementZ != 0.0D) {
                // Get the direction for the Z axis in world space
                Vec3 zAxisDir = RotationUtil.vecPlayerToWorldVec(new Vec3(0, 0, 1), gravityDirection).normalize();

                // Calculate the axis and direction for collision testing
                Direction.Axis zAxis = getClosestAxis(zAxisDir);
                int zStep = getAxisDirection(zAxisDir, zAxis);

                // Apply collision along Z axis
                playerMovementZ = Shapes.collide(zAxis, workingBox, collisions, playerMovementZ * zStep) * zStep;

                // Move the working box if there's remaining movement
                if (playerMovementZ != 0.0D) {
                    Vec3 zOffset = RotationUtil.vecPlayerToWorldVec(new Vec3(0.0D, 0.0D, playerMovementZ), gravityDirection);
                    workingBox = workingBox.move(zOffset);
                }
            }

            // Handle X axis
            if (playerMovementX != 0.0D) {
                // Get the direction for the X axis in world space
                Vec3 xAxisDir = RotationUtil.vecPlayerToWorldVec(new Vec3(1, 0, 0), gravityDirection).normalize();

                // Calculate the axis and direction for collision testing
                Direction.Axis xAxis = getClosestAxis(xAxisDir);
                int xStep = getAxisDirection(xAxisDir, xAxis);

                // Apply collision along X axis
                playerMovementX = Shapes.collide(xAxis, workingBox, collisions, playerMovementX * xStep) * xStep;

                // Move the working box if there's remaining movement and X is handled first
                if (!isZLargerThanX && playerMovementX != 0.0D) {
                    Vec3 xOffset = RotationUtil.vecPlayerToWorldVec(new Vec3(playerMovementX, 0.0D, 0.0D), gravityDirection);
                    workingBox = workingBox.move(xOffset);
                }
            }

            // Handle Z axis last if X had larger movement
            if (!isZLargerThanX && playerMovementZ != 0.0D) {
                // Get the direction for the Z axis in world space
                Vec3 zAxisDir = RotationUtil.vecPlayerToWorldVec(new Vec3(0, 0, 1), gravityDirection).normalize();

                // Calculate the axis and direction for collision testing
                Direction.Axis zAxis = getClosestAxis(zAxisDir);
                int zStep = getAxisDirection(zAxisDir, zAxis);

                // Apply collision along Z axis
                playerMovementZ = Shapes.collide(zAxis, workingBox, collisions, playerMovementZ * zStep) * zStep;
            }

            // Transform the final movement back to world space
            return RotationUtil.vecPlayerToWorldVec(new Vec3(playerMovementX, playerMovementY, playerMovementZ), gravityDirection);
        } finally {
            // Always clear the collision context
            System.out.println("EntityCollision onCollide");
            CollisionContext.clearCurrentEntity();
        }

    }

    /**
     * Helper method to get the closest Minecraft axis to a direction vector
     */
    private static Direction.Axis getClosestAxis(Vec3 dir) {
        double absX = Math.abs(dir.x);
        double absY = Math.abs(dir.y);
        double absZ = Math.abs(dir.z);

        if (absX >= absY && absX >= absZ) {
            return Direction.Axis.X;
        } else if (absY >= absX && absY >= absZ) {
            return Direction.Axis.Y;
        } else {
            return Direction.Axis.Z;
        }
    }

    /**
     * Helper method to get the axis direction (positive or negative)
     */
    private static int getAxisDirection(Vec3 dir, Direction.Axis axis) {
        double component = switch (axis) {
            case X -> dir.x;
            case Y -> dir.y;
            case Z -> dir.z;
        };

        return component >= 0 ? 1 : -1;
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


}
