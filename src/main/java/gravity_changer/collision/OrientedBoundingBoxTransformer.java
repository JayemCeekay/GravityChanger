package gravity_changer.collision;

import com.google.common.collect.Maps;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

/**
 * Utility class for transforming between AABBs and OrientedBoundingBoxes.
 * This is used for off-axis collision detection with arbitrary gravity directions.
 */
public class OrientedBoundingBoxTransformer {
    
    // Cache for transformed boxes to avoid recalculating them every frame
    private static final Map<Pair<AABB, Vec3>, OrientedBoundingBox> OBB_CACHE = Maps.newHashMap();
    
    // Maximum cache size to prevent memory leaks
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * Transforms an AABB to an OrientedBoundingBox based on the gravity direction.
     * 
     * @param box The AABB to transform
     * @param gravityDir The gravity direction vector
     * @return The transformed OrientedBoundingBox
     */
    public static OrientedBoundingBox transformToOBB(AABB box, Vec3 gravityDir) {
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
            return new OrientedBoundingBox(box, gravity_changer.util.Rotor.identity(), center);
        }
        
        // Check cache first
        Pair<AABB, Vec3> key = Pair.of(box, gravityDir);
        OrientedBoundingBox cachedOBB = OBB_CACHE.get(key);
        if (cachedOBB != null) {
            return cachedOBB;
        }
        
        // Create a new OrientedBoundingBox
        OrientedBoundingBox obb = OrientedBoundingBox.fromAABB(box, gravityDir);
        
        // Manage cache size
        if (OBB_CACHE.size() >= MAX_CACHE_SIZE) {
            // Remove a random entry if cache is full
            OBB_CACHE.remove(OBB_CACHE.keySet().iterator().next());
        }
        
        // Cache the result
        OBB_CACHE.put(key, obb);
        
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
     * Transforms an OrientedBoundingBox to an AABB that contains it.
     * This is useful for broad-phase collision detection.
     * 
     * @param obb The OrientedBoundingBox to transform
     * @return The bounding AABB
     */
    public static AABB transformToAABB(OrientedBoundingBox obb) {
        return obb.getBoundingAABB();
    }
    
    /**
     * Clears the OBB cache.
     * This should be called when resources are being reloaded or when the cache is no longer needed.
     */
    public static void clearCache() {
        OBB_CACHE.clear();
    }
}