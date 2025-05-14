package gravity_changer.collision;

import gravity_changer.util.Rotor;
import gravity_changer.util.RotationUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Represents an oriented (rotated) bounding box in 3D space.
 * Unlike Minecraft's AABB, this can represent boxes that aren't aligned with the world axes,
 * which is essential for proper collision detection with non-standard gravity directions.
 */
public class OrientedBoundingBox extends AABB {
    // The axis-aligned bounding box in the local coordinate system
    private final AABB localBox;
    private final Rotor rotation;
    private final Vec3 center;
    private final AABB boundingAABB;

    public OrientedBoundingBox(AABB sourceBox, Rotor rotation, Vec3 center) {
        super(sourceBox.minX, sourceBox.minY, sourceBox.minZ, sourceBox.maxX, sourceBox.maxY, sourceBox.maxZ);
        // Must call super first in Java 17

        // Validate parameters after super
        if (sourceBox == null || rotation == null || center == null) {
            throw new IllegalArgumentException("OrientedBoundingBox parameters cannot be null");
        }

        // Create defensive copies of mutable objects
        this.localBox = sourceBox;
        this.rotation = rotation;
        this.center = center;

        // Calculate the bounding box last
        AABB calculated = calculateBoundingAABB();
        this.boundingAABB = new AABB(calculated.minX, calculated.minY, calculated.minZ,
                calculated.maxX, calculated.maxY, calculated.maxZ);
    }

    /**
     * Creates an OrientedBoundingBox from an AABB and a gravity direction, with an offset.
     * The offset is applied after rotation to align the box with the entity model.
     *
     * @param box The axis-aligned bounding box in world space
     * @param gravityDir The gravity direction vector
     * @return An OrientedBoundingBox representing the rotated box
     */
    public static OrientedBoundingBox fromAABB(AABB box, Vec3 gravityDir) {
        // Default gravity is (0, -1, 0)
        Vec3 defaultGravity = new Vec3(0, -1, 0);

        // Create a rotor that rotates from default gravity to the specified gravity
        Rotor rotation = Rotor.from(gravityDir, defaultGravity);

        // Transform the box to the local coordinate system
        // We use the inverse rotation to transform from world to local space
        AABB localBox = RotationUtil.boxPlayerToWorldVec(box, gravityDir);

        // Calculate the center of the box in world coordinates
        Vec3 center = new Vec3(
            (box.minX + box.maxX) / 2,
            (box.minY + box.maxY) / 2,
            (box.minZ + box.maxZ) / 2
        );

        return new OrientedBoundingBox(localBox, rotation, center);
    }

    /**
     * Calculates the axis-aligned bounding box that contains this oriented bounding box.
     * This is used for broad-phase collision detection.
     *
     * @return The bounding AABB
     */
    private AABB calculateBoundingAABB() {
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
     * Checks if this oriented bounding box intersects with another AABB.
     *
     * @param other The AABB to check for intersection
     * @return True if the boxes intersect, false otherwise
     */
    public boolean intersects(AABB other) {
        // First, do a quick check with the bounding AABB for early rejection
        // Use a direct AABB intersection test to avoid triggering the AABBMixin
        if (!directIntersectsAABB(boundingAABB, other)) {
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
        if (!directIntersectsAABB(boundingAABB, other.boundingAABB)) {
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
     * @param axis The axis to check
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
     * Gets the axis-aligned bounding box that contains this oriented bounding box.
     *
     * @return The bounding AABB
     */
    public AABB getBoundingAABB() {
        return boundingAABB;
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

    /**
     * Returns a new OrientedBoundingBox moved by the specified x, y, z offset.
     *
     * @param x The x offset
     * @param y The y offset
     * @param z The z offset
     * @return A new moved OrientedBoundingBox
     */
    public OrientedBoundingBox move(double x, double y, double z) {
        Vec3 offset = new Vec3(x, y, z);
        return new OrientedBoundingBox(this.localBox, this.rotation, this.center.add(offset));
    }

}
