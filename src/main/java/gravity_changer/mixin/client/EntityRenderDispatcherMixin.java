package gravity_changer.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gravity_changer.RotationAnimation;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import gravity_changer.EntityTags;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    @Final
    private static RenderType SHADOW_RENDER_TYPE;

    @Shadow
    private boolean shouldRenderShadow;

    @Shadow
    private static void shadowVertex(PoseStack.Pose entry, VertexConsumer vertices, float alpha, float x, float y, float z, float u, float v) {
    }

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
            // Get Vec3 gravity directions
            Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

            if (!this.shouldRenderShadow) return;

            matrices.pushPose();
            RotationAnimation animation = GravityChangerAPI.getRotationAnimation(entity);
            if (animation == null) {
                return;
            }
            long timeMs = entity.level().getGameTime() * 50 + (long) (tickDelta * 50);

            // Check if we're using the default gravity direction
            boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;

            // For arbitrary directions, use the Vec3-based method
            matrices.mulPose(new Quaternionf(animation.getCurrentGravityRotationVec(gravityDirectionVec, timeMs).conjugate()));
        }
    }

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
            if (!this.shouldRenderShadow) return;

            matrices.popPose();
        }
    }

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
            // Get Vec3 gravity direction
            Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

            // Check if we're using the default gravity direction
            boolean isDefaultGravity = gravityDirectionVec.y < 0 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
            if (isDefaultGravity) return;
            if (!this.shouldRenderShadow) return;

            // For arbitrary directions, use the Vec3-based method
            matrices.mulPose(RotationUtil.getCameraRotationQuaternionVec(gravityDirectionVec));
        }
    }

    @Inject(
            method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void inject_renderShadow(PoseStack matrices, MultiBufferSource vertexConsumers, Entity entity, float opacity, float tickDelta, LevelReader world, float radius, CallbackInfo ci) {
        // Get Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        ci.cancel();

        double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());

        Vec3 minShadowPos, maxShadowPos;

        // For arbitrary directions, use the Vec3-based method
        minShadowPos = RotationUtil.vecPlayerToWorldVec(new Vec3(-radius, -radius, -radius), gravityDirectionVec).add(x, y, z);
        maxShadowPos = RotationUtil.vecPlayerToWorldVec(new Vec3(radius, 0.0D, radius), gravityDirectionVec).add(x, y, z);


        PoseStack.Pose entry = matrices.last();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(SHADOW_RENDER_TYPE);

        for (BlockPos blockPos : BlockPos.betweenClosed(BlockPos.containing(minShadowPos), BlockPos.containing(maxShadowPos))) {
            gravitychanger$renderShadowPartPlayer(entity, entry, vertexConsumer, world, blockPos, x, y, z, radius, opacity, gravityDirectionVec);
        }
    }

    private static void gravitychanger$renderShadowPartPlayer(PoseStack.Pose entry, VertexConsumer vertices, LevelReader world, BlockPos pos, double x, double y, double z, float radius, float opacity) {
        // This method is kept for backward compatibility
        // gravitychanger$renderShadowPartPlayer(entry, vertices, world, pos, x, y, z, radius, opacity, gravityDirection, null);
    }

    private static void gravitychanger$renderShadowPartPlayer(Entity entity, PoseStack.Pose entry, VertexConsumer vertices, LevelReader world, BlockPos pos, double x, double y, double z, float radius, float opacity, Vec3 gravityDirectionVec) {
        // Get the block below based on gravity direction
        BlockPos posBelow;

        // For arbitrary directions, calculate the block position in the gravity direction
        Vec3 gravityOffset = gravityDirectionVec.normalize();
        posBelow = BlockPos.containing(pos.getX() + gravityOffset.x, pos.getY() + gravityOffset.y, pos.getZ() + gravityOffset.z);


        BlockState blockStateBelow = world.getBlockState(posBelow);
        if (blockStateBelow.getRenderShape() != RenderShape.INVISIBLE && world.getMaxLocalRawBrightness(pos) > 3) {
            if (blockStateBelow.isCollisionShapeFullBlock(world, posBelow)) {
                VoxelShape voxelShape = blockStateBelow.getShape(world, posBelow);
                if (!voxelShape.isEmpty()) {
                    Vec3 playerPos;
                    Vec3 centerPos = Vec3.atCenterOf(pos);
                    Vec3 playerCenterPos;

                    // For arbitrary directions, use the Vec3-based method
                    playerPos = RotationUtil.vecWorldToPlayerVec(new Vec3(x, y, z), gravityDirectionVec);
                    playerCenterPos = RotationUtil.vecWorldToPlayerVec(centerPos, gravityDirectionVec);


                    float alpha = (float) (((double) opacity - (playerPos.y - (playerCenterPos.y - 0.5D)) / 2.0D) * 0.5D * (double) world.getLightLevelDependentMagicValue(pos));
                    if (alpha >= 0.0F) {
                        if (alpha > 1.0F) {
                            alpha = 1.0F;
                        }

                        Vec3 playerRelNN = playerCenterPos.add(-0.5D, -0.5D, -0.5D).subtract(playerPos);
                        Vec3 playerRelPP = playerCenterPos.add(0.5D, -0.5D, 0.5D).subtract(playerPos);

                        Vec3 relNN, relNP, relPN, relPP;


                        // For arbitrary directions, use the Vec3-based method
                        relNN = RotationUtil.vecWorldToPlayerVec(centerPos.add(RotationUtil.vecPlayerToWorldVec(new Vec3(-0.5D, -0.5D, -0.5D), gravityDirectionVec)).subtract(x, y, z), gravityDirectionVec);
                        relNP = RotationUtil.vecWorldToPlayerVec(centerPos.add(RotationUtil.vecPlayerToWorldVec(new Vec3(-0.5D, -0.5D, 0.5D), gravityDirectionVec)).subtract(x, y, z), gravityDirectionVec);
                        relPN = RotationUtil.vecWorldToPlayerVec(centerPos.add(RotationUtil.vecPlayerToWorldVec(new Vec3(0.5D, -0.5D, -0.5D), gravityDirectionVec)).subtract(x, y, z), gravityDirectionVec);
                        relPP = RotationUtil.vecWorldToPlayerVec(centerPos.add(RotationUtil.vecPlayerToWorldVec(new Vec3(0.5D, -0.5D, 0.5D), gravityDirectionVec)).subtract(x, y, z), gravityDirectionVec);


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

    @Redirect(
            method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;"
            )
    )
    private static AABB redirect_renderHitbox_move(AABB box, double x, double y, double z, PoseStack matrices, VertexConsumer vertices, Entity entity, float tickDelta) {
        // First, move the box as normal
        AABB movedBox = box.move(x, y, z);

        // Get Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return movedBox;
        }

        // For cardinal directions, use the existing code path for backward compatibility
        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.boxWorldToPlayerVec(movedBox, gravityDirectionVec);
    }

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

        // Get Vec3 gravity directions
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(instance);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return viewVector;
        }

        // For arbitrary directions, use the Vec3-based method
        return RotationUtil.vecWorldToPlayerVec(viewVector, gravityDirectionVec);
    }
}
