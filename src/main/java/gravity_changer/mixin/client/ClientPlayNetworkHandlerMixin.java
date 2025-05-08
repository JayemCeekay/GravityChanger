package gravity_changer.mixin.client;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientPacketListener.class, priority = 1001)
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Map<UUID, PlayerInfo> playerInfoMap;

    @Redirect(
        method = "Lnet/minecraft/client/multiplayer/ClientPacketListener;handleGameEvent(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getEyeY()D",
            ordinal = 0
        )
    )
    private double redirect_onGameStateChange_getEyeY_0(Player playerEntity) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(playerEntity);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(playerEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return playerEntity.getEyeY();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return playerEntity.getEyePosition().y;
    }

    @Redirect(
        method = "Lnet/minecraft/client/multiplayer/ClientPacketListener;handleGameEvent(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getX()D",
            ordinal = 0
        )
    )
    private double redirect_onGameStateChange_getX_0(Player playerEntity) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(playerEntity);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(playerEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return playerEntity.getX();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return playerEntity.getEyePosition().x;
    }

    @Redirect(
        method = "Lnet/minecraft/client/multiplayer/ClientPacketListener;handleGameEvent(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getZ()D",
            ordinal = 0
        )
    )
    private double redirect_onGameStateChange_getZ_0(Player playerEntity) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(playerEntity);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(playerEntity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return playerEntity.getZ();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return playerEntity.getEyePosition().z;
    }

    @WrapOperation(
        method = "handleExplosion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;",
            ordinal = 0
        )
    )
    private Vec3 wrapOperation_onExplosion_add_0(Vec3 vec3d, double x, double y, double z, Operation<Vec3> original) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(minecraft.player);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(minecraft.player);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return original.call(vec3d, x, y, z);
        }

        Vec3 player;
        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(minecraft.player)) {
            player = RotationUtil.vecWorldToPlayer(x, y, z, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            player = RotationUtil.vecWorldToPlayerVec(new Vec3(x, y, z), gravityDirectionVec);
        }

        return original.call(vec3d, player.x, player.y, player.z);
    }
}
