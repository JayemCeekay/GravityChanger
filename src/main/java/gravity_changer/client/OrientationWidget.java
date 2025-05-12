package gravity_changer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gravity_changer.GravityComponent;
import gravity_changer.api.GravityChangerAPI;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Widget that displays the player's orientation relative to the world.
 * Shows the current gravity direction and world directions (N, E, S, W, Up, Down).
 */
public class OrientationWidget {
    private static final int WIDGET_SIZE = 80;
    private static final int WIDGET_MARGIN = 10;

    private static final Map<Direction, Vector3f> DIRECTION_VECTORS = new HashMap<>();
    private static final Map<Direction, Integer> DIRECTION_COLORS = new HashMap<>();

    // Track previous state for smooth animations
    private static float prevYaw = 0;
    private static float prevPitch = 0;
    private static Vector3f prevGravityVec = new Vector3f(0, -1, 0);

    static {
        // Setup direction vectors
        DIRECTION_VECTORS.put(Direction.NORTH, new Vector3f(0, 0, -1));
        DIRECTION_VECTORS.put(Direction.SOUTH, new Vector3f(0, 0, 1));
        DIRECTION_VECTORS.put(Direction.EAST, new Vector3f(1, 0, 0));
        DIRECTION_VECTORS.put(Direction.WEST, new Vector3f(-1, 0, 0));
        DIRECTION_VECTORS.put(Direction.UP, new Vector3f(0, 1, 0));
        DIRECTION_VECTORS.put(Direction.DOWN, new Vector3f(0, -1, 0));

        // Setup direction colors
        DIRECTION_COLORS.put(Direction.NORTH, 0xFF0000FF); // Blue
        DIRECTION_COLORS.put(Direction.SOUTH, 0xFFFF00FF); // Magenta
        DIRECTION_COLORS.put(Direction.EAST, 0xFFFF0000); // Red
        DIRECTION_COLORS.put(Direction.WEST, 0xFF00FF00); // Green
        DIRECTION_COLORS.put(Direction.UP, 0xFFFFFF00); // Yellow
        DIRECTION_COLORS.put(Direction.DOWN, 0xFF00FFFF); // Cyan
    }

    /**
     * Register the orientation widget to be rendered on the HUD.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(OrientationWidget::render);
    }

    private static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        Player player = minecraft.player;
        GravityComponent gravityComponent = GravityChangerAPI.GRAVITY_COMPONENT.get(player);
        Vec3 gravityDirection = gravityComponent.getCurrGravityDirectionVec();

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Position in bottom right corner
        int x = screenWidth - WIDGET_SIZE - WIDGET_MARGIN;
        int y = screenHeight - WIDGET_SIZE - WIDGET_MARGIN;

        // Draw a gradient background
        drawGradientRect(graphics, x, y, x + WIDGET_SIZE, y + WIDGET_SIZE, 0xFF333333, 0xFF111111);

        // Draw border with slightly rounded corners
        drawBorder(graphics, x, y, x + WIDGET_SIZE, y + WIDGET_SIZE, 0xAAFFFFFF);

        // Add fancy title with drop shadow effect
        drawFancyTitle(graphics, "Personal Orientation", x + 5, y + 5);

        // Center of widget
        int centerX = x + WIDGET_SIZE / 2;
        int centerY = y + WIDGET_SIZE / 2;

        // Get camera orientation
        float yaw = minecraft.gameRenderer.getMainCamera().getYRot();
        float pitch = minecraft.gameRenderer.getMainCamera().getXRot();

        // Smooth interpolation for better visual experience
        float interpolatedYaw = lerpAngle(prevYaw, yaw, 0.3f);
        float interpolatedPitch = lerpAngle(prevPitch, pitch, 0.3f);

        // Create current gravity vector
        Vector3f gravityVec = new Vector3f((float)gravityDirection.x, (float)gravityDirection.y, (float)gravityDirection.z);

        // Interpolate gravity vector for smooth transitions
        Vector3f interpolatedGravityVec = lerpVector(prevGravityVec, gravityVec, 0.3f);

        // Draw reference grid
        drawReferenceGrid(graphics, centerX, centerY, 25);

        // Draw 3D orientation with fancy techniques
        drawCompass(graphics, centerX, centerY, 25, interpolatedYaw, interpolatedPitch);

        // Draw gravity direction with enhanced visuals
        drawGravityIndicator(graphics, centerX, centerY, interpolatedGravityVec, interpolatedYaw, interpolatedPitch);

        // Add gravity info
        if (gravityVec != null) {
            // Format the vector with 2 decimal places to keep it readable
            String gravityInfo = String.format("Gravity: (%.2f, %.2f, %.2f)",
                    gravityVec.x, gravityVec.y, gravityVec.z);
            graphics.drawString(minecraft.font, gravityInfo,
                    x + WIDGET_SIZE/2 - minecraft.font.width(gravityInfo)/2,
                    y + WIDGET_SIZE - 14, 0xFFCCCCCC);
        }


        // Update previous values for next frame
        prevYaw = interpolatedYaw;
        prevPitch = interpolatedPitch;
        prevGravityVec.set(interpolatedGravityVec);
    }

    /**
     * Draw a fancy title with shadow effect
     */
    private static void drawFancyTitle(GuiGraphics graphics, String title, int x, int y) {
        // Draw shadow
        graphics.drawString(Minecraft.getInstance().font, title, x + 1, y + 1, 0x55000000);

        // Draw main text with subtle gradient
        for (int i = 0; i < title.length(); i++) {
            float ratio = (float)i / title.length();
            int color = interpolateColor(0xFFFFFFFF, 0xFFAAEEFF, ratio);
            String character = String.valueOf(title.charAt(i));
            graphics.drawString(Minecraft.getInstance().font, character,
                    x + Minecraft.getInstance().font.width(title.substring(0, i)),
                    y, color);
        }
    }

