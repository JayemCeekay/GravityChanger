package gravity_changer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import gravity_changer.GravityChangerMod;
import gravity_changer.GravityComponent;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.Rotor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Command for diagnosing and exporting gravity system information
 */
public class GravityDiagnosticCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("gravitydiag")
                        .requires(source -> source.hasPermission(0)) // Allow all players to use this command
                        .executes(context -> exportDiagnosticsToFile(context.getSource(), "gravity_diagnostics"))
                        .then(Commands.argument("filename", StringArgumentType.word())
                                .executes(context -> exportDiagnosticsToFile(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "filename")
                                ))
                        )
        );
    }

    private static int exportDiagnosticsToFile(CommandSourceStack source, String baseFilename) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) {
            source.sendFailure(Component.literal("This command must be executed by a player"));
            return 0;
        }

        // Generate filename with timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String filename = baseFilename + "_" + timestamp + ".txt";

        try {
            // Create directory if it doesn't exist
            File gravityDir = new File("gravity_diagnostics");
            if (!gravityDir.exists() && !gravityDir.mkdir()) {
                source.sendFailure(Component.literal("Failed to create directory: gravity_diagnostics")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }

            // Create file
            File outputFile = new File(gravityDir, filename);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                // Write diagnostics
                writeDiagnostics(writer, player);

                // Success message
                String path = outputFile.getAbsolutePath();
                source.sendSuccess(
                        () -> Component.literal("Gravity diagnostics exported to: ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(path).withStyle(ChatFormatting.YELLOW)),
                        true
                );

                // Send a message to the player with a summary of key information
                source.sendSuccess(
                        () -> Component.literal("Gravity Direction: ")
                                .withStyle(ChatFormatting.AQUA)
                                .append(Component.literal(formatVec3(GravityChangerAPI.getGravityDirectionVec(player)))
                                        .withStyle(ChatFormatting.WHITE)),
                        false
                );

                return 1;
            }
        } catch (SecurityException e) {
            source.sendFailure(Component.literal("Security error: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            GravityChangerMod.LOGGER.error("Security error while creating diagnostics file", e);
            return 0;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write diagnostics file: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            GravityChangerMod.LOGGER.error("IO error while writing diagnostics file", e);
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Unexpected error: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            GravityChangerMod.LOGGER.error("Unexpected error while creating diagnostics file", e);
            return 0;
        }
    }

    private static void writeDiagnostics(BufferedWriter writer, Player player) throws IOException {
        // Get gravity component
        GravityComponent gravityComponent = GravityChangerAPI.GRAVITY_COMPONENT.get(player);
        boolean isClientSide = player.level().isClientSide;

        // Basic header
        writer.write("=== Gravity Changer Diagnostics ===");
        writer.newLine();
        writer.write("Generated: " + new Date().toString());
        writer.newLine();
        writer.write("Side: " + (isClientSide ? "Client" : "Server"));
        writer.newLine();
        writer.newLine();

        // Player information
        writer.write("--- Player Information ---");
        writer.newLine();
        writer.write("UUID: " + player.getUUID());
        writer.newLine();
        writer.write("Name: " + player.getName().getString());
        writer.newLine();
        writer.write("Position: " + formatVec3(player.position()));
        writer.newLine();
        writer.write("Dimension: " + player.level().dimension().location());
        writer.newLine();
        writer.newLine();

        // Camera information from client side
        if (isClientSide) {
            writer.write("--- Camera Information ---");
            writer.newLine();
            Camera mainCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
            writer.write("Camera Entity: " + (mainCamera.getEntity() != null ? mainCamera.getEntity().getName().getString() : "None"));
            writer.newLine();
            writer.write("Camera Position: " + formatVec3(new Vec3(mainCamera.getPosition().x, mainCamera.getPosition().y, mainCamera.getPosition().z)));
            writer.newLine();
            writer.write("Camera Yaw: " + mainCamera.getYRot());
            writer.newLine();
            writer.write("Camera Pitch: " + mainCamera.getXRot());
            writer.newLine();
            writer.write("Camera View Vector: " + formatVec3(getForwardVector(mainCamera)));
            writer.newLine();
            writer.write("Camera Up Vector: " + formatVec3(getUpVector(mainCamera)));
            writer.newLine();
            writer.write("Camera Type: " + mainCamera.getClass().getSimpleName());
            writer.newLine();
            writer.write("Camera Rotation: " + formatQuaternion(getCameraRotation(mainCamera)));
            writer.newLine();
            writer.write("Camera Detached: " + mainCamera.isDetached());
            writer.newLine();
            writer.write("Camera Rotation-Based Gravity Angle: " + calculateAngleBetween(getUpVector(mainCamera), gravityComponent.getCurrGravityDirectionVec().scale(-1)));
            writer.newLine();
            writer.newLine();
        } else {
            writer.write("--- Camera Information ---");
            writer.newLine();
            writer.write("Camera details unavailable on server side");
            writer.newLine();
            writer.newLine();
        }

        // Player rotation and view
        writer.write("--- Player View Information ---");
        writer.newLine();
        writer.write("Yaw: " + player.getYRot());
        writer.newLine();
        writer.write("Pitch: " + player.getXRot());
        writer.newLine();
        writer.write("View Vector: " + formatVec3(player.getViewVector(1.0F)));
        writer.newLine();
        writer.write("Looking Direction: " + player.getDirection());
        writer.newLine();
        writer.write("Head Yaw: " + player.yHeadRot);
        writer.newLine();
        writer.newLine();

        // Gravity information
        writer.write("--- Gravity Information ---");
        writer.newLine();
        writer.write("Base Direction: " + formatVec3(gravityComponent.getBaseGravityDirectionVec()));
        writer.newLine();
        writer.write("Direction Vector: " + formatVec3(gravityComponent.getCurrGravityDirectionVec()));
        writer.newLine();
        writer.write("Base Strength: " + gravityComponent.getBaseGravityStrength());
        writer.newLine();
        writer.write("Current Strength: " + gravityComponent.getCurrGravityStrength());
        writer.newLine();

        // Get animation status if available
        if (GravityChangerAPI.getRotationAnimation(player) != null) {
            writer.write("In Animation: " + GravityChangerAPI.getRotationAnimation(player).isInAnimation());
            writer.newLine();
            writer.write("Animation Time: " + GravityChangerAPI.getRotationAnimation(player).getAnimationTimeMs());
            writer.newLine();
        }
        writer.newLine();

        // Movement information
        writer.write("--- Movement Information ---");
        writer.newLine();
        writer.write("Velocity: " + formatVec3(player.getDeltaMovement()));
        writer.newLine();
        writer.write("On Ground: " + player.onGround());
        writer.newLine();
        writer.write("Sprint: " + player.isSprinting());
        writer.newLine();
        writer.write("Swimming: " + player.isSwimming());
        writer.newLine();
        writer.write("Flying: " + player.getAbilities().flying);
        writer.newLine();
        writer.newLine();

        // Vanilla Bounding box information
        AABB box = player.getBoundingBox();
        writer.write("--- Vanilla Bounding Box Information ---");
        writer.newLine();
        writer.write("Min: " + formatVec3(new Vec3(box.minX, box.minY, box.minZ)));
        writer.newLine();
        writer.write("Max: " + formatVec3(new Vec3(box.maxX, box.maxY, box.maxZ)));
        writer.newLine();
        writer.write("Size: " + formatVec3(new Vec3(box.getXsize(), box.getYsize(), box.getZsize())));
        writer.newLine();
        writer.write("Center: " + formatVec3(box.getCenter()));
        writer.newLine();

        // Rotated box information (calculate how the box would look in default gravity)
        Vec3 defaultGravity = new Vec3(0, -1, 0);
        if (!gravityComponent.getCurrGravityDirectionVec().equals(defaultGravity)) {
            AABB rotatedBox = player.getBoundingBox();
            writer.write("Gravity-Rotated Box Min: " + formatVec3(new Vec3(rotatedBox.minX, rotatedBox.minY, rotatedBox.minZ)));
            writer.newLine();
            writer.write("Gravity-Rotated Box Max: " + formatVec3(new Vec3(rotatedBox.maxX, rotatedBox.maxY, rotatedBox.maxZ)));
            writer.newLine();
            writer.write("Gravity-Rotated Box Size: " + formatVec3(new Vec3(rotatedBox.getXsize(), rotatedBox.getYsize(), rotatedBox.getZsize())));
            writer.newLine();
        }
        writer.newLine();

        // Rotation information
        writer.write("--- Rotation Matrices and Angles ---");
        writer.newLine();

        // Calculate rotors and quaternions between gravity directions
        Rotor gravityRotor = Rotor.from(new Vec3(0, -1, 0), gravityComponent.getCurrGravityDirectionVec());
        writer.write("Gravity Rotor: " + gravityRotor.toString());
        writer.newLine();

        // Get angles between standard directions and gravity
        for (Direction dir : Direction.values()) {
            Vec3 dirVec = Vec3.atLowerCornerOf(dir.getNormal());
            double angle = Math.toDegrees(Math.acos(dirVec.dot(gravityComponent.getCurrGravityDirectionVec())));
            writer.write("Angle between " + dir + " and gravity: " + String.format("%.2f", angle) + "°");
            writer.newLine();
        }
        writer.newLine();

        // Oriented Bounding Box information
        writer.write("--- Oriented Bounding Box Information ---");
        writer.newLine();

        // Only create an OrientedBoundingBox if we're not using default gravity
        if (!gravityComponent.getCurrGravityDirectionVec().equals(defaultGravity)) {
            // Create an OrientedBoundingBox from the player's AABB
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBB(
                player.getBoundingBox(), 
                gravityComponent.getCurrGravityDirectionVec(),
                player
            );

            // Get shrink factors and offsets
            Vec3 shrinkFactors = OrientedBoundingBoxTransformer.calculateShrinkFactors(player);
            Vec3 dynamicShrinkFactors = OrientedBoundingBoxTransformer.calculateDynamicShrinkFactors(
                player, 
                gravityComponent.getCurrGravityDirectionVec(), 
                player.getBoundingBox()
            );
            Vec3 offset = OrientedBoundingBoxTransformer.calculateOffset(player);
            Vec3 dynamicOffset = OrientedBoundingBoxTransformer.calculateDynamicOffset(
                player, 
                gravityComponent.getCurrGravityDirectionVec(), 
                player.getBoundingBox()
            );

            // Write the OrientedBoundingBox information
            writer.write("Local Box Min: " + formatVec3(new Vec3(obb.getLocalBox().minX, obb.getLocalBox().minY, obb.getLocalBox().minZ)));
            writer.newLine();
            writer.write("Local Box Max: " + formatVec3(new Vec3(obb.getLocalBox().maxX, obb.getLocalBox().maxY, obb.getLocalBox().maxZ)));
            writer.newLine();
            writer.write("Local Box Size: " + formatVec3(new Vec3(obb.getLocalBox().getXsize(), obb.getLocalBox().getYsize(), obb.getLocalBox().getZsize())));
            writer.newLine();
            writer.write("Center: " + formatVec3(obb.getCenter()));
            writer.newLine();
            writer.write("Rotation: " + obb.getRotation().toString());
            writer.newLine();
            writer.write("OBB Min: " + formatVec3(new Vec3(obb.minX, obb.minY, obb.minZ)));
            writer.newLine();
            writer.write("OBB Max: " + formatVec3(new Vec3(obb.maxX, obb.maxY, obb.maxZ)));
            writer.newLine();
            writer.write("OBB Size: " + formatVec3(new Vec3(obb.getXsize(), obb.getYsize(), obb.getZsize())));
            writer.newLine();

            // Write shrink factors and offsets
            writer.write("--- OBB Shrink Factors and Offsets ---");
            writer.newLine();
            writer.write("Default Shrink Factors: " + formatVec3(shrinkFactors));
            writer.newLine();
            writer.write("Dynamic Shrink Factors: " + formatVec3(dynamicShrinkFactors));
            writer.newLine();
            writer.write("Default Offset: " + formatVec3(offset));
            writer.newLine();
            writer.write("Dynamic Offset: " + formatVec3(dynamicOffset));
            writer.newLine();

            // Check if we're using diagonal gravity (which triggers shrinking)
            boolean isDiagonalGravity = Math.abs(gravityComponent.getCurrGravityDirectionVec().x) > 0.01 || 
                                       Math.abs(gravityComponent.getCurrGravityDirectionVec().z) > 0.01;
            writer.write("Using Diagonal Gravity: " + isDiagonalGravity);
            writer.newLine();
            writer.newLine();

            // Calculate and write the corners of the oriented bounding box
            writer.write("Corners of the Oriented Bounding Box:");
            writer.newLine();

            // Get the 8 corners of the local box
            Vec3[] corners = new Vec3[8];
            corners[0] = new Vec3(obb.getLocalBox().minX, obb.getLocalBox().minY, obb.getLocalBox().minZ);
            corners[1] = new Vec3(obb.getLocalBox().maxX, obb.getLocalBox().minY, obb.getLocalBox().minZ);
            corners[2] = new Vec3(obb.getLocalBox().minX, obb.getLocalBox().maxY, obb.getLocalBox().minZ);
            corners[3] = new Vec3(obb.getLocalBox().maxX, obb.getLocalBox().maxY, obb.getLocalBox().minZ);
            corners[4] = new Vec3(obb.getLocalBox().minX, obb.getLocalBox().minY, obb.getLocalBox().maxZ);
            corners[5] = new Vec3(obb.getLocalBox().maxX, obb.getLocalBox().minY, obb.getLocalBox().maxZ);
            corners[6] = new Vec3(obb.getLocalBox().minX, obb.getLocalBox().maxY, obb.getLocalBox().maxZ);
            corners[7] = new Vec3(obb.getLocalBox().maxX, obb.getLocalBox().maxY, obb.getLocalBox().maxZ);

            // Rotate each corner and write to the file
            for (int i = 0; i < 8; i++) {
                Vec3 rotatedCorner = obb.getRotation().rotate(corners[i]).add(obb.getCenter());
                writer.write("Corner " + (i + 1) + ": " + formatVec3(rotatedCorner));
                writer.newLine();
            }
        } else {
            writer.write("No oriented bounding box information available (using default gravity)");
            writer.newLine();
        }
        writer.newLine();

        // Environment information
        writer.write("--- Environment Information ---");
        writer.newLine();
        writer.write("In Water: " + player.isInWater());
        writer.newLine();
        writer.write("In Lava: " + player.isInLava());
        writer.newLine();
        writer.write("Underwater: " + player.isUnderWater());
        writer.newLine();
        writer.write("Fall Distance: " + player.fallDistance);
        writer.newLine();
        writer.newLine();

        // Config information
        writer.write("--- Mod Configuration ---");
        writer.newLine();
        writer.write("Rotation Time: " + GravityChangerMod.config.rotationTime + "ms");
        writer.newLine();
        writer.write("World Velocity: " + GravityChangerMod.config.worldVelocity);
        writer.newLine();
        writer.write("Gravity Strength Multiplier: " + GravityChangerMod.config.gravityStrengthMultiplier);
        writer.newLine();
        writer.write("Reset on Respawn: " + GravityChangerMod.config.resetGravityOnRespawn);
        writer.newLine();
        writer.write("Void Damage Above World: " + GravityChangerMod.config.voidDamageAboveWorld);
        writer.newLine();
        writer.write("Void Damage On Horizontal Fall Too Far: " + GravityChangerMod.config.voidDamageOnHorizontalFallTooFar);
        writer.newLine();
        writer.write("Auto Jump On Gravity Plate Inner Corner: " + GravityChangerMod.config.autoJumpOnGravityPlateInnerCorner);
        writer.newLine();
        writer.write("Adjust Position After Changing Gravity: " + GravityChangerMod.config.adjustPositionAfterChangingGravity);
        writer.newLine();
        writer.newLine();

        // Version information
        writer.write("--- Version Information ---");
        writer.newLine();
        writer.write("Mod Version: 1.1.2");
        writer.newLine();
        writer.write("Minecraft Version: 1.20.1");
        writer.newLine();
        writer.write("Mod ID: gravity_changer_q");
        writer.newLine();
        writer.write("Mod Name: Gravity Changer (qouteall fork)");
        writer.newLine();
        writer.newLine();

        // System information
        writer.write("--- System Information ---");
        writer.newLine();
        writer.write("Java Version: " + System.getProperty("java.version"));
        writer.newLine();
        writer.write("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        writer.newLine();
        writer.write("Available Processors: " + Runtime.getRuntime().availableProcessors());
        writer.newLine();
        writer.write("Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");
        writer.newLine();
        writer.write("Total Memory: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB");
        writer.newLine();
        writer.write("Free Memory: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB");
        writer.newLine();
        writer.newLine();

        // Active effects information
        writer.write("--- Active Effects ---");
        writer.newLine();
        if (player.getActiveEffects().isEmpty()) {
            writer.write("No active effects");
            writer.newLine();
        } else {
            player.getActiveEffects().forEach(effect -> {
                try {
                    writer.write(effect.getEffect().getDisplayName().getString() + 
                                " (Level: " + (effect.getAmplifier() + 1) + 
                                ", Duration: " + (effect.getDuration() / 20) + "s)");
                    writer.newLine();
                } catch (IOException e) {
                    // Ignore exceptions during effect writing
                }
            });
        }
        writer.newLine();

        // Nearby entities information
        writer.write("--- Nearby Entities (within 16 blocks) ---");
        writer.newLine();
        int entityCount = 0;
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(16.0))) {
            try {
                entityCount++;
                writer.write(entity.getName().getString() + 
                            " (" + entity.getType().getDescriptionId() + 
                            ") at " + formatVec3(entity.position()) + 
                            ", Distance: " + String.format("%.2f", entity.position().distanceTo(player.position())) + 
                            ", Gravity: " + formatVec3(GravityChangerAPI.getGravityDirectionVec(entity)));
                writer.newLine();

                // Limit to 50 entities to avoid huge files
                if (entityCount >= 50) {
                    writer.write("... and more (limited to 50 entities)");
                    writer.newLine();
                    break;
                }
            } catch (Exception e) {
                try {
                    writer.write("Error getting entity info: " + e.getMessage());
                    writer.newLine();
                } catch (IOException ioe) {
                    // Ignore
                }
            }
        }
        if (entityCount == 0) {
            writer.write("No nearby entities found");
            writer.newLine();
        }
        writer.newLine();

        // Nearby gravity-affecting blocks
        writer.write("--- Nearby Gravity Plating Blocks ---");
        writer.newLine();
        try {
            int searchRadius = 16;
            int blockCount = 0;
            BlockPos playerBlockPos = player.blockPosition();

            // Get blocks in a cube around the player
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int y = -searchRadius; y <= searchRadius; y++) {
                    for (int z = -searchRadius; z <= searchRadius; z++) {
                        // Skip blocks that are too far away
                        if (x*x + y*y + z*z > searchRadius*searchRadius) {
                            continue;
                        }

                        BlockPos pos = playerBlockPos.offset(x, y, z);
                        BlockState blockState = player.level().getBlockState(pos);
                        Block block = blockState.getBlock();

                        // Check if this is a gravity plating block
                        if (block.getDescriptionId().contains("gravity_changer.plating")) {
                            blockCount++;
                            Vec3 blockCenter = new Vec3(
                                pos.getX() + 0.5, 
                                pos.getY() + 0.5, 
                                pos.getZ() + 0.5
                            );
                            double distance = player.position().distanceTo(blockCenter);

                            writer.write("Gravity Plating at " + formatVec3(blockCenter) + 
                                ", Distance: " + String.format("%.2f", distance));
                            writer.newLine();

                            // Limit to 20 blocks to avoid huge files
                            if (blockCount >= 20) {
                                writer.write("... and more (limited to 20 blocks)");
                                writer.newLine();
                                break;
                            }
                        }
                    }
                    if (blockCount >= 20) break;
                }
                if (blockCount >= 20) break;
            }

            if (blockCount == 0) {
                writer.write("No gravity plating blocks found nearby");
                writer.newLine();
            }
        } catch (Exception e) {
            writer.write("Error scanning for gravity plating blocks: " + e.getMessage());
            writer.newLine();
        }
        writer.newLine();
    }

    private static String formatVec3(Vec3 vec) {
        return String.format("(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z);
    }

    private static String formatQuaternion(Quaternionf quaternion) {
        return String.format("(%.2f, %.2f, %.2f, %.2f)", quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    }

    private static Vec3 getForwardVector(Camera camera) {
        Vector3f lookVec = new Vector3f(0, 0, 1);
        lookVec.rotate(camera.rotation());
        return new Vec3(-lookVec.x, -lookVec.y, -lookVec.z);
    }

    private static Vec3 getUpVector(Camera camera) {
        Vector3f upVec = new Vector3f(0, 1, 0);
        upVec.rotate(camera.rotation());
        return new Vec3(upVec.x, upVec.y, upVec.z);
    }

    private static Quaternionf getCameraRotation(Camera camera) {
        return camera.rotation();
    }

    private static double calculateAngleBetween(Vec3 vec1, Vec3 vec2) {
        double dot = vec1.normalize().dot(vec2.normalize());
        return Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, dot))));
    }
}
