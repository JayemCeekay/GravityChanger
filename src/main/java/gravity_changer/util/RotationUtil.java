package gravity_changer.util;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class RotationUtil {
    private static final Direction[][] DIR_WORLD_TO_PLAYER = new Direction[6][];

    static {
        for (Direction gravityDirection : Direction.values()) {
            DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()] = new Direction[6];
            for (Direction direction : Direction.values()) {
                Vec3 directionVector = Vec3.atLowerCornerOf(direction.getNormal());
                directionVector = RotationUtil.vecWorldToPlayer(directionVector, gravityDirection);
                DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()][direction.get3DDataValue()] =
                    Direction.getNearest(directionVector.x, directionVector.y, directionVector.z);
            }
        }
    }

    public static Direction dirWorldToPlayer(Direction direction, Direction gravityDirection) {
        return DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()][direction.get3DDataValue()];
    }

    private static final Direction[][] DIR_PLAYER_TO_WORLD = new Direction[6][];

    static {
        for (Direction gravityDirection : Direction.values()) {
            DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()] = new Direction[6];
            for (Direction direction : Direction.values()) {
                Vec3 directionVector = Vec3.atLowerCornerOf(direction.getNormal());
                directionVector = RotationUtil.vecPlayerToWorld(directionVector, gravityDirection);
                DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()][direction.get3DDataValue()] =
                    Direction.getNearest(directionVector.x, directionVector.y, directionVector.z);
            }
        }
    }

    public static Direction dirPlayerToWorld(Direction direction, Direction gravityDirection) {
        return DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()][direction.get3DDataValue()];
    }

    public static Vec3 vecWorldToPlayer(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, z, -y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(-z, x, -y);
            case EAST -> new Vec3(z, -x, -y);
        };
    }

    public static Vec3 vecWorldToPlayer(Vec3 vec3d, Direction gravityDirection) {
        return vecWorldToPlayer(vec3d.x, vec3d.y, vec3d.z, gravityDirection);
    }

    public static Vec3 vecPlayerToWorld(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, -z, y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(y, -z, -x);
            case EAST -> new Vec3(-y, -z, x);
        };
    }

    public static Vec3 vecPlayerToWorld(Vec3 vec3d, Direction gravityDirection) {
        return vecPlayerToWorld(vec3d.x, vec3d.y, vec3d.z, gravityDirection);
    }

    public static Vector3f vecWorldToPlayer(float x, float y, float z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vector3f(x, y, z);
            case UP -> new Vector3f(-x, -y, z);
            case NORTH -> new Vector3f(x, z, -y);
            case SOUTH -> new Vector3f(-x, -z, -y);
            case WEST -> new Vector3f(-z, x, -y);
            case EAST -> new Vector3f(z, -x, -y);
        };
    }

    public static Vector3f vecWorldToPlayer(Vector3f vector3F, Direction gravityDirection) {
        return vecWorldToPlayer(vector3F.x(), vector3F.y(), vector3F.z(), gravityDirection);
    }

    public static Vector3f vecPlayerToWorld(float x, float y, float z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vector3f(x, y, z);
            case UP -> new Vector3f(-x, -y, z);
            case NORTH -> new Vector3f(x, -z, y);
            case SOUTH -> new Vector3f(-x, -z, -y);
            case WEST -> new Vector3f(y, -z, -x);
            case EAST -> new Vector3f(-y, -z, x);
        };
    }

    public static Vector3f vecPlayerToWorld(Vector3f vector3F, Direction gravityDirection) {
        return vecPlayerToWorld(vector3F.x(), vector3F.y(), vector3F.z(), gravityDirection);
    }

    public static Vec3 maskWorldToPlayer(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN, UP -> new Vec3(x, y, z);
            case NORTH, SOUTH -> new Vec3(x, z, y);
            case WEST, EAST -> new Vec3(z, x, y);
        };
    }

    public static Vec3 maskWorldToPlayer(Vec3 vec3d, Direction gravityDirection) {
        return maskWorldToPlayer(vec3d.x, vec3d.y, vec3d.z, gravityDirection);
    }

    public static Vec3 maskPlayerToWorld(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN, UP -> new Vec3(x, y, z);
            case NORTH, SOUTH -> new Vec3(x, z, y);
            case WEST, EAST -> new Vec3(y, z, x);
        };
    }

    public static Vec3 maskPlayerToWorld(Vec3 vec3d, Direction gravityDirection) {
        return maskPlayerToWorld(vec3d.x, vec3d.y, vec3d.z, gravityDirection);
    }



    public static Vec3 rotToVec(float yaw, float pitch) {
        double radPitch = pitch * 0.017453292;
        double radNegYaw = -yaw * 0.017453292;
        double cosNegYaw = Math.cos(radNegYaw);
        double sinNegYaw = Math.sin(radNegYaw);
        double cosPitch = Math.cos(radPitch);
        double sinPitch = Math.sin(radPitch);
        return new Vec3(sinNegYaw * cosPitch, -sinPitch, cosNegYaw * cosPitch);
    }

    public static Vec2 vecToRot(double x, double y, double z) {
        double sinPitch = -y;
        double radPitch = Math.asin(sinPitch);
        double cosPitch = Math.cos(radPitch);
        double sinNegYaw = x / cosPitch;
        double cosNegYaw = Mth.clamp(z / cosPitch, -1, 1);
        double radNegYaw = Math.acos(cosNegYaw);
        if (sinNegYaw < 0) radNegYaw = Math.PI * 2 - radNegYaw;

        return new Vec2(Mth.wrapDegrees((float) (-radNegYaw) / 0.017453292F), (float) (radPitch) / 0.017453292F);
    }

    public static Vec2 vecToRot(Vec3 vec3d) {
        return vecToRot(vec3d.x, vec3d.y, vec3d.z);
    }

    private static final Quaternionf[] WORLD_ROTATION_QUATERNIONS = new Quaternionf[6];

    static {
        WORLD_ROTATION_QUATERNIONS[0] = new Quaternionf();

        WORLD_ROTATION_QUATERNIONS[1] = Axis.ZP.rotationDegrees(-180);

        WORLD_ROTATION_QUATERNIONS[2] = Axis.XP.rotationDegrees(-90);

        WORLD_ROTATION_QUATERNIONS[3] = Axis.XP.rotationDegrees(-90);
        WORLD_ROTATION_QUATERNIONS[3].mul(Axis.YP.rotationDegrees(-180));

        WORLD_ROTATION_QUATERNIONS[4] = Axis.XP.rotationDegrees(-90);
        WORLD_ROTATION_QUATERNIONS[4].mul(Axis.YP.rotationDegrees(-90));

        WORLD_ROTATION_QUATERNIONS[5] = Axis.XP.rotationDegrees(-90);
        WORLD_ROTATION_QUATERNIONS[5].mul(Axis.YP.rotationDegrees(-270));
    }

    /**
     * Note: don't modify the quaternion object in-place
     */
    public static Quaternionf getWorldRotationQuaternion(Direction gravityDirection) {
        return WORLD_ROTATION_QUATERNIONS[gravityDirection.get3DDataValue()];
    }

    private static final Quaternionf[] ENTITY_ROTATION_QUATERNIONS = new Quaternionf[6];
    private static final Rotor[] ENTITY_ROTATION_ROTORS = new Rotor[6];

    static {
        for (int i = 0; i < 6; i++) {
            ENTITY_ROTATION_QUATERNIONS[i] = new Quaternionf().set(WORLD_ROTATION_QUATERNIONS[i]).conjugate();
            // Initialize the corresponding rotors
            ENTITY_ROTATION_ROTORS[i] = Rotor.fromQuaternion(ENTITY_ROTATION_QUATERNIONS[i]);
        }
    }

    /**
     * Note: don't modify the quaternion object in-place
     * For backward compatibility
     */
    public static Quaternionf getCameraRotationQuaternion(Direction gravityDirection) {
        return ENTITY_ROTATION_QUATERNIONS[gravityDirection.get3DDataValue()];
    }

    /**
     * Get the camera rotation rotor for a cardinal direction
     * Note: don't modify the rotor object in-place
     */
    public static Rotor getCameraRotationRotor(Direction gravityDirection) {
        return ENTITY_ROTATION_ROTORS[gravityDirection.get3DDataValue()];
    }

    /**
     * Get the camera rotation quaternion for an arbitrary gravity direction
     * Note: don't modify the quaternion object in-place
     * For backward compatibility
     */
    public static Quaternionf getCameraRotationQuaternionVec(Vec3 gravityDirection) {
        // Convert the rotor to quaternion for backward compatibility
        return getCameraRotationRotorVec(gravityDirection).toQuaternion();
    }

    /**
     * Get the camera rotation rotor for an arbitrary gravity direction
     * Note: don't modify the rotor object in-place
     */
    public static Rotor getCameraRotationRotorVec(Vec3 gravityDirection) {
        // Normalize the gravity direction
        gravityDirection = gravityDirection.normalize();

        // Create a rotor that rotates from the gravity direction to DOWN
        Vec3 downVector = new Vec3(0, -1, 0); // Standard DOWN direction
        Rotor rotation = getRotorBetweenVec(gravityDirection, downVector);

        // Return the inverse for camera rotation
        return rotation;
    }

    /**
     * Get the rotation quaternion between two cardinal directions
     * For backward compatibility
     */
    public static Quaternionf getRotationBetween(Direction d1, Direction d2) {
        Vec3 start = new Vec3(d1.step());
        Vec3 end = new Vec3(d2.step());
        if (d1.getOpposite() == d2) {
            return new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 0, -1), 180.0f);
        }
        else {
            return QuaternionUtil.getRotationBetween(start, end);
        }
    }

    /**
     * Get the rotation rotor between two cardinal directions
     */
    public static Rotor getRotorBetween(Direction d1, Direction d2) {
        Vec3 start = new Vec3(d1.step());
        Vec3 end = new Vec3(d2.step());
        return Rotor.from(start, end);
    }

    /**
     * Get the rotation quaternion between two arbitrary gravity directions
     */
    public static Quaternionf getRotationBetweenVec(Vec3 v1, Vec3 v2) {
        // Use the more robust QuaternionUtil.getRotationBetween method for all cases
        return QuaternionUtil.getRotationBetween(v1, v2);
    }

    /**
     * Get the rotation rotor between two arbitrary gravity directions
     */
    public static Rotor getRotorBetweenVec(Vec3 v1, Vec3 v2) {
        return Rotor.from(v1, v2);
    }

    public static Quaternionf interpolate(Quaternionf startGravityRotation, Quaternionf endGravityRotation, float progress) {
        return new Quaternionf().set(startGravityRotation).slerp(endGravityRotation, progress);
    }

    /**
     * Interpolate between two rotors using spherical linear interpolation
     */
    public static Rotor interpolateRotors(Rotor startRotor, Rotor endRotor, float progress) {
        return Rotor.slerp(startRotor, endRotor, progress);
    }

    /**
     * Converts a Direction to a normalized Vec3
     */
    public static Vec3 directionToVec3(Direction direction) {
        return new Vec3(direction.step()).normalize();
    }

    /**
     * Converts a Vec3 to the closest cardinal Direction
     * If the vector is zero, returns Direction.DOWN as default
     */
    public static Direction vec3ToDirection(Vec3 vec) {
        if (vec.equals(Vec3.ZERO)) {
            return Direction.DOWN;
        }

        vec = vec.normalize();

        // Find the direction with the closest alignment to the vector
        Direction closestDir = Direction.DOWN;
        double closestDot = Double.NEGATIVE_INFINITY;

        for (Direction dir : Direction.values()) {
            Vec3 dirVec = directionToVec3(dir);
            double dot = vec.dot(dirVec);
            if (dot > closestDot) {
                closestDot = dot;
                closestDir = dir;
            }
        }

        return closestDir;
    }

    /**
     * Convert a vector from player space to world space using an arbitrary gravity direction
     */
    public static Vec3 vecPlayerToWorldVec(Vec3 vec, Vec3 gravityDirection) {
        // Normalize the gravity direction
        gravityDirection = gravityDirection.normalize();

        // Create a rotor that rotates from DOWN to the gravity direction
        Vec3 downVector = new Vec3(0, -1, 0); // Standard DOWN direction
        Rotor rotor = getRotorBetweenVec(downVector, gravityDirection);

        // Apply the rotation to the vector
        return rotor.rotate(vec);
    }

    /**
     * Convert a vector from world space to player space using an arbitrary gravity direction
     */
    public static Vec3 vecWorldToPlayerVec(Vec3 vec, Vec3 gravityDirection) {
        // Normalize the gravity direction
        gravityDirection = gravityDirection.normalize();

        // Create a rotor that rotates from the gravity direction to DOWN
        Vec3 downVector = new Vec3(0, -1, 0); // Standard DOWN direction
        Rotor rotor = getRotorBetweenVec(gravityDirection, downVector);

        // Apply the rotation to the vector
        return rotor.rotate(vec);
    }

    /**
     * Convert a bounding box from world space to player space using an arbitrary gravity direction
     */
    public static AABB boxWorldToPlayerVec(AABB box, Vec3 gravityVec) {
        return Rotor.inverseRotateBoxWithRotor(box, gravityVec);
    }


    /**
     * Convert a bounding box from player space to world space using an arbitrary gravity direction
     */
    public static AABB boxPlayerToWorldVec(AABB box, Vec3 gravityVec) {
        return Rotor.rotateBoxWithRotor(box, gravityVec);
    }


    /**
     * Convert rotation from world space to player space using an arbitrary gravity direction
     */
    public static Vec2 rotWorldToPlayerVec(float yaw, float pitch, Vec3 gravityDirection) {
        Vec3 vec3d = vecWorldToPlayerVec(rotToVec(yaw, pitch), gravityDirection);
        return vecToRot(vec3d.x, vec3d.y, vec3d.z);
    }

    /**
     * Convert rotation from world space to player space using an arbitrary gravity direction
     */
    public static Vec2 rotWorldToPlayerVec(Vec2 vec2f, Vec3 gravityDirection) {
        return rotWorldToPlayerVec(vec2f.x, vec2f.y, gravityDirection);
    }

    /**
     * Convert rotation from player space to world space using an arbitrary gravity direction
     */
    public static Vec2 rotPlayerToWorldVec(float yaw, float pitch, Vec3 gravityDirection) {
        Vec3 vec3d = vecPlayerToWorldVec(rotToVec(yaw, pitch), gravityDirection);
        return vecToRot(vec3d.x, vec3d.y, vec3d.z);
    }

    /**
     * Convert rotation from player space to world space using an arbitrary gravity direction
     */
    public static Vec2 rotPlayerToWorldVec(Vec2 vec2f, Vec3 gravityDirection) {
        return rotPlayerToWorldVec(vec2f.x, vec2f.y, gravityDirection);
    }


    public static Vec3 vecPlayerToWorldVec(double v, double v1, double v2, Vec3 gravityDirection) {
        return vecPlayerToWorldVec(new Vec3(v, v1, v2), gravityDirection);
    }
}
