package gravity_changer.mixin.world.entity.Entity;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityBoundingBoxMixin {
    @Shadow public abstract AABB getBoundingBox();

    @Shadow public abstract Vec3 position();

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) this;
        // Skip if default gravity or already in custom collision to prevent recursion
        if (GravityChangerAPI.getGravityDirectionVec(self).equals(new net.minecraft.world.phys.Vec3(0, -1, 0))
                || CollisionContext.isInCustomCollision()) {
            return;
        }

        // Mark entry into custom collision context
        CollisionContext.setInCustomCollision(true);
        try {
            // Original envelope
            AABB original = cir.getReturnValue();
            // Transform envelope according to gravity
            AABB transformed = OrientedBoundingBoxTransformer.transformToOBB(original,
                    GravityChangerAPI.getGravityDirectionVec(self));
            cir.setReturnValue(transformed.move(this.position().reverse()));
        } finally {
            // Ensure flag is cleared exactly once
            CollisionContext.setInCustomCollision(false);
        }
    }
}