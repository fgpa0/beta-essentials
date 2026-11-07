package dev.betaessentials.gui;

import dev.betaessentials.ItemNames;
import net.minecraft.client.gui.screen.ScreenBase;
import net.minecraft.client.gui.widgets.Button;
import net.minecraft.client.render.RenderHelper;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemInstance;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Creative item picker, modelled on modern Minecraft: click an item to pick one up (click again for more, middle
 * click or shift for a full stack), then click or drag it onto a hotbar slot. Clicking the item grid while holding
 * something deletes it. Hover an item and press 1-9 to put a stack in that hotbar slot.
 */
public class CreativeScreen extends ScreenBase {
    private static final ItemRenderer ITEMS = new ItemRenderer();
    private static final int COLS = 9, ROWS = 6, CELL = 18;
    private static String lastSearch = "";
    private static int lastScroll;

    /** Set while the creative screen opens the survival inventory, so the redirect lets it through. */
    public static boolean openingInventory;

    private String search = lastSearch;
    private boolean searchFocused;
    private int scroll = lastScroll;
    private List<ItemNames.Entry> shown = new ArrayList<>();
    private int left, top, panelW, panelH;
    private int ticks;

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        panelW = COLS * CELL + 14;
        panelH = 14 + 14 + ROWS * CELL + 8 + CELL + 8;
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        buttons.clear();
        buttons.add(new Button(0, left + panelW + 4, top, 60, 20, "Inventory"));
        filter();
    }

    @Override
    public void onClose() {
        Keyboard.enableRepeatEvents(false);
        if (!openingInventory && cursor() != null) {
            minecraft.player.inventory.addStack(cursor());
            setCursor(null);
        }
        lastSearch = search;
        lastScroll = scroll;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticks++;
    }

    private void filter() {
        shown.clear();
        String s = search.toLowerCase();
        for (ItemNames.Entry e : ItemNames.all()) {
            if (s.isEmpty() || e.display.toLowerCase().contains(s) || e.name.contains(s.replace(' ', '_'))) shown.add(e);
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        return Math.max(0, (shown.size() + COLS - 1) / COLS - ROWS);
    }

    private int gridX() {
        return left + 7;
    }

    private int gridY() {
        return top + 28;
    }

    private int hotbarY() {
        return gridY() + ROWS * CELL + 8;
    }

    private ItemNames.Entry entryAt(int mx, int my) {
        int cx = (mx - gridX()) / CELL, cy = (my - gridY()) / CELL;
        if (mx < gridX() || my < gridY() || cx >= COLS || cy >= ROWS) return null;
        int i = (cy + scroll) * COLS + cx;
        return i < shown.size() ? shown.get(i) : null;
    }

    private int hotbarAt(int mx, int my) {
        if (my < hotbarY() || my >= hotbarY() + CELL || mx < gridX()) return -1;
        int c = (mx - gridX()) / CELL;
        return c < 9 ? c : -1;
    }

    private boolean overSearch(int mx, int my) {
        return mx >= left + 7 && mx < left + panelW - 7 && my >= top + 14 && my < top + 26;
    }

    @Override
    protected void buttonClicked(Button b) {
        if (b.id == 0) {
            openingInventory = true;
            try {
                minecraft.openScreen(new net.minecraft.client.gui.screen.container.PlayerInventory(minecraft.player));
            } finally {
                openingInventory = false;
            }
        }
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            if (searchFocused) searchFocused = false;
            else minecraft.openScreen(null);
            return;
        }
        if (searchFocused) {
            if (key == Keyboard.KEY_BACK) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            } else if (key == Keyboard.KEY_RETURN) {
                searchFocused = false;
            } else if (c >= ' ' && c != 127 && search.length() < 30) {
                search += c;
            }
            filter();
            return;
        }
        if (key == minecraft.options.inventoryKey.key) {
            minecraft.openScreen(null);
            return;
        }
        if (key >= Keyboard.KEY_1 && key <= Keyboard.KEY_9) {
            int mx = Mouse.getX() * width / minecraft.actualWidth, my = height - Mouse.getY() * height / minecraft.actualHeight - 1;
            ItemNames.Entry e = entryAt(mx, my);
            if (e != null) {
                minecraft.player.inventory.main[key - Keyboard.KEY_1] = e.stack(e.maxStack());
            }
            return;
        }
        // typing starts a search
        if (Character.isLetterOrDigit(c)) {
            searchFocused = true;
            search += c;
            filter();
        }
    }

    private ItemInstance cursor() {
        return minecraft.player.inventory.getCursorItem();
    }

    private void setCursor(ItemInstance st) {
        minecraft.player.inventory.setCursorItem(st);
    }

    private static boolean same(ItemInstance a, ItemInstance b) {
        return a != null && b != null && a.itemId == b.itemId && a.getDamage() == b.getDamage();
    }

    private int pressedHotbar = -1;
    private boolean pressedGrid;

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        searchFocused = overSearch(mx, my);
        pressedHotbar = -1;
        pressedGrid = false;
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        PlayerInventory inv = minecraft.player.inventory;
        ItemNames.Entry e = entryAt(mx, my);
        if (e != null) {
            pressedGrid = true;
            ItemInstance c = cursor();
            ItemInstance item = e.stack(1);
            if (shift && c == null) {
                // shift: a full stack straight into the hotbar/inventory
                inv.addStack(e.stack(e.maxStack()));
            } else if (c == null) {
                setCursor(e.stack(button == 2 ? e.maxStack() : 1));
            } else if (same(c, item)) {
                if (button == 0) c.count = shift ? e.maxStack() : Math.min(e.maxStack(), c.count + 1);
                else if (button == 1 && --c.count <= 0) setCursor(null);
            } else {
                setCursor(null); // dropping something on the item list deletes it
            }
            return;
        }
        if (inGrid(mx, my) && cursor() != null) {
            setCursor(null);
            return;
        }
        int h = hotbarAt(mx, my);
        if (h >= 0) {
            pressedHotbar = h;
            clickHotbar(h, button, shift);
        }
    }

    /** Self-test: click grid cell (col,row) and release on hotbar slot h. */
    public String testDrag(int col, int row, int h) {
        int gx = gridX() + col * CELL + 8, gy = gridY() + row * CELL + 8;
        mouseClicked(gx, gy, 0);
        String held = cursor() == null ? "nothing" : cursor().count + "x" + cursor().itemId;
        mouseReleased(gridX() + h * CELL + 8, hotbarY() + 8, 0);
        ItemInstance s = minecraft.player.inventory.main[h];
        return "picked " + held + ", slot " + h + " now " + (s == null ? "empty" : s.count + "x" + s.itemId) + ", cursor " + (cursor() == null ? "empty" : "busy");
    }

    private boolean inGrid(int mx, int my) {
        return mx >= gridX() && my >= gridY() && mx < gridX() + COLS * CELL && my < gridY() + ROWS * CELL;
    }

    /** Container-style click on a hotbar slot. */
    private void clickHotbar(int h, int button, boolean shift) {
        PlayerInventory inv = minecraft.player.inventory;
        ItemInstance slot = inv.main[h], c = cursor();
        if (shift) {
            inv.main[h] = null;
            return;
        }
        if (c == null) {
            if (slot == null) return;
            if (button == 1) {
                int half = (slot.count + 1) / 2;
                setCursor(new ItemInstance(slot.itemId, half, slot.getDamage()));
                slot.count -= half;
                if (slot.count <= 0) inv.main[h] = null;
            } else {
                setCursor(slot);
                inv.main[h] = null;
            }
            return;
        }
        int max = c.getMaxStackSize();
        if (button == 1) {
            if (slot == null) inv.main[h] = new ItemInstance(c.itemId, 1, c.getDamage());
            else if (same(slot, c) && slot.count < max) slot.count++;
            else return;
            if (--c.count <= 0) setCursor(null);
            return;
        }
        if (slot == null) {
            inv.main[h] = c;
            setCursor(null);
        } else if (same(slot, c)) {
            int moved = Math.min(c.count, max - slot.count);
            slot.count += moved;
            c.count -= moved;
            if (c.count <= 0) setCursor(null);
        } else {
            inv.main[h] = c;
            setCursor(slot);
        }
    }

    /** Drag and drop: releasing over another hotbar slot places the held stack there. */
    @Override
    protected void mouseReleased(int mx, int my, int button) {
        super.mouseReleased(mx, my, button);
        if (button != 0 || cursor() == null) return;
        int h = hotbarAt(mx, my);
        if (h >= 0 && h != pressedHotbar && (pressedGrid || pressedHotbar >= 0)) clickHotbar(h, 0, false);
        pressedHotbar = -1;
        pressedGrid = false;
    }

    @Override
    public void onMouseEvent() {
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll + (wheel > 0 ? -1 : 1)));
            return;
        }
        super.onMouseEvent();
    }

    private void slot(int x, int y, boolean selected) {
        fill(x, y, x + 17, y + 17, selected ? 0xFFFFFFFF : 0xFF373737);
        fill(x + 1, y + 1, x + 17, y + 17, 0xFFFFFFFF);
        fill(x + 1, y + 1, x + 16, y + 16, 0xFF8B8B8B);
    }

    @Override
    public void render(int mx, int my, float delta) {
        renderBackground();
        // panel
        fill(left, top, left + panelW, top + panelH, 0xFF000000);
        fill(left + 1, top + 1, left + panelW - 1, top + panelH - 1, 0xFFC6C6C6);
        textManager.drawText("Creative items" + (shown.size() != ItemNames.all().size() ? " (" + shown.size() + ")" : ""), left + 7, top + 4, 0x404040);
        // search field
        fill(left + 7, top + 14, left + panelW - 7, top + 26, searchFocused ? 0xFFFFFFFF : 0xFF8B8B8B);
        fill(left + 8, top + 15, left + panelW - 8, top + 25, 0xFF000000);
        String hint = search.isEmpty() && !searchFocused ? "§8Type to search..." : search + (searchFocused && ticks / 6 % 2 == 0 ? "_" : "");
        textManager.drawText(hint, left + 10, top + 16, 0xFFFFFF);
        // slots
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) slot(gridX() + c * CELL - 1, gridY() + r * CELL - 1, false);
        PlayerInventory inv = minecraft.player.inventory;
        for (int c = 0; c < 9; c++) slot(gridX() + c * CELL - 1, hotbarY() - 1, c == inv.selectedHotbarSlot);
        // scroll bar
        int max = maxScroll();
        if (max > 0) {
            int bx = left + panelW - 5, by = gridY(), bh = ROWS * CELL - 2;
            fill(bx, by, bx + 3, by + bh, 0xFF8B8B8B);
            int th = Math.max(8, bh * ROWS / (max + ROWS));
            int ty = by + (bh - th) * scroll / max;
            fill(bx, ty, bx + 3, ty + th, 0xFFFFFFFF);
        }

        GL11.glPushMatrix();
        GL11.glRotatef(120.0F, 1.0F, 0.0F, 0.0F);
        RenderHelper.enableLighting();
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(32826);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int i = (r + scroll) * COLS + c;
                if (i >= shown.size()) break;
                ItemInstance st = shown.get(i).stack(1);
                ITEMS.method_1487(textManager, minecraft.textureManager, st, gridX() + c * CELL, gridY() + r * CELL);
            }
        }
        for (int c = 0; c < 9; c++) {
            ItemInstance st = inv.main[c];
            if (st == null) continue;
            ITEMS.method_1487(textManager, minecraft.textureManager, st, gridX() + c * CELL, hotbarY());
            ITEMS.method_1488(textManager, minecraft.textureManager, st, gridX() + c * CELL, hotbarY());
        }
        GL11.glDisable(32826);
        RenderHelper.disableLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        ItemNames.Entry hover = entryAt(mx, my);
        if (hover != null) {
            int cx = gridX() + (mx - gridX()) / CELL * CELL, cy = gridY() + (my - gridY()) / CELL * CELL;
            fillGradient(cx, cy, cx + 16, cy + 16, 0x80FFFFFF, 0x80FFFFFF);
        }
        super.render(mx, my, delta);
        if (cursor() != null) {
        } else if (hover != null) tooltip(hover.display + " §8/give " + dev.betaessentials.Ess.mc.player.name + " " + hover.name, mx, my);
        else {
            int h = hotbarAt(mx, my);
            if (h >= 0 && inv.main[h] != null) {
                ItemNames.Entry e = ItemNames.find(inv.main[h].itemId + ":" + inv.main[h].getDamage());
                if (e != null) tooltip(e.display, mx, my);
            }
        }
        ItemInstance c = cursor();
        if (c != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 32F);
            GL11.glPushMatrix();
            GL11.glRotatef(120.0F, 1.0F, 0.0F, 0.0F);
            RenderHelper.enableLighting();
            GL11.glPopMatrix();
            GL11.glEnable(32826);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            ITEMS.method_1487(textManager, minecraft.textureManager, c, mx - 8, my - 8);
            ITEMS.method_1488(textManager, minecraft.textureManager, c, mx - 8, my - 8);
            GL11.glDisable(32826);
            RenderHelper.disableLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glPopMatrix();
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private void tooltip(String s, int mx, int my) {
        int w = textManager.getTextWidth(s);
        int x = Math.min(mx + 12, width - w - 4), y = my - 12;
        fillGradient(x - 3, y - 3, x + w + 3, y + 11, 0xC0000000, 0xC0000000);
        textManager.drawTextWithShadow(s, x, y, 0xFFFFFF);
    }
}
