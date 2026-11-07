package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import net.minecraft.client.BaseClientInteractionManager;
import net.minecraft.entity.EntityBase;
import net.minecraft.entity.player.PlayerBase;
import net.minecraft.item.ItemInstance;
import net.minecraft.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseClientInteractionManager.class)
public abstract class BaseInteractionMixin {
    /** Right click on a block: spectators can't; in creative placed blocks aren't used up. */
    @Inject(method = "method_1713(Lnet/minecraft/entity/player/PlayerBase;Lnet/minecraft/level/Level;Lnet/minecraft/item/ItemInstance;IIII)Z", at = @At("HEAD"), cancellable = true)
    private void ess$useOnBlock(PlayerBase p, Level l, ItemInstance st, int x, int y, int z, int face, CallbackInfoReturnable<Boolean> cir) {
        if (Ess.spectator()) cir.setReturnValue(false);
    }

    @Redirect(method = "method_1713(Lnet/minecraft/entity/player/PlayerBase;Lnet/minecraft/level/Level;Lnet/minecraft/item/ItemInstance;IIII)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemInstance;useOnTile(Lnet/minecraft/entity/player/PlayerBase;Lnet/minecraft/level/Level;IIII)Z"))
    private boolean ess$infinite(ItemInstance st, PlayerBase p, Level l, int x, int y, int z, int face) {
        int count = st.count, damage = st.getDamage();
        boolean r = st.useOnTile(p, l, x, y, z, face);
        if (Ess.creative()) {
            st.count = count;
            st.setDamage(damage);
        }
        return r;
    }

    @Inject(method = "method_1712(Lnet/minecraft/entity/player/PlayerBase;Lnet/minecraft/level/Level;Lnet/minecraft/item/ItemInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void ess$useItem(PlayerBase p, Level l, ItemInstance st, CallbackInfoReturnable<Boolean> cir) {
        if (Ess.spectator()) cir.setReturnValue(false);
    }

    @Inject(method = "method_1714(Lnet/minecraft/entity/player/PlayerBase;Lnet/minecraft/entity/EntityBase;)V", at = @At("HEAD"), cancellable = true)
    private void ess$interact(PlayerBase p, EntityBase e, CallbackInfo ci) {
        if (Ess.spectator()) ci.cancel();
    }

    @Inject(method = "attack(Lnet/minecraft/entity/player/PlayerBase;Lnet/minecraft/entity/EntityBase;)V", at = @At("HEAD"), cancellable = true)
    private void ess$attack(PlayerBase p, EntityBase e, CallbackInfo ci) {
        if (Ess.spectator()) ci.cancel();
    }
}
