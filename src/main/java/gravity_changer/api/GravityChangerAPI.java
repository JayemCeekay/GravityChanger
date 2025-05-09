package gravity_changer.api;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import gravity_changer.DimensionGravityDataComponent;
import gravity_changer.EntityTags;
import gravity_changer.GravityChangerComponents;
import gravity_changer.GravityComponent;
import gravity_changer.RotationAnimation;
import gravity_changer.util.RotationUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

public abstract class GravityChangerAPI {
    public static final ComponentKey<GravityComponent> GRAVITY_COMPONENT =
        GravityChangerComponents.GRAVITY_COMP_KEY;

    public static final ComponentKey<DimensionGravityDataComponent> DIMENSION_DATA_COMPONENT =
        GravityChangerComponents.DIMENSION_COMP_KEY;


    /**
     * Returns the applied gravity direction for the given entity as a cardinal Direction
     * For backward compatibility
     */
    public static Direction getGravityDirection(Entity entity) {
        // Convert Vec3 gravity to Direction for backward compatibility
        return RotationUtil.vec3ToDirection(getGravityDirectionVec(entity));
    }

    /**
     * Returns the applied gravity direction for the given entity as a Vec3 (arbitrary direction)
     */
    public static Vec3 getGravityDirectionVec(Entity entity) {
        return getGravityComponent(entity).getCurrGravityDirectionVec();
    }

    public static double getGravityStrength(Entity entity) {
        return getGravityComponent(entity).getCurrGravityStrength();
    }

    public static double getBaseGravityStrength(Entity entity) {
        return getGravityComponent(entity).getBaseGravityStrength();
    }

    public static void setBaseGravityStrength(Entity entity, double strength) {
        GravityComponent component = getGravityComponent(entity);

        component.setBaseGravityStrength(strength);
    }

    public static double getDimensionGravityStrength(Level world) {
        return DIMENSION_DATA_COMPONENT.get(world).getDimensionGravityStrength();
    }

    public static void setDimensionGravityStrength(Level world, double strength) {
        DIMENSION_DATA_COMPONENT.get(world).setDimensionGravityStrength(strength);
    }

    public static void resetGravity(Entity entity) {
        if (!EntityTags.canChangeGravity(entity)) {return;}

        getGravityComponent(entity).reset();
    }

    /**
     * Returns the main gravity direction for the given entity as a cardinal Direction
     * For backward compatibility
     */
    public static Direction getBaseGravityDirection(Entity entity) {
        // Convert Vec3 gravity to Direction for backward compatibility
        return RotationUtil.vec3ToDirection(getBaseGravityDirectionVec(entity));
    }

    /**
     * Returns the main gravity direction for the given entity as a Vec3 (arbitrary direction)
     */
    public static Vec3 getBaseGravityDirectionVec(Entity entity) {
        return getGravityComponent(entity).getBaseGravityDirectionVec();
    }

    /**
     * Sets the base gravity direction for the given entity using a cardinal Direction
     * For backward compatibility
     */
    public static void setBaseGravityDirection(
        Entity entity, Direction gravityDirection
    ) {
        GravityComponent component = getGravityComponent(entity);
        component.setBaseGravityDirection(gravityDirection);
    }

    /**
     * Sets the base gravity direction for the given entity using an arbitrary Vec3 direction
     * The vector will be normalized automatically
     */
    public static void setBaseGravityDirectionVec(
        Entity entity, Vec3 gravityDirection
    ) {
        GravityComponent component = getGravityComponent(entity);
        component.setBaseGravityDirectionVec(gravityDirection);
    }

    @Nullable
    @Environment(EnvType.CLIENT)
    public static RotationAnimation getRotationAnimation(Entity entity) {
        return getGravityComponent(entity).getRotationAnimation();
    }

    /**
     * Instantly set gravity direction on client side without performing animation.
     * Not needed in normal cases.
     * (Used by ImmPtl)
     * For backward compatibility
     */
    public static void instantlySetClientBaseGravityDirection(Entity entity, Direction direction) {
        // Convert Direction to Vec3 and call the Vec3 version
        instantlySetClientBaseGravityDirectionVec(entity, RotationUtil.directionToVec3(direction));
    }

    /**
     * Instantly set gravity direction on client side without performing animation using an arbitrary Vec3 direction.
     * Not needed in normal cases.
     * The vector will be normalized automatically
     */
    public static void instantlySetClientBaseGravityDirectionVec(Entity entity, Vec3 direction) {
        Validate.isTrue(entity.level().isClientSide(), "should only be used on client");

        GravityComponent component = getGravityComponent(entity);

        component.setBaseGravityDirectionVec(direction);

        component.updateGravityStatus();

        component.forceApplyGravityChange();
    }

    public static GravityComponent getGravityComponent(Entity entity) {
        return GRAVITY_COMPONENT.get(entity);
    }

    /**
     * Returns the world relative velocity for the given entity
     * Using minecraft's methods to get the velocity will return entity local velocity
     * For backward compatibility
     */
    public static Vec3 getWorldVelocity(Entity entity) {
        // Use Vec3-based method internally
        return getWorldVelocityVec(entity);
    }

    /**
     * Returns the world relative velocity for the given entity using arbitrary gravity direction
     * Using minecraft's methods to get the velocity will return entity local velocity
     */
    public static Vec3 getWorldVelocityVec(Entity entity) {
        return RotationUtil.vecPlayerToWorldVec(entity.getDeltaMovement(), getGravityDirectionVec(entity));
    }

    /**
     * Sets the world relative velocity for the given player
     * Using minecraft's methods to set the velocity of an entity will set player relative velocity
     * For backward compatibility
     */
    public static void setWorldVelocity(Entity entity, Vec3 worldVelocity) {
        // Use Vec3-based method internally
        setWorldVelocityVec(entity, worldVelocity);
    }

    /**
     * Sets the world relative velocity for the given player using arbitrary gravity direction
     * Using minecraft's methods to set the velocity of an entity will set player relative velocity
     */
    public static void setWorldVelocityVec(Entity entity, Vec3 worldVelocity) {
        entity.setDeltaMovement(RotationUtil.vecWorldToPlayerVec(worldVelocity, getGravityDirectionVec(entity)));
    }

    /**
     * Returns eye position offset from feet position for the given entity
     * For backward compatibility
     */
    public static Vec3 getEyeOffset(Entity entity) {
        // Use Vec3-based method internally
        return getEyeOffsetVec(entity);
    }

    /**
     * Returns eye position offset from feet position for the given entity using arbitrary gravity direction
     */
    public static Vec3 getEyeOffsetVec(Entity entity) {
        return RotationUtil.vecPlayerToWorldVec(new Vec3(0, entity.getEyeHeight(), 0), getGravityDirectionVec(entity));
    }

    public static boolean canChangeGravity(Entity entity) {
        return EntityTags.canChangeGravity(entity);
    }

    /**
     * Returns whether the entity is using Vec3-based gravity
     */
    public static boolean isUsingVec3Gravity(Entity entity) {
        return getGravityComponent(entity).isUsingVec3Gravity();
    }

    /**
     * Sets whether the entity should use Vec3-based gravity
     */
    public static void setUseVec3Gravity(Entity entity, boolean useVec3) {
        getGravityComponent(entity).setUseVec3Gravity(useVec3);
    }
}
