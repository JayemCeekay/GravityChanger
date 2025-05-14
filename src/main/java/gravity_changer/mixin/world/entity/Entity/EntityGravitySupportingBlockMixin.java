package gravity_changer.mixin.world.entity.Entity;

import com.llamalad7.mixinextras.sugar.Local;
import gravity_changer.collision.CollisionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityGravitySupportingBlockMixin {
    @Shadow protected boolean onGroundNoBlocks;
    @Shadow
    protected Optional<BlockPos> mainSupportingBlockPos;
    @Shadow protected Level level;
    @Shadow public abstract AABB getBoundingBox();

    /**
     * First supporting-block test: replace the “new AABB(minY–ε …)” with one
     * offset along your custom gravity vector.
     */
    @ModifyArg(
            method = "checkSupportingBlock(ZLnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;findSupportingBlock"
                            + "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/Optional;",
                    ordinal = 0
            ),
            index = 1
    )
    private AABB applyGravityProbeBox(AABB par2) {
        // get your gravity dir and a tiny epsilon
        Vec3 gravity = CollisionContext.getGravityDirection((Entity)(Object)this);
        double eps = 1.0E-6;
        AABB bb = this.getBoundingBox();
        // offset along gravity by ±eps
        Vec3 delta = gravity.scale(eps);
        return new AABB(
                bb.minX + Math.min(0, delta.x),
                bb.minY + Math.min(0, delta.y),
                bb.minZ + Math.min(0, delta.z),
                bb.maxX + Math.max(0, delta.x),
                bb.maxY + Math.max(0, delta.y),
                bb.maxZ + Math.max(0, delta.z)
        );
    }

    /**
     * Second supporting‐block test (the “else if (vec3!=null)” branch):
     * re-shift the probe box by the horizontal component of motion relative to gravity.
     */
    @ModifyArg(
            method = "checkSupportingBlock(ZLnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;findSupportingBlock"
                            + "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/Optional;",
                    ordinal = 1
            ),
            index = 1
    )
    private AABB applyGravitySecondProbeBox(AABB par2, @Local(ordinal = 0, argsOnly = true) Vec3 motion) {
        Vec3 gravity = CollisionContext.getGravityDirection((Entity)(Object)this);
        // remove component along gravity → the “horizontal” motion
        Vec3 horiz = motion.subtract(gravity.scale(motion.dot(gravity)));
        // shift the AABB by -horiz
        return par2.move(-horiz.x, -horiz.y, -horiz.z);
    }
}