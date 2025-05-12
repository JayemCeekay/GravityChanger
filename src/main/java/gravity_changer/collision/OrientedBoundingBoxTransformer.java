package gravity_changer.collision;

import com.google.common.collect.Maps;
import gravity_changer.EntityTags;
import gravity_changer.util.RotationUtil;
import gravity_changer.util.Rotor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Map;

/**
 * Utility class for transforming between AABBs and OrientedBoundingBoxes.
 * This is used for off-axis collision detection with arbitrary gravity directions.
 * 
 * <p>This class now supports entity-specific shrink factors for diagonal gravity directions.
 * When entities have a diagonal gravity direction, their bounding boxes can become too large
 * due to the transformation from an axis-aligned bounding box (AABB) to an oriented bounding box (OBB).
 * This can cause collision issues, such as entities getting stuck in tight spaces or not fitting
 * through gaps they should be able to pass through.</p>
 * 
 * <p>There are three ways to use the entity-specific shrink factor functionality:</p>
 * <ol>
 *   <li>Direct Method Call: Call {@link #transformToOBB(AABB, Vec3, Entity)} directly, passing the entity as a parameter.</li>
 *   <li>Entity Context: Set the entity context using {@link #setCurrentEntityContext(Entity)} before calling
 *       the original {@link #transformToOBB(AABB, Vec3)} method, and it will use that entity to calculate
 *       the shrink factor. Don't forget to clear the context using {@link #clearCurrentEntityContext()} afterward.</li>
 *   <li>Utility Method: Use the {@code GravityCollisionUtil} class, which handles setting and clearing
 *       the entity context for you.</li>
 * </ol>
 * 
 * <p>The default shrink factors are:</p>
 * <ul>
 *   <li>Players: 0.85</li>
 *   <li>Minecarts: 0.9</li>
 *   <li>Living Entities: 0.8</li>
 *   <li>Other Entities: 0.85</li>
 * </ul>
 * 
 * <p>These values can be adjusted in the {@link #calculateShrinkFactors(Entity)} method.</p>
 * 
 * <p>This class now also supports per-axis shrinking, which allows different shrink factors
 * for each axis (X, Y, Z). This can be useful for entities that have different proportions in different
 * directions, such as minecarts (which are wider than they are tall).</p>
 * 
 * <p>There are several ways to use the per-axis shrinking functionality:</p>
 * <ol>
 *   <li>Direct Method Call: Call {@link #transformToOBBWithFactors(AABB, Vec3, Vec3)} directly, passing a Vec3 of shrink factors.</li>
 *   <li>Entity-Based Method: Call {@link #transformToOBB(AABB, Vec3, Entity)}, which will use the entity-specific
 *       per-axis shrink factors calculated by {@link #calculateShrinkFactors(Entity)}.</li>
 *   <li>Utility Method: Use the {@code GravityCollisionUtil.transformToOBBWithFactors} method, which provides
 *       a simpler interface for using per-axis shrinking.</li>
 * </ol>
 * 
 * <p>The default per-axis shrink factors are calculated based on the entity type:</p>
 * <ul>
 *   <li>Players: (0.85, 0.9, 0.85) - Less shrinking on the Y axis to maintain height</li>
 *   <li>Minecarts: (0.9, 0.8, 0.9) - More shrinking on the Y axis since they are wider than tall</li>
 *   <li>Other Entities: Same factor for all axes</li>
 * </ul>
 * 
 * <p>These values can be adjusted in the {@link #calculateShrinkFactors(Entity)} method.</p>
 */
public class OrientedBoundingBoxTransformer {

    // Cache for transformed boxes to avoid recalculating them every frame
    private static final Map<Pair<AABB, Vec3>, OrientedBoundingBox> OBB_CACHE = Maps.newHashMap();

    // Maximum cache size to prevent memory leaks
    private static final int MAX_CACHE_SIZE = 1000;

