package gravity_changer.mixin.world.entity.Entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Invoker("makeBoundingBox")
    AABB gc_makeBoundingBox();

    /**
     * Gives you access to the protected isHorizontalCollisionMinor(Vec3) method.
     */
    @Invoker("isHorizontalCollisionMinor")
    boolean invokeIsHorizontalCollisionMinor(Vec3 deltaMovement);
}
