package gravity_changer.mixin.client;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRenderMixin {
    //@Redirect(
    //        method = "renderLabelIfPresent",
    //        at = @At(
    //                value = "INVOKE",
    //                target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;getRotation()Lnet/minecraft/util/math/Quaternion;",
    //                ordinal = 0
    //        )
    //)
    //private Quaternionf redirect_renderLabelIfPresent_getRotation_0(EntityRenderDispatcher entityRenderDispatcher, Entity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
    //    Direction gravityDirection = GravityChangerAPI.getGravityDirection(entity);
    //    if(gravityDirection == Direction.DOWN) {
    //        return entityRenderDispatcher.getRotation();
    //    }

    /// /
    //    Quaternionf Quaternionf = RotationUtil.getCameraRotationQuaternion(gravityDirection).copy();
    //    quaternion.conjugate();
    //    quaternion.hamiltonProduct(entityRenderDispatcher.getRotation().copy());
    //    return quaternion;
    //}
    @ModifyExpressionValue(
            method = "renderNameTag",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;cameraOrientation()Lorg/joml/Quaternionf;",
                    ordinal = 0
            )
    )
    private Quaternionf modifyExpressionValue_renderLabelIfPresent_getRotation_0(Quaternionf originalRotation, Entity entity) {
        // Get Vec3 gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 && gravityDirectionVec.x() == 0 && gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return originalRotation;
        }

        Quaternionf quaternion;

        // For arbitrary directions, use the Vec3-based method
        quaternion = RotationUtil.getCameraRotationQuaternionVec(gravityDirectionVec);
        quaternion.conjugate();

        quaternion.mul(originalRotation);
        return quaternion;
    }
}
