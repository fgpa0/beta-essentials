package dev.betaessentials.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Command {
    public final String name;
    public final String usage;
    public final String desc;
    public final String[] aliases;

    protected Command(String name, String usage, String desc, String... aliases) {
        this.name = name;
        this.usage = usage;
        this.desc = desc;
        this.aliases = aliases;
    }

    /** Thrown to report a usage error. */
    public static final class Fail extends RuntimeException {
        public Fail(String m) {
            super(m, null, false, false);
        }
    }

    public abstract void run(String[] args);

    /** Whether the world must have cheats on. Commands that only show information can return false. */
    public boolean needsCheats() {
        return true;
    }

    /** Hidden commands (protocol helpers of other mods) are left out of /help and TAB completion. */
    public boolean listed() {
        return true;
    }

    /** Suggestions for argument number {@code index} (0-based) given the arguments typed so far. */
    public List<String> complete(String[] args, int index) {
        return Collections.emptyList();
    }

    protected Fail usage() {
        return new Fail("Usage: " + usage);
    }

    protected static List<String> list(String... s) {
        List<String> l = new ArrayList<>();
        Collections.addAll(l, s);
        return l;
    }

    protected static int parseInt(String s, int min, int max) {
        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new Fail("'" + s + "' is not a number");
        }
        if (v < min || v > max) throw new Fail("'" + s + "' must be between " + min + " and " + max);
        return v;
    }

    /** A coordinate, absolute or relative (~, ~5, ~-2). */
    protected static double parseCoord(String s, double base, boolean center) {
        try {
            if (s.startsWith("~")) return base + (s.length() > 1 ? Double.parseDouble(s.substring(1)) : 0);
            double v = Double.parseDouble(s);
            if (center && !s.contains(".")) v += 0.5;
            return v;
        } catch (NumberFormatException e) {
            throw new Fail("'" + s + "' is not a coordinate");
        }
    }
}
