package gravity_changer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gravity_changer.GravityChangerMod;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.OrientedBoundingBox;
import gravity_changer.collision.OrientedBoundingBoxTransformer;
import gravity_changer.util.Rotor;
import gravity_changer.util.RotationUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class BoundingBoxDebugRenderer {
    private static boolean showBoundingBoxes = false;
    private static KeyMapping keyBinding;

    public static void register() {
        // Register key binding (B key by default)
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.gravity_changer.toggle_bounding_boxes",
            GLFW.GLFW_KEY_B,
            "key.categories.gravity_changer"
        ));

        // Register tick event to handle key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyBinding.consumeClick()) {
                showBoundingBoxes = !showBoundingBoxes;
                if (client.player != null) {
                    client.player.sendSystemMessage(
                        Component.translatable("message.gravity_changer.bounding_boxes." + (showBoundingBoxes ? "enabled" : "disabled"))
                    );
                }
            }
        });

        // Register render event to draw bounding boxes
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (showBoundingBoxes) {
                renderBoundingBoxes(context.matrixStack());
            }
        });

        GravityChangerMod.LOGGER.info("Bounding Box Debug Renderer initialized");
    }

    private static void renderBoundingBoxes(PoseStack matrixStack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        // Get all entities in render distance
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity.isInvisible()) {
                continue;
            }

            // Calculate entity position relative to camera
            Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
            double x = entity.getX() - cameraPos.x;
            double y = entity.getY() - cameraPos.y;
            double z = entity.getZ() - cameraPos.z;

            // Push matrix stack
            matrixStack.pushPose();
            matrixStack.translate(x, y, z);

            // Get vertex consumer for lines
            VertexConsumer vertexConsumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());

            // Render entity bounding box
            renderEntityBoundingBox(matrixStack, vertexConsumer, entity);

            // Pop matrix stack
            matrixStack.popPose();
        }

        // Ensure all rendering is flushed
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }

    private static void renderEntityBoundingBox(PoseStack matrixStack, VertexConsumer vertexConsumer, Entity entity) {
        // Get the entity's bounding box - already rotated for gravity
        AABB boundingBox = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());

        // Get gravity direction
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;

        if (!isDefaultGravity) {
            // For non-default gravity, render both the bounding AABB and the oriented box

            // Render the bounding AABB in white (semi-transparent)
            LevelRenderer.renderLineBox(matrixStack, vertexConsumer, boundingBox, 1.0F, 1.0F, 1.0F, 0.5F);

            // Get the oriented bounding box
            // The boundingBox is already in local space (relative to entity position)
            // We need to create the OBB in world space for proper alignment
            AABB worldBox = boundingBox.move(entity.getX(), entity.getY(), entity.getZ());

            // Create the OBB directly to ensure proper alignment with the player
            // Default gravity is (0, -1, 0)
            Vec3 defaultGravity = new Vec3(0, -1, 0);

            // Create a rotor that rotates from default gravity to the specified gravity
            Rotor rotation = Rotor.from(defaultGravity, gravityDirectionVec);

            // Calculate the center of the box in world coordinates
            Vec3 center = worldBox.getCenter();

            // Transform the box to the local coordinate system using RotationUtil
            AABB localBox = RotationUtil.boxWorldToPlayerVec(worldBox, gravityDirectionVec);

            // Create the OBB with the local box, rotation, and center
            OrientedBoundingBox obb = OrientedBoundingBoxTransformer.transformToOBBDynamic(localBox, gravityDirectionVec, entity);

            // Render the oriented bounding box in green
            renderOrientedBoundingBox(matrixStack, vertexConsumer, obb, 0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            // For default gravity, just render the AABB in white
            LevelRenderer.renderLineBox(matrixStack, vertexConsumer, boundingBox, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        // We're not handling special cases like EnderDragon parts to keep it simple

        // For living entities, render eye height in red
        if (entity instanceof LivingEntity) {
            float eyeHeight = entity.getEyeHeight();

            if (!isDefaultGravity) {
                // For non-default gravity, we need to transform the eye height plane
                AABB eyeBox = new AABB(
                    boundingBox.minX, eyeHeight - 0.01F, boundingBox.minZ,
                    boundingBox.maxX, eyeHeight + 0.01F, boundingBox.maxZ
                );

                // Apply the same rotation as for the bounding box
                eyeBox = Rotor.rotateBoxWithRotor(eyeBox, gravityDirectionVec);

                LevelRenderer.renderLineBox(matrixStack, vertexConsumer, eyeBox, 1.0F, 0.0F, 0.0F, 1.0F);
            } else {
                // For default gravity, use the standard approach
                LevelRenderer.renderLineBox(
                    matrixStack, 
                    vertexConsumer, 
                    boundingBox.minX, 
                    eyeHeight - 0.01F, 
                    boundingBox.minZ, 
                    boundingBox.maxX, 
                    eyeHeight + 0.01F, 
                    boundingBox.maxZ, 
                    1.0F, 0.0F, 0.0F, 1.0F
                );
            }
        }

        // Render view vector in blue
        Vec3 viewVector = entity.getViewVector(Minecraft.getInstance().getFrameTime());
        Matrix4f pose = matrixStack.last().pose();
        Matrix3f normal = matrixStack.last().normal();

        // Get eye position
        Vec3 eyePos;
        if (!isDefaultGravity) {
            // For non-default gravity, transform the eye position
            Rotor rotor = Rotor.from(new Vec3(0, -1, 0), gravityDirectionVec);
            eyePos = rotor.rotate(new Vec3(0, entity.getEyeHeight(), 0));
        } else {
            eyePos = new Vec3(0, entity.getEyeHeight(), 0);
        }

        // Draw view vector
        vertexConsumer.vertex(pose, (float)eyePos.x, (float)eyePos.y, (float)eyePos.z)
            .color(0, 0, 255, 255)
            .normal(normal, (float) viewVector.x, (float) viewVector.y, (float) viewVector.z)
            .endVertex();

        Vec3 endPos = eyePos.add(viewVector.scale(2.0));
        vertexConsumer.vertex(pose, (float)endPos.x, (float)endPos.y, (float)endPos.z)
            .color(0, 0, 255, 255)
            .normal(normal, (float) viewVector.x, (float) viewVector.y, (float) viewVector.z)
            .endVertex();
    }

    /**
     * Renders an OrientedBoundingBox by drawing lines between its 8 corners.
     * 
     * @param matrixStack The matrix stack
     * @param vertexConsumer The vertex consumer
     * @param obb The oriented bounding box to render
     * @param red The red component of the color (0.0-1.0)
     * @param green The green component of the color (0.0-1.0)
     * @param blue The blue component of the color (0.0-1.0)
     * @param alpha The alpha component of the color (0.0-1.0)
     */
    private static void renderOrientedBoundingBox(PoseStack matrixStack, VertexConsumer vertexConsumer, 
                                                 OrientedBoundingBox obb, float red, float green, float blue, float alpha) {
        // Get the local box and rotation
        AABB localBox = obb.getLocalBox();
        Rotor rotation = obb.getRotation();
        Vec3 center = obb.getCenter();

        // Get the 8 corners of the local box
        Vec3[] corners = new Vec3[8];
        corners[0] = new Vec3(localBox.minX, localBox.minY, localBox.minZ);
        corners[1] = new Vec3(localBox.maxX, localBox.minY, localBox.minZ);
        corners[2] = new Vec3(localBox.minX, localBox.maxY, localBox.minZ);
        corners[3] = new Vec3(localBox.maxX, localBox.maxY, localBox.minZ);
        corners[4] = new Vec3(localBox.minX, localBox.minY, localBox.maxZ);
        corners[5] = new Vec3(localBox.maxX, localBox.minY, localBox.maxZ);
        corners[6] = new Vec3(localBox.minX, localBox.maxY, localBox.maxZ);
        corners[7] = new Vec3(localBox.maxX, localBox.maxY, localBox.maxZ);

        // Rotate each corner and move to world space
        for (int i = 0; i < 8; i++) {
            corners[i] = rotation.rotate(corners[i]).add(center)
                .subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        }

        Matrix4f pose = matrixStack.last().pose();

        // Draw the 12 edges of the box
        // Bottom face
        drawLine(vertexConsumer, pose, corners[0], corners[1], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[1], corners[5], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[5], corners[4], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[4], corners[0], red, green, blue, alpha);

        // Top face
        drawLine(vertexConsumer, pose, corners[2], corners[3], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[3], corners[7], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[7], corners[6], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[6], corners[2], red, green, blue, alpha);

        // Connecting edges
        drawLine(vertexConsumer, pose, corners[0], corners[2], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[1], corners[3], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[5], corners[7], red, green, blue, alpha);
        drawLine(vertexConsumer, pose, corners[4], corners[6], red, green, blue, alpha);
    }

    /**
     * Draws a line between two points.
     * 
     * @param vertexConsumer The vertex consumer
     * @param pose The matrix pose
     * @param start The start point
     * @param end The end point
     * @param red The red component of the color (0.0-1.0)
     * @param green The green component of the color (0.0-1.0)
     * @param blue The blue component of the color (0.0-1.0)
     * @param alpha The alpha component of the color (0.0-1.0)
     */
    private static void drawLine(VertexConsumer vertexConsumer, Matrix4f pose, 
                                Vec3 start, Vec3 end, float red, float green, float blue, float alpha) {
        vertexConsumer.vertex(pose, (float)start.x, (float)start.y, (float)start.z)
            .color(red, green, blue, alpha)
            .normal(0, 1, 0)
            .endVertex();
        vertexConsumer.vertex(pose, (float)end.x, (float)end.y, (float)end.z)
            .color(red, green, blue, alpha)
            .normal(0, 1, 0)
            .endVertex();
    }
}
