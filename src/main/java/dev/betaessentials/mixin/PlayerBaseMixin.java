package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.GameRules;
import dev.betaessentials.PlayerState;
import net.minecraft.entity.EntityBase;
import net.minecraft.entity.player.AbstractClientPlayer;
import net.minecraft.entity.player.PlayerBase;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerBase.class)
public abstract class PlayerBaseMixin {
    @Inject(method = "damage(Lnet/minecraft/entity/EntityBase;I)Z", at = @At("HEAD"), cancellable = true)
    private void ess$invulnerable(EntityBase source, int amount, CallbackInfoReturnable<Boolean> cir) {
        if (Ess.isLocalPlayer(this) && Ess.invulnerable() && !PlayerState.killing) cir.setReturnValue(false);
    }

    /** /back after death returns to where you died. */
    @Inject(method = "onKilledBy(Lnet/minecraft/entity/EntityBase;)V", at = @At("HEAD"))
    private void ess$died(EntityBase killer, CallbackInfo ci) {
        if (Ess.isLocalPlayer(this)) Ess.rememberBack(Ess.here());
    }

    @Redirect(method = "onKilledBy(Lnet/minecraft/entity/EntityBase;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;dropInventory()V"))
    private void ess$keepInventory(PlayerInventory inv) {
        if (Ess.isLocalPlayer(this) && GameRules.get(GameRules.KEEP_INVENTORY)) PlayerState.keepInventory((AbstractClientPlayer) (Object) this);
        else inv.dropInventory();
    }

    @Inject(method = "jump()V", at = @At("TAIL"))
    private void ess$sprintJump(CallbackInfo ci) {
        if (Ess.isLocalPlayer(this)) PlayerState.jumped((AbstractClientPlayer) (Object) this);
    }

    /** Spectators can be inside blocks: no suffocation overlay or push-out. */
    @Inject(method = "isInsideWall()Z", at = @At("HEAD"), cancellable = true)
    private void ess$insideWall(CallbackInfoReturnable<Boolean> cir) {
        if (Ess.isLocalPlayer(this) && Ess.spectator()) cir.setReturnValue(false);
    }
}
