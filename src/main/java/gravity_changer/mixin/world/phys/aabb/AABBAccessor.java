package gravity_changer.mixin.world.phys.aabb;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin for the AABB class to make private methods accessible.
 */
@Mixin(AABB.class)
public interface AABBAccessor {

    /**
     * Accessor for the private getDirection method.
     */
    @Invoker("getDirection")
    static Direction invokeGetDirection(AABB aabb, Vec3 start, double[] minDistance, Direction facing, double deltaX, double deltaY, double deltaZ) {
        throw new AssertionError("This should be overridden by the mixin");
    }
}