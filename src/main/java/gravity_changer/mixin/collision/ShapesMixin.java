package gravity_changer.mixin.collision;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.collision.VoxelShapeTransformer;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(Shapes.class)
public abstract class ShapesMixin {

    /**
     * Injects into the 'collide' method to modify collision response for off-axis gravity.
     * This transforms the inputs to gravity-aligned space, performs collision detection,
     * and transforms the result back to world space.
     */
    @Inject(method = "collide(Lnet/minecraft/core/Direction$Axis;Lnet/minecraft/world/phys/AABB;Ljava/lang/Iterable;D)D", at = @At("HEAD"), cancellable = true)
    private static void onCollide(Direction.Axis movementAxis, AABB collisionBox, Iterable<VoxelShape> possibleHits, double desiredOffset, CallbackInfoReturnable<Double> cir) {
        // Only apply custom collision for entities with custom gravity
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        // Check if we're already inside a custom collision calculation
        // This prevents recursion when OrientedBoundingBox.intersects calls AABB.intersects
        if (CollisionContext.isInCustomCollision()) {
            return;
        }

        // Check if the entity has a gravity component before proceeding
        try {
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                // Entity doesn't have a gravity component yet, use default collision
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

        // Set the flag to indicate we're inside a custom collision calculation
        CollisionContext.setInCustomCollision(true);
        try {
            // Transform the entity bounding box to gravity-aligned space
            // Use OrientedBoundingBoxTransformer for consistency with AABBMixin
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(collisionBox, gravityDirectionVec);
            AABB transformedBox = obb.getLocalBox();

            // Transform each VoxelShape in the possibleHits list to gravity-aligned space
            List<VoxelShape> transformedShapes = new java.util.ArrayList<>();
            for (VoxelShape shape : possibleHits) {
                transformedShapes.add(VoxelShapeTransformer.transformVoxelShape(shape, gravityDirectionVec));
            }

            // Transform the axis to gravity-aligned space
            Direction.Axis transformedAxis;
            Direction cardinalGravity = RotationUtil.vec3ToDirection(gravityDirectionVec);

            switch (movementAxis) {
                case X:
                    transformedAxis = RotationUtil.dirWorldToPlayer(Direction.EAST, cardinalGravity).getAxis();
                    break;
                case Y:
                    transformedAxis = RotationUtil.dirWorldToPlayer(Direction.UP, cardinalGravity).getAxis();
                    break;
                case Z:
                    transformedAxis = RotationUtil.dirWorldToPlayer(Direction.SOUTH, cardinalGravity).getAxis();
                    break;
                default:
                    return; // Should never happen
            }

            // Calculate the movement direction in gravity-aligned space
            Vec3 movementVec = Vec3.ZERO;
            switch (movementAxis) {
                case X:
                    movementVec = new Vec3(desiredOffset, 0, 0);
                    break;
                case Y:
                    movementVec = new Vec3(0, desiredOffset, 0);
                    break;
                case Z:
                    movementVec = new Vec3(0, 0, desiredOffset);
                    break;
            }

            Vec3 transformedMovementVec = RotationUtil.vecWorldToPlayerVec(movementVec, gravityDirectionVec);
            double transformedOffset = 0;

            switch (transformedAxis) {
                case X:
                    transformedOffset = transformedMovementVec.x;
                    break;
                case Y:
                    transformedOffset = transformedMovementVec.y;
                    break;
                case Z:
                    transformedOffset = transformedMovementVec.z;
                    break;
            }

            // Perform collision detection in gravity-aligned space
            double collisionResult = Shapes.collide(transformedAxis, transformedBox, transformedShapes, transformedOffset);

            // Transform the result back to world space
            Vec3 resultVec = Vec3.ZERO;
            switch (transformedAxis) {
                case X:
                    resultVec = new Vec3(collisionResult, 0, 0);
                    break;
                case Y:
                    resultVec = new Vec3(0, collisionResult, 0);
                    break;
                case Z:
                    resultVec = new Vec3(0, 0, collisionResult);
                    break;
            }

            Vec3 worldResultVec = RotationUtil.vecPlayerToWorldVec(resultVec, gravityDirectionVec);
            double worldResult = 0;

            switch (movementAxis) {
                case X:
                    worldResult = worldResultVec.x;
                    break;
                case Y:
                    worldResult = worldResultVec.y;
                    break;
                case Z:
                    worldResult = worldResultVec.z;
                    break;
            }

            cir.setReturnValue(worldResult);
        } finally {
            // Clear the flag when we're done
            CollisionContext.setInCustomCollision(false);
        }
    }
}
