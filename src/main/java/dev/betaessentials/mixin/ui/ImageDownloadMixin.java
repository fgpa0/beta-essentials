package dev.betaessentials.mixin.ui;

import dev.betaessentials.tweaks.Skins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Player Skins: vanilla's image download thread (skins and capes) asks for the address Skins finds instead. */
@Mixin(targets = "net.minecraft.class_130$1")
public abstract class ImageDownloadMixin {
    @ModifyArg(method = "run()V", at = @At(value = "INVOKE", target = "Ljava/net/URL;<init>(Ljava/lang/String;)V", remap = false), remap = false)
    private String ess$skinAddress(String url) {
        return Skins.redirect(url);
    }
}
