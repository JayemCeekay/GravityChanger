package gravity_changer.collision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local context for tracking the current entity being processed for collision calculations.
 * This allows us to apply the correct gravity direction during collision detection.
 */
public class CollisionContext {
    private static final ThreadLocal<Entity> CURRENT_ENTITY = new ThreadLocal<>();
    private static final ThreadLocal<Entity> LAST_KNOWN_ENTITY = new ThreadLocal<>();
    private static final ThreadLocal<Map<Entity, Vec3>> lastMotion = ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<Entity, Vec3>> gravityDir = ThreadLocal.withInitial(HashMap::new);


    // Flag to prevent recursion in collision detection
    private static final ThreadLocal<Boolean> IN_CUSTOM_COLLISION = new ThreadLocal<>();

    public static void setCurrentEntity(Entity entity) {
        CURRENT_ENTITY.set(entity);
        if (entity != null) {
            LAST_KNOWN_ENTITY.set(entity);
        }
    }

    public static Entity getCurrentEntity() {
        Entity current = CURRENT_ENTITY.get();
        if (current != null) {
            return current;
        }
        // Fall back to last known entity if current is null
        return LAST_KNOWN_ENTITY.get();
    }

    public static void clearCurrentEntity() {
        CURRENT_ENTITY.remove();
        // Note: We intentionally don't clear LAST_KNOWN_ENTITY here
    }

    public static void clearAllEntityReferences() {
        CURRENT_ENTITY.remove();
        LAST_KNOWN_ENTITY.remove();
    }
    
    /**
     * Set whether we're currently inside a custom collision calculation.
     * This is used to prevent recursion in collision detection.
     *
     * @param inCustomCollision True if we're inside a custom collision calculation, false otherwise
     */
    public static void setInCustomCollision(boolean inCustomCollision) {
        IN_CUSTOM_COLLISION.set(inCustomCollision);
    }

    /**
     * Check if we're currently inside a custom collision calculation.
     *
     * @return True if we're inside a custom collision calculation, false otherwise
     */
    public static boolean isInCustomCollision() {
        Boolean value = IN_CUSTOM_COLLISION.get();
        return value != null && value;
    }

    /**
     * Clear the custom collision flag from the thread-local storage.
     * This should be called after collision processing is complete to prevent memory leaks.
     */
    public static void clearInCustomCollision() {
        IN_CUSTOM_COLLISION.remove();
    }

    public static void setLastMotion(Entity e, Vec3 v) {
        lastMotion.get().put(e, v);
    }
    public static Vec3 getLastMotion(Entity e) {
        return lastMotion.get().getOrDefault(e, Vec3.ZERO);
    }

    public static void setGravityDirection(Entity e, Vec3 dir) {
        gravityDir.get().put(e, dir);
    }
    public static Vec3 getGravityDirection(Entity e) {
        return gravityDir.get().getOrDefault(e, new Vec3(0, -1, 0));
    }
}
