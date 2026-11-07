package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.GameRules;
import net.minecraft.entity.EntityBase;
import net.minecraft.entity.player.PlayerBase;
import net.minecraft.level.Level;
import net.minecraft.level.LevelProperties;
import net.minecraft.sortme.LevelMonsterSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Shadow protected LevelProperties properties;
    @Shadow public boolean isServerSide;
    @Shadow protected int field_212;

    @Shadow public abstract void saveLevel(boolean b, net.minecraft.util.ProgressListener l);

    @Unique private int ess$frozenTicks;

    @Unique
    private boolean ess$mine() {
        return !isServerSide && Ess.mc != null && Ess.mc.level == (Object) this;
    }

    @Unique
    private boolean ess$rule(String name) {
        return !ess$mine() || GameRules.get(name);
    }

    /** enableNightmares: monsters spawning by the bed while you sleep. */
    @Redirect(method = "method_242()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/sortme/LevelMonsterSpawner;method_1869(Lnet/minecraft/level/Level;Ljava/util/List;)Z"))
    private boolean ess$nightmares(Level level, List players) {
        return ess$rule(GameRules.ENABLE_NIGHTMARES) && LevelMonsterSpawner.method_1869(level, players);
    }

    @Redirect(method = "method_242()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/sortme/LevelMonsterSpawner;method_1870(Lnet/minecraft/level/Level;ZZ)I"))
    private int ess$mobSpawning(Level level, boolean monsters, boolean animals) {
        return ess$rule(GameRules.DO_MOB_SPAWNING) ? LevelMonsterSpawner.method_1870(level, monsters, animals) : 0;
    }

    /** doDaylightCycle: the per-tick time advance (the second setTime in the tick; the first is skipping the night). */
    @Redirect(method = "method_242()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/level/LevelProperties;setTime(J)V", ordinal = 1))
    private void ess$daylight(LevelProperties p, long time) {
        if (ess$rule(GameRules.DO_DAYLIGHT_CYCLE)) {
            p.setTime(time);
            return;
        }
        // time stands still, but the autosave that runs off the time still has to happen
        if (++ess$frozenTicks % field_212 == 0) saveLevel(false, null);
    }

    /** doWeatherCycle: keep the weather timers from running out. */
    @Inject(method = "method_245()V", at = @At("HEAD"))
    private void ess$weather(CallbackInfo ci) {
        if (ess$rule(GameRules.DO_WEATHER_CYCLE)) return;
        if (properties.getRainTime() < 2) properties.setRainTime(2);
        if (properties.getThunderTime() < 2) properties.setThunderTime(2);
    }

    /** Monsters ignore players in creative and spectator. */
    @Inject(method = "getClosestPlayerTo(Lnet/minecraft/entity/EntityBase;D)Lnet/minecraft/entity/player/PlayerBase;", at = @At("RETURN"), cancellable = true)
    private void ess$ignore(EntityBase e, double dist, CallbackInfoReturnable<PlayerBase> cir) {
        PlayerBase p = cir.getReturnValue();
        if (dist > 0 && p != null && Ess.isLocalPlayer(p) && Ess.invulnerable()) cir.setReturnValue(null);
    }
}
