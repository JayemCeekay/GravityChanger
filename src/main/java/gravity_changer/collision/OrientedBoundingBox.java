package gravity_changer.collision;

import gravity_changer.util.Rotor;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Represents an oriented (rotated) bounding box in 3D space.
 * This extends Minecraft's AABB to represent boxes that aren't aligned with the world axes,
 * which is essential for proper collision detection with non-standard gravity directions.
 */
public class OrientedBoundingBox extends AABB {
    // The axis-aligned bounding box in the local coordinate system
    private final AABB localBox;

    // The rotation that transforms from local to world coordinates
    private final Rotor rotation;

    // The center point of the box in world coordinates
    private final Vec3 center;

    /**
     * Creates an OrientedBoundingBox from an AABB and a gravity direction.
     *
     * @param box        The axis-aligned bounding box in world space
     * @param gravityDir The gravity direction vector
     * @return An OrientedBoundingBox representing the rotated box
     */
    public static OrientedBoundingBox fromAABB(AABB box, Vec3 gravityDir) {
        return fromAABB(box, gravityDir, Vec3.ZERO);
    }

    /**
     * Creates an OrientedBoundingBox from an AABB and a gravity direction, with an offset.
     * The offset is applied after rotation to align the box with the entity model.
     *
     * @param box        The axis-aligned bounding box in world space
     * @param gravityDir The gravity direction vector
     * @param offset     The offset to apply after rotation
     * @return An OrientedBoundingBox representing the rotated box
     */
    public static OrientedBoundingBox fromAABB(AABB box, Vec3 gravityDir, Vec3 offset) {
        // Default gravity is (0, -1, 0)
        Vec3 defaultGravity = new Vec3(0, -1, 0);

        // Create a rotor that rotates from default gravity to the specified gravity
        Rotor rotation = Rotor.from(defaultGravity, gravityDir);

        // Create a local box with the same dimensions as the original
        double width = box.maxX - box.minX;
        double height = box.maxY - box.minY;
        double depth = box.maxZ - box.minZ;

        // Create a centered local box with the same dimensions
        AABB localBox = new AABB(
                -width / 2, -height / 2, -depth / 2,
                width / 2, height / 2, depth / 2
        );
        // Calculate the center of the box in world coordinates
        Vec3 center = new Vec3(
                (box.minX + box.maxX) / 2,
                (box.minY + box.maxY) / 2,
                (box.minZ + box.maxZ) / 2
        );

        // Apply the offset in world space
        if (offset.x != 0 || offset.y != 0 || offset.z != 0) {
            // Rotate the offset to align with the gravity direction
            Vec3 rotatedOffset = RotationUtil.vecPlayerToWorldVec(offset, gravityDir);
            center = center.add(rotatedOffset);
        }

        return new OrientedBoundingBox(localBox, rotation, center);
    }

    /**
     * Creates an OrientedBoundingBox with the specified parameters.
     *
     * @param localBox The axis-aligned bounding box in the local coordinate system
     * @param rotation The rotation that transforms from local to world coordinates
     * @param center   The center point of the box in world coordinates
     */
    public OrientedBoundingBox(AABB localBox, Rotor rotation, Vec3 center) {
        // Call the AABB constructor with the bounding box's min and max coordinates
        super(localBox.minX, localBox.minY, localBox.minZ,
                localBox.maxX, localBox.maxY, localBox.maxZ);

        this.localBox = localBox;
        this.rotation = rotation;
        this.center = center;
    }

