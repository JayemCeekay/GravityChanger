package gravity_changer.mixin.client;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {
    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    protected abstract boolean suffocatesAt(BlockPos pos);

    @Redirect(
        method = "Lnet/minecraft/client/player/LocalPlayer;suffocatesAt(Lnet/minecraft/core/BlockPos;)Z",
        at = @At(
            value = "NEW",
            target = "(DDDDDD)Lnet/minecraft/world/phys/AABB;",
            ordinal = 0
        )
    )
    private AABB redirect_wouldCollideAt_new_0(double x1, double y1, double z1, double x2, double y2, double z2, BlockPos pos) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) {
            return new AABB(x1, y1, z1, x2, y2, z2);
        }

        AABB playerBox = this.getBoundingBox();
        AABB posBox = new AABB(pos);

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(this)) {
            Vec3 playerMask = RotationUtil.maskPlayerToWorld(0.0D, 1.0D, 0.0D, gravityDirection);
            Vec3 posMask = RotationUtil.maskPlayerToWorld(1.0D, 0.0D, 1.0D, gravityDirection);

            return new AABB(
                playerMask.multiply(playerBox.minX, playerBox.minY, playerBox.minZ).add(posMask.multiply(posBox.minX, posBox.minY, posBox.minZ)),
                playerMask.multiply(playerBox.maxX, playerBox.maxY, playerBox.maxZ).add(posMask.multiply(posBox.maxX, posBox.maxY, posBox.maxZ))
            );
        } else {
            // For arbitrary directions, we need a different approach
            // Since maskPlayerToWorld is for cardinal directions only
            // We'll use the bounding box transformation methods

            // Transform player box to world space
            Vec3 playerMin = RotationUtil.vecPlayerToWorldVec(new Vec3(playerBox.minX, playerBox.minY, playerBox.minZ), gravityDirectionVec);
            Vec3 playerMax = RotationUtil.vecPlayerToWorldVec(new Vec3(playerBox.maxX, playerBox.maxY, playerBox.maxZ), gravityDirectionVec);

            // Combine with position box
            return new AABB(
                playerMin.x + posBox.minX, playerMin.y + posBox.minY, playerMin.z + posBox.minZ,
                playerMax.x + posBox.maxX, playerMax.y + posBox.maxY, playerMax.z + posBox.maxZ
            );
        }
    }

    @Inject(
        method = "Lnet/minecraft/client/player/LocalPlayer;moveTowardsClosestSpace(DD)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject_pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        // Get both Direction and Vec3 gravity directions
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(this);
        Vec3 gravityDirectionVec = GravityChangerAPI.getGravityDirectionVec(this);

        // Check if we're using the default gravity direction
        boolean isDefaultGravity = gravityDirectionVec.y < -0.99 && gravityDirectionVec.x == 0 && gravityDirectionVec.z == 0;
        if (isDefaultGravity) return;

        ci.cancel();

        Vec3 offset = new Vec3(x - this.getX(), 0.0D, z - this.getZ());
        Vec3 worldOffset;

        // For cardinal directions, use the existing code path for backward compatibility
        if (!GravityChangerAPI.isUsingVec3Gravity(this)) {
            worldOffset = RotationUtil.vecPlayerToWorld(offset, gravityDirection);
        } else {
            // For arbitrary directions, use the Vec3-based method
            worldOffset = RotationUtil.vecPlayerToWorldVec(offset, gravityDirectionVec);
        }

        Vec3 pos = worldOffset.add(this.position());
        BlockPos blockPos = BlockPos.containing(pos);

        if (this.suffocatesAt(blockPos)) {
            double dx = pos.x - (double) blockPos.getX();
            double dy = pos.y - (double) blockPos.getY();
            double dz = pos.z - (double) blockPos.getZ();
            Direction direction = null;
            double minDistToEdge = Double.MAX_VALUE;

            Direction[] directions = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
            for (Direction playerDirection : directions) {
                Direction worldDirection;

                // For cardinal directions, use the existing code path for backward compatibility
                if (!GravityChangerAPI.isUsingVec3Gravity(this)) {
                    worldDirection = RotationUtil.dirPlayerToWorld(playerDirection, gravityDirection);
                } else {
                    // For arbitrary directions, we need to handle this differently
                    // We'll convert the player direction to a Vec3, transform it, and find the closest cardinal direction
                    Vec3 playerDirVec = new Vec3(playerDirection.getStepX(), playerDirection.getStepY(), playerDirection.getStepZ());
                    Vec3 worldDirVec = RotationUtil.vecPlayerToWorldVec(playerDirVec, gravityDirectionVec);
                    worldDirection = Direction.getNearest(worldDirVec.x, worldDirVec.y, worldDirVec.z);
                }

                double g = worldDirection.getAxis().choose(dx, dy, dz);
                double distToEdge = worldDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0D - g : g;
                if (distToEdge < minDistToEdge && !this.suffocatesAt(blockPos.relative(worldDirection))) {
                    minDistToEdge = distToEdge;
                    direction = playerDirection;
                }
            }

            if (direction != null) {
                Vec3 velocity = this.getDeltaMovement();
                if (direction.getAxis() == Direction.Axis.X) {
                    this.setDeltaMovement(0.1D * (double) direction.getStepX(), velocity.y, velocity.z);
                }
                else if (direction.getAxis() == Direction.Axis.Z) {
                    this.setDeltaMovement(velocity.x, velocity.y, 0.1D * (double) direction.getStepZ());
                }
            }
        }
    }
}
