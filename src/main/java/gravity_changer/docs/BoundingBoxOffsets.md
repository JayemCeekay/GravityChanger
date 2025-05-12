# Bounding Box Offsets for Entity Models

## Overview

This document explains the implementation of bounding box offsets in the GravityChanger mod. This feature allows offsets to be applied to entity bounding boxes after rotation to align them with entity models, particularly when entities have non-default gravity directions.

## Background

When entities have a non-default gravity direction, their bounding boxes are rotated to match the gravity direction. However, this rotation can sometimes cause the bounding box to be misaligned with the entity model, resulting in visual discrepancies and gameplay issues.

Previously, we implemented per-axis shrinking to reduce the inflated collision box size for diagonal gravity directions. While this helped with collision issues, it didn't address the alignment problem between the bounding box and the entity model.

## Implementation

The bounding box offset feature allows offsets to be applied to entity bounding boxes after rotation to align them with entity models. This is particularly useful for entities that have models that don't perfectly align with their bounding boxes, such as players and minecarts.

### Key Components

1. **OrientedBoundingBox**
   - Added a new overload of the `fromAABB` method that accepts an offset parameter
   - The offset is applied after rotation to align the box with the entity model

2. **OrientedBoundingBoxTransformer**
   - Added a cache for entity-specific offsets
   - Added a method `calculateOffset(Entity)` that returns a Vec3 of offsets for each axis
   - Added a new method `transformToOBBWithFactorsAndOffset` that applies both shrink factors and offsets
   - Updated the existing `transformToOBB(AABB, Vec3, Entity)` method to use entity-specific offsets

3. **EntityRenderDispatcherMixin**
   - Updated to use the entity-specific shrink factors and offsets for debug hitbox rendering

### Default Offsets

The default offsets are calculated based on the entity type:

- **Players**: (0, 0.1, 0) - Offset slightly upward to align with the model
- **Minecarts**: (0, -0.1, 0) - Offset downward to align with the model
- **Living Entities**: (0, 0.05, 0) - Slight upward offset
- **Other Entities**: (0, 0, 0) - No offset

These values can be adjusted in the `calculateOffset` method in the `OrientedBoundingBoxTransformer` class.

## Usage

The offset functionality is automatically applied when using the entity-specific transformation methods:

```java
// Get the entity-specific offset and apply it
OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir, entity);
```

You can also specify a custom offset:

```java
// Create a Vec3 with custom offsets
Vec3 offset = new Vec3(0, 0.2, 0);

// Use the transformToOBBWithFactorsAndOffset method
OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBBWithFactorsAndOffset(box, gravityDir, shrinkFactors, offset);
```

## Benefits

The bounding box offset feature provides several benefits:

1. **Better Visual Alignment**: The bounding box is better aligned with the entity model, resulting in a more accurate visual representation.

2. **Improved Gameplay**: Entities can interact with the world more naturally, as their bounding boxes better match their visual appearance.

3. **Entity-Specific Customization**: Different entity types can have different offsets, allowing for better customization based on the entity's characteristics.

## Conclusion

The bounding box offset feature enhances the visual and gameplay experience in the GravityChanger mod by allowing better alignment between entity bounding boxes and their models. This is particularly important for non-default gravity directions, where the rotation can cause misalignment issues.