    /**
     * Draw border with slightly rounded corners
     */
    private static void drawBorder(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        // Top and bottom horizontal lines (inset by 1 pixel at corners)
        graphics.hLine(x1 + 1, x2 - 1, y1, color);
        graphics.hLine(x1 + 1, x2 - 1, y2 - 1, color);

        // Left and right vertical lines (inset by 1 pixel at corners)
        graphics.vLine(x1, y1 + 1, y2 - 1, color);
        graphics.vLine(x2 - 1, y1 + 1, y2 - 1, color);
    }

    /**
     * Draw reference grid for better visual reference
     */
    private static void drawReferenceGrid(GuiGraphics graphics, int centerX, int centerY, int radius) {
        // Draw concentric circles
        drawCircle(graphics, centerX, centerY, radius, 0x55777777);
        drawCircle(graphics, centerX, centerY, radius/2, 0x55777777);

        // Draw cross hairs
        drawLine(graphics, centerX - radius, centerY, centerX + radius, centerY, 0x33FFFFFF);
        drawLine(graphics, centerX, centerY - radius, centerX, centerY + radius, 0x33FFFFFF);
    }

    /**
     * Draw gradient filled rectangle
     */
    private static void drawGradientRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color1, int color2) {
        int height = y2 - y1;

        for (int y = 0; y < height; y++) {
            float ratio = (float)y / height;
            int color = interpolateColor(color1, color2, ratio);
            graphics.hLine(x1, x2, y1 + y, color);
        }
    }

    /**
     * Draws a simplified 3D compass with colored direction indicators and fancy effects
     */
    private static void drawCompass(GuiGraphics graphics, int centerX, int centerY, int radius, float yaw, float pitch) {
        // Draw the compass background with gradient
        drawGradientCircle(graphics, centerX, centerY, radius, 0xFF333333, 0xFF111111);

        // Convert camera angles to radians
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Draw direction axes
        for (Direction direction : Direction.values()) {
            Vector3f dirVec = DIRECTION_VECTORS.get(direction);
            int color = DIRECTION_COLORS.get(direction);

            // Project the 3D direction onto 2D screen space
            Vector2f screenPos = projectTo2D(dirVec, yawRad, pitchRad, radius);

            // Draw direction marker - only if it's on the visible side (z > 0)
            if (isVisible(dirVec, yawRad, pitchRad)) {
                int markerX = centerX + (int)screenPos.x;
                int markerY = centerY + (int)screenPos.y;

                // Make major axes more prominent
                boolean isMajorAxis = (direction == Direction.NORTH ||
                        direction == Direction.UP ||
                        direction == Direction.EAST);
                int markerSize = isMajorAxis ? 6 : 4;

                // Draw line from center to marker with gradient
                drawGradientLine(graphics, centerX, centerY, markerX, markerY, 0x33FFFFFF, color);

                // Draw direction marker
                drawFilledCircle(graphics, markerX, markerY, markerSize, color);

                // Draw direction label
                String label = getDirectionLabel(direction);
                float distance = (float)Math.sqrt(screenPos.x * screenPos.x + screenPos.y * screenPos.y);
                if (distance > radius * 0.4f) {
                    graphics.drawString(Minecraft.getInstance().font, label,
                            markerX - Minecraft.getInstance().font.width(label) / 2,
                            markerY - 10,
                            color);
                }
            }
        }
    }

    /**
     * Draws the gravity direction indicator with fancy effects
     */
    private static void drawGravityIndicator(GuiGraphics graphics, int centerX, int centerY, Vector3f gravityVec, float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Create a normalized copy
        Vector3f normalizedGravity = new Vector3f(gravityVec);
        normalizedGravity.normalize();

        // Calculate gravity direction on screen
        Vector2f screenPos = projectTo2D(normalizedGravity, yawRad, pitchRad, 30);

        if (isVisible(normalizedGravity, yawRad, pitchRad)) {
            int gravX = centerX + (int)screenPos.x;
            int gravY = centerY + (int)screenPos.y;

            // Draw pulse effect around gravity marker
            long time = System.currentTimeMillis();
            float pulseFactor = (float) (0.7f + 0.3f * Math.sin(time / 500.0));
            int pulseRadius = (int)(9 * pulseFactor);
            drawGradientCircle(graphics, gravX, gravY, pulseRadius, 0x99FFFFFF, 0x00FFFFFF);

            // Draw gravity influence ray with gradient
            drawGradientLine(graphics, centerX, centerY, gravX, gravY, 0x33FFFFFF, 0xFFFFFFFF);

            // Draw gravity marker (larger than direction markers)
            drawFilledCircle(graphics, gravX, gravY, 7, 0xFFFFFFFF);

            // Draw "G" label with shadow
            graphics.drawString(Minecraft.getInstance().font, "G", gravX - 4, gravY - 4, 0xFF000000);
        }
    }

    /**
     * Find the closest cardinal direction to a vector
     */
    private static Direction getClosestDirection(Vector3f vec) {
        Direction closest = null;
        float maxDot = -1;

        for (Map.Entry<Direction, Vector3f> entry : DIRECTION_VECTORS.entrySet()) {
            Vector3f dirVec = entry.getValue();
            float dot = Math.abs(vec.dot(dirVec));
            if (dot > maxDot) {
                maxDot = dot;
                closest = entry.getKey();
            }
        }

        return closest;
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
     * Helper method to draw a line with solid color
     */
    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        drawGradientLine(graphics, x1, y1, x2, y2, color, color);
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
     * Helper method to draw a circle
     */
    private static void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        drawCircleOutline(graphics, centerX, centerY, radius, color);
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
     * Get a short label for each direction
     */
    private static String getDirectionLabel(Direction direction) {
        switch (direction) {
            case NORTH: return "N";
            case SOUTH: return "S";
            case EAST: return "E";
            case WEST: return "W";
            case UP: return "U";
            case DOWN: return "D";
            default: return "";
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
     * Helper method for smooth angle interpolation
     */
    private static float lerpAngle(float start, float end, float delta) {
        float diff = end - start;
        while (diff < -180f) diff += 360f;
        while (diff > 180f) diff -= 360f;
        return start + diff * delta;
    }

    /**
     * Helper method for smooth vector interpolation
     */
    private static Vector3f lerpVector(Vector3f start, Vector3f end, float delta) {
        Vector3f result = new Vector3f();
        result.x = start.x + (end.x - start.x) * delta;
        result.y = start.y + (end.y - start.y) * delta;
        result.z = start.z + (end.z - start.z) * delta;
        return result;
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