package dev.betaessentials.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widgets.Button;

/** A 20 x 20 button showing an undo arrow (reset to default), used by the settings and controls screens. */
public class ResetButton extends Button {
    private static final String[] ICON = {
            "..#......",
            ".######..",
            "###....#.",
            ".##.....#",
            "..#.....#",
            ".......#.",
            "..#####..",
    };

    public ResetButton(int x) {
        super(0, x, 0, 20, 20, "");
    }

    @Override
    protected void postRender(Minecraft mc, int mx, int my) {
        boolean hover = mx >= x && my >= y && mx < x + width && my < y + height;
        int c = !active ? 0xFF606060 : hover ? 0xFFFFFFA0 : 0xFFE0E0E0;
        for (int j = 0; j < ICON.length; j++)
            for (int i = 0; i < ICON[j].length(); i++)
                if (ICON[j].charAt(i) == '#') fill(x + 5 + i, y + 6 + j, x + 6 + i, y + 7 + j, c);
    }
}
