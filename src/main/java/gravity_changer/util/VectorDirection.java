package gravity_changer.util;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.StringRepresentable;
import com.mojang.serialization.Codec;

/**
 * Represents a directional component of a vector.
 * Analogous to Direction.AxisDirection but for arbitrary vectors rather than just axis-aligned directions.
 */
public enum VectorDirection implements StringRepresentable {
    POSITIVE(1, "Towards positive"),
    NEGATIVE(-1, "Towards negative");

    public static final StringRepresentable.EnumCodec<VectorDirection> CODEC = StringRepresentable.fromEnum(VectorDirection::values);
    private final int step;
    private final String name;

    private VectorDirection(int step, String name) {
        this.step = step;
        this.name = name;
    }

    /**
     * Gets the step value representing the direction.
     * @return 1 for positive direction, -1 for negative direction
     */
    public int getStep() {
        return this.step;
    }

    /**
     * Gets the human-readable name of this direction.
     * @return The direction name
     */
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Returns the opposite direction.
     * @return NEGATIVE if this is POSITIVE, POSITIVE if this is NEGATIVE
     */
    public VectorDirection opposite() {
        return this == POSITIVE ? NEGATIVE : POSITIVE;
    }

    /**
     * Get the VectorDirection that a vector points along another reference vector.
     *
     * @param vec The vector to check
     * @param reference The reference vector to check against
     * @return POSITIVE if the vector component along the reference is positive,
     *         NEGATIVE otherwise
     */
    public static VectorDirection fromVector(Vec3 vec, Vec3 reference) {
        double dot = vec.dot(reference);
        return dot >= 0 ? POSITIVE : NEGATIVE;
    }

    /**
     * Applies this direction to a vector by scaling it appropriately.
     *
     * @param vector The vector to apply this direction to
     * @return A new vector scaled by the direction's step value
     */
    public Vec3 apply(Vec3 vector) {
        return vector.scale(this.step);
    }

    /**
     * Gets a VectorDirection from a Direction.AxisDirection
     *
     * @param axisDirection The vanilla AxisDirection
     * @return The equivalent VectorDirection
     */
    public static VectorDirection fromAxisDirection(net.minecraft.core.Direction.AxisDirection axisDirection) {
        return axisDirection.getStep() > 0 ? POSITIVE : NEGATIVE;
    }

    /**
     * Converts this VectorDirection to a Direction.AxisDirection
     *
     * @return The equivalent vanilla AxisDirection
     */
    public net.minecraft.core.Direction.AxisDirection toAxisDirection() {
        return this == POSITIVE ?
                net.minecraft.core.Direction.AxisDirection.POSITIVE :
                net.minecraft.core.Direction.AxisDirection.NEGATIVE;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
}