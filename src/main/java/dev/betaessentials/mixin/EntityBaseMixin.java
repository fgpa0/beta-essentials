package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.PlayerState;
import net.minecraft.entity.EntityBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityBase.class)
public abstract class EntityBaseMixin {
    @Shadow public boolean onGround;

    /**
     * While the player is in the air (falling, jumping, dropping out of flight) the walked distance keeps growing but no
     * footsteps play; vanilla then plays the whole backlog, one per tick, after landing. Keep the step counter caught up
     * so landing plays at most one step.
     */
    @Inject(method = "move(DDD)V", at = @At("HEAD"))
    private void ess$airSteps(double dx, double dy, double dz, CallbackInfo ci) {
        if (!onGround && Ess.isLocalPlayer(this)) PlayerState.catchUpSteps((EntityAccessor) this);
    }
}