    // Default shrink factor for diagonal gravity directions
    // 1.0 means no shrinking, 0.5 would shrink by half, etc.
    private static final double DEFAULT_DIAGONAL_SHRINK_FACTOR = 0.85;

    // Cache for entity-specific per-axis shrink factors to avoid recalculating them every frame
    private static final Map<Entity, Vec3> SHRINK_FACTORS_CACHE = Maps.newHashMap();

    // Cache for entity-specific offsets to avoid recalculating them every frame
    private static final Map<Entity, Vec3> OFFSET_CACHE = Maps.newHashMap();

    // Thread-local storage for the current entity context
    private static final ThreadLocal<Entity> CURRENT_ENTITY_CONTEXT = new ThreadLocal<>();

    /**
     * Sets the current entity context for the thread.
     * This is used to provide entity-specific shrink factors without modifying method signatures.
     * 
     * @param entity The entity to set as the current context
     */
    public static void setCurrentEntityContext(Entity entity) {
        CURRENT_ENTITY_CONTEXT.set(entity);
    }

    /**
     * Gets the current entity context for the thread.
     * 
     * @return The current entity context, or null if not set
     */
    public static Entity getCurrentEntityContext() {
        return CURRENT_ENTITY_CONTEXT.get();
    }

    /**
     * Clears the current entity context for the thread.
     * This should be called after the operation is complete to prevent memory leaks.
     */
    public static void clearCurrentEntityContext() {
        CURRENT_ENTITY_CONTEXT.remove();
    }


    /**
     * Calculates per-axis shrink factors for an entity based on its characteristics.
     * Different entity types may need different shrink factors for optimal gameplay,
     * and some entities may need different shrink factors for different axes.
     * 
     * @param entity The entity to calculate the shrink factors for
     * @return A Vec3 containing the shrink factors for each axis (1.0 means no shrinking)
     */
    public static Vec3 calculateShrinkFactors(Entity entity) {
        // Check if we have a cached value
        if (SHRINK_FACTORS_CACHE.containsKey(entity)) {
            return SHRINK_FACTORS_CACHE.get(entity);
        }

        // Default shrink factors
        Vec3 shrinkFactors;

        // Adjust per-axis shrink factors based on entity type
        if (entity instanceof Player) {
            // Players: Less shrinking on the Y axis to maintain height
            shrinkFactors = new Vec3(0.85, 0.9, 0.85);
        } else if (entity instanceof Minecart) {
            // Minecarts: More shrinking on the Y axis since they are wider than tall
            shrinkFactors = new Vec3(0.9, 0.8, 0.9);
        } else if (entity instanceof LivingEntity) {
            // Living entities: Slightly more shrinking overall
            double factor = 0.8;
            shrinkFactors = new Vec3(factor, factor, factor);
        } else {
            // Other entities: Default shrink factor
            double factor = DEFAULT_DIAGONAL_SHRINK_FACTOR;
            shrinkFactors = new Vec3(factor, factor, factor);
        }

        // Cache the result
        SHRINK_FACTORS_CACHE.put(entity, shrinkFactors);

        return shrinkFactors;
    }

