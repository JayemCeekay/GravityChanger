package gravity_changer.mixin.world.phys.aabb;

import gravity_changer.collision.OrientedBoundingBox;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the AABB class to handle equals and hashCode methods with custom gravity directions.
 * This ensures that OrientedBoundingBoxes are compared correctly.
 */
@Mixin(AABB.class)
public abstract class AABBEqualsHashCodeMixin {

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void onEquals(Object object, CallbackInfoReturnable<Boolean> cir) {
        // Check if this is an OrientedBoundingBox
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Check if the other object is an OrientedBoundingBox
        if (!(object instanceof OrientedBoundingBox)) {
            // If the other object is not an OrientedBoundingBox, use the default equals method
            return;
        }

        // Both objects are OrientedBoundingBoxes, so we need to compare them properly
        OrientedBoundingBox thisOBB = (OrientedBoundingBox)(Object)this;
        OrientedBoundingBox otherOBB = (OrientedBoundingBox)object;

        // Compare the rotations and centers first as they shouldn't be null
        if (!thisOBB.getRotation().equals(otherOBB.getRotation()) ||
                !thisOBB.getCenter().equals(otherOBB.getCenter())) {
            cir.setReturnValue(false);
            return;
        }

        // Now handle local boxes which might be null
        AABB thisLocal = thisOBB.getLocalBox();
        AABB otherLocal = otherOBB.getLocalBox();

        if (thisLocal == null && otherLocal == null) {
            cir.setReturnValue(true);
            return;
        }
        if (thisLocal == null || otherLocal == null) {
            cir.setReturnValue(false);
            return;
        }

        // Both local boxes exist, compare them
        cir.setReturnValue(thisLocal.equals(otherLocal));
    }

    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void onHashCode(CallbackInfoReturnable<Integer> cir) {
        // Check if this is an OrientedBoundingBox
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Get the OrientedBoundingBox
        OrientedBoundingBox obb = (OrientedBoundingBox)(Object)this;

        // Calculate hash code starting with rotation and center
        int result = 31;
        result = 31 * result + obb.getRotation().hashCode();
        result = 31 * result + obb.getCenter().hashCode();

        // Handle local box which might be null
        AABB localBox = obb.getLocalBox();
        result = 31 * result + (localBox != null ? localBox.hashCode() : 0);

        // Return the result
        cir.setReturnValue(result);
    }
}
