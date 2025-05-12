package gravity_changer.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import gravity_changer.util.Rotor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Redirect(
        method = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 modify_setupTransforms_Vec3d_0(AbstractClientPlayer instance, float partialTick) {
        Vec3 viewVector = instance.getViewVector(partialTick);

        // Get both Direction and Vec3 gravity directions
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
