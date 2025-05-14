package gravity_changer.mixin.world.level;

import gravity_changer.collision.CollisionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin for the Level class to set and clear the entity context before and after collision detection.
 * This allows the AABB Collision Mixin to work correctly by providing the entity context during collision detection.
 */
@Mixin(value = CommonLevelAccessor.class, targets = "net/minecraft/world/level/CommonLevelAccessor")
public interface CommonLevelAccessorMixin {

    /**
     * Sets the entity context before processing collisions.
     */
    @Inject(method = "getEntityCollisions", at = @At("HEAD"))
    default void onGetEntityCollisions(Entity entity, AABB box,
                                      CallbackInfoReturnable<List<VoxelShape>> cir) {
        CollisionContext.setCurrentEntity(entity);
        // Ensure the custom collision flag is reset at the start of collision detection
        CollisionContext.setInCustomCollision(false);
    }

    /**
     * Clears the entity context after processing collisions.
     */
    @Inject(method = "getEntityCollisions", at = @At("RETURN"))
    default void afterGetEntityCollisions(Entity entity, AABB box,
                                         CallbackInfoReturnable<List<VoxelShape>> cir) {
        CollisionContext.clearCurrentEntity();
        // Also clear the custom collision flag to prevent it from affecting future collision checks
        CollisionContext.clearInCustomCollision();
    }
}
