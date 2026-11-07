package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.GameRules;
import dev.betaessentials.PlayerState;
import net.minecraft.entity.EntityBase;
import net.minecraft.entity.Living;
import net.minecraft.entity.player.AbstractClientPlayer;
import net.minecraft.entity.player.PlayerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Living.class)
public abstract class LivingMixin {
    @Inject(method = "travel(FF)V", at = @At("HEAD"), cancellable = true)
    private void ess$fly(float strafe, float forward, CallbackInfo ci) {
        if (Ess.isLocalPlayer(this) && PlayerState.flying()) {
            PlayerState.fly((AbstractClientPlayer) (Object) this, strafe, forward);
            ci.cancel();
        }
    }

    /** Sprinting: faster acceleration on land (the third call is the normal ground/air movement). */
    @ModifyArg(method = "travel(FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Living;movementInputToVelocity(FFF)V", ordinal = 2), index = 2)
    private float ess$sprint(float speed) {
        return speed * PlayerState.sprintFactor(this);
    }

    @Inject(method = "handleFallDamage(F)V", at = @At("HEAD"), cancellable = true)
    private void ess$fallDamage(float distance, CallbackInfo ci) {
        if ((Object) this instanceof PlayerBase && Ess.active() && (!GameRules.get(GameRules.FALL_DAMAGE) || (Ess.isLocalPlayer(this) && Ess.invulnerable()))) ci.cancel();
    }
}