    /**
     * Calculates dynamic per-axis shrink factors for an entity based on its characteristics,
     * gravity direction, and original bounding box. This method attempts to minimize the
     * difference between the original and transformed bounding box dimensions.
     * 
     * @param entity The entity to calculate the shrink factors for
     * @param gravityDir The gravity direction vector
     * @param originalBox The original bounding box of the entity
     * @return A Vec3 containing the shrink factors for each axis (1.0 means no shrinking)
     */
    public static Vec3 calculateDynamicShrinkFactors(Entity entity, Vec3 gravityDir, AABB originalBox) {
        // For default gravity, no shrinking is needed
        if (gravityDir.equals(new Vec3(0, -1, 0))) {
            return new Vec3(1.0, 1.0, 1.0);
        }

        // For cardinal directions (aligned with axes), less shrinking is needed
        if (Math.abs(gravityDir.x) > 0.99 || Math.abs(gravityDir.y) > 0.99 || Math.abs(gravityDir.z) > 0.99) {
            return new Vec3(0.95, 0.95, 0.95);
        }

        // Calculate the original dimensions
        double originalWidth = originalBox.maxX - originalBox.minX;
        double originalHeight = originalBox.maxY - originalBox.minY;
        double originalDepth = originalBox.maxZ - originalBox.minZ;

        // Start with entity-specific base factors
        Vec3 baseFactors = calculateShrinkFactors(entity);

        // Calculate the angle between gravity direction and each axis
        // The more aligned the gravity is with an axis, the less shrinking needed for that axis
        double xAngle = Math.abs(gravityDir.x);
        double yAngle = Math.abs(gravityDir.y);
        double zAngle = Math.abs(gravityDir.z);

        // Adjust shrink factors based on gravity alignment
        // More shrinking for axes that are more perpendicular to gravity
        double xFactor = baseFactors.x * (1.0 - 0.2 * xAngle);
        double yFactor = baseFactors.y * (1.0 - 0.2 * yAngle);
        double zFactor = baseFactors.z * (1.0 - 0.2 * zAngle);

        // Ensure factors are within reasonable bounds
        xFactor = Math.max(0.7, Math.min(1.0, xFactor));
        yFactor = Math.max(0.7, Math.min(1.0, yFactor));
        zFactor = Math.max(0.7, Math.min(1.0, zFactor));

        // Fine-tune based on entity proportions
        // For tall entities, shrink less in the height dimension
        if (originalHeight > originalWidth * 1.5 && originalHeight > originalDepth * 1.5) {
            yFactor = Math.min(1.0, yFactor + 0.1);
        }

        // For wide entities, shrink less in the width dimension
        if (originalWidth > originalHeight * 1.5 && originalWidth > originalDepth * 1.5) {
            xFactor = Math.min(1.0, xFactor + 0.1);
        }

        // For deep entities, shrink less in the depth dimension
        if (originalDepth > originalHeight * 1.5 && originalDepth > originalWidth * 1.5) {
            zFactor = Math.min(1.0, zFactor + 0.1);
        }

        return new Vec3(xFactor, yFactor, zFactor);
    }

    /**
     * Calculates an offset for an entity's bounding box based on its characteristics.
     * This offset is applied after rotation to align the box with the entity model.
     * 
     * @param entity The entity to calculate the offset for
     * @return A Vec3 containing the offset for each axis
     */
    public static Vec3 calculateOffset(Entity entity) {
        // Check if we have a cached value
        if (OFFSET_CACHE.containsKey(entity)) {
            return OFFSET_CACHE.get(entity);
        }

        // Default offset (no offset)
        Vec3 offset = Vec3.ZERO;

        // Adjust offset based on entity type
        if (entity instanceof Player) {
            // Players: Offset slightly upward to align with the model
            offset = new Vec3(0, 0.1, 0);
        } else if (entity instanceof Minecart) {
            // Minecarts: Offset downward to align with the model
            offset = new Vec3(0, -0.1, 0);
        } else if (entity instanceof LivingEntity) {
            // Living entities: Slight upward offset
            offset = new Vec3(0, 0.05, 0);
        }

        // Cache the result
        OFFSET_CACHE.put(entity, offset);

        return offset;
    }

