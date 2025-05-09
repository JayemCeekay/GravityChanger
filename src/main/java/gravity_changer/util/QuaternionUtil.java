package gravity_changer.util;

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

    public static Quaternionf getRotationBetween(Vec3 from, Vec3 to) {
        // Normalize inputs
        Vec3 f = from.normalize();
        Vec3 t = to.normalize();

        // Dot product tells us how aligned they are
        double dot = f.dot(t);

        // Use a consistent reference frame for calculating the rotation axis
        // This ensures that the rotation is applied correctly for all directions
        Vec3 axis;
        if (Math.abs(dot) > 0.999999) {
            // Vectors are nearly parallel or opposite
            // Use a more stable approach for finding the rotation axis
            Vec3 ref = new Vec3(0, 0, 1); // Use Z axis as reference
            if (Math.abs(f.dot(ref)) > 0.999) {
                ref = new Vec3(1, 0, 0); // If aligned with Z, use X instead
            }
            axis = f.cross(ref).normalize();
        } else {
            axis = f.cross(t).normalize();
        }

        float angle = (float) Math.acos(dot);
        return new Quaternionf().fromAxisAngleRad(
                new Vector3f((float) axis.x, (float) axis.y, (float) axis.z),
                angle
        );
    }


    // using mutable objects could easily cause bugs if forget to copy
    public static Vec3 rotate(Vec3 vec, Quaternionf quaternionf) {
        Vector3f vector3f = vec.toVector3f();
        vector3f.rotate(quaternionf);
        return new Vec3(vector3f);
    }
}
