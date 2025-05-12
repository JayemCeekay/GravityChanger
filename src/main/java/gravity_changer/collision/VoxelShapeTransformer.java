package gravity_changer.collision;

import com.google.common.collect.Maps;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for transforming VoxelShapes between coordinate systems.
 * This is used for off-axis collision detection with arbitrary gravity directions.
 */
public class VoxelShapeTransformer {
    
    // Cache for transformed shapes to avoid recalculating them every frame
    private static final Map<Pair<VoxelShape, Vec3>, VoxelShape> SHAPE_CACHE = Maps.newHashMap();
    
    // Maximum cache size to prevent memory leaks
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * Transforms a VoxelShape from world space to player space based on the gravity direction.
     * 
     * @param shape The VoxelShape to transform
     * @param gravityDir The gravity direction vector
     * @return The transformed VoxelShape
     */
    public static VoxelShape transformVoxelShape(VoxelShape shape, Vec3 gravityDir) {
        if (shape.isEmpty()) return shape;
        
        // Check if gravity is default (downward)
        boolean isDefaultGravity = gravityDir.y < -0.99 && gravityDir.x == 0 && gravityDir.z == 0;
        if (isDefaultGravity) return shape;
        
        // Check cache first
        Pair<VoxelShape, Vec3> key = Pair.of(shape, gravityDir);
        VoxelShape cachedShape = SHAPE_CACHE.get(key);
        if (cachedShape != null) {
            return cachedShape;
        }
        
        // Get all the individual boxes that make up this shape
        List<AABB> boxes = new ArrayList<>();
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> 
            boxes.add(new AABB(x1, y1, z1, x2, y2, z2))
        );
        
        // Transform each box and create a new combined shape
        VoxelShape result = Shapes.empty();
        for (AABB box : boxes) {
            AABB rotatedBox = RotationUtil.boxWorldToPlayerVec(box, gravityDir);
            VoxelShape rotatedPart = Shapes.create(rotatedBox);
            result = Shapes.or(result, rotatedPart);
        }
        
        // Manage cache size
        if (SHAPE_CACHE.size() >= MAX_CACHE_SIZE) {
            // Remove a random entry if cache is full
            SHAPE_CACHE.remove(SHAPE_CACHE.keySet().iterator().next());
        }
        
        // Cache the result
        SHAPE_CACHE.put(key, result);
        
        return result;
    }
    
    /**
     * Transforms a VoxelShape from player space to world space based on the gravity direction.
     * 
     * @param shape The VoxelShape to transform
     * @param gravityDir The gravity direction vector
     * @return The transformed VoxelShape
     */
    public static VoxelShape inverseTransformVoxelShape(VoxelShape shape, Vec3 gravityDir) {
        if (shape.isEmpty()) return shape;
        
        // Check if gravity is default (downward)
        boolean isDefaultGravity = gravityDir.y < -0.99 && gravityDir.x == 0 && gravityDir.z == 0;
        if (isDefaultGravity) return shape;
        
        // Get all the individual boxes that make up this shape
        List<AABB> boxes = new ArrayList<>();
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> 
            boxes.add(new AABB(x1, y1, z1, x2, y2, z2))
        );
        
        // Transform each box and create a new combined shape
        VoxelShape result = Shapes.empty();
        for (AABB box : boxes) {
            AABB rotatedBox = RotationUtil.boxPlayerToWorldVec(box, gravityDir);
            VoxelShape rotatedPart = Shapes.create(rotatedBox);
            result = Shapes.or(result, rotatedPart);
        }
        
        return result;
    }
    
    /**
     * Clears the shape cache.
     * This should be called when resources are being reloaded or when the cache is no longer needed.
     */
    public static void clearCache() {
        SHAPE_CACHE.clear();
    }
}