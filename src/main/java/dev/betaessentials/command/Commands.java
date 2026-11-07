package dev.betaessentials.command;

import dev.betaessentials.Ess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Commands {
    private Commands() {}

    static Source source;

    public static void execute(String line, Source actor) {
        Source previous = source;
        source = actor;
        try { execute(line); }
        finally { source = previous; }
    }

    public static List<String> complete(String line, Source actor) {
        Source previous = source;
        source = actor;
        try { return complete(line); }
        finally { source = previous; }
    }

    /** Primary names in registration order. */
    public static final Map<String, Command> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, Command> LOOKUP = new LinkedHashMap<>();

    static {
        All.register();
        // commands of other mods: see CommandProvider
        for (CommandProvider p : net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("beta-essentials:commands", CommandProvider.class)) {
            try {
                p.registerCommands();
            } catch (Throwable t) {
                Ess.LOG("could not add the commands of " + p.getClass().getName() + ": " + t);
            }
        }
    }

    /** Adds a command. A name or alias that is already taken is not replaced. */
    public static void register(Command c) {
        if (LOOKUP.containsKey(c.name)) {
            Ess.LOG("command /" + c.name + " already exists; " + c.getClass().getName() + " not added");
            return;
        }
        BY_NAME.put(c.name, c);
        LOOKUP.put(c.name, c);
        for (String a : c.aliases) LOOKUP.putIfAbsent(a, c);
    }

    /** Can the current actor run this command now? */
    public static boolean allowed(Command c) {
        return !c.needsCheats() || cheats();
    }

    public static Command get(String name) {
        return LOOKUP.get(name.toLowerCase());
    }

    /** Runs "/name args...". */
    public static void execute(String line) {
        String body = line.startsWith("/") ? line.substring(1) : line;
        String[] parts = body.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;
        Command c = get(parts[0]);
        if ((c == null && !cheats()) || (c != null && !allowed(c))) {
            Ess.err("Cheats are not enabled in this world. Turn them on with the Cheats button in the game menu.");
            return;
        }
        if (c == null) {
            Ess.err("Unknown command '/" + parts[0] + "'. Type /help for a list.");
            return;
        }
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        try {
            c.run(args);
        } catch (Command.Fail f) {
            Ess.err(f.getMessage());
        } catch (Throwable t) {
            Ess.err("/" + c.name + " failed: " + t);
            t.printStackTrace();
        }
    }

    /**
     * Completion candidates for the last (partial) word of {@code line}, which starts with "/".
     * Returns the full replacement words, already filtered by the typed prefix.
     */
    public static boolean cheats() {
        dev.betaessentials.EssData d = Ess.data();
        return d != null && d.cheats;
    }

    public static List<String> complete(String line) {
        List<String> out = new ArrayList<>();
        if (line == null || !line.startsWith("/")) return out;
        String body = line.substring(1);
        boolean trailingSpace = body.endsWith(" ");
        String[] parts = body.trim().isEmpty() ? new String[0] : body.trim().split("\\s+");
        int index = trailingSpace ? parts.length : parts.length - 1;
        String prefix = trailingSpace || parts.length == 0 ? "" : parts[parts.length - 1];
        if (index <= 0) {
            for (Command c : BY_NAME.values()) if (c.listed() && allowed(c) && c.name.startsWith(prefix.toLowerCase())) out.add(c.name);
            return out;
        }
        Command c = get(parts[0]);
        if (c == null || !c.listed() || !allowed(c)) return out;
        String[] args = new String[index];
        for (int i = 0; i < index; i++) args[i] = i + 1 < parts.length ? parts[i + 1] : "";
        for (String s : c.complete(args, index - 1)) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase()) && !out.contains(s)) out.add(s);
        }
        return out;
    }
}
