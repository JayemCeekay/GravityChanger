package gravity_changer.mixin;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityCollisionContext.class)
public abstract class EntityShapeContextMixin {
    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private double entityBottom;

    @Redirect(
        method = "<init>(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getY()D",
            ordinal = 0
        )
    )
    private static double redirect_init_getY_0(Entity entity) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(entity);
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return entity.getY();
        }

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(entity)) {
            return RotationUtil.boxWorldToPlayer(entity.getBoundingBox(), gravityDirection).minY;
        } else {
            // For arbitrary directions, use the Vec3-based method
            return RotationUtil.boxWorldToPlayerVec(entity.getBoundingBox(), gravityDirectionVec).minY;
        }
    }

    @Inject(
        method = "Lnet/minecraft/world/phys/shapes/EntityCollisionContext;isAbove(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/core/BlockPos;Z)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject_isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue, CallbackInfoReturnable<Boolean> cir) {
        if (this.entity == null) return;

        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(this.entity);
        net.minecraft.world.phys.Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this.entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(entity)) {
            cir.setReturnValue(this.entityBottom > RotationUtil.boxWorldToPlayer(new AABB(pos), gravityDirection).minY + 
                RotationUtil.boxWorldToPlayer(shape.bounds().inflate(-9.999999747378752E-6D), gravityDirection).maxX);
        } else {
            // For arbitrary directions, use the Vec3-based method
            cir.setReturnValue(this.entityBottom > RotationUtil.boxWorldToPlayerVec(new AABB(pos), gravityDirectionVec).minY + 
                RotationUtil.boxWorldToPlayerVec(shape.bounds().inflate(-9.999999747378752E-6D), gravityDirectionVec).maxX);
        }
    }
}
