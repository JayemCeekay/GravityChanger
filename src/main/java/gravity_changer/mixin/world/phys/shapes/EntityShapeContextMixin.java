package gravity_changer.mixin.world.phys.shapes;


import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
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
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return entity.getY();
        }

        AABB boundingBox = entity.getBoundingBox();
        if (boundingBox == null) {
            return entity.getY(); // fallback to normal behavior
        }

        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(boundingBox, gravityDirectionVec);
        if (obb == null || obb.getLocalBox() == null) {
            return entity.getY(); // fallback if OBB creation failed
        }

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
        OrientedBoundingBox posBoundingBox = OrientedBoundingBoxTransformer.transformToOBB(posBox, gravityDirectionVec);
        OrientedBoundingBox shapeBoundingBox = OrientedBoundingBoxTransformer.transformToOBB(shapeBox, gravityDirectionVec);

        cir.setReturnValue(this.entityBottom > posBoundingBox.getLocalBox().minY + shapeBoundingBox.getLocalBox().maxY);

    }
}
