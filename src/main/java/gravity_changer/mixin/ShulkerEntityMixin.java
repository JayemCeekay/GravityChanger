package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Shulker.class, priority = 1001)
public abstract class ShulkerEntityMixin {
    @WrapOperation(
            method = "onPeekAmountChange",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 0
            )
    )
    private void wrapOperation_pushEntities_move_0(Entity entity, MoverType movementType, Vec3 vec3d, Operation<Void> original) {
        // Get Vec3 gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 && gravityDirectionVec.x() == 0 && gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            original.call(entity, movementType, vec3d);
            return;
        }

        // Transform the movement vector for arbitrary gravity directions
        Vec3 transformedVec = RotationUtil.vecWorldToPlayerVec(vec3d, gravityDirectionVec);
        original.call(entity, movementType, transformedVec);
    }
}