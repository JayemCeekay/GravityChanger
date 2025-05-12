package gravity_changer.mixin;


import gravity_changer.api.GravityChangerAPI;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LookAtPlayerGoal.class)
public abstract class LookAtEntityGoalMixin {
    @Redirect(
        method = "Lnet/minecraft/world/entity/ai/goal/LookAtPlayerGoal;tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getEyeY()D",
            ordinal = 0
        )
    )
    private double redirect_tick_getEyeY_0(Entity entity) {
        // Get both Direction and Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return entity.getEyeY();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return entity.getEyePosition().y;
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/ai/goal/LookAtPlayerGoal;tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getX()D",
            ordinal = 0
        )
    )
    private double redirect_tick_getX_0(Entity entity) {
        // Get both Direction and Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return entity.getX();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return entity.getEyePosition().x;
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/ai/goal/LookAtPlayerGoal;tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getZ()D",
            ordinal = 0
        )
    )
    private double redirect_tick_getZ_0(Entity entity) {
        // Get both Direction and Vec3 gravity directions
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return entity.getZ();
        }

        // For both cardinal and arbitrary directions, we can use getEyePosition
        // which already handles the correct eye position calculation
        return entity.getEyePosition().z;
    }
}
