package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.GameRules;
import net.minecraft.block.Fire;
import net.minecraft.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Fire.class)
public abstract class FireMixin {
    /** doFireTick=false: fire neither spreads nor burns out. */
    @Inject(method = "onScheduledTick(Lnet/minecraft/level/Level;IIILjava/util/Random;)V", at = @At("HEAD"), cancellable = true)
    private void ess$fireTick(Level level, int x, int y, int z, Random r, CallbackInfo ci) {
        if (!level.isServerSide && Ess.mc != null && Ess.mc.level == level && !GameRules.get(GameRules.DO_FIRE_TICK)) ci.cancel();
    }
}
