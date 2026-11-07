package dev.betaessentials.gui;

import dev.betaessentials.Ess;
import dev.betaessentials.EssData;
import dev.betaessentials.GameRules;
import dev.betaessentials.settings.Setting;
import dev.betaessentials.settings.Settings;
import net.minecraft.client.gui.screen.ScreenBase;
import net.minecraft.client.gui.widgets.Button;
import net.minecraft.client.gui.widgets.Textbox;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Beta Essentials settings: a tab per category, collapsible groups, a search over every tab, changed settings marked
 * in yellow with a reset button each, and a tooltip with the details. Opened from Create World (the new world's
 * game rules) or from Options (client settings, plus the open world's game rules).
 */
public class SettingsScreen extends ScreenBase {
    private static final int ROW = 22, TAB_H = 14;
    private static final int ID_CHANGED = 100, ID_RESET_TAB = 101, ID_RESET_ALL = 102, ID_DONE = 200;
    private static final int YELLOW = 0xFFFF55, GREY = 0xA0A0A0;

    /** The open tab and the collapsed groups ("category/group"), kept while the game runs. */
    private static String lastTab;
    private static final Set<String> collapsed = new HashSet<>();

    private final ScreenBase parent;
    /** Editing the game rules of the world about to be created ({@link GameRules#pending}). */
    private final boolean newWorld;
    private Textbox search;
    private String searchText = "";
    private boolean changedOnly;
    private String tab;
    private final List<String> tabs = new ArrayList<>();
    /** Tab rectangles: x0, y0, x1, y1, in the order of {@link #tabs}. */
    private final List<int[]> tabBoxes = new ArrayList<>();
    private int listTop;

    /** A heading (setting == null; collapseKey null in search results) or one setting with its control and reset button. */
    private static final class Row {
        final String heading, collapseKey;
        final Setting setting;
        Button control, reset;

        Row(String heading, String collapseKey) {
            this.heading = heading;
            this.collapseKey = collapseKey;
            this.setting = null;
        }

        Row(Setting setting) {
            this.heading = null;
            this.collapseKey = null;
            this.setting = setting;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private boolean draggingBar;
    private Button pressed, changedButton, resetTab, resetAll;
    /** The reset button waiting for a second click, and until when. */
    private int confirmId = -1;
    private long confirmUntil;

    private SettingsScreen(ScreenBase parent, boolean newWorld) {
        this.parent = parent;
        this.newWorld = newWorld;
        if (newWorld && GameRules.pending == null) {
            GameRules.pending = new LinkedHashMap<>();
            for (Setting s : Settings.of(Setting.Scope.WORLD)) GameRules.pending.put(s.key, s.def);
        }
    }

    /** Client settings, and the open world's game rules if a world is open. */
    public SettingsScreen(ScreenBase parent) {
        this(parent, false);
    }

    /** Game rules for the world about to be created. */
    public static SettingsScreen forNewWorld(ScreenBase parent) {
        return new SettingsScreen(parent, true);
    }

    // ---- values ----

    private boolean shown(Setting s) {
        if (newWorld) return s.scope == Setting.Scope.WORLD;
        return s.scope == Setting.Scope.CLIENT || Ess.data() != null;
    }

    /** World rules of an open world only change with cheats on (like /gamerule). */
    private boolean locked(Setting s) {
        if (newWorld || s.scope != Setting.Scope.WORLD) return false;
        EssData d = Ess.data();
        return d == null || !d.cheats;
    }

    private int get(Setting s) {
        if (newWorld) return GameRules.pending.getOrDefault(s.key, s.def);
        return s.value();
    }

    private void put(Setting s, int v) {
        if (newWorld) GameRules.pending.put(s.key, s.clamp(v));
        else Settings.set(s, v);
    }

    private boolean changed(Setting s) {
        return get(s) != s.def;
    }

    private boolean searching() {
        return !searchText.isEmpty();
    }

    /** Searching or showing only changed settings: the list covers every tab, under "Category > Group" headings. */
    private boolean global() {
        return searching() || changedOnly;
    }

    private boolean matches(Setting s) {
        if (changedOnly && !changed(s)) return false;
        if (!searching()) return true;
        String q = searchText.toLowerCase(Locale.ROOT);
        for (String f : new String[]{s.title, s.key, s.desc, s.category, s.group()})
            if (f != null && f.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    private boolean anyChanged(String category) {
        for (Setting s : Settings.ALL.values())
            if (shown(s) && (category == null || s.category.equals(category)) && changed(s) && !locked(s)) return true;
        return false;
    }

    // ---- layout ----

    private int left() {
        return width / 2 - 155;
    }

    private int right() {
        return width / 2 + 155;
    }

    private int controlX() {
        return right() - 6 - 22 - 110;
    }

    private void layoutTabs() {
        tabs.clear();
        tabBoxes.clear();
        for (String c : Settings.categories()) {
            for (Setting s : Settings.ALL.values()) {
                if (s.category.equals(c) && shown(s)) {
                    tabs.add(c);
                    break;
                }
            }
        }
        if (tab == null || !tabs.contains(tab)) tab = tabs.contains(lastTab) ? lastTab : tabs.isEmpty() ? null : tabs.get(0);
        // the tab bar may be wider than the list (up to 420), rows of tabs are centred
        int span = Math.max(310, Math.min(width - 16, 420)), x0 = width / 2 - span / 2;
        List<List<int[]>> lines = new ArrayList<>();
        List<int[]> line = new ArrayList<>();
        int x = 0;
        for (String c : tabs) {
            int w = textManager.getTextWidth(c) + 18;
            if (x + w > span && !line.isEmpty()) {
                lines.add(line);
                line = new ArrayList<>();
                x = 0;
            }
            line.add(new int[]{x, w});
            x += w + 2;
        }
        if (!line.isEmpty()) lines.add(line);
        int y = 20;
        for (List<int[]> l : lines) {
            int[] last = l.get(l.size() - 1);
            int shift = x0 + (span - (last[0] + last[1])) / 2;
            for (int[] t : l) tabBoxes.add(new int[]{shift + t[0], y, shift + t[0] + t[1], y + TAB_H});
            y += TAB_H + 2;
        }
        int searchY = (tabBoxes.isEmpty() ? 20 : tabBoxes.get(tabBoxes.size() - 1)[3]) + 5;
        listTop = searchY + 24;
    }

    private int visibleRows() {
        return Math.max(1, (height - 32 - listTop) / ROW);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    /** A category's listed settings by group (null first), groups in the order they first appear. */
    private Map<String, List<Setting>> grouped(String category) {
        Map<String, List<Setting>> out = new LinkedHashMap<>();
        out.put(null, new ArrayList<>());
        for (Setting s : Settings.ALL.values())
            if (s.category.equals(category) && shown(s) && matches(s)) out.computeIfAbsent(s.group(), g -> new ArrayList<>()).add(s);
        return out;
    }

    private void rebuild() {
        rows.clear();
        if (global()) {
            for (String c : tabs) {
                for (Map.Entry<String, List<Setting>> g : grouped(c).entrySet()) {
                    if (g.getValue().isEmpty()) continue;
                    rows.add(new Row(g.getKey() == null ? c : c + " > " + g.getKey(), null));
                    for (Setting s : g.getValue()) rows.add(row(s));
                }
            }
        } else if (tab != null) {
            for (Map.Entry<String, List<Setting>> g : grouped(tab).entrySet()) {
                if (g.getValue().isEmpty()) continue;
                String key = tab + "/" + g.getKey();
                if (g.getKey() != null) rows.add(new Row(g.getKey(), key));
                if (g.getKey() == null || !collapsed.contains(key)) for (Setting s : g.getValue()) rows.add(row(s));
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private Row row(Setting s) {
        Row r = new Row(s);
        int x = controlX();
        if (s.isNumber()) r.control = new Slider(0, x, 0, 110, s.min, s.max, get(s), v -> put(s, v), s::display);
        else r.control = new Button(0, x, 0, 110, 20, s.display(get(s)));
        r.reset = new ResetButton(right() - 6 - 20);
        return r;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        Keyboard.enableRepeatEvents(true);
        layoutTabs();
        int searchY = listTop - 24;
        search = new Textbox(this, textManager, left(), searchY, 310 - 106, 20, searchText);
        search.setSelected(true);
        search.setMaxLength(32);
        buttons.clear();
        changedButton = new Button(ID_CHANGED, right() - 100, searchY, 100, 20, changedLabel());
        resetTab = new Button(ID_RESET_TAB, left(), height - 26, 100, 20, "");
        resetAll = new Button(ID_RESET_ALL, left() + 105, height - 26, 100, 20, "");
        buttons.add(changedButton);
        buttons.add(resetTab);
        buttons.add(resetAll);
        buttons.add(new Button(ID_DONE, left() + 210, height - 26, 100, 20, "Done"));
        rebuild();
        updateButtons();
    }

    private String changedLabel() {
        return changedOnly ? "Show: Changed" : "Show: All";
    }

    private void updateButtons() {
        if (confirmId >= 0 && System.currentTimeMillis() > confirmUntil) confirmId = -1;
        resetTab.text = confirmId == ID_RESET_TAB ? "§eClick again" : global() ? "Reset Shown" : "Reset Tab";
        resetAll.text = confirmId == ID_RESET_ALL ? "§eClick again" : "Reset All";
        boolean shownChanged = false;
        for (Row r : rows) if (r.setting != null && changed(r.setting) && !locked(r.setting)) shownChanged = true;
        resetTab.active = global() ? shownChanged : tab != null && anyChanged(tab); // a tab counts its collapsed groups too
        resetAll.active = anyChanged(null);
    }

    @Override
    public void onClose() {
        Keyboard.enableRepeatEvents(false);
        if (!newWorld) Settings.save();
    }

    @Override
    public void tick() {
        search.tick();
        updateButtons();
    }

    // ---- input ----

    private void reset(Setting s) {
        if (!locked(s)) put(s, s.def);
    }

    /** Rebuilds the list (new values, filters or groups) keeping the scroll position where possible. */
    private void refreshRows() {
        int keep = scroll;
        rebuild();
        scroll = Math.min(keep, maxScroll());
        updateButtons();
    }

    @Override
    protected void buttonClicked(Button b) {
        if (b.id == ID_DONE) {
            minecraft.openScreen(parent);
        } else if (b.id == ID_CHANGED) {
            changedOnly = !changedOnly;
            b.text = changedLabel();
            scroll = 0;
            refreshRows();
        } else if (b.id == ID_RESET_TAB || b.id == ID_RESET_ALL) {
            if (confirmId != b.id) {
                confirmId = b.id;
                confirmUntil = System.currentTimeMillis() + 3000;
            } else {
                confirmId = -1;
                if (b.id == ID_RESET_ALL) {
                    for (Setting s : Settings.ALL.values()) if (shown(s)) reset(s);
                } else if (global()) {
                    for (Row r : rows) if (r.setting != null) reset(r.setting);
                } else {
                    for (Setting s : Settings.ALL.values()) if (shown(s) && s.category.equals(tab)) reset(s);
                }
                refreshRows();
            }
            updateButtons();
        }
    }

    /** Switches flip; choices step forward (backwards with Shift). Sliders handle themselves. */
    private void activate(Row r) {
        Setting s = r.setting;
        if (s.isNumber()) return;
        int v = get(s);
        boolean back = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (s.isSwitch) v = v != 0 ? 0 : 1;
        else v = back ? (v <= s.min ? s.max : v - 1) : (v >= s.max ? s.min : v + 1);
        put(s, v);
        r.control.text = s.display(get(s));
    }

    /** Puts the visible rows' widgets in place (clicks can come before the first render). */
    private void place() {
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.setting == null) continue;
            boolean visible = i >= scroll && i < scroll + visibleRows();
            boolean locked = locked(r.setting);
            r.control.y = r.reset.y = listTop + (i - scroll) * ROW;
            r.control.visible = r.reset.visible = visible;
            r.control.active = visible && !locked;
            r.reset.active = visible && !locked && changed(r.setting);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        for (int i = 0; i < tabBoxes.size(); i++) {
            int[] b = tabBoxes.get(i);
            if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                openTab(tabs.get(i));
                minecraft.soundHelper.playSound("random.click", 1.0F, 1.0F);
                return;
            }
        }
        if (maxScroll() > 0 && mx >= right() - 5 && mx < right() && my >= listTop && my < listTop + visibleRows() * ROW) {
            draggingBar = true;
            return;
        }
        place();
        for (int i = scroll; i < Math.min(rows.size(), scroll + visibleRows()); i++) {
            Row r = rows.get(i);
            if (r.setting == null) {
                int y = listTop + (i - scroll) * ROW;
                if (r.collapseKey != null && my >= y && my < y + ROW - 2 && mx >= left() && mx < right()) {
                    if (!collapsed.remove(r.collapseKey)) collapsed.add(r.collapseKey);
                    minecraft.soundHelper.playSound("random.click", 1.0F, 1.0F);
                    refreshRows();
                    return;
                }
            } else if (r.control.isMouseOver(minecraft, mx, my)) {
                minecraft.soundHelper.playSound("random.click", 1.0F, 1.0F);
                pressed = r.control;
                activate(r);
                updateButtons();
                return;
            } else if (r.reset.isMouseOver(minecraft, mx, my)) {
                minecraft.soundHelper.playSound("random.click", 1.0F, 1.0F);
                reset(r.setting);
                refreshRows();
                return;
            }
        }
    }

    /** Opens a tab (and leaves the search and the changed-only list). */
    public void openTab(String category) {
        if (!tabs.contains(category)) return;
        searchText = "";
        search.setText("");
        changedOnly = false;
        changedButton.text = changedLabel();
        tab = lastTab = category;
        scroll = 0;
        refreshRows();
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        super.mouseReleased(mx, my, button);
        if (button != 0) return;
        draggingBar = false;
        if (pressed != null) {
            pressed.mouseReleased(mx, my);
            pressed = null;
            updateButtons();
        }
    }

    @Override
    public void onMouseEvent() {
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll + (wheel > 0 ? -2 : 2)));
            return;
        }
        super.onMouseEvent();
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            minecraft.openScreen(parent);
            return;
        }
        search.keyPressed(c, key);
        if (!search.getText().equals(searchText)) {
            searchText = search.getText();
            scroll = 0;
            refreshRows();
        }
    }

    // ---- drawing ----

    @Override
    public void render(int mx, int my, float delta) {
        renderBackground();
        drawTextWithShadowCentred(textManager, newWorld ? "Game Rules" : "Beta Essentials", width / 2, 7, 0xFFFFFF);
        renderTabs(mx, my);
        search.draw();
        if (searchText.isEmpty()) drawTextWithShadow(textManager, "Search all settings...", left() + 10, listTop - 18, 0x707070);

        if (draggingBar && maxScroll() > 0) {
            float f = (my - listTop) / (float) Math.max(1, visibleRows() * ROW);
            scroll = Math.max(0, Math.min(maxScroll(), Math.round(f * maxScroll())));
        }
        place();
        Row tip = null;
        if (rows.isEmpty()) {
            String msg = changedOnly ? "Nothing here is changed from its default"
                    : searching() ? "No settings match \"" + searchText + "\"" : "World rules show here while a world is open";
            drawTextWithShadowCentred(textManager, msg, width / 2, listTop + 20, GREY);
        }
        for (int i = scroll; i < Math.min(rows.size(), scroll + visibleRows()); i++) {
            Row r = rows.get(i);
            int y = listTop + (i - scroll) * ROW;
            if (r.setting == null) {
                renderHeading(r, y, mx, my);
                continue;
            }
            Setting s = r.setting;
            boolean locked = locked(s), changed = changed(s);
            boolean hover = mx >= left() && mx < right() - 6 && my >= y && my < y + ROW - 2;
            if (hover) fill(left(), y, right() - 6, y + 20, 0x20FFFFFF);
            if (changed) fill(left(), y, left() + 2, y + 20, 0xFFFFFF55);
            int color = locked ? 0x808080 : changed ? YELLOW : 0xFFFFFF;
            drawTextWithShadow(textManager, trim(s.title, controlX() - left() - 12), left() + 6, y + 6, color);
            r.control.render(minecraft, mx, my);
            r.reset.render(minecraft, mx, my);
            if (hover) tip = r;
        }
        if (maxScroll() > 0) {
            int trackH = visibleRows() * ROW - 2;
            int barH = Math.max(10, trackH * visibleRows() / rows.size());
            int barY = listTop + (trackH - barH) * scroll / maxScroll();
            fill(right() - 4, listTop, right() - 2, listTop + trackH, 0x40FFFFFF);
            fill(right() - 4, barY, right() - 2, barY + barH, draggingBar ? 0xFFFFFFA0 : 0xC0FFFFFF);
        }
        super.render(mx, my, delta);
        if (tip != null) renderTooltip(tip, mx, my);
    }

    private void renderTabs(int mx, int my) {
        for (int i = 0; i < tabs.size(); i++) {
            int[] b = tabBoxes.get(i);
            String c = tabs.get(i);
            boolean open = !global() && c.equals(tab);
            boolean hover = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
            fill(b[0], b[1], b[2], b[3], open ? 0xFFA0A0A0 : hover ? 0xFF707070 : 0xFF404040);
            fill(b[0] + 1, b[1] + 1, b[2] - 1, b[3] - (open ? 0 : 1), open ? 0xFF303030 : 0xC0000000);
            drawTextWithShadow(textManager, c, b[0] + 8, b[1] + 3, open ? 0xFFFFFF : hover ? 0xFFFFA0 : GREY);
            if (anyChanged(c)) drawTextWithShadow(textManager, "*", b[2] - 8, b[1] + 3, YELLOW);
        }
        if (!tabBoxes.isEmpty()) {
            int y = tabBoxes.get(tabBoxes.size() - 1)[3];
            fill(Math.min(left(), tabBoxes.get(0)[0]), y, Math.max(right(), tabBoxes.get(tabBoxes.size() - 1)[2]), y + 1, 0xFFA0A0A0);
        }
    }

    private void renderHeading(Row r, int y, int mx, int my) {
        String text = r.collapseKey == null ? r.heading : (collapsed.contains(r.collapseKey) ? "+ " : "- ") + r.heading;
        int w = textManager.getTextWidth(text), cy = y + 10;
        boolean hover = r.collapseKey != null && mx >= left() && mx < right() && my >= y && my < y + ROW - 2;
        fill(left(), cy, width / 2 - w / 2 - 6, cy + 1, 0x60FFFFFF);
        fill(width / 2 + w / 2 + 6, cy, right() - 6, cy + 1, 0x60FFFFFF);
        drawTextWithShadowCentred(textManager, text, width / 2, y + 6, hover ? 0xFFFFA0 : 0xE0E0E0);
    }

    private String trim(String text, int maxWidth) {
        if (textManager.getTextWidth(text) <= maxWidth) return text;
        while (text.length() > 1 && textManager.getTextWidth(text + "...") > maxWidth) text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    private void renderTooltip(Row r, int mx, int my) {
        Setting s = r.setting;
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        boolean overReset = mx >= r.reset.x && mx < r.reset.x + 20 && my >= r.reset.y && my < r.reset.y + 20;
        if (overReset) {
            lines.add(changed(s) ? "Reset to default: " + s.display(s.def) : "Already at its default");
            colors.add(0xFFFFFF);
        } else {
            lines.add(s.title);
            colors.add(0xFFFFFF);
            for (String l : wrap(s.desc, 200)) {
                lines.add(l);
                colors.add(GREY);
            }
            lines.add("Default: " + s.display(s.def) + (changed(s) ? " (changed)" : ""));
            colors.add(changed(s) ? YELLOW : 0x808080);
            lines.add(s.scope == Setting.Scope.CLIENT ? "Applies to every world" : newWorld ? "Saved in the new world" : "Saved in this world");
            colors.add(0x808080);
            if (locked(s)) {
                lines.add("Turn on cheats (game menu) to change");
                colors.add(0xFF6060);
            }
        }
        int w = 0;
        for (String l : lines) w = Math.max(w, textManager.getTextWidth(l));
        int h = lines.size() * 10;
        int x = mx + 12, y = my - 12;
        if (x + w + 4 > width) x = mx - w - 16;
        if (y + h + 4 > height) y = height - h - 4;
        if (y < 4) y = 4;
        fill(x - 3, y - 3, x + w + 3, y + h + 1, 0xF0100010);
        fill(x - 3, y - 3, x + w + 3, y - 2, 0xFF5000A0);
        fill(x - 3, y + h, x + w + 3, y + h + 1, 0xFF28007F);
        for (int i = 0; i < lines.size(); i++) drawTextWithShadow(textManager, lines.get(i), x, y + i * 10, colors.get(i));
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (textManager.getTextWidth(next) > maxWidth && !line.isEmpty()) {
                out.add(line);
                line = word;
            } else {
                line = next;
            }
        }
        if (!line.isEmpty()) out.add(line);
        return out;
    }

    // ---- for tests ----

    public List<String> tabs() {
        return tabs;
    }

    public String tab() {
        return tab;
    }

    /** The settings in the list as shown, headings left out. */
    public List<Setting> listed() {
        List<Setting> l = new ArrayList<>();
        for (Row r : rows) if (r.setting != null) l.add(r.setting);
        return l;
    }

    /** Screen position inside a setting's control (index 0) or reset button (1), scrolling it into view; null if not listed. */
    public int[] posOf(Setting s, int which) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).setting != s) continue;
            scroll = Math.max(0, Math.min(maxScroll(), i));
            int y = listTop + (i - scroll) * ROW + 5;
            return new int[]{which == 0 ? controlX() + 5 : right() - 6 - 20 + 5, y};
        }
        return null;
    }

    /** Screen position on a group heading of the open tab (scrolled to the top), or null. */
    public int[] headingPos(String group) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).setting != null || !group.equals(rows.get(i).heading)) continue;
            scroll = Math.max(0, Math.min(maxScroll(), i));
            return new int[]{width / 2, listTop + (i - scroll) * ROW + 6};
        }
        return null;
    }

    /** Screen position inside a tab. */
    public int[] tabPos(String category) {
        int i = tabs.indexOf(category);
        if (i < 0) return null;
        int[] b = tabBoxes.get(i);
        return new int[]{b[0] + 3, b[1] + 3};
    }
}
