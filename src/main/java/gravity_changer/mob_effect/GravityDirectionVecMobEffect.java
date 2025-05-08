package gravity_changer.mob_effect;

import gravity_changer.GravityComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A mob effect that changes the gravity direction to an arbitrary vector.
 * This extends the functionality of GravityDirectionMobEffect to support
 * arbitrary gravity directions instead of just the six cardinal directions.
 */
public class GravityDirectionVecMobEffect extends MobEffect {
    public static final int COLOR = 0x98D982;
    
    public static final ResourceLocation PHASE = new ResourceLocation("gravity_changer:dir_vec_mob_effect_phase");
    public static final ResourceLocation ID = new ResourceLocation("gravity_changer:direction_vec");
    
    public static final GravityDirectionVecMobEffect INSTANCE = new GravityDirectionVecMobEffect();
    
    private GravityDirectionVecMobEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }
    
    /**
     * Initialize the mob effect by registering it and setting up the event handler.
     */
    public static void init() {
        Registry.register(
            BuiltInRegistries.MOB_EFFECT, ID, INSTANCE
        );
        
        // Register after the cardinal direction effects
        GravityComponent.GRAVITY_UPDATE_EVENT.addPhaseOrdering(
            GravityDirectionMobEffect.PHASE, PHASE
        );
        
        // Register before the invert effect
        GravityComponent.GRAVITY_UPDATE_EVENT.addPhaseOrdering(
            PHASE, GravityInvertMobEffect.PHASE
        );
        
        GravityComponent.GRAVITY_UPDATE_EVENT.register(
            PHASE, (entity, component) -> {
                if (!(entity instanceof LivingEntity livingEntity)) {
                    return;
                }
                
                MobEffectInstance effectInstance = livingEntity.getEffect(INSTANCE);
                if (effectInstance != null) {
                    int amplifier = effectInstance.getAmplifier();
                    
                    // Get the gravity direction from the effect's tag compound
                    // This would require additional code to store and retrieve the direction
                    // For now, we'll use a default arbitrary direction as an example
                    Vec3 gravityDirection = new Vec3(1, 1, 0).normalize();
                    
                    component.applyGravityDirectionEffectVec(
                        gravityDirection,
                        null,
                        amplifier + 1.0
                    );
                }
            }
        );
    }
}