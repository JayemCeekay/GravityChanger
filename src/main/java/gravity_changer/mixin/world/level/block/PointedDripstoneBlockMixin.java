package gravity_changer.mixin.world.level.block;


import gravity_changer.api.GravityChangerAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneBlockMixin {
    // use Comparable<Direction> instead of Direction because of erased signature
    @WrapOperation(
            method = "fallOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;",
                    ordinal = 0
            )
    )
    private Comparable<Direction> wrapOperation_onLandedUpon_get_0(BlockState blockState, Property<Direction> property, Operation<Comparable<Direction>> original, Level world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        // Get Vec3 gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y() < -0.99 && gravityDirectionVec.x() == 0 && gravityDirectionVec.z() == 0;
        if (isDefaultGravity) {
            return original.call(blockState, property);
        }

        // Get the current tip direction of the dripstone
        Direction tipDirection = (Direction)original.call(blockState, property);

        // Convert the gravity direction to a cardinal direction for comparison
        Direction gravityDirection = Direction.getNearest(
                gravityDirectionVec.x(),
                gravityDirectionVec.y(),
                gravityDirectionVec.z()
        );

        // Check if the tip is pointing against gravity (which would hurt the player)
        return tipDirection == gravityDirection.getOpposite() ? Direction.UP : Direction.DOWN;
    }
}