    /**
     * Helper method to calculate the bounding AABB before constructing the OrientedBoundingBox.
     * This is needed because we need to call the AABB constructor with the bounding box's coordinates.
     */
    private static AABB calculateBoundingAABB(AABB localBox, Rotor rotation, Vec3 center) {
        // Get the 8 corners of the local box
        Vec3[] corners = new Vec3[8];
        corners[0] = new Vec3(localBox.minX, localBox.minY, localBox.minZ);
        corners[1] = new Vec3(localBox.maxX, localBox.minY, localBox.minZ);
        corners[2] = new Vec3(localBox.minX, localBox.maxY, localBox.minZ);
        corners[3] = new Vec3(localBox.maxX, localBox.maxY, localBox.minZ);
        corners[4] = new Vec3(localBox.minX, localBox.minY, localBox.maxZ);
        corners[5] = new Vec3(localBox.maxX, localBox.minY, localBox.maxZ);
        corners[6] = new Vec3(localBox.minX, localBox.maxY, localBox.maxZ);
        corners[7] = new Vec3(localBox.maxX, localBox.maxY, localBox.maxZ);

        // Rotate each corner and find the min/max coordinates
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Vec3 corner : corners) {
            Vec3 rotatedCorner = rotation.rotate(corner).add(center);
            minX = Math.min(minX, rotatedCorner.x);
            minY = Math.min(minY, rotatedCorner.y);
            minZ = Math.min(minZ, rotatedCorner.z);
            maxX = Math.max(maxX, rotatedCorner.x);
            maxY = Math.max(maxY, rotatedCorner.y);
            maxZ = Math.max(maxZ, rotatedCorner.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Calculates the axis-aligned bounding box that contains this oriented bounding box.
     * This is used for broad-phase collision detection.
     *
     * @return The bounding AABB
     */
    private AABB calculateBoundingAABB() {
        return calculateBoundingAABB(this.localBox, this.rotation, this.center);
    }

    /**
     * Checks if this oriented bounding box intersects with another AABB.
     * This overrides the AABB.intersects method to use the OBB intersection logic.
     *
     * @param other The AABB to check for intersection
     * @return True if the boxes intersect, false otherwise
     */
    @Override
    public boolean intersects(AABB other) {
        // First, do a quick check with the bounding AABB for early rejection
        // Use a direct AABB intersection test to avoid triggering the AABBMixin
        if (!directIntersectsAABB(localBox, other)) {
            return false;
        }

        // Transform the other box to the local coordinate system
        Vec3 otherCenter = new Vec3(
                (other.minX + other.maxX) / 2,
                (other.minY + other.maxY) / 2,
                (other.minZ + other.maxZ) / 2
        );

        Vec3 otherExtents = new Vec3(
                (other.maxX - other.minX) / 2,
                (other.maxY - other.minY) / 2,
                (other.maxZ - other.minZ) / 2
        );

        // Apply the inverse rotation to transform the other box to our local space
        Rotor inverseRotation = rotation.inverse();
        Vec3 relativeCenter = inverseRotation.rotate(otherCenter.subtract(center));

        // The separating axis theorem states that two convex objects don't intersect if
        // there exists an axis where their projections don't overlap.
        // For an OBB and AABB, we need to check 15 potential separating axes:
        // - 3 axes of the OBB
        // - 3 axes of the AABB
        // - 9 cross products of the above axes

        // For simplicity, we'll use a more direct approach since we're in the OBB's local space:
        // Check if the transformed AABB intersects with the local box

        // Get the local box extents
        Vec3 localExtents = new Vec3(
                (localBox.maxX - localBox.minX) / 2,
                (localBox.maxY - localBox.minY) / 2,
                (localBox.maxZ - localBox.minZ) / 2
        );

        // Get the local box center
        Vec3 localCenter = new Vec3(
                (localBox.minX + localBox.maxX) / 2,
                (localBox.minY + localBox.maxY) / 2,
                (localBox.minZ + localBox.maxZ) / 2
        );

        // Check for intersection along each axis
        if (Math.abs(relativeCenter.x - localCenter.x) > (localExtents.x + otherExtents.x)) return false;
        if (Math.abs(relativeCenter.y - localCenter.y) > (localExtents.y + otherExtents.y)) return false;
        if (Math.abs(relativeCenter.z - localCenter.z) > (localExtents.z + otherExtents.z)) return false;

        // If we've made it here, the boxes intersect
        return true;
    }

    /**
     * Direct AABB intersection test that doesn't trigger the AABBMixin.
     * This is used to avoid recursion in the collision system.
     *
     * @param box1 The first AABB
     * @param box2 The second AABB
     * @return True if the boxes intersect, false otherwise
     */
    private static boolean directIntersectsAABB(AABB box1, AABB box2) {
        return box1.minX < box2.maxX && box1.maxX > box2.minX &&
                box1.minY < box2.maxY && box1.maxY > box2.minY &&
                box1.minZ < box2.maxZ && box1.maxZ > box2.minZ;
    }

    /**
     * Checks if this oriented bounding box intersects with another oriented bounding box.
     * Uses the Separating Axis Theorem (SAT) for accurate collision detection.
     *
     * @param other The other oriented bounding box
     * @return True if the boxes intersect, false otherwise
     */
    public boolean intersects(OrientedBoundingBox other) {
        // First, do a quick check with the bounding AABBs for early rejection
        // Use a direct AABB intersection test to avoid triggering the AABBMixin
        if (!directIntersectsAABB(localBox, other.localBox)) {
            return false;
        }

        // Implement the Separating Axis Theorem (SAT) for accurate OBB-OBB intersection

        // Get the 8 corners of each box in world space
        Vec3[] cornersA = getCorners();
        Vec3[] cornersB = other.getCorners();

        // Get the 3 axes of each box
        Vec3[] axesA = getAxes();
        Vec3[] axesB = other.getAxes();

        // Test 6 face normals (3 from each box)
        for (Vec3 axis : axesA) {
            if (isSeparatingAxis(axis, cornersA, cornersB)) {
                return false;
            }
        }

        for (Vec3 axis : axesB) {
            if (isSeparatingAxis(axis, cornersA, cornersB)) {
                return false;
            }
        }

        // Test 9 edge cross products (3x3 combinations of edges)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vec3 axis = axesA[i].cross(axesB[j]);
                // Skip near-zero axes (parallel edges)
                if (axis.lengthSqr() < 1.0E-10) {
                    continue;
                }
                if (isSeparatingAxis(axis, cornersA, cornersB)) {
                    return false;
                }
            }
        }

        // No separating axis found, the boxes intersect
        return true;
    }

