# Dynamic Shrink Factors for Entity Bounding Boxes

This document explains how to use the dynamic shrink factor functionality for entity bounding boxes in the Gravity Changer mod.

## Overview

When entities have a diagonal gravity direction, their bounding boxes can become too large due to the transformation from an axis-aligned bounding box (AABB) to an oriented bounding box (OBB). This can cause collision issues, such as entities getting stuck in tight spaces or not fitting through gaps they should be able to pass through.

To address this issue, we've implemented a dynamic shrink factor system that can adjust the size of the bounding box based on the entity type and other characteristics.

## How It Works

The `OrientedBoundingBoxTransformer` class now supports entity-specific shrink factors. When transforming an AABB to an OBB, it can take into account the entity type and apply an appropriate shrink factor.

There are three ways to use this functionality:

1. **Direct Method Call**: You can call `OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir, entity)` directly, passing the entity as a parameter.

2. **Entity Context**: You can set the entity context before calling the original `transformToOBB` method, and it will use that entity to calculate the shrink factor:
   ```java
   OrientedBoundingBoxTransformer.setCurrentEntityContext(entity);
   try {
       OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir);
       // Use the OBB...
   } finally {
       OrientedBoundingBoxTransformer.clearCurrentEntityContext();
   }
   ```

3. **Utility Method**: You can use the `GravityCollisionUtil` class, which handles setting and clearing the entity context for you:
   ```java
   OrientedBoundingBox obb = GravityCollisionUtil.transformToOBB(box, gravityDir, entity);
   ```

## Default Shrink Factors

The default shrink factors are:
- Players: 0.85
- Minecarts: 0.9
- Living Entities: 0.8
- Other Entities: 0.85

These values can be adjusted in the `OrientedBoundingBoxTransformer.calculateShrinkFactor` method.

## Custom Shrink Factors

If you need to use a custom shrink factor for a specific entity or situation, you can call:
```java
OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(box, gravityDir, customShrinkFactor);
```

## Implementation Details

The shrink factor is applied only when the gravity direction is diagonal (i.e., not aligned with the world axes). It shrinks the AABB proportionally from its center before transforming it to an OBB.

The `calculateShrinkFactor` method in `OrientedBoundingBoxTransformer` determines the appropriate shrink factor based on the entity type. You can modify this method to add more entity-specific shrink factors or adjust the existing ones.

## Caching

To improve performance, the calculated shrink factors are cached per entity. The cache is cleared when the `clearCache` method is called on the `OrientedBoundingBoxTransformer` class.

## Thread Safety

The entity context is stored in a ThreadLocal variable, so it's safe to use in a multi-threaded environment. However, you should always clear the context after using it to prevent memory leaks.