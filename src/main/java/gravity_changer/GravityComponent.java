package gravity_changer;

import com.mojang.logging.LogUtils;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.CommonTickingComponent;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.api.RotationParameters;
import gravity_changer.mixin.entity.EntityAccessor;
import gravity_changer.util.GCUtil;
import gravity_changer.util.Rotor;
import gravity_changer.util.RotationUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The gravity is determined by the follows:
 * 1. base gravity
 * 2. gravity modifier, can override base gravity (determined from modifier events)
 * 3. gravity effects, can override modified gravity
 * The result of applying 1 and 2 is called modified gravity and is synced.
 * The result of 3 is current gravity and is not synced.
 * The gravity effect should be applied both on client and server, except for remote players.
 * (The client player's gravity attributes are separately computed.
 * Other client entities' are synced from server.)
 */
public class GravityComponent implements Component, AutoSyncedComponent, CommonTickingComponent {

    public static interface GravityUpdateCallback {
        void update(Entity entity, GravityComponent component);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Fired every tick for every entity, both on client and server.
     * <p>
     * In the event, it can call
     * {@link GravityComponent#applyGravityDirectionEffect(Direction, RotationParameters, double)}
     * and
     * {@link GravityComponent#applyGravityStrengthEffect(double)}
     * (these two applying methods can also be called outside the event)
     * <p>
     * To keep the result consistent between client and server,
     * the event listener should only use synchronized information.
     * <p>
     * In the event, it can read the current gravity direction for use cases like gravity inverting. (It requires phase ordering to keep the execution order.)
     */
    public static final Event<GravityUpdateCallback> GRAVITY_UPDATE_EVENT =
        EventFactory.createArrayBacked(
            GravityUpdateCallback.class,
            listeners -> (entity, component) -> {
                for (GravityUpdateCallback callback : listeners) {
                    callback.update(entity, component);
                }
            }
        );

    boolean initialized = false;

    // not synchronized
    private Vec3 prevGravityDirectionVec = new Vec3(0, -1, 0); // DOWN direction as a normalized vector
    private double prevGravityStrength = 1.0;

    // the base gravity direction
    Vec3 baseGravityDirectionVec = new Vec3(0, -1, 0); // DOWN direction as a normalized vector

    // Flag to specify if we're using Vec3 gravity or Direction gravity
    boolean useVec3Gravity = false;

    // the base gravity strength
    double baseGravityStrength = 1.0;

    @Nullable RotationParameters currentRotationParameters = RotationParameters.getDefault();

    // Only used on client, not synchronized.
    @Nullable
    public final RotationAnimation animation;

    public final Entity entity;

    private Vec3 currGravityDirectionVec = new Vec3(0, -1, 0); // DOWN direction as a normalized vector
    private double currGravityStrength = 1.0;
    private double currentEffectPriority = Double.MIN_VALUE;

    private boolean isFiringUpdateEvent = false;

    private @Nullable GravityComponent.GravityDirEffect delayApplyDirEffect = null;
    private @Nullable GravityComponent.GravityDirEffectVec delayApplyDirEffectVec = null;
    private double delayApplyStrengthEffect = 1.0;

    // if it equals entity.tickCount,
    // it means that the gravity update event has already fired in this tick
    private long lastUpdateTickCount = 0;

    // only used on server side
    private boolean needsSync = false;

    public GravityComponent(Entity entity) {
        this.entity = entity;
        if (entity.level().isClientSide()) {
            animation = new RotationAnimation();
        }
        else {
            animation = null;
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        // Read Vec3-based gravity
        if (tag.contains("baseGravityDirectionVecX")) {
            double x = tag.getDouble("baseGravityDirectionVecX");
            double y = tag.getDouble("baseGravityDirectionVecY");
            double z = tag.getDouble("baseGravityDirectionVecZ");
            baseGravityDirectionVec = new Vec3(x, y, z).normalize();
        }
        else {
            baseGravityDirectionVec = new Vec3(0, -1, 0); // Default to DOWN
        }

        if (tag.contains("baseGravityStrength")) {
            baseGravityStrength = tag.getDouble("baseGravityStrength");
        }
        else {
            baseGravityStrength = 1.0;
        }

        // the current gravity is serialized to avoid unnecessary gravity rotation when entering world
        // do not deserialize it when for client player when not initializing
        if (!initialized || shouldAcceptServerSync()) {
            // Read Vec3-based gravity
            if (tag.contains("currentGravityDirectionVecX")) {
                double x = tag.getDouble("currentGravityDirectionVecX");
                double y = tag.getDouble("currentGravityDirectionVecY");
                double z = tag.getDouble("currentGravityDirectionVecZ");
                currGravityDirectionVec = new Vec3(x, y, z).normalize();
            }
            else {
                currGravityDirectionVec = new Vec3(0, -1, 0); // Default to DOWN
            }

            if (tag.contains("currentGravityStrength")) {
                currGravityStrength = tag.getDouble("currentGravityStrength");
            }
            else {
                currGravityStrength = 1.0;
            }
        }

        if (!initialized) {
            prevGravityDirectionVec = currGravityDirectionVec;
            prevGravityStrength = currGravityStrength;
            initialized = true;
            applyGravityDirectionChangeVec(
                prevGravityDirectionVec, currGravityDirectionVec, currentRotationParameters, true
            );
        }
    }

    private boolean shouldAcceptServerSync() {
        return entity.level().isClientSide() && !GCUtil.isClientPlayer(entity);
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag tag) {
        // Write Vec3-based gravity
        tag.putDouble("baseGravityDirectionVecX", baseGravityDirectionVec.x);
        tag.putDouble("baseGravityDirectionVecY", baseGravityDirectionVec.y);
        tag.putDouble("baseGravityDirectionVecZ", baseGravityDirectionVec.z);

        tag.putDouble("currentGravityDirectionVecX", currGravityDirectionVec.x);
        tag.putDouble("currentGravityDirectionVecY", currGravityDirectionVec.y);
        tag.putDouble("currentGravityDirectionVecZ", currGravityDirectionVec.z);

        tag.putDouble("baseGravityStrength", baseGravityStrength);
        tag.putDouble("currentGravityStrength", currGravityStrength);
    }

    @Override
    public void tick() {
        if (!canChangeGravity()) {
            return;
        }

        updateGravityStatus();

        applyGravityChange();

        if (!entity.level().isClientSide()) {
            if (needsSync) {
                needsSync = false;
                GravityChangerComponents.GRAVITY_COMP_KEY.sync(entity);
            }
        }
    }

    public void updateGravityStatus() {
        // for the remote players and non-player entities,
        // their effect data is not synchronized to the client
        // (possibly for making it harder to cheat for hacked clients)
        // then we don't calculate its gravity in normal way in client
        if (shouldAcceptServerSync()) {
            return;
        }

        Vec3 oldGravityDirectionVec = currGravityDirectionVec;
        double oldGravityStrength = currGravityStrength;

        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            // If riding a vehicle, inherit its gravity settings
            currGravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(vehicle);
            currGravityStrength = GravityChangerAPI.getGravityStrength(vehicle);
        }
        else {
            // Always use Vec3-based gravity
            currGravityDirectionVec = baseGravityDirectionVec;

            currGravityStrength = baseGravityStrength;
            currGravityStrength *= GravityChangerAPI.getDimensionGravityStrength(entity.level());
            currGravityStrength *= GravityChangerMod.config.gravityStrengthMultiplier;
            // the rotation parameters is not being reset here
            // the rotation parameter is kept when an effect vanishes
            currentEffectPriority = Double.MIN_VALUE;

            isFiringUpdateEvent = true;
            try {
                GRAVITY_UPDATE_EVENT.invoker().update(entity, this);

                // Handle Vec3-based gravity effects
                if (delayApplyDirEffectVec != null) {
                    applyGravityDirectionEffectVec(
                        delayApplyDirEffectVec.direction(),
                        delayApplyDirEffectVec.rotationParameters(), delayApplyDirEffectVec.priority()
                    );
                    delayApplyDirEffectVec = null;
                }

                // Handle Direction-based gravity effects for backward compatibility
                if (delayApplyDirEffect != null) {
                    applyGravityDirectionEffect(
                        delayApplyDirEffect.direction(),
                        delayApplyDirEffect.rotationParameters(), delayApplyDirEffect.priority()
                    );
                    delayApplyDirEffect = null;
                }

                currGravityStrength *= delayApplyStrengthEffect;
                delayApplyStrengthEffect = 1.0;
            }
            finally {
                isFiringUpdateEvent = false;
            }

            if (currentEffectPriority == Double.MIN_VALUE) {
                // if no effect is applied, reset the rotation parameters
                currentRotationParameters = RotationParameters.getDefault();
            }

            lastUpdateTickCount = entity.tickCount;
        }

        boolean changed = !gravityDirectionsEqual(oldGravityDirectionVec, currGravityDirectionVec) ||
            Math.abs(oldGravityStrength - currGravityStrength) > 0.0001;
        if (changed) {
            sendSyncPacketToOtherPlayers();
        }
    }

    private void sendSyncPacketToOtherPlayers() {
        GravityChangerComponents.GRAVITY_COMP_KEY.sync(entity, this, p -> p != entity);
    }

    /**
     * Apply a gravity direction effect using a cardinal Direction
     * For backward compatibility
     */
    public void applyGravityDirectionEffect(
        @NotNull Direction direction,
        @Nullable RotationParameters rotationParameters,
        double priority
    ) {
        // Convert Direction to Vec3 and call the Vec3 version
        applyGravityDirectionEffectVec(
            directionToVec3(direction),
            rotationParameters,
            priority
        );

        // When not firing event, store it on delayApplyEffect for backward compatibility
        if (!isFiringUpdateEvent) {
            // The effect could come from another entity ticking,
            // but there is no guarantee for ticking order between entities.
            // (the ticking order does not change according to EntityTickList)
            if (delayApplyDirEffect == null || priority > delayApplyDirEffect.priority()) {
                delayApplyDirEffect = new GravityDirEffect(
                    direction, rotationParameters, priority
                );
            }
        }
    }

    /**
     * Apply a gravity direction effect using an arbitrary Vec3 direction
     */
    public void applyGravityDirectionEffectVec(
        @NotNull Vec3 direction,
        @Nullable RotationParameters rotationParameters,
        double priority
    ) {
        // Normalize the direction vector
        direction = direction.normalize();

        if (isFiringUpdateEvent) {
            if (priority > currentEffectPriority) {
                currentEffectPriority = priority;
                currGravityDirectionVec = direction;

                if (rotationParameters != null) {
                    currentRotationParameters = rotationParameters;
                }
            }
        }
        else {
            // When not firing event, store it on delayApplyEffect.
            // The effect could come from another entity ticking,
            // but there is no guarantee for ticking order between entities.
            // (the ticking order does not change according to EntityTickList)
            if (delayApplyDirEffectVec == null || priority > delayApplyDirEffectVec.priority()) {
                delayApplyDirEffectVec = new GravityDirEffectVec(
                    direction, rotationParameters, priority
                );
            }
        }
    }

    public void applyGravityStrengthEffect(
        double strengthMultiplier
    ) {
        if (isFiringUpdateEvent) {
            currGravityStrength *= strengthMultiplier;
        }
        else {
            delayApplyStrengthEffect *= strengthMultiplier;
        }
    }

    @Override
    public void applySyncPacket(FriendlyByteBuf buf) {
        AutoSyncedComponent.super.applySyncPacket(buf);

        if (entity.level().isClientSide()) {
            // the packet should be handled on client thread
            // start the gravity animation (doing that during ticking is too late)
            applyGravityChange();
        }
    }



    /**
     * Vec3-based version of getRealWorldVelocity for arbitrary gravity directions
     */
    private static Vec3 getRealWorldVelocityVec(Entity entity, Vec3 prevGravityDirection) {
        if (entity.isControlledByLocalInstance()) {
            return new Vec3(
                entity.getX() - entity.xo,
                entity.getY() - entity.yo,
                entity.getZ() - entity.zo
            );
        }

        return RotationUtil.vecPlayerToWorldVec(entity.getDeltaMovement(), prevGravityDirection);
    }

    @NotNull
    private static Vec3 getLocalRotationCenter(
        Entity entity,
        Direction oldGravity, Direction newGravity, RotationParameters rotationParameters
    ) {
        if (entity instanceof EndCrystal) {
            //In the middle of the block below
            return new Vec3(0, -0.5, 0);
        }

        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        if (newGravity.getOpposite() == oldGravity) {
            // In the center of the hit-box
            return new Vec3(0, dimensions.height / 2, 0);
        }
        else {
            return Vec3.ZERO;
        }
    }

    /**
     * Vec3-based version of getLocalRotationCenter for arbitrary gravity directions
     */
    @NotNull
    private static Vec3 getLocalRotationCenterVec(
        Entity entity,
        Vec3 oldGravity, Vec3 newGravity, RotationParameters rotationParameters
    ) {
        if (entity instanceof EndCrystal) {
            //In the middle of the block below
            return new Vec3(0, -0.5, 0);
        }

        EntityDimensions dimensions = entity.getDimensions(entity.getPose());

        // Check if vectors are opposite (or nearly opposite)
        double dot = oldGravity.normalize().dot(newGravity.normalize());
        if (dot < -0.9) {
            // In the center of the hit-box
            return new Vec3(0, dimensions.height / 2, 0);
        }
        else {
            return Vec3.ZERO;
        }
    }

    // Adjust position to avoid suffocation in blocks when changing gravity
    private void adjustEntityPosition(Direction oldGravity, Direction newGravity, AABB entityBoundingBox) {
        if (!GravityChangerMod.config.adjustPositionAfterChangingGravity) {
            return;
        }

        if (entity instanceof AreaEffectCloud || entity instanceof AbstractArrow || entity instanceof EndCrystal) {
            return;
        }

        // for example, if gravity changed from down to north, move up
        // if gravity changed from down to up, also move up
        Direction movingDirection = oldGravity.getOpposite();

        Iterable<VoxelShape> collisions = entity.level().getCollisions(
            entity,
            entityBoundingBox.inflate(-0.01) // shrink to avoid floating point error
        );
        AABB totalCollisionBox = null;
        for (VoxelShape collision : collisions) {
            if (!collision.isEmpty()) {
                AABB boundingBox = collision.bounds();
                if (totalCollisionBox == null) {
                    totalCollisionBox = boundingBox;
                }
                else {
                    totalCollisionBox = totalCollisionBox.minmax(boundingBox);
                }
            }
        }

        if (totalCollisionBox != null) {
            Vec3 positionAdjustmentOffset = getPositionAdjustmentOffset(
                entityBoundingBox, totalCollisionBox, movingDirection
            );
            if (entity instanceof Player) {
                LOGGER.info("Adjusting player position {} {}", positionAdjustmentOffset, entity);
            }
            entity.setPos(entity.position().add(positionAdjustmentOffset));
        }
    }

    /**
     * Vec3-based version of adjustEntityPosition for arbitrary gravity directions
     */
    private void adjustEntityPositionVec(Vec3 oldGravity, Vec3 newGravity, AABB entityBoundingBox) {
        if (!GravityChangerMod.config.adjustPositionAfterChangingGravity) {
            return;
        }

        if (entity instanceof AreaEffectCloud || entity instanceof AbstractArrow || entity instanceof EndCrystal) {
            return;
        }

        // Normalize the gravity directions
        oldGravity = oldGravity.normalize();
        newGravity = newGravity.normalize();

        // Calculate the moving direction (opposite of old gravity)
        Vec3 movingDirection = oldGravity.scale(-1);

        Iterable<VoxelShape> collisions = entity.level().getCollisions(
            entity,
            entityBoundingBox.inflate(-0.01) // shrink to avoid floating point error
        );
        AABB totalCollisionBox = null;
        for (VoxelShape collision : collisions) {
            if (!collision.isEmpty()) {
                AABB boundingBox = collision.bounds();
                if (totalCollisionBox == null) {
                    totalCollisionBox = boundingBox;
                }
                else {
                    totalCollisionBox = totalCollisionBox.minmax(boundingBox);
                }
            }
        }

        if (totalCollisionBox != null) {
            Vec3 positionAdjustmentOffset = getPositionAdjustmentOffsetVec(
                entityBoundingBox, totalCollisionBox, movingDirection
            );
            if (entity instanceof Player) {
                LOGGER.info("Adjusting player position {} {}", positionAdjustmentOffset, entity);
            }
            entity.setPos(entity.position().add(positionAdjustmentOffset));
        }
    }

    private static Vec3 getPositionAdjustmentOffset(
        AABB entityBoundingBox, AABB nearbyCollisionUnion, Direction movingDirection
    ) {
        Direction.Axis axis = movingDirection.getAxis();
        double offset = 0;
        if (movingDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            double pushing = nearbyCollisionUnion.max(axis);
            double pushed = entityBoundingBox.min(axis);
            if (pushing > pushed) {
                offset = pushing - pushed;
            }
        }
        else {
            double pushing = nearbyCollisionUnion.min(axis);
            double pushed = entityBoundingBox.max(axis);
            if (pushing < pushed) {
                offset = pushed - pushing;
            }
        }

        return new Vec3(movingDirection.step()).scale(offset);
    }

    /**
     * Vec3-based version of getPositionAdjustmentOffset for arbitrary gravity directions
     */
    private static Vec3 getPositionAdjustmentOffsetVec(
        AABB entityBoundingBox, AABB nearbyCollisionUnion, Vec3 movingDirection
    ) {
        // Normalize the moving direction
        movingDirection = movingDirection.normalize();

        // Find the primary axis of the moving direction
        double absX = Math.abs(movingDirection.x);
        double absY = Math.abs(movingDirection.y);
        double absZ = Math.abs(movingDirection.z);

        double offset = 0;

        if (absX >= absY && absX >= absZ) {
            // X is the primary axis
            if (movingDirection.x > 0) {
                double pushing = nearbyCollisionUnion.maxX;
                double pushed = entityBoundingBox.minX;
                if (pushing > pushed) {
                    offset = pushing - pushed;
                }
            } else {
                double pushing = nearbyCollisionUnion.minX;
                double pushed = entityBoundingBox.maxX;
                if (pushing < pushed) {
                    offset = pushed - pushing;
                }
            }
        } else if (absY >= absX && absY >= absZ) {
            // Y is the primary axis
            if (movingDirection.y > 0) {
                double pushing = nearbyCollisionUnion.maxY;
                double pushed = entityBoundingBox.minY;
                if (pushing > pushed) {
                    offset = pushing - pushed;
                }
            } else {
                double pushing = nearbyCollisionUnion.minY;
                double pushed = entityBoundingBox.maxY;
                if (pushing < pushed) {
                    offset = pushed - pushing;
                }
            }
        } else {
            // Z is the primary axis
            if (movingDirection.z > 0) {
                double pushing = nearbyCollisionUnion.maxZ;
                double pushed = entityBoundingBox.minZ;
                if (pushing > pushed) {
                    offset = pushing - pushed;
                }
            } else {
                double pushing = nearbyCollisionUnion.minZ;
                double pushed = entityBoundingBox.maxZ;
                if (pushing < pushed) {
                    offset = pushed - pushing;
                }
            }
        }

        return movingDirection.scale(offset);
    }

    public double getBaseGravityStrength() {
        return baseGravityStrength;
    }

    public void setBaseGravityStrength(double strength) {
        if (!canChangeGravity()) {
            return;
        }

        baseGravityStrength = strength;
        needsSync = true;
    }

    /**
     * Get the current gravity direction as a Vec3 (arbitrary direction)
     */
    public Vec3 getCurrGravityDirectionVec() {
        return currGravityDirectionVec;
    }

    public double getCurrGravityStrength() {
        return currGravityStrength;
    }

    private boolean canChangeGravity() {
        return EntityTags.canChangeGravity(entity);
    }

    /**
     * Get the previous gravity direction as a Vec3 (arbitrary direction)
     */
    public Vec3 getPrevGravityDirectionVec() {
        return prevGravityDirectionVec;
    }


    /**
     * Get the base gravity direction as a Vec3 (arbitrary direction)
     */
    public Vec3 getBaseGravityDirectionVec() {
        return baseGravityDirectionVec;
    }

    /**
     * Set the base gravity direction using a cardinal Direction
     * For backward compatibility
     */
    public void setBaseGravityDirection(Direction gravityDirection) {
        if (!canChangeGravity()) {
            return;
        }

        // Convert Direction to Vec3 and call the Vec3-based version
        setBaseGravityDirectionVec(directionToVec3(gravityDirection));
    }

    /**
     * Set the base gravity direction using an arbitrary Vec3 direction
     */
    public void setBaseGravityDirectionVec(Vec3 gravityDirection) {
        if (!canChangeGravity()) {
            return;
        }

        // Normalize the direction vector
        gravityDirection = gravityDirection.normalize();

        baseGravityDirectionVec = gravityDirection;

        needsSync = true;
    }

    /**
     * Reset gravity to default (DOWN direction)
     */
    public void reset() {
        baseGravityDirectionVec = new Vec3(0, -1, 0);
        baseGravityStrength = 1.0;
        needsSync = true;
    }

    @Environment(EnvType.CLIENT)
    public RotationAnimation getRotationAnimation() {
        return animation;
    }

    public void applyGravityChange() {
        if (currentRotationParameters == null) {
            currentRotationParameters = RotationParameters.getDefault();
        }

        // Always use Vec3-based gravity change
        if (!gravityDirectionsEqual(prevGravityDirectionVec, currGravityDirectionVec)) {
            applyGravityDirectionChangeVec(
                prevGravityDirectionVec, currGravityDirectionVec,
                currentRotationParameters, false
            );
            prevGravityDirectionVec = currGravityDirectionVec;
        }

        if (Math.abs(currGravityStrength - prevGravityStrength) > 0.0001) {
            prevGravityStrength = currGravityStrength;
        }
    }

    /**
     * Apply a gravity direction change using arbitrary Vec3 directions
     */
    public void applyGravityDirectionChangeVec(
        Vec3 oldGravity, Vec3 newGravity,
        RotationParameters rotationParameters, boolean isInitialization
    ) {
        if (!canChangeGravity()) {
            return;
        }

        // update bounding box
        entity.setBoundingBox(((EntityAccessor) entity).gc_makeBoundingBox());

        if (isInitialization) {
            return;
        }

        entity.fallDistance = 0;

        long timeMs = entity.level().getGameTime() * 50;

        // Use the same logic as applyGravityDirectionChange but with Vec3 directions
        Vec3 relativeRotationCenter = getLocalRotationCenterVec(
            entity, oldGravity, newGravity, rotationParameters
        );
        Vec3 oldPos = entity.position();
        Vec3 oldLastTickPos = new Vec3(entity.xOld, entity.yOld, entity.zOld);
        Vec3 rotationCenter = oldPos.add(RotationUtil.vecPlayerToWorldVec(relativeRotationCenter, oldGravity));
        Vec3 newPos = rotationCenter.subtract(RotationUtil.vecPlayerToWorldVec(relativeRotationCenter, newGravity));
        Vec3 posTranslation = newPos.subtract(oldPos);
        Vec3 newLastTickPos = oldLastTickPos.add(posTranslation);

        entity.setPos(newPos);
        entity.xo = newLastTickPos.x;
        entity.yo = newLastTickPos.y;
        entity.zo = newLastTickPos.z;
        entity.xOld = newLastTickPos.x;
        entity.yOld = newLastTickPos.y;
        entity.zOld = newLastTickPos.z;

        adjustEntityPositionVec(oldGravity, newGravity, entity.getBoundingBox());

        if (entity.level().isClientSide()) {
            Validate.notNull(animation, "gravity animation is null");

            int rotationTimeMS = rotationParameters.rotationTimeMS();

            animation.startRotationAnimationVec(
                newGravity, oldGravity,
                rotationTimeMS,
                entity, timeMs, rotationParameters.rotateView(),
                relativeRotationCenter
            );
        }

        Vec3 realWorldVelocity = getRealWorldVelocityVec(entity, oldGravity);
        if (rotationParameters.rotateVelocity()) {
            // Rotate velocity with gravity, this will cause things to appear to take a sharp turn
            // Use Rotor for rotation
            Rotor rotor = RotationUtil.getRotorBetweenVec(oldGravity, newGravity);
            Vec3 rotatedVelocity = rotor.rotate(realWorldVelocity);
            entity.setDeltaMovement(RotationUtil.vecWorldToPlayerVec(rotatedVelocity, newGravity));
        }
        else {
            // Velocity will be conserved relative to the world, will result in more natural motion
            entity.setDeltaMovement(RotationUtil.vecWorldToPlayerVec(realWorldVelocity, newGravity));
        }
    }

    /**
     * Not needed in normal cases.
     * Only used in {@link GravityChangerAPI#instantlySetClientBaseGravityDirectionVec(Entity, Vec3)}
     * Used by ImmPtl.
     */
    public void forceApplyGravityChange() {
        prevGravityDirectionVec = currGravityDirectionVec;
        prevGravityStrength = currGravityStrength;
    }

    private static record GravityDirEffect(
        @NotNull Direction direction,
        @Nullable RotationParameters rotationParameters,
        double priority
    ) {
        /**
         * Get the direction as a Vec3
         */
        public Vec3 directionVec() {
            return directionToVec3(direction);
        }
    }

    /**
     * A version of GravityDirEffect that uses Vec3 for gravity direction
     */
    private static record GravityDirEffectVec(
        @NotNull Vec3 direction,
        @Nullable RotationParameters rotationParameters,
        double priority
    ) {
        /**
         * Constructor that normalizes the direction vector
         */
        public GravityDirEffectVec {
            direction = direction.normalize();
        }

        /**
         * Get the closest cardinal direction
         */
        public Direction directionCardinal() {
            return vec3ToDirection(direction);
        }
    }

    // Utility methods for converting between Direction and Vec3

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
     * Checks if two Vec3 gravity directions are approximately equal
     */
    public static boolean gravityDirectionsEqual(Vec3 dir1, Vec3 dir2) {
        // Normalize both vectors to ensure consistent comparison
        dir1 = dir1.normalize();
        dir2 = dir2.normalize();

        // Check if the dot product is close to 1 (vectors pointing in same direction)
        return dir1.dot(dir2) > 0.9999;
    }
}
