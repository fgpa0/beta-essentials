package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.GameRules;
import dev.betaessentials.TreeFeller;
import net.minecraft.block.BlockBase;
import net.minecraft.client.SinglePlayerClientInteractionManager;
import net.minecraft.entity.player.AbstractClientPlayer;
import net.minecraft.item.ItemInstance;
import net.minecraft.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SinglePlayerClientInteractionManager.class)
public abstract class SinglePlayerInteractionMixin {
    @Shadow private int hitDelay;

    @Shadow public abstract boolean method_1716(int x, int y, int z, int face);

    @Unique private int ess$brokenId, ess$brokenMeta;

    /** Start mining: creative breaks instantly; spectators can't touch blocks. */
    @Inject(method = "method_1707(IIII)V", at = @At("HEAD"), cancellable = true)
    private void ess$startMining(int x, int y, int z, int face, CallbackInfo ci) {
        if (!Ess.active()) return;
        if (Ess.spectator()) {
            ci.cancel();
        } else if (Ess.creative()) {
            ci.cancel();
            if (Ess.mc.level.getTileId(x, y, z) != 0) method_1716(x, y, z, face);
            hitDelay = (GameRules.get(GameRules.BREAK_DELAY) ? 5 : 0);
        }
    }

    @Inject(method = "method_1721(IIII)V", at = @At("HEAD"), cancellable = true)
    private void ess$continueMining(int x, int y, int z, int face, CallbackInfo ci) {
        if (!Ess.active()) return;
        if (Ess.spectator()) {
            ci.cancel();
        } else if (Ess.creative()) {
            ci.cancel();
            if (hitDelay > 0) {
                hitDelay--;
                return;
            }
            if (Ess.mc.level.getTileId(x, y, z) != 0) method_1716(x, y, z, face);
            hitDelay = (GameRules.get(GameRules.BREAK_DELAY) ? 5 : 0);
        }
    }

    /** breakDelay: vanilla waits 5 ticks after a block breaks before mining the next. */
    @Inject(method = "method_1721(IIII)V", at = @At("TAIL"))
    private void ess$breakDelay(int x, int y, int z, int face, CallbackInfo ci) {
        if (!Ess.active()) return;
        int d = (GameRules.get(GameRules.BREAK_DELAY) ? 5 : 0);
        if (hitDelay > d) hitDelay = d;
    }

    /** Breaking a block: creative drops nothing and doesn't wear tools; logs may fell the whole tree. */
    @Inject(method = "method_1716(IIII)Z", at = @At("HEAD"), cancellable = true)
    private void ess$break(int x, int y, int z, int face, CallbackInfoReturnable<Boolean> cir) {
        if (!Ess.active()) return;
        Level level = Ess.mc.level;
        ess$brokenId = level.getTileId(x, y, z);
        ess$brokenMeta = level.getTileMeta(x, y, z);
        if (Ess.creative()) {
            BlockBase b = BlockBase.BY_ID[ess$brokenId];
            if (b == null) {
                cir.setReturnValue(false);
                return;
            }
            level.playLevelEvent(2001, x, y, z, b.id + ess$brokenMeta * 256);
            boolean removed = level.setTile(x, y, z, 0);
            if (removed) b.activate(level, x, y, z, ess$brokenMeta);
            if (removed) ess$fellTree(x, y, z, true);
            cir.setReturnValue(removed);
        }
    }

    @Inject(method = "method_1716(IIII)Z", at = @At("RETURN"))
    private void ess$broken(int x, int y, int z, int face, CallbackInfoReturnable<Boolean> cir) {
        if (Ess.active() && !Ess.creative() && cir.getReturnValueZ()) ess$fellTree(x, y, z, false);
    }

    @Unique
    private void ess$fellTree(int x, int y, int z, boolean creative) {
        if (ess$brokenId != 17 || !GameRules.get(GameRules.TREE_CAPACITOR)) return;
        AbstractClientPlayer p = Ess.mc.player;
        if (p.method_1373()) return; // sneaking: just this log
        ItemInstance held = p.getHeldItem();
        if (held == null || !(held.getType() instanceof net.minecraft.item.tool.Hatchet)) return;
        TreeFeller.fell(Ess.mc.level, p, x, y, z, ess$brokenMeta, creative);
    }
}
