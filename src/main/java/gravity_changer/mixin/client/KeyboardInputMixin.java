package gravity_changer.mixin.client;

import gravity_changer.util.KeyboardInputHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Shadow @Final private Options options;

    /**
     * Inject at the beginning of the tick method to override keyboard input with emulated input if enabled
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(boolean bl, float f, CallbackInfo ci) {
        if (KeyboardInputHandler.isEmulatingInput()) {
            // Get the current instance
            KeyboardInput input = (KeyboardInput) (Object) this;

            // Set the input values based on our emulated values
            input.up = KeyboardInputHandler.isEmulatedUp();
            input.down = KeyboardInputHandler.isEmulatedDown();
            input.left = KeyboardInputHandler.isEmulatedLeft();
            input.right = KeyboardInputHandler.isEmulatedRight();
            input.jumping = KeyboardInputHandler.isEmulatedJumping();
            input.shiftKeyDown = KeyboardInputHandler.isEmulatedShiftKeyDown();

            // Calculate impulses using the same method as the original
            input.forwardImpulse = calculateImpulse(input.up, input.down);
            input.leftImpulse = calculateImpulse(input.left, input.right);

            // Apply the slow down factor if needed
            if (bl) {
                input.leftImpulse *= f;
                input.forwardImpulse *= f;
            }

            // Cancel the original method since we've handled everything
            ci.cancel();
        }
    }

    /**
     * Copied from KeyboardInput to ensure consistent behavior
     */
    private static float calculateImpulse(boolean input, boolean otherInput) {
        if (input == otherInput) {
            return 0.0F;
        } else {
            return input ? 1.0F : -1.0F;
        }
    }
}
