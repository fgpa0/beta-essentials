package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.PlayerState;
import net.minecraft.class_556;
import net.minecraft.sortme.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** No hand or held item in spectator mode. */
    @Redirect(method = "method_1845(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_556;method_1860(F)V"))
    private void ess$hand(class_556 renderer, float delta) {
        if (!Ess.spectator()) renderer.method_1860(delta);
    }

    /** Sprinting / flying FOV. */
    @Inject(method = "method_1848(F)F", at = @At("RETURN"), cancellable = true)
    private void ess$fov(float delta, CallbackInfoReturnable<Float> cir) {
        float f = PlayerState.fovFactor(delta);
        if (f != 1F) cir.setReturnValue(cir.getReturnValue() * f);
    }
}
