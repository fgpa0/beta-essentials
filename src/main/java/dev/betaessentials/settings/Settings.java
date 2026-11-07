package dev.betaessentials.settings;

import dev.betaessentials.Ess;
import dev.betaessentials.EssData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Every Beta Essentials setting, in screen order. World settings live in level.dat ({@link EssData#rules}), client
 * settings in config/beta-essentials.properties. Everything defaults to vanilla behaviour.
 */
public final class Settings {
    private Settings() {}

    /** Categories, in the order the settings screen shows them. */
    public static final String GAME_RULES = "Game Rules", CLIENT = "Client";
    private static final List<String> CATEGORY_ORDER = List.of(GAME_RULES, CLIENT);

    public static final Map<String, Setting> ALL = new LinkedHashMap<>();

    /** config/beta-essentials.properties as read at startup; settings registered later pick their value up from here. */
    private static final Properties file = new Properties();

    public static Setting toggle(String key, Setting.Scope scope, String category, String title, boolean def, String desc) {
        return add(new Setting(key, scope, category, title, true, def ? 1 : 0, 0, 1, null, desc));
    }

    public static Setting number(String key, Setting.Scope scope, String category, String title, int def, int min, int max, String desc) {
        return add(new Setting(key, scope, category, title, false, def, min, max, null, desc));
    }

    /** A choice between named values; the value is the index into {@code choices}. */
    public static Setting choice(String key, Setting.Scope scope, String category, String title, int def, String desc, String... choices) {
        return add(new Setting(key, scope, category, title, false, def, 0, choices.length - 1, choices, desc));
    }

    private static Setting add(Setting s) {
        if (ALL.containsKey(s.key)) throw new IllegalStateException("Duplicate setting " + s.key);
        ALL.put(s.key, s);
        if (s.scope == Setting.Scope.CLIENT) fromFile(s);
        return s;
    }

    /** Takes a client setting's value from the file, if the file sets it. */
    private static void fromFile(Setting s) {
        String v = file.getProperty(s.key);
        if (v == null) return;
        Integer parsed = s.parse(v);
        if (parsed != null) s.current = parsed;
    }

    public static int value(Setting s) {
        if (s.scope == Setting.Scope.CLIENT) return s.current;
        EssData d = Ess.data();
        Integer v = d == null ? null : d.rules.get(s.key);
        return v != null ? s.clamp(v) : s.def;
    }

    /** Changes a setting; a world setting needs a world to be open. Client settings are saved by {@link #save}. */
    public static void set(Setting s, int v) {
        v = s.clamp(v);
        if (s.scope == Setting.Scope.CLIENT) {
            s.current = v;
        } else {
            EssData d = Ess.data();
            if (d != null) d.rules.put(s.key, v);
        }
    }

    public static Setting find(String key) {
        for (Setting s : ALL.values()) if (s.key.equalsIgnoreCase(key)) return s;
        return null;
    }

    public static List<Setting> of(Setting.Scope scope) {
        List<Setting> l = new ArrayList<>();
        for (Setting s : ALL.values()) if (s.scope == scope) l.add(s);
        return l;
    }

    /** Categories in screen order: the known ones first, then any others in the order they were first used. */
    public static List<String> categories() {
        List<String> l = new ArrayList<>(CATEGORY_ORDER);
        for (Setting s : ALL.values()) if (!l.contains(s.category)) l.add(s.category);
        return l;
    }

    private static File file() {
        return new File("config", "beta-essentials.properties");
    }

    /** Reads config/beta-essentials.properties (once, at startup), or the file of versions before the rename. */
    public static void load() {
        File f = file();
        if (!f.exists()) f = new File("config", "essentials.properties");
        if (!f.exists()) return;
        try (FileInputStream in = new FileInputStream(f)) {
            file.load(in);
        } catch (Exception e) {
            Ess.LOG("Could not read " + f + ": " + e);
        }
        for (Setting s : of(Setting.Scope.CLIENT)) fromFile(s);
    }

    /**
     * Writes config/beta-essentials.properties: every client setting with its explanation, but only changed ones as
     * active lines (unchanged ones are commented out), so a later change of a default still reaches the player.
     */
    public static void save() {
        List<Setting> list = of(Setting.Scope.CLIENT);
        if (list.isEmpty()) return;
        File f = file();
        try {
            f.getParentFile().mkdirs();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                w.write("# Beta Essentials settings (the same in every world). Change them in game under\n"
                        + "# Options > Beta Essentials..., or here while the game is closed. Lines starting with #\n"
                        + "# are settings left at their default.\nformat=2\n");
                for (Setting s : list) {
                    w.write("\n# " + s.title + ": " + s.desc + " (default " + s.format(s.def) + ")\n");
                    w.write((s.changed() ? "" : "#") + s.key + "=" + s.format(value(s)) + "\n");
                }
            }
        } catch (Exception e) {
            Ess.LOG("Could not write " + f + ": " + e);
        }
    }
}