    /**
     * Calculates a dynamic offset for an entity's bounding box based on its characteristics,
     * gravity direction, and original bounding box. This method attempts to align the
     * transformed bounding box with the entity model.
     * 
     * @param entity The entity to calculate the offset for
     * @param gravityDir The gravity direction vector
     * @param originalBox The original bounding box of the entity
     * @return A Vec3 containing the offset for each axis
     */
    public static Vec3 calculateDynamicOffset(Entity entity, Vec3 gravityDir, AABB originalBox) {
        // For default gravity, use the standard offset
        if (gravityDir.equals(new Vec3(0, -1, 0))) {
            return calculateOffset(entity);
        }

        // Start with the base offset for this entity type
        Vec3 baseOffset = calculateOffset(entity);

        // Calculate the original dimensions and center
        double originalWidth = originalBox.maxX - originalBox.minX;
        double originalHeight = originalBox.maxY - originalBox.minY;
        double originalDepth = originalBox.maxZ - originalBox.minZ;
        Vec3 originalCenter = new Vec3(
            (originalBox.minX + originalBox.maxX) / 2,
            (originalBox.minY + originalBox.maxY) / 2,
            (originalBox.minZ + originalBox.maxZ) / 2
        );

        // Create a test box with the base offset
        AABB testBox = originalBox.move(originalCenter.reverse());
        OrientedBoundingBox testOBB = OrientedBoundingBox.fromAABB(testBox, gravityDir, baseOffset);

        // Calculate the transformed dimensions
        double transformedWidth = testOBB.maxX - testOBB.minX;
        double transformedHeight = testOBB.maxY - testOBB.minY;
        double transformedDepth = testOBB.maxZ - testOBB.minZ;

        // Calculate the center offset between original and transformed
        Vec3 transformedCenter = new Vec3(
            (testOBB.minX + testOBB.maxX) / 2,
            (testOBB.minY + testOBB.maxY) / 2,
            (testOBB.minZ + testOBB.maxZ) / 2
        );

        // Calculate the difference in centers
        Vec3 centerDifference = transformedCenter.subtract(originalCenter);

        // Adjust the offset to compensate for the center difference
        // We want to move the transformed box back to align with the original center
        Vec3 adjustedOffset = baseOffset.subtract(centerDifference.scale(0.5));

        // Adjust based on gravity direction
        // For diagonal gravity, we need to adjust the offset differently
        if (Math.abs(gravityDir.x) > 0.01 && Math.abs(gravityDir.y) > 0.01) {
            // For gravity with X and Y components, adjust Y offset
            adjustedOffset = new Vec3(
                adjustedOffset.x,
                adjustedOffset.y + 0.05 * Math.signum(gravityDir.y),
                adjustedOffset.z
            );
        }

        if (Math.abs(gravityDir.z) > 0.01 && Math.abs(gravityDir.y) > 0.01) {
            // For gravity with Z and Y components, adjust Z offset
            adjustedOffset = new Vec3(
                adjustedOffset.x,
                adjustedOffset.y,
                adjustedOffset.z + 0.05 * Math.signum(gravityDir.z)
            );
        }

        // Ensure the offset is within reasonable bounds
        double maxOffset = Math.max(originalWidth, Math.max(originalHeight, originalDepth)) * 0.2;
        adjustedOffset = new Vec3(
            Math.max(-maxOffset, Math.min(maxOffset, adjustedOffset.x)),
            Math.max(-maxOffset, Math.min(maxOffset, adjustedOffset.y)),
            Math.max(-maxOffset, Math.min(maxOffset, adjustedOffset.z))
        );

        return adjustedOffset;
    }

