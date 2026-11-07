package dev.betaessentials.settings;

/**
 * One Beta Essentials setting: a switch, a number or a choice between named values. Every value is held as an int
 * (switches as 0/1, choices as the index into {@link #choices}).
 */
public final class Setting {
    public enum Scope {
        /** Saved in level.dat, chosen per world (game rules). */
        WORLD,
        /** Saved in config/beta-essentials.properties, the same in every world. */
        CLIENT
    }

    public final String key, title, category, desc;
    public final Scope scope;
    public final int def, min, max;
    /** Names of the values min..max for a choice, null for switches and numbers. */
    public final String[] choices;
    public final boolean isSwitch;
    /** Sub-heading within the category (null for none). */
    private String group;
    /** The current value of a client setting (read on any thread, e.g. by a performance mod's worker threads). */
    volatile int current;

    Setting(String key, Scope scope, String category, String title, boolean isSwitch, int def, int min, int max,
            String[] choices, String desc) {
        this.key = key;
        this.scope = scope;
        this.category = category;
        this.title = title;
        this.isSwitch = isSwitch;
        this.def = def;
        this.min = min;
        this.max = max;
        this.choices = choices;
        this.desc = desc;
        this.current = def;
    }

    /** Puts this setting under a sub-heading of its category. */
    public Setting group(String group) {
        this.group = group;
        return this;
    }

    public String group() {
        return group;
    }

    /** Differs from its default (the settings screen marks it and offers a reset). */
    public boolean changed() {
        return value() != def;
    }

    public boolean isNumber() {
        return !isSwitch && choices == null;
    }

    public int value() {
        return Settings.value(this);
    }

    public boolean on() {
        return value() != 0;
    }

    public int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    /** The value as commands and the config file write it. */
    public String format(int v) {
        if (isSwitch) return Boolean.toString(v != 0);
        if (choices != null) return choices[clamp(v) - min].toLowerCase().replace(' ', '_');
        return Integer.toString(v);
    }

    /** The value as the settings screen shows it. */
    public String display(int v) {
        if (isSwitch) return v != 0 ? "ON" : "OFF";
        if (choices != null) return choices[clamp(v) - min];
        return Integer.toString(v);
    }

    /** Parses a value for this setting, or returns null. */
    public Integer parse(String s) {
        s = s.trim();
        if (isSwitch) {
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on")) return 1;
            if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off")) return 0;
            return null;
        }
        if (choices != null) {
            for (int i = 0; i < choices.length; i++) if (format(min + i).equalsIgnoreCase(s.replace(' ', '_'))) return min + i;
            return null;
        }
        try {
            int v = Integer.parseInt(s);
            return v < min || v > max ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** What a command should say when {@link #parse} fails. */
    public String expected() {
        if (isSwitch) return "Value must be true or false";
        if (choices != null) {
            StringBuilder b = new StringBuilder("Value must be one of:");
            for (int i = min; i <= max; i++) b.append(' ').append(format(i));
            return b.toString();
        }
        return "Value must be a number from " + min + " to " + max;
    }
}
