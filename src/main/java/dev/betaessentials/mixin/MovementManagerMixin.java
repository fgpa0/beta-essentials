package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.PlayerState;
import dev.betaessentials.TreeFeller;
import net.minecraft.client.MovementManager;
import net.minecraft.entity.player.PlayerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementManager.class)
public abstract class MovementManagerMixin {
    @org.spongepowered.asm.mixin.Shadow private net.minecraft.client.options.GameOptions options;
    @org.spongepowered.asm.mixin.Shadow private boolean[] keys;

    @Inject(method = "onKeyPressed(IZ)V", at = @At("HEAD"))
    private void ess$key(int key, boolean down, CallbackInfo ci) {
        // only a real press: the key was up before (ignores repeats)
        if (down && key == options.forwardKey.key && !keys[0] && Ess.active()) PlayerState.forwardPressed();
        if (down && key == options.jumpKey.key && !keys[4] && Ess.active()) PlayerState.jumpPressed();
    }

    @Inject(method = "updatePlayer(Lnet/minecraft/entity/player/PlayerBase;)V", at = @At("TAIL"))
    private void ess$input(PlayerBase player, CallbackInfo ci) {
        if (!Ess.isLocalPlayer(player)) return;
        PlayerState.input((MovementManager) (Object) this);
        TreeFeller.tick(Ess.mc.level);
    }
}
