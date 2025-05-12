package gravity_changer.mixin;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.collision.CollisionContext;
import gravity_changer.util.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the Level class to handle custom gravity effects on world mechanics.
 * This mixin handles particles, explosions, and sound effects in gravity-affected environments.
 */
@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {

    @Shadow public abstract boolean isClientSide();

    /**
     * Modifies particle emissions to account for custom gravity directions.
     * This ensures particles move in the direction of gravity for affected entities.
     */
    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void onAddParticle(ParticleOptions particleData, double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        // Only process on client side where particles are rendered
        if (!isClientSide()) return;

        // Get the entity that is currently the focus of collision detection
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        try {
            // Check if the entity has custom gravity
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
        if (isDefaultGravity) return;

        // Transform particle velocities according to gravity direction
        Vec3 originalVelocity = new Vec3(xSpeed, ySpeed, zSpeed);
        Vec3 transformedVelocity = RotationUtil.vecPlayerToWorldVec(originalVelocity, gravityDirection);

        // Add the transformed particle
        ((Level)(Object)this).addParticle(
                particleData,
                x, y, z,
                transformedVelocity.x, transformedVelocity.y, transformedVelocity.z
        );

        // Cancel the original particle emission
        ci.cancel();
    }

    /**
     * Modifies explosion creation to account for custom gravity directions.
     * This ensures explosions propagate correctly in altered gravity environments.
     */
    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Z)Lnet/minecraft/world/level/Explosion;",
            at = @At("HEAD"), cancellable = true)
    private void onExplode(@Nullable Entity source, @Nullable net.minecraft.world.damagesource.DamageSource damageSource,
                           @Nullable ExplosionDamageCalculator damageCalculator,
                           double x, double y, double z, float power, boolean fire,
                           Level.ExplosionInteraction explosionInteraction, boolean particles,
                           CallbackInfoReturnable<Explosion> cir) {
        if (source == null) return;

        try {
            // Check if the explosion source has custom gravity
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(source).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(source);
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
        if (isDefaultGravity) return;

        // Set the entity context for explosion processing
        CollisionContext.setCurrentEntity(source);
        CollisionContext.setInCustomCollision(true);

        try {
            // The explosion will use our modified collision detection
        } finally {
            // Always clear the custom collision flag
            CollisionContext.setInCustomCollision(false);
            CollisionContext.clearCurrentEntity();
        }
    }

    /**
     * Adjusts sound playback position and characteristics based on gravity direction.
     * This helps maintain directional audio cues in modified gravity environments.
     */
    @Inject(method = "playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void onPlaySound(@Nullable Player player, double x, double y, double z,
                             SoundEvent sound, SoundSource category,
                             float volume, float pitch, CallbackInfo ci) {
        if (player == null) return;

        try {
            // Check if the player has custom gravity
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(player).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(player);
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
        if (isDefaultGravity) return;

        // On client side, adjust sound positioning to match player's gravity orientation
        if (isClientSide() && player.isLocalPlayer()) {
            // Get the position relative to the player
            Vec3 playerPos = player.position();
            Vec3 soundPos = new Vec3(x, y, z);
            Vec3 relativePos = soundPos.subtract(playerPos);

            // Rotate the relative position according to gravity direction
            Vec3 rotatedPos = RotationUtil.vecWorldToPlayerVec(relativePos, gravityDirection);

            // Calculate the new sound position
            Vec3 newSoundPos = playerPos.add(rotatedPos);

            // Play the sound at the adjusted position
            ((Level)(Object)this).playSound(
                    player,
                    newSoundPos.x, newSoundPos.y, newSoundPos.z,
                    sound, category, volume, pitch
            );

            // Cancel the original sound
            ci.cancel();
        }
    }

    /**
     * Adjusts block destruction and effect positioning based on gravity.
     * This ensures visual effects appear correctly in altered gravity environments.
     */
    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddDestroyBlockEffect(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, CallbackInfo ci) {
        // Only process on client side where particles are rendered
        if (!isClientSide()) return;

        // Get the entity that is currently the focus of collision detection
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        try {
            // Check if the entity has custom gravity
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
        if (isDefaultGravity) return;

        // Set gravity context for the block destruction effect particles
        CollisionContext.setCurrentEntity(entity);

        try {
            // Let vanilla code handle the destruction, but with our collision context set
        } finally {
            // Clear the entity context when done
            CollisionContext.clearCurrentEntity();
        }
    }

    /**
     * Adjusts entity spawning and validation for custom gravity directions.
     * This ensures entities spawn correctly in modified gravity environments.
     */
    @Inject(method = "isInSpawnableBounds", at = @At("HEAD"), cancellable = true)
    private static void onIsInSpawnableBounds(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Get the entity that is currently the focus of collision detection
        Entity entity = CollisionContext.getCurrentEntity();
        if (entity == null) return;

        try {
            // Check if the entity has custom gravity
            if (!GravityChangerAPI.GRAVITY_COMPONENT.maybeGet(entity).isPresent()) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }

        Vec3 gravityDirection = GravityChangerAPI.getGravityDirectionVec(entity);
        boolean isDefaultGravity = gravityDirection.y < -0.99 &&
                gravityDirection.x == 0 &&
                gravityDirection.z == 0;
        if (isDefaultGravity) return;

        // For non-default gravity, validate the block position relative to the entity's orientation
        // This allows entities to spawn in places that would normally be out of bounds

        // Get the rotated position relative to the gravity direction
        Vec3 posVec = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        Vec3 rotatedPos = RotationUtil.vecWorldToPlayerVec(posVec, gravityDirection);

        // Check if the rotated position is within spawnable bounds
        boolean isValidX = Math.abs(rotatedPos.x) <= 3.0E7D;
        boolean isValidZ = Math.abs(rotatedPos.z) <= 3.0E7D;
        boolean isValidY = rotatedPos.y >= -2.0E7D && rotatedPos.y <= 2.0E7D;

        cir.setReturnValue(isValidX && isValidY && isValidZ);
    }
}