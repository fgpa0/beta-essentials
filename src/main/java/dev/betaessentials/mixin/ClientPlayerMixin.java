package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import net.minecraft.entity.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class ClientPlayerMixin {
    /** Vanilla pushes the player out of blocks; spectators fly through them. */
    @Inject(method = "method_1372(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void ess$noPushOut(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Ess.isLocalPlayer(this) && Ess.spectator()) cir.setReturnValue(false);
    }
}
