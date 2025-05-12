package gravity_changer.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gravity_changer.EntityTags;
import gravity_changer.RotationAnimation;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.RotationUtil;
import gravity_changer.util.Rotor;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    @Final
    private static RenderType SHADOW_RENDER_TYPE;

    @Shadow
    private boolean shouldRenderShadow;

    @Shadow private Camera camera;

    @Shadow private Level level;

    @Shadow
    private static void shadowVertex(PoseStack.Pose entry, VertexConsumer vertices, float alpha, float x, float y, float z, float u, float v) {}

    @Shadow public abstract double distanceToSqr(double x, double y, double z);

    /**
     * Inject into the prepare method to potentially rotate the camera orientation
     * based on gravity direction.
     */
    @Inject(
            method = "prepare",
            at = @At("RETURN")
    )
    private void onPrepare(Level level, Camera camera, Entity entity, CallbackInfo ci) {
        // We don't modify the camera here directly, but we could use this point
        // to store information about the current entity being rendered
        if (entity != null) {
            Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
            // Store or use gravity direction information if needed
        }
    }

    /**
     * Inject just after the start of the main render method to apply gravity-specific transformations
     */
    @Inject(
            method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void inject_render_0(Entity entity, double x, double y, double z, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (!(entity instanceof Projectile) && !(entity instanceof ExperienceOrb) && EntityTags.allowGravityTransformationInRendering(entity)) {
            Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
            if (!this.shouldRenderShadow) return;

            matrices.pushPose();
            RotationAnimation animation = GravityChangerAPI.getRotationAnimation(entity);
            if (animation == null) {
                return;
            }
            long timeMs = entity.level().getGameTime() * 50 + (long) (tickDelta * 50);
            matrices.mulPose(new Quaternionf(animation.getCurrentGravityRotationVec(gravityDirection, timeMs)).conjugate());
        }
    }

    /**
     * Inject before popping the matrix stack to clean up after our custom transformations
     */
    @Inject(
            method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
                    ordinal = 1
            )
    )
    private void inject_render_1(Entity entity, double x, double y, double z, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (!(entity instanceof Projectile) && !(entity instanceof ExperienceOrb) && EntityTags.allowGravityTransformationInRendering(entity)) {
            Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
            if (!this.shouldRenderShadow) return;

            matrices.popPose();
        }
    }

    /**
     * Inject after translating back to setup shadow rendering with proper gravity orientation
     */
    @Inject(
            method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void inject_render_2(Entity entity, double x, double y, double z, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (!(entity instanceof Projectile) && !(entity instanceof ExperienceOrb) && EntityTags.allowGravityTransformationInRendering(entity)) {
            Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
            if (Objects.equals(gravityDirection, new Vec3(0, -1, 0))) return;
            if (!this.shouldRenderShadow) return;

            matrices.mulPose(RotationUtil.getCameraRotationQuaternionVec(gravityDirection));
        }
    }

    /**
     * Completely override the renderShadow method for entities with custom gravity
     */
    @Inject(
            method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void inject_renderShadow(PoseStack matrices, MultiBufferSource vertexConsumers, Entity entity, float opacity, float tickDelta, LevelReader world, float radius, CallbackInfo ci) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        ci.cancel();

        double x = Mth.lerp((double)tickDelta, entity.xOld, entity.getX());
        double y = Mth.lerp((double)tickDelta, entity.yOld, entity.getY());
        double z = Mth.lerp((double)tickDelta, entity.zOld, entity.getZ());
        Vec3 minShadowPos = RotationUtil.vecPlayerToWorldVec(new Vec3((double) -radius, (double) -radius, (double) -radius), gravityDirection).add(x, y, z);
        Vec3 maxShadowPos = RotationUtil.vecPlayerToWorldVec(new Vec3((double) radius, 0.0D, (double) radius), gravityDirection).add(x, y, z);
        PoseStack.Pose entry = matrices.last();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(SHADOW_RENDER_TYPE);

        for (BlockPos blockPos : BlockPos.betweenClosed(BlockPos.containing(minShadowPos), BlockPos.containing(maxShadowPos))) {
            gravitychanger$renderShadowPartPlayer(entry, vertexConsumer, world, blockPos, x, y, z, radius, opacity, gravityDirection);
        }
    }

    /**
     * Render a shadow for a single block position, accounting for custom gravity direction
     */
    private static void gravitychanger$renderShadowPartPlayer(PoseStack.Pose entry, VertexConsumer vertices, LevelReader world, BlockPos pos, double x, double y, double z, float radius, float opacity, Vec3 gravityDirection) {
        BlockPos posBelow = getBlockPosInGravityDirection(pos, gravityDirection);
        BlockState blockStateBelow = world.getBlockState(posBelow);
        if (blockStateBelow.getRenderShape() != RenderShape.INVISIBLE && world.getMaxLocalRawBrightness(pos) > 3) {
            if (blockStateBelow.isCollisionShapeFullBlock(world, posBelow)) {
                VoxelShape voxelShape = blockStateBelow.getShape(world, posBelow);
                if (!voxelShape.isEmpty()) {
                    Vec3 playerPos = RotationUtil.vecWorldToPlayerVec(new Vec3(x, y, z), gravityDirection);
                    float alpha = (float) (((double) opacity - (playerPos.y - (RotationUtil.vecWorldToPlayerVec(Vec3.atCenterOf(pos), gravityDirection).y - 0.5D)) / 2.0D) * 0.5D * (double) world.getLightLevelDependentMagicValue(pos));
                    if (alpha >= 0.0F) {
                        if (alpha > 1.0F) {
                            alpha = 1.0F;
                        }

                        Vec3 centerPos = Vec3.atCenterOf(pos);
                        Vec3 playerCenterPos = RotationUtil.vecWorldToPlayerVec(centerPos, gravityDirection);

                        Vec3 playerRelNN = playerCenterPos.add(-0.5D, -0.5D, -0.5D).subtract(playerPos);
                        Vec3 playerRelPP = playerCenterPos.add(0.5D, -0.5D, 0.5D).subtract(playerPos);

                        Vec3 relNN = RotationUtil.vecWorldToPlayerVec(centerPos.add(RotationUtil.vecPlayerToWorldVec(-0.5D, -0.5D, -0.5D, gravityDirection)).subtract(x, y, z), gravityDirection);
                        Vec3 relNP = RotationUtil.vecWorldToPlayerVec(centerPos.add(RotationUtil.vecPlayerToWorldVec(-0.5D, -0.5D, 0.5D, gravityDirection)).subtract(x, y, z), gravityDirection);
                        Vec3 relPN = RotationUtil.vecWorldToPlayerVec(centerPos.add(RotationUtil.vecPlayerToWorldVec(0.5D, -0.5D, -0.5D, gravityDirection)).subtract(x, y, z), gravityDirection);
                        Vec3 relPP = RotationUtil.vecWorldToPlayerVec(centerPos.add(RotationUtil.vecPlayerToWorldVec(0.5D, -0.5D, 0.5D, gravityDirection)).subtract(x, y, z), gravityDirection);

                        float minU = -(float) playerRelNN.x / 2.0F / radius + 0.5F;
                        float maxU = -(float) playerRelPP.x / 2.0F / radius + 0.5F;
                        float minV = -(float) playerRelNN.z / 2.0F / radius + 0.5F;
                        float maxV = -(float) playerRelPP.z / 2.0F / radius + 0.5F;

                        shadowVertex(entry, vertices, alpha, (float) relNN.x, (float) relNN.y, (float) relNN.z, minU, minV);
                        shadowVertex(entry, vertices, alpha, (float) relNP.x, (float) relNP.y, (float) relNP.z, minU, maxV);
                        shadowVertex(entry, vertices, alpha, (float) relPP.x, (float) relPP.y, (float) relPP.z, maxU, maxV);
                        shadowVertex(entry, vertices, alpha, (float) relPN.x, (float) relPN.y, (float) relPN.z, maxU, minV);
                    }
                }
            }
        }
    }

    /**
     * Redirect view vector calculation for proper orientation in the hitbox rendering
     */
    @Redirect(
            method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            )
    )
    private static Vec3 redirectViewVector(Entity instance, float partialTicks) {
        Vec3 viewVector = instance.getViewVector(partialTicks);
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(instance);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return viewVector;
        }

        return RotationUtil.vecWorldToPlayerVec(viewVector, gravityDirection);
    }

    /**
     * Inject into shouldRender to handle visibility checks with custom gravity
     */
    @Inject(
            method = "shouldRender",
            at = @At("HEAD"),
            cancellable = true
    )
    private <E extends Entity> void onShouldRender(E entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        // If it's default gravity, let vanilla handle it
        if (gravityDirection.equals(new Vec3(0, -1, 0))) {
            return;
        }
        cir.setReturnValue(true);

        // For custom gravity, we might need to adjust the culling calculations
        // This is just a placeholder - we're not canceling by default
    }

    /**
     * Redirect camera orientation handling for entities with custom gravity
     */
    @Inject(
            method = "cameraOrientation",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetCameraOrientation(CallbackInfoReturnable<Quaternionf> cir) {
        // This method returns the camera orientation quaternion
        // For now, we're not overriding this, but it's a place where we could
        // if needed for specific camera rotation scenarios
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