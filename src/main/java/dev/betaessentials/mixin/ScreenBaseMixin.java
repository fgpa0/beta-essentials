package dev.betaessentials.mixin;

import net.minecraft.client.gui.screen.ScreenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenBase.class)
public abstract class ScreenBaseMixin {
    /** In the main menus (no world open) ESC goes back one screen, to the main menu from a first-level screen. */
    @Inject(method = "onKeyboardEvent()V", at = @At("HEAD"), cancellable = true)
    private void ess$escBack(CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = dev.betaessentials.Ess.mc;
        Object self = this;
        if (mc == null || mc.level != null || mc.currentScreen != self) return;
        if (self instanceof net.minecraft.client.gui.screen.menu.MainMenu || self instanceof dev.betaessentials.gui.SettingsScreen) return;
        if (!org.lwjgl.input.Keyboard.getEventKeyState() || org.lwjgl.input.Keyboard.getEventKey() != 1) return;
        try {
            ScreenBase parent = null;
            for (Class<?> c = self.getClass(); c != null && c != ScreenBase.class; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    String n = f.getName();
                    if ((n.equals("selectedButton") || n.equals("field_2540")) && f.getType() == int.class) {
                        f.setAccessible(true);
                        if (f.getInt(self) >= 0) return; // Controls is waiting for a key to bind
                    }
                    if (parent == null && ScreenBase.class.isAssignableFrom(f.getType()) && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        Object v = f.get(self);
                        if (v != self) parent = (ScreenBase) v;
                    }
                }
            }
            mc.openScreen(parent); // null: back to the main menu
            ci.cancel();
        } catch (Exception ignored) {
        }
    }
}
