package gravity_changer.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class QuaternionUtil {
    public static Quaternionf getViewRotation(float pitch, float yaw) {
        Quaternionf r1 = new Quaternionf().fromAxisAngleDeg(new Vector3f(1, 0, 0), pitch);
        Quaternionf r2 = new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), yaw + 180);
        r1.mul(r2);
        return r1;
    }

    // Handles rotation between two vectors, including the case when they are opposite
    public static Quaternionf getRotationBetween(Vec3 from, Vec3 to) {
        from = from.normalize();
        to = to.normalize();

        // Check if vectors are opposite or nearly opposite
        double cos = from.dot(to);
        if (cos < -0.9999) {
            // Vectors are opposite, need to find a perpendicular axis
            // First try cross product with UP vector
            Vec3 axis = from.cross(new Vec3(0, 1, 0));
            // If that's too small, try with EAST vector
            if (axis.lengthSqr() < 0.0001) {
                axis = from.cross(new Vec3(1, 0, 0));
            }
            // If that's still too small, try with NORTH vector
            if (axis.lengthSqr() < 0.0001) {
                axis = from.cross(new Vec3(0, 0, 1));
            }

            // If all cross products are too small, use a default axis
            if (axis.lengthSqr() < 0.0001) {
                axis = new Vec3(0, 0, 1); // Default to Z-axis
            }

            // Normalize the axis
            axis = axis.normalize();

            // Create a 180-degree rotation around this axis
            return new Quaternionf().fromAxisAngleDeg(
                new Vector3f((float)axis.x, (float)axis.y, (float)axis.z), 180.0f
            );
        }

        // Handle normal case (non-opposite vectors)
        Vec3 axis = from.cross(to);

        // Check if cross product is too small (vectors are nearly parallel)
        if (axis.lengthSqr() < 0.0001) {
            // Vectors are nearly parallel, no rotation needed
            if (cos > 0.9999) {
                return new Quaternionf(); // Identity quaternion
            }
            // Otherwise, find a perpendicular axis as above
            axis = from.cross(new Vec3(0, 1, 0));
            if (axis.lengthSqr() < 0.0001) {
                axis = from.cross(new Vec3(1, 0, 0));
            }
            if (axis.lengthSqr() < 0.0001) {
                axis = from.cross(new Vec3(0, 0, 1));
            }
            if (axis.lengthSqr() < 0.0001) {
                axis = new Vec3(0, 0, 1);
            }
        }

        // Normalize the axis and calculate the angle
        axis = axis.normalize();
        double angle = Math.acos(Mth.clamp(cos, -1.0, 1.0)); // Clamp to avoid NaN

        return new Quaternionf().fromAxisAngleRad(
            new Vector3f((float) axis.x, (float) axis.y, (float) axis.z),
            (float) angle
        );
    }

    // using mutable objects could easily cause bugs if forget to copy
    public static Vec3 rotate(Vec3 vec, Quaternionf quaternionf) {
        Vector3f vector3f = vec.toVector3f();
        vector3f.rotate(quaternionf);
        return new Vec3(vector3f);
    }
}
