package gravity_changer.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class to handle emulated keyboard input for testing
 */
public class KeyboardInputHandler {
    // Static fields to control the emulated input
    private static final AtomicBoolean emulatingInput = new AtomicBoolean(false);
    private static final AtomicReference<Boolean> emulatedUp = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> emulatedDown = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> emulatedLeft = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> emulatedRight = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> emulatedJumping = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> emulatedShiftKeyDown = new AtomicReference<>(false);

    /**
     * Set the emulated input values
     */
    public static void setEmulatedInput(boolean up, boolean down, boolean left, boolean right, boolean jumping, boolean shiftKeyDown) {
        emulatedUp.set(up);
        emulatedDown.set(down);
        emulatedLeft.set(left);
        emulatedRight.set(right);
        emulatedJumping.set(jumping);
        emulatedShiftKeyDown.set(shiftKeyDown);
        emulatingInput.set(true);
    }

    /**
     * Reset emulated input to use real keyboard input
     */
    public static void resetEmulatedInput() {
        emulatingInput.set(false);
    }

    /**
     * Check if we're currently emulating input
     */
    public static boolean isEmulatingInput() {
        return emulatingInput.get();
    }

    /**
     * Get the emulated up state
     */
    public static boolean isEmulatedUp() {
        return emulatedUp.get();
    }

    /**
     * Get the emulated down state
     */
    public static boolean isEmulatedDown() {
        return emulatedDown.get();
    }

    /**
     * Get the emulated left state
     */
    public static boolean isEmulatedLeft() {
        return emulatedLeft.get();
    }

    /**
     * Get the emulated right state
     */
    public static boolean isEmulatedRight() {
        return emulatedRight.get();
    }

    /**
     * Get the emulated jumping state
     */
    public static boolean isEmulatedJumping() {
        return emulatedJumping.get();
    }

    /**
     * Get the emulated shift key down state
     */
    public static boolean isEmulatedShiftKeyDown() {
        return emulatedShiftKeyDown.get();
    }
}