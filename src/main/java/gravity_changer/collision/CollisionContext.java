package gravity_changer.collision;

import net.minecraft.world.entity.Entity;

/**
 * Thread-local context for tracking the current entity being processed for collision calculations.
 * This allows us to apply the correct gravity direction during collision detection.
 */
public class CollisionContext {
    private static final ThreadLocal<Entity> CURRENT_ENTITY = new ThreadLocal<>();

    // Flag to prevent recursion in collision detection
    private static final ThreadLocal<Boolean> IN_CUSTOM_COLLISION = new ThreadLocal<>();

    /**
     * Set the current entity being processed for collision calculations.
     * 
     * @param entity The entity being processed
     */
    public static void setCurrentEntity(Entity entity) {
        CURRENT_ENTITY.set(entity);
    }

    /**
     * Get the current entity being processed for collision calculations.
     * 
     * @return The current entity, or null if no entity is being processed
     */
    public static Entity getCurrentEntity() {
        return CURRENT_ENTITY.get();
    }

    /**
     * Clear the current entity from the thread-local storage.
     * This should be called after collision processing is complete to prevent memory leaks.
     */
    public static void clearCurrentEntity() {
        CURRENT_ENTITY.remove();
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
}
