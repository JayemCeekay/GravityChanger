package gravity_changer.mixin;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonBlockEntityMixin {
    @Redirect(
            method = "Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;moveEntityByPiston(Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/Entity;DLnet/minecraft/core/Direction;)V",
            at = @At(
                    value = "NEW",
                    target = "(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            )
    )
    private static Vec3 redirect_moveEntity_Vec3d_0(double x, double y, double z, Direction direction, Entity entity, double d, Direction direction2) {
        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction (0, -1, 0)
        boolean isDefaultGravity = gravityDirection.y < -0.99 && gravityDirection.x == 0 && gravityDirection.z == 0;
        if (isDefaultGravity) {
            return new Vec3(x, y, z);
        }

        // Transform the world vector to player-relative vector based on gravity direction
        return RotationUtil.vecWorldToPlayerVec(new Vec3(x, y, z), gravityDirection);
    }
}
