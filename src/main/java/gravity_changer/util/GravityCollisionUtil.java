package gravity_changer.util;

import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for gravity-related collision operations.
 * This class provides methods to work with entity-specific collision boxes.
 * 
 * <p>This utility class simplifies the use of entity-specific shrink factors
 * for diagonal gravity directions. It handles setting and clearing the entity context
 * for you, so you don't have to worry about it.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * // Instead of:
 * OrientedBoundingBoxTransformer.setCurrentEntityContext(entity);
 * try {
 *     OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir);
 *     // Use the OBB...
 * } finally {
 *     OrientedBoundingBoxTransformer.clearCurrentEntityContext();
 * }
 * 
 * // You can simply use:
 * OrientedBoundingBox obb = GravityCollisionUtil.transformToOBB(box, gravityDir, entity);
 * </pre>
 * 
 * <p>This class also provides methods for per-axis shrinking, which allows different shrink factors
 * for each axis (X, Y, Z). This can be useful for entities that have different proportions in different
 * directions, such as minecarts (which are wider than they are tall).</p>
 * 
 * <p>Example usage with per-axis shrinking:</p>
 * <pre>
 * // Create a Vec3 with different shrink factors for each axis
 * Vec3 shrinkFactors = new Vec3(0.85, 0.9, 0.85);
 * 
 * // Use the transformToOBBWithFactors method
 * OrientedBoundingBox obb = GravityCollisionUtil.transformToOBBWithFactors(box, gravityDir, shrinkFactors);
 * </pre>
 * 
 * <p>This class also provides a similar utility method for the {@code intersects} method.</p>
 * 
 * @see gravity_changer.collision.OrientedBoundingBoxTransformer
 */
public class GravityCollisionUtil {

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction and entity.
     * This method sets the entity context before calling transformToOBB and clears it afterward.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @param entity The entity that owns this bounding box
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBB(AABB box, Vec3 gravityDir, Entity entity) {
        try {
            // Set the entity context
            OrientedBoundingBoxTransformer.setCurrentEntityContext(entity);

            // Call the transformToOBB method
            return OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir);
        } finally {
            // Clear the entity context to prevent memory leaks
            OrientedBoundingBoxTransformer.clearCurrentEntityContext();
        }
    }

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction and custom shrink factors.
     * This method allows you to specify different shrink factors for each axis.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @param shrinkFactors A Vec3 containing the shrink factors for each axis (1.0 = no shrinking)
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBBWithFactors(AABB box, Vec3 gravityDir, Vec3 shrinkFactors) {
        // Call the transformToOBBWithFactors method directly
        return OrientedBoundingBoxTransformer.transformToOBBWithFactors(box, gravityDir, shrinkFactors);
    }

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction, entity, and custom shrink factors.
     * This method allows you to specify different shrink factors for each axis, overriding the entity's default factors.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @param entity The entity that owns this bounding box (used for caching)
     * @param shrinkFactors A Vec3 containing the shrink factors for each axis (1.0 = no shrinking)
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBBWithFactors(AABB box, Vec3 gravityDir, Entity entity, Vec3 shrinkFactors) {
        try {
            // Set the entity context (for caching purposes)
            OrientedBoundingBoxTransformer.setCurrentEntityContext(entity);

            // Call the transformToOBBWithFactors method
            return OrientedBoundingBoxTransformer.transformToOBBWithFactors(box, gravityDir, shrinkFactors);
        } finally {
            // Clear the entity context to prevent memory leaks
            OrientedBoundingBoxTransformer.clearCurrentEntityContext();
        }
    }

    /**
     * Checks if two AABBs intersect, taking into account the gravity direction and entity.
     * This method sets the entity context before calling intersects and clears it afterward.
     * 
     * @param box1 The first AABB
     * @param box2 The second AABB
     * @param gravityDir The gravity direction vector
     * @param entity The entity that owns the first bounding box
     * @return True if the boxes intersect, false otherwise
     */
    public static boolean intersects(AABB box1, AABB box2, Vec3 gravityDir, Entity entity) {
        try {
            // Set the entity context
            OrientedBoundingBoxTransformer.setCurrentEntityContext(entity);

            // Call the intersects method
            return OrientedBoundingBoxTransformer.intersects(box1, box2, gravityDir);
        } finally {
            // Clear the entity context to prevent memory leaks
            OrientedBoundingBoxTransformer.clearCurrentEntityContext();
        }
    }
}
