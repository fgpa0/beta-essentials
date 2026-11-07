package dev.betaessentials.mixin;

import dev.betaessentials.GameMode;
import dev.betaessentials.GameRules;
import dev.betaessentials.gui.SettingsScreen;
import net.minecraft.client.gui.screen.ScreenBase;
import net.minecraft.client.gui.screen.menu.CreateLevel;
import net.minecraft.client.gui.widgets.Button;
import net.minecraft.client.gui.widgets.Textbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create World layout: name and seed, then game mode | difficulty and cheats | game rules, with Create New World and
 * Cancel side by side on a bottom row.
 */
@Mixin(CreateLevel.class)
public abstract class CreateLevelMixin extends ScreenBase {
    @Shadow private Textbox worldName;
    @Shadow private Textbox worldSeed;

    @Unique private String ess$savedName, ess$savedSeed;
    @Unique private static final String[] ESS_DIFFICULTY = {"Peaceful", "Easy", "Normal", "Hard"};

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screen/ScreenBase;)V", at = @At("RETURN"))
    private void ess$fresh(ScreenBase parent, CallbackInfo ci) {
        GameRules.pending = null;
        GameRules.pendingCheats = false;
        GameRules.pendingMode = GameMode.SURVIVAL;
    }

    @Inject(method = "init()V", at = @At("RETURN"))
    @SuppressWarnings("unchecked")
    private void ess$init(CallbackInfo ci) {
        int cx = this.width / 2;
        String name = ess$savedName != null ? ess$savedName : worldName.getText();
        String seed = ess$savedName != null ? ess$savedSeed : worldSeed.getText();
        ess$savedName = ess$savedSeed = null;
        worldName = new Textbox(this, this.textManager, cx - 100, 50, 200, 20, name);
        worldName.setSelected(true);
        worldName.setMaxLength(32);
        worldSeed = new Textbox(this, this.textManager, cx - 100, 86, 200, 20, seed);
        // keep Create at index 0 (the screen's tick enables it by index)
        int bottom = this.height - 28;
        this.buttons.clear();
        this.buttons.add(new Button(0, cx - 155, bottom, 150, 20, "Create New World"));
        this.buttons.add(new Button(1, cx + 5, bottom, 150, 20, "Cancel"));
        this.buttons.add(new Button(52, cx - 155, 116, 150, 20, ess$modeLabel()));
        this.buttons.add(new Button(53, cx + 5, 116, 150, 20, ess$difficultyLabel()));
        this.buttons.add(new Button(51, cx - 155, 140, 150, 20, ess$cheatsLabel()));
        this.buttons.add(new Button(50, cx + 5, 140, 150, 20, "Game Rules..."));
        ((Button) this.buttons.get(0)).active = name.length() > 0;
    }

    @Inject(method = "render(IIF)V", at = @At("HEAD"), cancellable = true)
    private void ess$render(int mx, int my, float delta, CallbackInfo ci) {
        int cx = this.width / 2;
        this.renderBackground();
        this.drawTextWithShadowCentred(this.textManager, "Create New World", cx, 20, 0xFFFFFF);
        this.drawTextWithShadow(this.textManager, "World Name", cx - 100, 38, 0xA0A0A0);
        this.drawTextWithShadow(this.textManager, "Seed (leave blank for a random one)", cx - 100, 74, 0xA0A0A0);
        worldName.draw();
        worldSeed.draw();
        super.render(mx, my, delta);
        ci.cancel();
    }

    @Unique
    private static String ess$cheatsLabel() {
        return "Allow Cheats: " + (GameRules.pendingCheats ? "ON" : "OFF");
    }

    @Unique
    private static String ess$modeLabel() {
        return "Game Mode: " + GameRules.pendingMode.display();
    }

    @Unique
    private String ess$difficultyLabel() {
        int d = Math.max(0, Math.min(3, this.minecraft.options.difficulty));
        return "Difficulty: " + ESS_DIFFICULTY[d];
    }

    @Inject(method = "buttonClicked(Lnet/minecraft/client/gui/widgets/Button;)V", at = @At("HEAD"), cancellable = true)
    private void ess$clicked(Button b, CallbackInfo ci) {
        if (b.id == 51) {
            GameRules.pendingCheats = !GameRules.pendingCheats;
            b.text = ess$cheatsLabel();
            ci.cancel();
        } else if (b.id == 52) {
            GameRules.pendingMode = GameRules.pendingMode == GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL;
            b.text = ess$modeLabel();
            ci.cancel();
        } else if (b.id == 53) {
            this.minecraft.options.difficulty = (this.minecraft.options.difficulty + 1) & 3;
            this.minecraft.options.saveOptions();
            b.text = ess$difficultyLabel();
            ci.cancel();
        } else if (b.id == 50) {
            ess$savedName = worldName.getText();
            ess$savedSeed = worldSeed.getText();
            this.minecraft.openScreen(SettingsScreen.forNewWorld((ScreenBase) (Object) this));
            ci.cancel();
        }
    }
}
