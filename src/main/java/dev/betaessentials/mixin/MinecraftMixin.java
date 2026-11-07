package dev.betaessentials.mixin;

import dev.betaessentials.Ess;
import dev.betaessentials.GameRules;
import dev.betaessentials.PlayerState;
import dev.betaessentials.gui.CreativeScreen;
import dev.betaessentials.gui.EssChatScreen;
import dev.betaessentials.settings.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ScreenBase;
import net.minecraft.client.gui.screen.container.PlayerInventory;
import net.minecraft.client.gui.screen.ingame.Chat;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow public ScreenBase currentScreen;

    @Shadow public abstract void openScreen(ScreenBase screen);

    @Inject(method = "init()V", at = @At("HEAD"))
    private void ess$init(CallbackInfo ci) {
        Ess.started((Minecraft) (Object) this);
        GameRules.names(); // registers the world settings
        dev.betaessentials.tweaks.Skins.register();
        Settings.load();
    }

    /** Our chat replaces vanilla's; in creative the inventory key opens the item picker; spectators have no inventory. */
    @Inject(method = "openScreen(Lnet/minecraft/client/gui/screen/ScreenBase;)V", at = @At("HEAD"), cancellable = true)
    private void ess$openScreen(ScreenBase screen, CallbackInfo ci) {
        if (screen == null) return;
        if (screen.getClass() == Chat.class) {
            ci.cancel();
            openScreen(new EssChatScreen(""));
        } else if (screen.getClass() == PlayerInventory.class && Ess.active()) {
            if (Ess.spectator()) {
                ci.cancel();
            } else if (Ess.creative() && !CreativeScreen.openingInventory) {
                ci.cancel();
                openScreen(new CreativeScreen());
            }
        }
    }

    /**
     * "/" opens the chat with the slash typed; in singleplayer the chat key (T) opens chat too (vanilla only allows
     * chat in multiplayer). Runs where vanilla checks the chat key.
     */
    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;hasLevel()Z", ordinal = 0))
    private void ess$chatKeys(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (currentScreen != null || !Keyboard.getEventKeyState() || mc.level == null) return;
        if (Keyboard.getEventKey() == Keyboard.KEY_SLASH || Keyboard.getEventCharacter() == '/') {
            openScreen(new EssChatScreen("/"));
        } else if (Keyboard.getEventKey() == mc.options.chatKey.key && Ess.active()) {
            openScreen(new EssChatScreen(""));
        }
    }

    /** Creative pick block (middle click). */
    @Inject(method = "method_2103()V", at = @At("HEAD"), cancellable = true)
    private void ess$pickBlock(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (!Ess.active() || mc.hitResult == null) return;
        if (Ess.spectator()) {
            ci.cancel();
        } else if (Ess.creative()) {
            ci.cancel();
            if (mc.hitResult.type == net.minecraft.util.hit.HitType.field_789)
                dev.betaessentials.PickBlock.pick(mc.level, mc.hitResult.x, mc.hitResult.y, mc.hitResult.z);
        }
    }

    @Inject(method = "method_2122(ZI)V", at = @At("RETURN"))
    private void ess$respawned(boolean b, int dim, CallbackInfo ci) {
        PlayerState.afterRespawn(((Minecraft) (Object) this).player);
    }

    @Inject(method = "createOrLoadWorld(Ljava/lang/String;Ljava/lang/String;J)V", at = @At("HEAD"))
    private void ess$newWorld(String dir, String name, long seed, CallbackInfo ci) {
        PlayerState.resetWorld();
    }
}
