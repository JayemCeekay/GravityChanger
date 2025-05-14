package gravity_changer.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Simple rotor class based on geometric algebra for 3D space (G3).
 * Only supports normalized rotation rotors for now.
 */
public class Rotor {
    // Rotor = scalar + bivector (xy, yz, zx planes)
    private final float scalar; // real part
    private final Vec3 bivector; // bivector part (imaginary, like quaternion xyz)

    public Rotor(float scalar, Vec3 bivector) {
        this.scalar = scalar;
        this.bivector = bivector;
    }

    /**
     * Create a rotor that rotates vector `from` to vector `to`.
     */
    public static Rotor from(Vec3 from, Vec3 to) {
        Vec3 f = from.normalize();
        Vec3 t = to.normalize();
        double cosTheta = f.dot(t);

        // Handle special cases with more precision
        if (cosTheta > 0.9999) return new Rotor(1f, new Vec3(0, 0, 0)); // Nearly identical vectors
        if (cosTheta < -0.9999) {
            // 180° rotation around any orthogonal axis
            Vec3 ortho = f.cross(new Vec3(1, 0, 0));
            if (ortho.lengthSqr() < 1e-6) ortho = f.cross(new Vec3(0, 1, 0));
            if (ortho.lengthSqr() < 1e-6) ortho = f.cross(new Vec3(0, 0, 1)); // Extra fallback
            ortho = ortho.normalize();
            return new Rotor(0f, ortho);
        }

        // For diagonal directions, use a more robust approach
        Vec3 axis = f.cross(t); // Rotation axis is perpendicular to both vectors

        // If vectors are nearly parallel, use the bisector method
        if (axis.lengthSqr() < 1e-6) {
            axis = f.add(t).normalize();
            Vec3 biv = f.cross(axis);
            float s = (float) Math.sqrt((1.0 + cosTheta) * 0.5);
            Vec3 bv = biv.normalize().scale((float) Math.sqrt((1.0 - cosTheta) * 0.5));
            return new Rotor(s, bv);
        }

        // Otherwise use the direct axis method
        axis = axis.normalize();
        double angle = Math.acos(cosTheta);
        float s = (float) Math.cos(angle * 0.5);
        Vec3 bv = axis.scale((float) Math.sin(angle * 0.5));
        return new Rotor(s, bv);
    }

    /**
     * Create a rotor from Minecraft yaw and pitch.
     */
    public static Rotor fromYawPitch(float yaw, float pitch) {
        Vec3 look = rotToVec(yaw, pitch);
        return Rotor.from(new Vec3(0, 0, -1), look); // -Z is vanilla forward
    }

    /**
     * Convert this rotor to yaw and pitch.
     */
    public Vec2 toYawPitch() {
        Vec3 forward = this.rotate(new Vec3(0, 0, -1));
        return vecToRot(forward);
    }

    /**
     * Rotate a vector by this rotor.
     * Formula: v' = r v r⁻¹ using geometric algebra.
     */
    public Vec3 rotate(Vec3 vec) {
        Quaternionf q = toQuaternion();
        Vector3f v = vec.toVector3f();
        v.rotate(q);
        return new Vec3(v);
    }

    /**
     * Compose this rotor with another (apply this, then other).
     */
    public Rotor then(Rotor other) {
        Quaternionf q1 = this.toQuaternion();
        Quaternionf q2 = other.toQuaternion();
        Quaternionf composed = new Quaternionf(q2).mul(q1);
        return Rotor.fromQuaternion(composed);
    }

    /**
     * Inverse of this rotor.
     */
    public Rotor inverse() {
        return new Rotor(scalar, bivector.scale(-1));
    }

    /**
     * Spherical linear interpolation between two rotors.
     */
    public static Rotor slerp(Rotor a, Rotor b, float t) {
        Quaternionf qa = a.toQuaternion();
        Quaternionf qb = b.toQuaternion();
        Quaternionf result = new Quaternionf();
        qa.slerp(qb, t, result);
        return Rotor.fromQuaternion(result);
    }

    /**
     * Convert to a Quaternionf for use in Minecraft rendering.
     */
    public Quaternionf toQuaternion() {
        return new Quaternionf((float) bivector.x, (float) bivector.y, (float) bivector.z, scalar);
    }

    /**
     * Create a rotor from a quaternion.
     */
    public static Rotor fromQuaternion(Quaternionf q) {
        return new Rotor(q.w, new Vec3(q.x, q.y, q.z));
    }

    public static Rotor identity() {
        return new Rotor(1f, new Vec3(0, 0, 0));
    }

