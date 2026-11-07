package dev.betaessentials.mixin;

import dev.betaessentials.gui.SettingsScreen;
import net.minecraft.client.gui.screen.ScreenBase;
import net.minecraft.client.gui.screen.menu.Options;
import net.minecraft.client.gui.widgets.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Beta Essentials..." on the free row above Video Settings. */
@Mixin(Options.class)
public abstract class OptionsMixin extends ScreenBase {
    @Inject(method = "init()V", at = @At("RETURN"))
    @SuppressWarnings("unchecked")
    private void ess$settingsButton(CallbackInfo ci) {
        this.buttons.add(new Button(61, this.width / 2 - 100, this.height / 6 + 72 + 12, "Beta Essentials..."));
    }

    @Inject(method = "buttonClicked(Lnet/minecraft/client/gui/widgets/Button;)V", at = @At("HEAD"), cancellable = true)
    private void ess$clicked(Button b, CallbackInfo ci) {
        if (b.id != 61) return;
        this.minecraft.options.saveOptions();
        this.minecraft.openScreen(new SettingsScreen((ScreenBase) (Object) this));
        ci.cancel();
    }
}
