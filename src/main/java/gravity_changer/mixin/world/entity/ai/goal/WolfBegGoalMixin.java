package gravity_changer.mixin.world.entity.ai.goal;

import gravity_changer.api.GravityChangerAPI;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BegGoal.class)
public abstract class WolfBegGoalMixin {
    // Helper method to check if using default (downward) gravity
    private boolean isDefaultGravity(Vec3 gravityDirection) {
        return gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/ai/goal/BegGoal;tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getEyeY()D",
                    ordinal = 0
            )
    )
    private double redirect_tick_getEyeY_0(Player playerEntity) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(playerEntity);
        if (isDefaultGravity(gravityDirection)) {
            return playerEntity.getEyeY();
        }

        return playerEntity.getEyePosition().y;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/ai/goal/BegGoal;tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getX()D",
                    ordinal = 0
            )
    )
    private double redirect_tick_getX_0(Player playerEntity) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(playerEntity);
        if (isDefaultGravity(gravityDirection)) {
            return playerEntity.getX();
        }

        return playerEntity.getEyePosition().x;
    }

    @Redirect(
            method = "Lnet/minecraft/world/entity/ai/goal/BegGoal;tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getZ()D",
                    ordinal = 0
            )
    )
    private double redirect_tick_getZ_0(Player playerEntity) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(playerEntity);
        if (isDefaultGravity(gravityDirection)) {
            return playerEntity.getZ();
        }

        return playerEntity.getEyePosition().z;
    }
}