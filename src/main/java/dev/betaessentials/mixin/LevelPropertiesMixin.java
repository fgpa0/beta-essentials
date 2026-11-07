package dev.betaessentials.mixin;

import dev.betaessentials.EssData;
import dev.betaessentials.EssProperties;
import dev.betaessentials.GameRules;
import net.minecraft.level.LevelProperties;
import net.minecraft.util.io.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelProperties.class)
public abstract class LevelPropertiesMixin implements EssProperties {
    @Unique private EssData ess$data = new EssData();

    @Override
    public EssData ess$data() {
        return ess$data;
    }

    @Inject(method = "<init>(Lnet/minecraft/util/io/CompoundTag;)V", at = @At("RETURN"))
    private void ess$load(CompoundTag tag, CallbackInfo ci) {
        // worlds saved before the rename to Beta Essentials keep their data under "Essentials"
        String key = tag.containsKey(EssData.TAG) ? EssData.TAG : tag.containsKey(EssData.OLD_TAG) ? EssData.OLD_TAG : null;
        ess$data = EssData.fromTag(key == null ? null : tag.getCompoundTag(key));
    }

    /** A brand new world: take the rules chosen on the Create World screen. */
    @Inject(method = "<init>(JLjava/lang/String;)V", at = @At("RETURN"))
    private void ess$create(long seed, String name, CallbackInfo ci) {
        ess$data.cheats = GameRules.pendingCheats;
        GameRules.pendingCheats = false;
        ess$data.mode = GameRules.pendingMode;
        GameRules.pendingMode = dev.betaessentials.GameMode.SURVIVAL;
        if (GameRules.pending != null) {
            // only store what differs from the defaults, so later default changes still reach this world
            for (java.util.Map.Entry<String, Integer> e : GameRules.pending.entrySet()) {
                dev.betaessentials.settings.Setting r = dev.betaessentials.settings.Settings.ALL.get(e.getKey());
                if (r != null && r.def != e.getValue()) ess$data.rules.put(e.getKey(), e.getValue());
            }
            GameRules.pending = null;
        }
    }

    /** Dimension changes copy the properties: share the same data object so nothing is lost. */
    @Inject(method = "<init>(Lnet/minecraft/level/LevelProperties;)V", at = @At("RETURN"))
    private void ess$copy(LevelProperties other, CallbackInfo ci) {
        ess$data = ((EssProperties) other).ess$data();
    }

    @Inject(method = "updateProperties(Lnet/minecraft/util/io/CompoundTag;Lnet/minecraft/util/io/CompoundTag;)V", at = @At("TAIL"))
    private void ess$save(CompoundTag tag, CompoundTag player, CallbackInfo ci) {
        tag.put(EssData.TAG, ess$data.toTag());
    }
}