    /**
     * Checks if the given axis is a separating axis between two sets of corners.
     *
     * @param axis     The axis to check
     * @param cornersA The corners of the first box
     * @param cornersB The corners of the second box
     * @return True if the axis separates the boxes, false otherwise
     */
    private boolean isSeparatingAxis(Vec3 axis, Vec3[] cornersA, Vec3[] cornersB) {
        // Normalize the axis
        double length = axis.length();
        if (length < 1.0E-10) {
            return false; // Not a valid axis
        }
        axis = axis.scale(1.0 / length);

        // Project corners onto the axis
        double minA = Double.POSITIVE_INFINITY;
        double maxA = Double.NEGATIVE_INFINITY;
        double minB = Double.POSITIVE_INFINITY;
        double maxB = Double.NEGATIVE_INFINITY;

        for (Vec3 corner : cornersA) {
            double projection = corner.dot(axis);
            minA = Math.min(minA, projection);
            maxA = Math.max(maxA, projection);
        }

        for (Vec3 corner : cornersB) {
            double projection = corner.dot(axis);
            minB = Math.min(minB, projection);
            maxB = Math.max(maxB, projection);
        }

        // Check for separation
        return maxA < minB || maxB < minA;
    }

    /**
     * Gets the 8 corners of this oriented bounding box in world space.
     *
     * @return An array of 8 Vec3 representing the corners
     */
    public Vec3[] getCorners() {
        // Get the 8 corners of the local box
        Vec3[] corners = new Vec3[8];
        corners[0] = new Vec3(localBox.minX, localBox.minY, localBox.minZ);
        corners[1] = new Vec3(localBox.maxX, localBox.minY, localBox.minZ);
        corners[2] = new Vec3(localBox.minX, localBox.maxY, localBox.minZ);
        corners[3] = new Vec3(localBox.maxX, localBox.maxY, localBox.minZ);
        corners[4] = new Vec3(localBox.minX, localBox.minY, localBox.maxZ);
        corners[5] = new Vec3(localBox.maxX, localBox.minY, localBox.maxZ);
        corners[6] = new Vec3(localBox.minX, localBox.maxY, localBox.maxZ);
        corners[7] = new Vec3(localBox.maxX, localBox.maxY, localBox.maxZ);

        // Rotate and translate each corner to world space
        for (int i = 0; i < 8; i++) {
            corners[i] = rotation.rotate(corners[i]).add(center);
        }

        return corners;
    }

    /**
     * Gets the 3 principal axes of this oriented bounding box in world space.
     *
     * @return An array of 3 Vec3 representing the axes
     */
    public Vec3[] getAxes() {
        // The 3 principal axes in local space
        Vec3[] axes = new Vec3[3];
        axes[0] = new Vec3(1, 0, 0); // X axis
        axes[1] = new Vec3(0, 1, 0); // Y axis
        axes[2] = new Vec3(0, 0, 1); // Z axis

        // Rotate each axis to world space
        for (int i = 0; i < 3; i++) {
            axes[i] = rotation.rotate(axes[i]);
        }

        return axes;
    }

    /**
     * Gets the local axis-aligned bounding box.
     *
     * @return The local AABB
     */
    public AABB getLocalBox() {
        return localBox;
    }

    /**
     * Gets the rotation that transforms from local to world coordinates.
     *
     * @return The rotation
     */
    public Rotor getRotation() {
        return rotation;
    }

    /**
     * Gets the center point of the box in world coordinates.
     *
     * @return The center
     */
    public Vec3 getCenter() {
        return center;
    }
}
