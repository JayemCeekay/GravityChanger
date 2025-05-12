package gravity_changer.mixin.collision.aabb;

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

    /**
     * Injects into the 'equals' method to handle comparing OrientedBoundingBoxes.
     */
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

        // Compare the local boxes, rotations, and centers
        boolean equals = thisOBB.getLocalBox().equals(otherOBB.getLocalBox()) &&
                         thisOBB.getRotation().equals(otherOBB.getRotation()) &&
                         thisOBB.getCenter().equals(otherOBB.getCenter());

        // Return the result
        cir.setReturnValue(equals);
    }

    /**
     * Injects into the 'hashCode' method to handle hashing OrientedBoundingBoxes.
     */
    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void onHashCode(CallbackInfoReturnable<Integer> cir) {
        // Check if this is an OrientedBoundingBox
        if (!((Object)this instanceof OrientedBoundingBox)) {
            return;
        }

        // Get the OrientedBoundingBox
        OrientedBoundingBox obb = (OrientedBoundingBox)(Object)this;

        // Calculate the hash code based on the local box, rotation, and center
        int result = obb.getLocalBox().hashCode();
        result = 31 * result + obb.getRotation().hashCode();
        result = 31 * result + obb.getCenter().hashCode();

        // Return the result
        cir.setReturnValue(result);
    }
}