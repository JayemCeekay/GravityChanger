# Per-Axis Shrinking for Bounding Boxes

## Overview

This document explains the implementation of per-axis shrinking for bounding boxes in the GravityChanger mod. This feature allows different shrink factors to be applied to each axis (X, Y, Z) of an entity's bounding box when the entity has a diagonal gravity direction.

## Background

When entities have a diagonal gravity direction, their bounding boxes can become too large due to the transformation from an axis-aligned bounding box (AABB) to an oriented bounding box (OBB). This can cause collision issues, such as entities getting stuck in tight spaces or not fitting through gaps they should be able to pass through.

Previously, we implemented a uniform shrinking mechanism that applied the same shrink factor to all dimensions of the bounding box. While this helped reduce the inflated collision box size, it wasn't optimal for all entity types, especially those with different proportions in different directions.

## Implementation

The per-axis shrinking feature allows different shrink factors to be applied to each axis of the bounding box. This is particularly useful for entities that have different proportions in different directions, such as minecarts (which are wider than they are tall).

### Key Components

1. **OrientedBoundingBoxTransformer**
   - Added a new method `calculateShrinkFactors(Entity)` that returns a Vec3 of shrink factors for each axis
   - Added a new method `shrinkAABBPerAxis(AABB, Vec3)` that applies different shrink factors to each axis
   - Added a new method `transformToOBBWithFactors(AABB, Vec3, Vec3)` that uses per-axis shrink factors
   - Updated the existing `transformToOBB(AABB, Vec3, Entity)` method to use per-axis shrink factors

2. **GravityCollisionUtil**
   - Added new methods `transformToOBBWithFactors` that provide a simpler interface for using per-axis shrinking

### Default Per-Axis Shrink Factors

The default per-axis shrink factors are calculated based on the entity type:

- **Players**: (0.85, 0.9, 0.85) - Less shrinking on the Y axis to maintain height
- **Minecarts**: (0.9, 0.8, 0.9) - More shrinking on the Y axis since they are wider than tall
- **Living Entities**: (0.8, 0.8, 0.8) - Slightly more shrinking overall
- **Other Entities**: (0.85, 0.85, 0.85) - Default shrink factor

These values can be adjusted in the `calculateShrinkFactors` method in the `OrientedBoundingBoxTransformer` class.

## Usage

There are several ways to use the per-axis shrinking functionality:

1. **Direct Method Call**:
   ```java
   // Create a Vec3 with different shrink factors for each axis
   Vec3 shrinkFactors = new Vec3(0.85, 0.9, 0.85);
   
   // Use the transformToOBBWithFactors method
   OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBBWithFactors(box, gravityDir, shrinkFactors);
   ```

2. **Entity-Based Method**:
   ```java
   // Use the entity-specific per-axis shrink factors
   OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir, entity);
   ```

3. **Utility Method**:
   ```java
   // Use the GravityCollisionUtil class for a simpler interface
   OrientedBoundingBox obb = GravityCollisionUtil.transformToOBBWithFactors(box, gravityDir, shrinkFactors);
   ```

## Benefits

The per-axis shrinking feature provides several benefits:

1. **Better Collision Detection**: By applying different shrink factors to each axis, the bounding box can better match the actual shape of the entity, resulting in more accurate collision detection.

2. **Improved Gameplay**: Entities can now navigate through tight spaces more easily, especially when they have a diagonal gravity direction.

3. **Entity-Specific Customization**: Different entity types can have different per-axis shrink factors, allowing for better customization based on the entity's characteristics.

## Conclusion

The per-axis shrinking feature enhances the collision detection system in the GravityChanger mod by allowing more precise control over the size of bounding boxes. This results in a better gameplay experience, especially when entities have diagonal gravity directions.