package gravity_changer.mixin;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.GravityCollisionUtil;
import gravity_changer.util.RotationUtil;
import gravity_changer.util.Rotor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    // inside redirect_init_getY_0
    @Redirect(
            method = "<init>(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getY()D",
                    ordinal = 0
            )
    )
    private static double redirect_init_getY_0(Entity entity) {
        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return entity.getY();
        }

        // Use the dynamic system with entity context for better accuracy
        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(entity.getBoundingBox(), gravityDirectionVec, entity);
        return obb.getLocalBox().minY;

    }

    @Inject(
            method = "Lnet/minecraft/world/phys/shapes/EntityCollisionContext;isAbove(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue, CallbackInfoReturnable<Boolean> cir) {
        if (this.entity == null) return;

        // Get gravity direction as Vec3
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this.entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        // For arbitrary directions, use the Vec3-based method
        AABB posBox = new AABB(pos);
        AABB shapeBox = shape.bounds().inflate(-9.999999747378752E-6D);

        // Use the dynamic system with entity context for better accuracy
        OrientedBoundingBox posBoundingBox = OrientedBoundingBoxTransformer.transformToOBB(posBox, gravityDirectionVec, this.entity);
        OrientedBoundingBox shapeBoundingBox = OrientedBoundingBoxTransformer.transformToOBB(shapeBox, gravityDirectionVec, this.entity);

        cir.setReturnValue(this.entityBottom > posBoundingBox.getLocalBox().minY + shapeBoundingBox.getLocalBox().maxY);

    }
}
