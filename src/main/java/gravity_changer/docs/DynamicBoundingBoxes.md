# Dynamic Bounding Box Optimization for Arbitrary Gravity Directions

## Overview

This document explains the implementation of dynamic bounding box optimization in the GravityChanger mod. This feature automatically calculates optimal shrink factors and offsets for entity bounding boxes based on their characteristics, current gravity direction, and original dimensions, resulting in more accurate collision detection and better visual representation.

## Background

When entities have a non-default gravity direction, especially diagonal gravity, their bounding boxes can become significantly larger than necessary due to the transformation from an axis-aligned bounding box (AABB) to an oriented bounding box (OBB). This can cause various issues:

1. **Collision Detection Problems**: Entities may get stuck in tight spaces or fail to fit through gaps they should be able to pass through.
2. **Visual Discrepancies**: The debug hitbox rendering may show a much larger box than the entity's actual model.
3. **Gameplay Inconsistencies**: The inflated bounding box can make movement and interaction feel inconsistent between different gravity directions.

Previously, we implemented fixed shrink factors based on entity type and per-axis shrinking to mitigate these issues. While these approaches helped, they didn't fully address the problem, as the optimal shrink factors and offsets can vary based on the specific gravity direction and entity dimensions.

## Implementation

The dynamic bounding box optimization system analyzes the entity's original bounding box and gravity direction to calculate optimal shrink factors and offsets that minimize the difference between the original and transformed bounding boxes.

### Key Components

1. **Dynamic Shrink Factor Calculation**
   - Added a method `calculateDynamicShrinkFactor` that finds the optimal uniform shrink factor
   - Added a method `calculateDynamicShrinkFactors` that calculates per-axis shrink factors
   - These methods analyze the entity's original bounding box and gravity direction to determine the best shrink factors

2. **Dynamic Offset Calculation**
   - Added a method `calculateDynamicOffset` that computes the optimal offset to align the transformed box with the entity model
   - This method takes into account the entity's characteristics, gravity direction, and original bounding box

3. **Integration with Existing System**
   - Updated the `transformToOBB` method to use dynamic calculations for non-default gravity directions
   - Maintained backward compatibility with existing code

### How It Works

1. **For Default Gravity (Down)**
   - No shrinking or offsetting is needed, as the bounding box is already aligned with the world axes

2. **For Cardinal Directions (Aligned with Axes)**
   - Minimal shrinking is applied (0.95 factor)
   - Standard offsets based on entity type are used

3. **For Diagonal Gravity Directions**
   - **Dynamic Shrink Factor Calculation**:
     - Tests different shrink factors and finds the one that results in a transformed box with a volume closest to the original
     - Takes into account the entity's proportions (tall, wide, or deep)
     - Adjusts factors based on gravity alignment with each axis

   - **Dynamic Offset Calculation**:
     - Starts with the base offset for the entity type
     - Creates a test box and analyzes the transformed result
     - Adjusts the offset to compensate for center differences
     - Makes additional adjustments based on gravity direction
     - Ensures the offset is within reasonable bounds

## Usage

The dynamic bounding box optimization is automatically applied whenever an entity's bounding box is transformed for a non-default gravity direction. No additional code changes are needed to use this feature.

```java
// The existing code already uses the dynamic calculations
OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir, entity);
```

## Benefits

The dynamic bounding box optimization provides several benefits:

1. **More Accurate Collision Detection**: By minimizing the difference between the original and transformed bounding boxes, entities can navigate through tight spaces more naturally, regardless of gravity direction.

2. **Better Visual Representation**: The debug hitbox rendering more accurately reflects the entity's actual model, making it easier to understand collision boundaries.

3. **Consistent Gameplay Experience**: Movement and interaction feel more consistent across different gravity directions, as the bounding box size and alignment are optimized for each specific case.

4. **Adaptive to Entity Characteristics**: The system takes into account the entity's type, proportions, and original dimensions, providing tailored optimization for each entity.

## Conclusion

The dynamic bounding box optimization system enhances the GravityChanger mod by providing more accurate and consistent collision detection and visual representation for entities with non-default gravity directions. This results in a better gameplay experience, especially when using diagonal gravity directions.