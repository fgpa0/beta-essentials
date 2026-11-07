package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import net.minecraft.entity.Item;
import net.minecraft.entity.player.PlayerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public abstract class ItemEntityMixin {
    /** Spectators don't pick up items. */
    @Inject(method = "onPlayerCollision(Lnet/minecraft/entity/player/PlayerBase;)V", at = @At("HEAD"), cancellable = true)
    private void ess$pickup(PlayerBase p, CallbackInfo ci) {
        if (Ess.isLocalPlayer(p) && Ess.spectator()) ci.cancel();
    }
}
