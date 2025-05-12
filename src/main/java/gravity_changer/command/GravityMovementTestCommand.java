package gravity_changer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import gravity_changer.GravityChangerMod;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.KeyboardInputHandler;
import gravity_changer.util.RotationUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GravityMovementTestCommand {
    private static final String[] MOVEMENT_DIRECTIONS = {"forward", "backward", "left", "right", "up", "down"};

    // Define test angles for yaw and pitch
    private static final float[] TEST_YAW_ANGLES = {0f, 45f, 90f, 180f, 270f, 315f};
    private static final float[] TEST_PITCH_ANGLES = {0f, 45f, 90f, -45f, -90f};

    // Test duration in ticks (3 seconds = 60 ticks at 20 ticks per second)
    private static final int TEST_DURATION_TICKS = 60;

    // State tracking for the test
    private static AtomicBoolean isTestRunning = new AtomicBoolean(false);
    private static AtomicReference<BufferedWriter> currentWriter = new AtomicReference<>(null);
    private static AtomicReference<Player> currentPlayer = new AtomicReference<>(null);
    private static AtomicReference<FabricClientCommandSource> currentSource = new AtomicReference<>(null);
    private static AtomicReference<String> currentFilePath = new AtomicReference<>(null);

    // Test state indices
    private static AtomicInteger yawIndex = new AtomicInteger(0);
    private static AtomicInteger pitchIndex = new AtomicInteger(0);
    private static AtomicInteger directionIndex = new AtomicInteger(0);
    private static AtomicInteger tickCounter = new AtomicInteger(0);

    // Original player state
    private static float originalYaw;
    private static float originalPitch;
    private static Vec3 originalPosition;
    private static Vec3 originalVelocity;

    // Current test data
    private static Vec3 startPosition;
    private static Vec3 currentInputVector;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("gravitymovementtest")
                .executes(context -> executeMovementTest(context.getSource(), "movement_test"))
                .then(ClientCommandManager.argument("filename", StringArgumentType.word())
                    .executes(context -> executeMovementTest(context.getSource(), StringArgumentType.getString(context, "filename"))))
        );
    }

    private static int executeMovementTest(FabricClientCommandSource source, String baseFilename) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            source.sendFeedback(Component.literal("Player not found"));
            return 0;
        }

        if (!player.getAbilities().flying) {
            source.sendFeedback(Component.literal("Player must be in creative flight mode"));
            return 0;
        }

        // Check if a test is already running
        if (isTestRunning.get()) {
            source.sendFeedback(Component.literal("A movement test is already running"));
            return 0;
        }

        try {
            // Create directory if it doesn't exist
            File directory = new File("gravity_diagnostics");
            if (!directory.exists()) {
                directory.mkdir();
            }

            // Create file with timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            File file = new File(directory, baseFilename + "_" + timestamp + ".txt");

            // Store the file path for feedback
            currentFilePath.set(file.getPath());

            // Initialize the writer and keep it open for the duration of the test
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            // Write header
            writer.write("=== Gravity Movement Test ===\n");
            writer.write("Timestamp: " + timestamp + "\n\n");

            // Save original player state
            originalYaw = player.getYRot();
            originalPitch = player.getXRot();
            originalPosition = player.position();
            originalVelocity = player.getDeltaMovement();

            // Get gravity information
            Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(player);

            writer.write("Player Information:\n");
            writer.write("  Position: " + formatVec3(originalPosition) + "\n");
            writer.write("  Velocity: " + formatVec3(originalVelocity) + "\n");
            writer.write("  Gravity Direction Vector: " + formatVec3(gravityDirectionVec) + "\n\n");

            writer.write("Movement Test Results:\n");
            writer.flush();

            // Initialize test state
            currentWriter.set(writer);
            currentPlayer.set(player);
            currentSource.set(source);

            yawIndex.set(0);
            pitchIndex.set(0);
            directionIndex.set(0);
            tickCounter.set(0);

            // Start the test
            isTestRunning.set(true);

            // Register tick handler if not already registered
            registerTickHandler();

            source.sendFeedback(Component.literal("Starting movement test..."));
            return 1;
        } catch (IOException e) {
            source.sendFeedback(Component.literal("Error writing to file: " + e.getMessage()));
            GravityChangerMod.LOGGER.error("Error writing movement test to file", e);
            return 0;
        }
    }

    // Static tick handler registration flag
    private static boolean tickHandlerRegistered = false;

    /**
     * Registers the tick handler if it hasn't been registered already
     */
    private static void registerTickHandler() {
        if (!tickHandlerRegistered) {
            ClientTickEvents.START_CLIENT_TICK.register(client -> {
                if (isTestRunning.get() && client.player != null) {
                    tickTest(client);
                }
            });
            tickHandlerRegistered = true;
        }
    }

    /**
     * Handles a single tick of the test
     */
    private static void tickTest(Minecraft client) {
        Player player = currentPlayer.get();
        if (player == null || !player.isAlive() || !player.getAbilities().flying) {
            // Player is no longer valid, stop the test
            cleanupTest("Test stopped: player is no longer valid");
            return;
        }

        try {
            // Get current test parameters
            int currentYawIndex = yawIndex.get();
            int currentPitchIndex = pitchIndex.get();
            int currentDirectionIndex = directionIndex.get();
            int currentTick = tickCounter.get();

            if (currentYawIndex >= TEST_YAW_ANGLES.length) {
                // All tests completed
                cleanupTest("Movement test completed successfully");
                return;
            }

            float yaw = TEST_YAW_ANGLES[currentYawIndex];
            float pitch = TEST_PITCH_ANGLES[currentPitchIndex];
            String direction = MOVEMENT_DIRECTIONS[currentDirectionIndex];

            // If we're starting a new test
            if (currentTick == 0) {
                // Set camera orientation
                player.setYRot(yaw);
                player.setXRot(pitch);

                // Get input vector for this direction
                currentInputVector = getInputVectorForDirection(direction);

                // Record start position
                startPosition = player.position();

                // Log start of test
                BufferedWriter writer = currentWriter.get();
                if (currentDirectionIndex == 0) {
                    writer.write("\n=== Camera Orientation: Yaw " + yaw + "°, Pitch " + pitch + "° ===\n");
                }

                // Get gravity direction for reference
                Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(player);
                Vec3 worldVector = RotationUtil.vecPlayerToWorldVec(currentInputVector, gravityDirectionVec);

                writer.write("  Direction: " + direction + "\n");
                writer.write("    Raw Input Vector: " + formatVec3(currentInputVector) + "\n");
                writer.write("    Transformed World Vector: " + formatVec3(worldVector) + "\n");
                writer.write("    Start Position: " + formatVec3(startPosition) + "\n");
                writer.flush();
            }

            // Apply input to player
            applyInputToPlayer(player, direction);

            // Increment tick counter
            int newTick = currentTick + 1;
            tickCounter.set(newTick);

            // If we've reached the end of this test
            if (newTick >= TEST_DURATION_TICKS) {
                // Log results
                Vec3 endPosition = player.position();
                Vec3 deltaMovement = player.getDeltaMovement();
                Vec3 totalMovement = endPosition.subtract(startPosition);

                // Get gravity direction for reference
                Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(player);
                Vec3 worldVector = RotationUtil.vecPlayerToWorldVec(currentInputVector, gravityDirectionVec);

                BufferedWriter writer = currentWriter.get();
                writer.write("    Raw Input Vector: " + formatVec3(currentInputVector) + "\n");
                writer.write("    Transformed World Vector: " + formatVec3(worldVector) + "\n");
                writer.write("    Actual End Position: " + formatVec3(endPosition) + "\n");
                writer.write("    Actual Total Movement: " + formatVec3(totalMovement) + "\n");
                writer.write("    Actual Final Velocity: " + formatVec3(deltaMovement) + "\n\n");
                writer.flush();

                // Move to next test
                tickCounter.set(0);

                // Increment direction index
                int newDirectionIndex = currentDirectionIndex + 1;
                if (newDirectionIndex >= MOVEMENT_DIRECTIONS.length) {
                    // Move to next pitch
                    directionIndex.set(0);
                    int newPitchIndex = currentPitchIndex + 1;
                    if (newPitchIndex >= TEST_PITCH_ANGLES.length) {
                        // Move to next yaw
                        pitchIndex.set(0);
                        yawIndex.set(currentYawIndex + 1);
                    } else {
                        pitchIndex.set(newPitchIndex);
                    }
                } else {
                    directionIndex.set(newDirectionIndex);
                }
            }
        } catch (Exception e) {
            GravityChangerMod.LOGGER.error("Error during movement test", e);
            cleanupTest("Test stopped due to error: " + e.getMessage());
        }
    }

    /**
     * Applies the specified input direction to the player using emulated keyboard input
     */
    private static void applyInputToPlayer(Player player, String direction) {
        // Set the appropriate keyboard input flags based on the direction
        boolean up = false;
        boolean down = false;
        boolean left = false;
        boolean right = false;
        boolean jumping = false;
        boolean shiftKeyDown = false;

        switch (direction) {
            case "forward":
                up = true;
                break;
            case "backward":
                down = true;
                break;
            case "left":
                left = true;
                break;
            case "right":
                right = true;
                break;
            case "up":
                jumping = true;
                break;
            case "down":
                shiftKeyDown = true;
                break;
        }

        // Apply the emulated input using the KeyboardInputHandler
        KeyboardInputHandler.setEmulatedInput(up, down, left, right, jumping, shiftKeyDown);
    }

    /**
     * Cleans up the test and restores the player's state
     */
    private static void cleanupTest(String message) {
        try {
            // Restore player state
            Player player = currentPlayer.get();
            if (player != null) {
                player.setYRot(originalYaw);
                player.setXRot(originalPitch);
                player.setDeltaMovement(originalVelocity);
            }

            // Close writer
            BufferedWriter writer = currentWriter.get();
            if (writer != null) {
                writer.close();
            }

            // Send feedback
            FabricClientCommandSource source = currentSource.get();
            if (source != null) {
                source.sendFeedback(Component.literal(message));
                if (currentFilePath.get() != null) {
                    source.sendFeedback(Component.literal("Results saved to " + currentFilePath.get()));
                }
            }
        } catch (Exception e) {
            GravityChangerMod.LOGGER.error("Error cleaning up movement test", e);
        } finally {
            // Reset test state
            isTestRunning.set(false);
            currentWriter.set(null);
            currentPlayer.set(null);
            currentSource.set(null);
            currentFilePath.set(null);

            // Reset emulated input
            KeyboardInputHandler.resetEmulatedInput();
        }
    }

    private static Vec3 getInputVectorForDirection(String direction) {
        switch (direction) {
            case "forward":
                return new Vec3(0, 0, 1);
            case "backward":
                return new Vec3(0, 0, -1);
            case "left":
                return new Vec3(-1, 0, 0);
            case "right":
                return new Vec3(1, 0, 0);
            case "up":
                return new Vec3(0, 1, 0);
            case "down":
                return new Vec3(0, -1, 0);
            default:
                return Vec3.ZERO;
        }
    }

    private static String formatVec3(Vec3 vec) {
        return String.format("(%.6f, %.6f, %.6f)", vec.x, vec.y, vec.z);
    }
}
