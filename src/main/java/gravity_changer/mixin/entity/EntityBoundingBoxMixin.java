package gravity_changer.mixin.entity;

import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to transform the bounding box returned by Entity.getBoundingBox() into an OrientedBoundingBox
 * for entities with non-default gravity.
 */
@Mixin(Entity.class)
public abstract class EntityBoundingBoxMixin {

    @Shadow
    private Vec3 position;

    @Shadow
    public abstract AABB getBoundingBox();

    /**
     * Injects into Entity.getBoundingBox() to return an OrientedBoundingBox instead of an AABB
     * for entities with non-default gravity.
     */
    @Inject(
            method = "getBoundingBox",
            at = @At("RETURN"),
            cancellable = true
    )
    private void inject_getBoundingBox(CallbackInfoReturnable<AABB> cir) {
        Entity entity = ((Entity) (Object) this);
        if (entity instanceof Projectile) return;

        // Check if the entity has a gravity component
        try {
            if (((ComponentProvider) entity).getComponentContainer() == null) {
                return;
            }

            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            // Entity's component container might not be initialized yet
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        if (gravityDirection.equals(new Vec3(0, -1, 0))) return;

        // Get the current AABB
        AABB box = cir.getReturnValue();

        // If it's already an OrientedBoundingBox, no need to transform it
        if (box instanceof OrientedBoundingBox) return;

        // Transform the AABB to an OrientedBoundingBox
        // First move the box to local space (relative to entity position)
        OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(
            box.move(this.position.reverse()), gravityDirection);

        // Move the OBB back to the entity's position
        // Since OrientedBoundingBox now extends AABB, we can return it directly
        cir.setReturnValue(obb.move(this.position));
    }
}
