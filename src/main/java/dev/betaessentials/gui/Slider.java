package dev.betaessentials.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widgets.Button;
import org.lwjgl.opengl.GL11;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** A vanilla-looking slider over whole numbers from min to max. */
public class Slider extends Button {
    private final int min, max;
    private int value;
    private boolean dragged;
    private final IntConsumer onChange;
    private final IntFunction<String> label;
    /** Called when the mouse lets go of the slider (null for none). */
    public Runnable onRelease;

    public Slider(int id, int x, int y, int w, int min, int max, int value, IntConsumer onChange, IntFunction<String> label) {
        super(id, x, y, w, 20, "");
        this.min = min;
        this.max = max;
        this.value = value;
        this.onChange = onChange;
        this.label = label;
        this.text = label.apply(value);
    }

    private void setFromMouse(int mouseX) {
        double f = (double) (mouseX - (this.x + 4)) / (double) (this.width - 8);
        int v = min + (int) Math.round(Math.max(0, Math.min(1, f)) * (max - min));
        if (v != value) {
            value = v;
            onChange.accept(v);
        }
        this.text = label.apply(value);
    }

    @Override
    protected int getYImage(boolean hovered) {
        return 0;
    }

    @Override
    protected void postRender(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        if (dragged) setFromMouse(mouseX);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int px = this.x + (max > min ? (value - min) * (this.width - 8) / (max - min) : 0);
        this.blit(px, this.y, 0, 66, 4, 20);
        this.blit(px + 4, this.y, 196, 66, 4, 20);
    }

    @Override
    public boolean isMouseOver(Minecraft mc, int mouseX, int mouseY) {
        if (super.isMouseOver(mc, mouseX, mouseY)) {
            setFromMouse(mouseX);
            dragged = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        if (dragged && onRelease != null) onRelease.run();
        dragged = false;
    }
}
