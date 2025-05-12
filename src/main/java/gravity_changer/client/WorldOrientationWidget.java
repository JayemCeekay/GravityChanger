package gravity_changer.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Widget that displays the standard world orientation.
 * This widget is fixed to the standard Minecraft coordinate system
 * regardless of player gravity, helping players maintain spatial awareness.
 */
public class WorldOrientationWidget {
    private static final int WIDGET_SIZE = 80;
    private static final int WIDGET_MARGIN = 10;

    private static final Map<Direction, Vector3f> DIRECTION_VECTORS = new HashMap<>();
    private static final Map<Direction, Integer> DIRECTION_COLORS = new HashMap<>();

    // Track previous state for smooth animations
    private static float prevYaw = 0;
    private static float prevPitch = 0;

    static {
        // Setup direction vectors - these are fixed world coordinates
        DIRECTION_VECTORS.put(Direction.NORTH, new Vector3f(0, 0, -1));
        DIRECTION_VECTORS.put(Direction.SOUTH, new Vector3f(0, 0, 1));
        DIRECTION_VECTORS.put(Direction.EAST, new Vector3f(1, 0, 0));
        DIRECTION_VECTORS.put(Direction.WEST, new Vector3f(-1, 0, 0));
        DIRECTION_VECTORS.put(Direction.UP, new Vector3f(0, 1, 0));
        DIRECTION_VECTORS.put(Direction.DOWN, new Vector3f(0, -1, 0));

        // Setup direction colors - same as the player orientation widget for consistency
        DIRECTION_COLORS.put(Direction.NORTH, 0xFF0000FF); // Blue
        DIRECTION_COLORS.put(Direction.SOUTH, 0xFFFF00FF); // Magenta
        DIRECTION_COLORS.put(Direction.EAST, 0xFFFF0000); // Red
        DIRECTION_COLORS.put(Direction.WEST, 0xFF00FF00); // Green
        DIRECTION_COLORS.put(Direction.UP, 0xFFFFFF00); // Yellow
        DIRECTION_COLORS.put(Direction.DOWN, 0xFF00FFFF); // Cyan
    }

