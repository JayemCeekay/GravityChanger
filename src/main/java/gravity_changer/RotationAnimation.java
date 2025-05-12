package gravity_changer;

import gravity_changer.util.QuaternionUtil;
import gravity_changer.util.Rotor;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RotationAnimation {
    private boolean inAnimation = false;
    private Rotor startGravityRotation;
    private Rotor endGravityRotation;
    private Vec3 relativeRotationCenter = Vec3.ZERO;

    private long startTimeMs;
    private long endTimeMs;

    // For arbitrary gravity directions
    private Vec3 currentGravityDirectionVec = new Vec3(0, -1, 0); // Default DOWN direction

    /**
     * Start a rotation animation with Direction-based gravity directions
     * For backward compatibility
     */
    public void startRotationAnimation(
        Direction newGravity, Direction prevGravity,
        long durationTimeMs, Entity entity, long timeMs,
        boolean rotateView, Vec3 relativeRotationCenter
    ) {
        // Convert Direction to Vec3 and call the Vec3 version
        startRotationAnimationVec(
            RotationUtil.directionToVec3(newGravity),
            RotationUtil.directionToVec3(prevGravity),
            durationTimeMs, entity, timeMs,
            rotateView, relativeRotationCenter
        );

        // Store the current gravity direction for backward compatibility
        currentGravityDirectionVec = RotationUtil.directionToVec3(newGravity);
    }

    /**
     * Start a rotation animation with Vec3-based gravity directions
     * Supports arbitrary gravity directions
     */
    public void startRotationAnimationVec(
        Vec3 newGravity, Vec3 prevGravity,
        long durationTimeMs, Entity entity, long timeMs,
        boolean rotateView, Vec3 relativeRotationCenter
    ) {
        if (durationTimeMs == 0) {
            inAnimation = false;
            return;
        }

        Validate.notNull(entity);

        Vec3 newLookingDirection = getNewLookingDirectionVec(newGravity, prevGravity, entity, rotateView);

        // Get view rotation as a rotor
        Rotor oldViewRotor = QuaternionUtil.getViewRotor(entity.getXRot(), entity.getYRot());

        update(timeMs);

        // Get the current gravity rotation using the arbitrary gravity direction
        Rotor currentAnimatedGravityRotor = getCurrentGravityRotorVec(prevGravity, timeMs);

        // camera rotation = view rotation(pitch and yaw) * gravity rotation(animated)
        Rotor currentAnimatedCameraRotor = oldViewRotor.then(currentAnimatedGravityRotor);

        // Create a rotor that rotates from DOWN to the new gravity direction
        Vec3 downVector = new Vec3(0, -1, 0); // Standard DOWN direction
        Rotor newEndGravityRotor = RotationUtil.getRotorBetweenVec(downVector, newGravity);

        // Use the Vec3-based method for world to player conversion
        Vec2 newYawAndPitch = RotationUtil.vecToRot(
            RotationUtil.vecWorldToPlayerVec(newLookingDirection, newGravity)
        );
        float newPitch = newYawAndPitch.y;
        float newYaw = newYawAndPitch.x;
        float deltaYaw = newYaw - entity.getYRot();
        float deltaPitch = newPitch - entity.getXRot();
        entity.setYRot(entity.getYRot() + deltaYaw);
        entity.setXRot(entity.getXRot() + deltaPitch);
        entity.yRotO += deltaYaw;
        entity.xRotO += deltaPitch;
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.yBodyRot += deltaYaw;
            livingEntity.yBodyRotO += deltaYaw;
            livingEntity.yHeadRot += deltaYaw;
            livingEntity.yHeadRotO += deltaYaw;
        }

        // Get new view rotation as a rotor
        Rotor newViewRotor = QuaternionUtil.getViewRotor(entity.getXRot(), entity.getYRot());

        // gravity rotation = (view rotation^-1) * camera rotation
        // For rotors: inverse() is equivalent to conjugate for quaternions
        Rotor animationStartGravityRotor = newViewRotor.inverse().then(currentAnimatedCameraRotor);

        this.relativeRotationCenter = relativeRotationCenter;
        inAnimation = true;
        startGravityRotation = animationStartGravityRotor;
        endGravityRotation = newEndGravityRotor;
        startTimeMs = timeMs;
        endTimeMs = timeMs + durationTimeMs;

        // Store the current gravity direction
        currentGravityDirectionVec = newGravity.normalize();
    }

    /**
     * Get the new looking direction for Vec3-based gravity
     * Supports arbitrary gravity directions
     */
    private Vec3 getNewLookingDirectionVec(
        Vec3 newGravity, Vec3 prevGravity, Entity player,
        boolean rotateView
    ) {
        // Normalize the gravity directions
        newGravity = newGravity.normalize();
        prevGravity = prevGravity.normalize();

        // Use the Vec3-based method for player to world conversion
        Vec3 oldLookingDirection = RotationUtil.vecPlayerToWorldVec(
            RotationUtil.rotToVec(player.getYRot(), player.getXRot()),
            prevGravity
        );

        if (!rotateView) {
            return oldLookingDirection;
        }

        // Check if vectors are opposite (or nearly opposite)
        double dot = prevGravity.dot(newGravity);
        if (dot < -0.9999) {
            return oldLookingDirection.scale(-1);
        }

        // Use the rotor-based rotation method for arbitrary vectors
        Rotor deltaRotor = RotationUtil.getRotorBetweenVec(prevGravity, newGravity);

        // Rotate the looking direction using the rotor
        Vec3 newLookingDirection = deltaRotor.rotate(oldLookingDirection);
        return newLookingDirection;
    }

    /**
     * It returns the rotation that applies to world for rendering.
     * To get the rotation that applies entity, conjugate it.
     * Supports arbitrary gravity directions
     * For backward compatibility
     */
    public Quaternionf getCurrentGravityRotationVec(Vec3 currentGravity, long timeMs) {
        // Convert the rotor to quaternion for backward compatibility
        return getCurrentGravityRotorVec(currentGravity, timeMs).toQuaternion();
    }

    /**
     * It returns the rotor that applies to world for rendering.
     * To get the rotor that applies to entity, use inverse().
     * Supports arbitrary gravity directions
     */
    public Rotor getCurrentGravityRotorVec(Vec3 currentGravity, long timeMs) {
        update(timeMs);

        if (!inAnimation) {
            // Create a rotor that rotates from DOWN to the current gravity direction
            Vec3 downVector = new Vec3(0, -1, 0); // Standard DOWN direction
            return RotationUtil.getRotorBetweenVec(downVector, currentGravity);
        }

        double delta = (double) (timeMs - startTimeMs) / (endTimeMs - startTimeMs);

        return RotationUtil.interpolateRotors(
            startGravityRotation, endGravityRotation,
            mapProgress((float) delta)
        );
    }

    public void update(long timeMs) {
        if (timeMs > endTimeMs) {
            inAnimation = false;
        }
    }

    /**
     * When doing gravity flipping, the rotation center is the player bounding box center.
     * But the player feet pos changes abruptly. So we need special calculation to eye offset.
     *
     * Note when rotateView is false, it will cause non-smooth eye offset change
     * 
     * Supports arbitrary gravity directions
     * For backward compatibility
     */
    public Vec3 getEyeOffsetVec(
        Quaternionf gravityRot, Vec3 localEyeOffset, Vec3 newGravity
    ) {
        // Convert quaternion to rotor and call the rotor version
        Rotor gravityRotor = Rotor.fromQuaternion(gravityRot);
        return getEyeOffsetVecRotor(gravityRotor, localEyeOffset, newGravity);
    }

    /**
     * When doing gravity flipping, the rotation center is the player bounding box center.
     * But the player feet pos changes abruptly. So we need special calculation to eye offset.
     *
     * Note when rotateView is false, it will cause non-smooth eye offset change
     * 
     * Supports arbitrary gravity directions
     * Uses rotors for rotation
     */
    public Vec3 getEyeOffsetVecRotor(
        Rotor gravityRotor, Vec3 localEyeOffset, Vec3 newGravity
    ) {
        Rotor gravityRotorForEntity = gravityRotor.inverse();

        if (!inAnimation || relativeRotationCenter.equals(Vec3.ZERO)) {
            return gravityRotorForEntity.rotate(localEyeOffset);
        }

        // Use the Vec3-based method for player to world conversion
        Vec3 rotationCenterOffset = RotationUtil.vecPlayerToWorldVec(relativeRotationCenter, newGravity);

        Vec3 eyeOffsetFromRotationCenter = localEyeOffset.subtract(relativeRotationCenter);
        Vec3 rotatedEyeOffsetFromRotationCenter = gravityRotorForEntity.rotate(eyeOffsetFromRotationCenter);

        return rotationCenterOffset.add(rotatedEyeOffsetFromRotationCenter);
    }

    private static float mapProgress(float delta) {
        return Mth.clamp((delta * delta * (3 - 2 * delta)), 0, 1);
    }

    public boolean isInAnimation() {
        return inAnimation;
    }

    public String getAnimationTimeMs() {
        if (!inAnimation) {
            return "0";
        }
        return String.valueOf(endTimeMs - startTimeMs);
    }
}