    /**
     * Helper method to shrink an AABB proportionally from its center, with different factors for each axis.
     * 
     * @param box The AABB to shrink
     * @param factors A Vec3 containing the shrink factors for each axis (1.0 = no shrinking)
     * @return The shrunk AABB
     */
    private static AABB shrinkAABBPerAxis(AABB box, Vec3 factors) {
        // If all factors are >= 1.0, no shrinking is needed
        if (factors.x >= 1.0 && factors.y >= 1.0 && factors.z >= 1.0) return box;

        // Calculate the center of the box
        Vec3 center = new Vec3(
            (box.minX + box.maxX) / 2,
            (box.minY + box.maxY) / 2,
            (box.minZ + box.maxZ) / 2
        );

        // Calculate the dimensions of the box
        double width = box.maxX - box.minX;
        double height = box.maxY - box.minY;
        double depth = box.maxZ - box.minZ;

        // Calculate the new dimensions, applying different factors for each axis
        double newWidth = width * factors.x;
        double newHeight = height * factors.y;
        double newDepth = depth * factors.z;

        // Calculate the new min and max coordinates
        double newMinX = center.x - (newWidth / 2);
        double newMinY = center.y - (newHeight / 2);
        double newMinZ = center.z - (newDepth / 2);
        double newMaxX = center.x + (newWidth / 2);
        double newMaxY = center.y + (newHeight / 2);
        double newMaxZ = center.z + (newDepth / 2);

        // Create and return the new AABB
        return new AABB(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
    }

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction.
     * If an entity context is set, it will use that entity to calculate the shrink factor.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBB(AABB box, Vec3 gravityDir) {
        Entity entityContext = getCurrentEntityContext();
        if (entityContext != null) {
            return transformToOBB(box, gravityDir, entityContext);
        }
        return transformToOBB(box, gravityDir, DEFAULT_DIAGONAL_SHRINK_FACTOR);
    }

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction and entity.
     * This method uses dynamic per-axis shrink factors and offsets calculated for the entity,
     * which are optimized to minimize the difference between the original and transformed
     * bounding boxes.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @param entity The entity that owns this bounding box
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBB(AABB box, Vec3 gravityDir, Entity entity) {
        if (entity == null) {
            return transformToOBB(box, gravityDir);
        }

        // For default gravity, use standard shrink factors and offsets
        if (gravityDir.equals(new Vec3(0, -1, 0))) {
            Vec3 shrinkFactors = calculateShrinkFactors(entity);
            Vec3 offset = calculateOffset(entity);
            return transformToOBBWithFactorsAndOffset(box, gravityDir, shrinkFactors, offset);
        }

        // For non-default gravity, use dynamic calculations
        // Calculate dynamic per-axis shrink factors
        Vec3 dynamicShrinkFactors = calculateDynamicShrinkFactors(entity, gravityDir, box);

        // Calculate dynamic offset
        Vec3 dynamicOffset = calculateDynamicOffset(entity, gravityDir, box);

        return transformToOBBWithFactorsAndOffset(box, gravityDir, dynamicShrinkFactors, dynamicOffset);
    }

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction and entity,
     * using dynamic shrink factors and offsets that are calculated to minimize the difference
     * between the original and transformed bounding boxes.
     *
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @param entity The entity that owns this bounding box
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBBDynamic(AABB box, Vec3 gravityDir, Entity entity) {
        if (entity == null) {
            return transformToOBB(box, gravityDir);
        }

        // For default gravity, no special handling is needed
        if (gravityDir.equals(new Vec3(0, -1, 0))) {
            return transformToOBB(box, gravityDir, entity);
        }

        // Calculate dynamic per-axis shrink factors
        Vec3 dynamicShrinkFactors = calculateDynamicShrinkFactors(entity, gravityDir, box);

        // Calculate dynamic offset
        Vec3 dynamicOffset = calculateDynamicOffset(entity, gravityDir, box);

        return transformToOBBWithFactorsAndOffset(box, gravityDir, dynamicShrinkFactors, dynamicOffset);
    }

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction and shrink factor.
     * This method uses the same shrink factor for all axes.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @param shrinkFactor The factor to shrink the box by for diagonal gravity (1.0 = no shrinking)
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBB(AABB box, Vec3 gravityDir, double shrinkFactor) {
        // Use the same shrink factor for all axes
        return transformToOBBWithFactors(box, gravityDir, new Vec3(shrinkFactor, shrinkFactor, shrinkFactor));
    }

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction and per-axis shrink factors.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @param shrinkFactors A Vec3 containing the shrink factors for each axis (1.0 = no shrinking)
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBBWithFactors(AABB box, Vec3 gravityDir, Vec3 shrinkFactors) {
        return transformToOBBWithFactorsAndOffset(box, gravityDir, shrinkFactors, Vec3.ZERO);
    }

    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction, per-axis shrink factors, and offset.
     * The offset is applied after rotation to align the box with the entity model.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @param shrinkFactors A Vec3 containing the shrink factors for each axis (1.0 = no shrinking)
     * @param offset The offset to apply after rotation
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBBWithFactorsAndOffset(AABB box, Vec3 gravityDir, Vec3 shrinkFactors, Vec3 offset) {
        if (box == null) return null;

        // Check if gravity is default (downward)
        boolean isDefaultGravity = gravityDir.y < -0.99 && gravityDir.x == 0 && gravityDir.z == 0;
        if (isDefaultGravity) {
            // For default gravity, create a simple OBB that's aligned with the world axes
            Vec3 center = new Vec3(
                (box.minX + box.maxX) / 2,
                (box.minY + box.maxY) / 2,
                (box.minZ + box.maxZ) / 2
            );

            // Apply offset for default gravity
            if (offset.x != 0 || offset.y != 0 || offset.z != 0) {
                center = center.add(offset);
            }

            return new OrientedBoundingBox(box, Rotor.identity(), center);
        }

        // For diagonal gravity directions, shrink the box before transformation
        // This helps reduce the inflated collision box size
        AABB boxToTransform = box;

        // Check if this is a diagonal gravity direction
        boolean isDiagonalGravity = Math.abs(gravityDir.x) > 0.01 || Math.abs(gravityDir.z) > 0.01;
        if (isDiagonalGravity && (shrinkFactors.x < 1.0 || shrinkFactors.y < 1.0 || shrinkFactors.z < 1.0)) {
            boxToTransform = shrinkAABBPerAxis(box, shrinkFactors);
        }

        // Create a cache key that includes the offset
        Triple<AABB, Vec3, Vec3> key = Triple.of(boxToTransform, gravityDir, offset);

        // Check cache first - we need to modify the cache to use Triple instead of Pair to include the offset
        OrientedBoundingBox cachedOBB = null;
        // Since we can't easily modify the cache type, we'll skip caching for non-zero offsets
        if (offset.equals(Vec3.ZERO)) {
            Pair<AABB, Vec3> simpleKey = Pair.of(boxToTransform, gravityDir);
            cachedOBB = OBB_CACHE.get(simpleKey);
            if (cachedOBB != null) {
                return cachedOBB;
            }
        }

        // Create a new OrientedBoundingBox with the offset
        OrientedBoundingBox obb = OrientedBoundingBox.fromAABB(boxToTransform, gravityDir, offset);

        // Only cache if offset is zero
        if (offset.equals(Vec3.ZERO)) {
            // Manage cache size
            if (OBB_CACHE.size() >= MAX_CACHE_SIZE) {
                // Remove a random entry if cache is full
                OBB_CACHE.remove(OBB_CACHE.keySet().iterator().next());
            }

            // Cache the result
            Pair<AABB, Vec3> simpleKey = Pair.of(boxToTransform, gravityDir);
            OBB_CACHE.put(simpleKey, obb);
        }

        return obb;
    }

    /**
     * Checks if two AABBs intersect, taking into account the gravity direction.
     * This is more accurate than the standard AABB.intersects method for non-standard gravity.
     * 
     * @param box1 The first AABB
     * @param box2 The second AABB
     * @param gravityDir The gravity direction vector
     * @return True if the boxes intersect, false otherwise
     */
    public static boolean intersects(AABB box1, AABB box2, Vec3 gravityDir) {
        // Check if gravity is default (downward)
        boolean isDefaultGravity = gravityDir.y < -0.99 && gravityDir.x == 0 && gravityDir.z == 0;
        if (isDefaultGravity) {
            // For default gravity, use the standard AABB intersection test
            return box1.intersects(box2);
        }

        // Transform the first box to an OrientedBoundingBox
        OrientedBoundingBox obb = transformToOBB(box1, gravityDir);

        // Check if the OBB intersects with the second AABB
        return obb.intersects(box2);
    }

    /**
     * Clears the OBB cache.
     * This should be called when resources are being reloaded or when the cache is no longer needed.
     */
    public static void clearCache() {
        OBB_CACHE.clear();
    }
}
