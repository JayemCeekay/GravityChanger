package gravity_changer.mob_effect;

import gravity_changer.GravityComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class GravityInvertMobEffect extends MobEffect {

    public static final int COLOR = 0x98D982;

    public static final ResourceLocation PHASE = new ResourceLocation("gravity_changer:invert_mob_effect_phase");

    public static final ResourceLocation ID = new ResourceLocation("gravity_changer:invert");

    public static final GravityInvertMobEffect INSTANCE = new GravityInvertMobEffect();

    private GravityInvertMobEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }

    public static void init() {
        GravityComponent.GRAVITY_UPDATE_EVENT.register(
            PHASE, (entity, component) -> {
                if (entity instanceof LivingEntity livingEntity) {
                    if (livingEntity.hasEffect(INSTANCE)) {
                        // Get the current gravity direction as a Vec3
                        Vec3 currGravityDirectionVec = component.getCurrGravityDirectionVec();

                        // Invert the gravity direction vector
                        Vec3 invertedGravityDirectionVec = currGravityDirectionVec.scale(-1.0);

                        // Apply the inverted gravity direction
                        component.applyGravityDirectionEffectVec(
                            invertedGravityDirectionVec,
                            null,
                            5.0
                        );
                    }
                }
            }
        );

        // Apply invert after gravity effects
        GravityComponent.GRAVITY_UPDATE_EVENT.addPhaseOrdering(
            GravityDirectionMobEffect.PHASE, GravityInvertMobEffect.PHASE
        );

        // Also apply after the Vec3-based gravity effect
        GravityComponent.GRAVITY_UPDATE_EVENT.addPhaseOrdering(
            GravityDirectionVecMobEffect.PHASE, GravityInvertMobEffect.PHASE
        );

        Registry.register(
            BuiltInRegistries.MOB_EFFECT, ID, INSTANCE
        );
    }


}