    /**
     * Convert Minecraft yaw/pitch to forward vector.
     */
    private static Vec3 rotToVec(float yaw, float pitch) {
        float f = (float) Math.cos(-yaw * Math.PI / 180.0 - Math.PI);
        float g = (float) Math.sin(-yaw * Math.PI / 180.0 - Math.PI);
        float h = -(float) Math.cos(-pitch * Math.PI / 180.0);
        float i = (float) Math.sin(-pitch * Math.PI / 180.0);
        return new Vec3(g * h, i, f * h);
    }

    /**
     * Convert a look vector to Minecraft yaw/pitch.
     */
    private static Vec2 vecToRot(Vec3 vec) {
        double f = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        float yaw = (float) (Math.atan2(vec.x, vec.z) * (180F / Math.PI));
        float pitch = (float) (Math.atan2(vec.y, f) * (180F / Math.PI));
        return new Vec2(yaw, pitch);
    }

    @Override
    public String toString() {
        return "Rotor[" + scalar + " + " + bivector + "]";
    }

    public static AABB rotateBoxWithRotor(AABB box, Vec3 gravityVec) {
        // Only apply rotation if we have non-default gravity
        boolean isDefaultGravity = gravityVec.y < -0.99 && gravityVec.x == 0 && gravityVec.z == 0;
        if (isDefaultGravity) {
            return box; // No rotation needed
        }

        // Create rotor from DOWN to gravity direction
        Rotor rotor = Rotor.from(new Vec3(0, -1, 0), gravityVec);

        // Get box center and dimensions
        Vec3 center = box.getCenter();

        center = rotor.rotate(center);

        Vec3 dimensions = new Vec3(
                box.getXsize(),
                box.getYsize(),
                box.getZsize()
        );

        // For player box (0.6, 1.8, 0.6) with gravity (0, 0, -1),
        // we want resulting box to be (0.6, 0.6, 1.8)

        // Calculate all 8 corners of the box in local space
        Vec3[] corners = new Vec3[8];
        int i = 0;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dy = -1; dy <= 1; dy += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    // Create corner offset from center
                    Vec3 localOffset = new Vec3(
                            (dimensions.x * 0.5) * dx,
                            (dimensions.y * 0.5) * dy,
                            (dimensions.z * 0.5) * dz
                    );

                    // Apply rotation
                    Vec3 rotatedOffset = rotor.rotate(localOffset);

                    // Add to corners array (still centered at origin)
                    corners[i++] = center.add(rotatedOffset);
                }
            }
        }

        // Find min/max of rotated corners to create new AABB
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Vec3 corner : corners) {
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }

        AABB newBox = new AABB(
                minX , minY, minZ ,
                maxX , maxY, maxZ);

        assert Math.abs(box.getSize() - newBox.getSize()) < 1e-6 : "Rotation caused unexpected scaling.";
        // Create a new AABB from the rotated corners, transformed back to world space
        return newBox;
    }


    public static AABB inverseRotateBoxWithRotor(AABB box, Vec3 gravityVec) {
        // Use the inverse rotor (from gravity vector back to down)
        Rotor rotor = Rotor.from(gravityVec, new Vec3(0, -1, 0));

        // Step 1: Get the center of the box
        Vec3 center = box.getCenter();

        center = rotor.rotate(center);

        // Step 2: Get the half-size of the box
        Vec3 halfSize = new Vec3(
                (box.maxX - box.minX) * 0.5,
                (box.maxY - box.minY) * 0.5,
                (box.maxZ - box.minZ) * 0.5
        );

        // Step 3: Calculate all 8 corners of the box in local space (relative to center)
        Vec3[] corners = new Vec3[8];
        int i = 0;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dy = -1; dy <= 1; dy += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    // Create corner in local space
                    Vec3 localCorner = new Vec3(
                            halfSize.x * dx,
                            halfSize.y * dy,
                            halfSize.z * dz
                    );
                    // Rotate the corner using the inverse rotor
                    Vec3 rotatedCorner = rotor.rotate(localCorner);
                    // Store the rotated corner (still in local space)
                    corners[i++] = center.add(rotatedCorner);
                }
            }
        }

        // Step 4: Find the minimum and maximum coordinates of the rotated corners
        double minX = corners[0].x;
        double minY = corners[0].y;
        double minZ = corners[0].z;
        double maxX = corners[0].x;
        double maxY = corners[0].y;
        double maxZ = corners[0].z;

        for (int j = 1; j < 8; j++) {
            minX = Math.min(minX, corners[j].x);
            minY = Math.min(minY, corners[j].y);
            minZ = Math.min(minZ, corners[j].z);
            maxX = Math.max(maxX, corners[j].x);
            maxY = Math.max(maxY, corners[j].y);
            maxZ = Math.max(maxZ, corners[j].z);
        }

        AABB newBox = new AABB(
                minX, minY, minZ,
                maxX, maxY, maxZ
        );
        // Create a new AABB from the rotated corners, transformed back to world space
        return newBox;
    }


}
