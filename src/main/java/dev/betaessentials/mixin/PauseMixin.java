package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.EssData;
import net.minecraft.client.gui.screen.ScreenBase;
import net.minecraft.client.gui.screen.ingame.Pause;
import net.minecraft.client.gui.widgets.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pause.class)
public abstract class PauseMixin extends ScreenBase {
    private static String ess$cheatsLabel() {
        EssData d = Ess.data();
        return "Cheats: " + (d != null && d.cheats ? "ON" : "OFF");
    }

    @Inject(method = "init()V", at = @At("RETURN"))
    @SuppressWarnings("unchecked")
    private void ess$cheatsButton(CallbackInfo ci) {
        if (!Ess.active()) return;
        // like modern Minecraft's Open to LAN: Options and this button share a row (the row above is often used by other mods)
        for (int i = 0; i < this.buttons.size(); i++) {
            Button o = (Button) this.buttons.get(i);
            if (o.id == 0) {
                this.buttons.set(i, new Button(0, o.x, o.y, 98, 20, o.text));
                this.buttons.add(new Button(60, o.x + 102, o.y, 98, 20, ess$cheatsLabel()));
                return;
            }
        }
    }

    /** Allow Cheats for this world (saved in level.dat); new worlds choose it on the Create World screen. */
    @Inject(method = "buttonClicked(Lnet/minecraft/client/gui/widgets/Button;)V", at = @At("HEAD"), cancellable = true)
    private void ess$clicked(Button b, CallbackInfo ci) {
        if (b.id != 60) return;
        EssData d = Ess.data();
        if (d != null) d.cheats = !d.cheats;
        b.text = ess$cheatsLabel();
        ci.cancel();
    }
}