    /**
     * Register the world orientation widget to be rendered on the HUD.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(WorldOrientationWidget::render);
    }

    private static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        Player player = minecraft.player;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Position in bottom left corner to distinguish from player orientation widget
        int x = WIDGET_MARGIN;
        int y = screenHeight - WIDGET_SIZE - WIDGET_MARGIN;

        // Draw a solid background
        graphics.fill(x, y, x + WIDGET_SIZE, y + WIDGET_SIZE, 0xFF222222);

        // Draw border
        graphics.hLine(x, x + WIDGET_SIZE, y, 0xFFFFFFFF);
        graphics.hLine(x, x + WIDGET_SIZE, y + WIDGET_SIZE, 0xFFFFFFFF);
        graphics.vLine(x, y, y + WIDGET_SIZE, 0xFFFFFFFF);
        graphics.vLine(x + WIDGET_SIZE, y, y + WIDGET_SIZE, 0xFFFFFFFF);

        // Add title
        graphics.drawString(minecraft.font, "World Axes", x + 5, y + 5, 0xFFFFFFFF);

        // Center of widget
        int centerX = x + WIDGET_SIZE / 2;
        int centerY = y + WIDGET_SIZE / 2;

        // Get camera orientation - we want smooth transitions
        float yaw = minecraft.gameRenderer.getMainCamera().getYRot();
        float pitch = minecraft.gameRenderer.getMainCamera().getXRot();

        // Smooth interpolation for camera movement
        float interpolatedYaw = lerpAngle(prevYaw, yaw, 0.3f);
        float interpolatedPitch = lerpAngle(prevPitch, pitch, 0.3f);

        // Draw 3D world axes with 2D techniques
        drawWorldAxes(graphics, centerX, centerY, 25, interpolatedYaw, interpolatedPitch);

        // Add coordinate hints
        graphics.drawString(minecraft.font, "X/Z", centerX - 12, y + WIDGET_SIZE - 15, 0xFFAAAAAA);

        // Store for next frame
        prevYaw = interpolatedYaw;
        prevPitch = interpolatedPitch;
    }

    /**
     * Draw world axes with labels and help context for the player
     */
    private static void drawWorldAxes(GuiGraphics graphics, int centerX, int centerY, int radius, float yaw, float pitch) {
        // Draw the widget background with grid
        drawGradientCircle(graphics, centerX, centerY, radius, 0xFF333333, 0xFF111111);
        drawCircle(graphics, centerX, centerY, radius/2, 0x55777777);
        drawCircle(graphics, centerX, centerY, radius, 0x55777777);

        // Convert camera angles to radians
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Draw each world axis
        for (Direction direction : Direction.values()) {
            Vector3f dirVec = DIRECTION_VECTORS.get(direction);
            int color = DIRECTION_COLORS.get(direction);

            // Project the 3D direction onto 2D screen space
            Vector2f screenPos = projectTo2D(dirVec, yawRad, pitchRad, radius);

            // Draw direction marker - only if it's on the visible side
            if (isVisible(dirVec, yawRad, pitchRad)) {
                int markerX = centerX + (int)screenPos.x;
                int markerY = centerY + (int)screenPos.y;

                // Make major axes more prominent
                boolean isMajorAxis = (direction == Direction.UP || direction == Direction.NORTH ||
                        direction == Direction.EAST);
                int markerSize = isMajorAxis ? 6 : 4;

                // Draw line from center to marker with a gradient
                drawGradientLine(graphics, centerX, centerY, markerX, markerY, 0x33FFFFFF, color);

                // Draw direction marker
                drawFilledCircle(graphics, markerX, markerY, markerSize, color);

                // Draw coordinate label
                String label = getCoordinateLabel(direction);
                float distance = (float)Math.sqrt(screenPos.x * screenPos.x + screenPos.y * screenPos.y);
                if (distance > radius * 0.4f) {
                    graphics.drawString(Minecraft.getInstance().font, label,
                            markerX - Minecraft.getInstance().font.width(label) / 2,
                            markerY - 10,
                            color);
                }
            }
        }

        // Draw additional coordinate system information
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            // Show player's current position in world coordinates
            int posX = (int)player.getX();
            int posY = (int)player.getY();
            int posZ = (int)player.getZ();
            String posString = String.format("%d,%d,%d", posX, posY, posZ);
            graphics.drawString(minecraft.font, posString,
                    centerX - minecraft.font.width(posString) / 2,
                    centerY + radius + 5, 0xFFAAAAAA);
        }
    }

    /**
     * Get coordinate labels (+X, -Z, etc.)
     */
    private static String getCoordinateLabel(Direction direction) {
        switch (direction) {
            case NORTH: return "-Z";
            case SOUTH: return "+Z";
            case EAST: return "+X";
            case WEST: return "-X";
            case UP: return "+Y";
            case DOWN: return "-Y";
            default: return "";
        }
    }

    /**
     * Draw a gradient filled circle
     */
    private static void drawGradientCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int innerColor, int outerColor) {
        for (int r = 0; r <= radius; r++) {
            float ratio = (float)r / radius;
            int color = interpolateColor(innerColor, outerColor, ratio);
            drawCircleOutline(graphics, centerX, centerY, r, color);
        }
    }

    /**
     * Draw just the outline of a circle
     */
    private static void drawCircleOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int x = radius;
        int y = 0;
        int radiusError = 1 - x;

        while (x >= y) {
            graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
            graphics.fill(centerX - x, centerY + y, centerX - x + 1, centerY + y + 1, color);
            graphics.fill(centerX + x, centerY - y, centerX + x + 1, centerY - y + 1, color);
            graphics.fill(centerX - x, centerY - y, centerX - x + 1, centerY - y + 1, color);
            graphics.fill(centerX + y, centerY + x, centerX + y + 1, centerY + x + 1, color);
            graphics.fill(centerX - y, centerY + x, centerX - y + 1, centerY + x + 1, color);
            graphics.fill(centerX + y, centerY - x, centerX + y + 1, centerY - x + 1, color);
            graphics.fill(centerX - y, centerY - x, centerX - y + 1, centerY - x + 1, color);

            y++;

            if (radiusError < 0) {
                radiusError += 2 * y + 1;
            } else {
                x--;
                radiusError += 2 * (y - x) + 1;
            }
        }
    }

    /**
     * Draw a gradient line
     */
    private static void drawGradientLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int startColor, int endColor) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int totalSteps = Math.max(dx, dy);
        int step = 0;

        while (true) {
            float ratio = totalSteps > 0 ? (float)step / totalSteps : 0;
            int color = interpolateColor(startColor, endColor, ratio);
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);

            if (x1 == x2 && y1 == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
            step++;
        }
    }

    /**
     * Interpolate between two colors
     */
    private static int interpolateColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * ratio);
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Helper method to draw a circle
     */
    private static void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int x = radius;
        int y = 0;
        int radiusError = 1 - x;

        while (x >= y) {
            // Draw 8 octants
            graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
            graphics.fill(centerX - x, centerY + y, centerX - x + 1, centerY + y + 1, color);
            graphics.fill(centerX + x, centerY - y, centerX + x + 1, centerY - y + 1, color);
            graphics.fill(centerX - x, centerY - y, centerX - x + 1, centerY - y + 1, color);
            graphics.fill(centerX + y, centerY + x, centerX + y + 1, centerY + x + 1, color);
            graphics.fill(centerX - y, centerY + x, centerX - y + 1, centerY + x + 1, color);
            graphics.fill(centerX + y, centerY - x, centerX + y + 1, centerY - x + 1, color);
            graphics.fill(centerX - y, centerY - x, centerX - y + 1, centerY - x + 1, color);

            y++;

            if (radiusError < 0) {
                radiusError += 2 * y + 1;
            } else {
                x--;
                radiusError += 2 * (y - x) + 1;
            }
        }
    }

    /**
     * Helper method to draw a filled circle
     */
    private static void drawFilledCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x*x + y*y <= radius*radius) {
                    graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    /**
     * Project a 3D direction vector to 2D screen space
     */
    private static Vector2f projectTo2D(Vector3f vec, float yawRad, float pitchRad, float scale) {
        // Rotate the vector based on camera orientation
        float x = vec.x;
        float y = vec.y;
        float z = vec.z;

        // Apply yaw rotation
        float tempX = x * (float)Math.cos(-yawRad) - z * (float)Math.sin(-yawRad);
        float tempZ = x * (float)Math.sin(-yawRad) + z * (float)Math.cos(-yawRad);
        x = tempX;
        z = tempZ;

        // Apply pitch rotation
        float tempY = y * (float)Math.cos(pitchRad) - z * (float)Math.sin(pitchRad);
        tempZ = y * (float)Math.sin(pitchRad) + z * (float)Math.cos(pitchRad);
        y = tempY;
        z = tempZ;

        // Project to 2D (simple orthographic projection)
        return new Vector2f(x * scale, y * scale);
    }

    /**
     * Check if a direction is visible from the current camera angle
     */
    private static boolean isVisible(Vector3f vec, float yawRad, float pitchRad) {
        // Rotate the vector based on camera orientation (as in projectTo2D)
        float x = vec.x;
        float y = vec.y;
        float z = vec.z;

        // Apply yaw rotation
        float tempX = x * (float)Math.cos(-yawRad) - z * (float)Math.sin(-yawRad);
        float tempZ = x * (float)Math.sin(-yawRad) + z * (float)Math.cos(-yawRad);
        x = tempX;
        z = tempZ;

        // Apply pitch rotation
        float tempY = y * (float)Math.cos(pitchRad) - z * (float)Math.sin(pitchRad);
        tempZ = y * (float)Math.sin(pitchRad) + z * (float)Math.cos(pitchRad);

        // If z is positive after rotation, the point is in front of the camera
        return tempZ > 0;
    }

    /**
     * Helper method for smooth angle interpolation
     */
    private static float lerpAngle(float start, float end, float delta) {
        float diff = end - start;
        while (diff < -180f) diff += 360f;
        while (diff > 180f) diff -= 360f;
        return start + diff * delta;
    }

    /**
     * Simple 2D vector class
     */
    private static class Vector2f {
        public float x;
        public float y;

        public Vector2f(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}