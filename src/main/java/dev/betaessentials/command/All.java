package dev.betaessentials.command;

import dev.betaessentials.Ess;
import dev.betaessentials.EssData;
import dev.betaessentials.GameMode;
import dev.betaessentials.GameRules;
import dev.betaessentials.ItemNames;
import dev.betaessentials.Loc;
import dev.betaessentials.settings.Setting;
import net.minecraft.entity.player.PlayerBase;
import net.minecraft.item.ItemInstance;
import net.minecraft.level.Level;
import net.minecraft.level.LevelProperties;
import net.minecraft.util.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** All built-in commands. */
final class All {
    private All() {}

    static void register() {
        Commands.register(new Command("help", "/help [command]", "List commands or show how to use one", "?") {
            public boolean needsCheats() {
                return false; // lists what can be used now
            }

            public void run(String[] a) {
                if (a.length > 0) {
                    Command c = Commands.get(a[0].replace("/", ""));
                    if (c == null) throw new Fail("Unknown command '" + a[0] + "'");
                    Ess.msg("§e" + c.usage);
                    Ess.ok(c.desc + (c.aliases.length > 0 ? " (also /" + String.join(", /", c.aliases) + ")" : ""));
                    return;
                }
                Ess.msg("§eCommands (TAB completes, /help <command> for details):");
                StringBuilder line = new StringBuilder();
                for (Command c : Commands.BY_NAME.values()) {
                    if (!c.listed() || !Commands.allowed(c)) continue;
                    String s = "/" + c.name;
                    if (line.length() + s.length() + 2 > 52) {
                        Ess.ok(line.toString());
                        line.setLength(0);
                    }
                    if (line.length() > 0) line.append("  ");
                    line.append(s);
                }
                if (line.length() > 0) Ess.ok(line.toString());
            }

            public List<String> complete(String[] a, int i) {
                if (i != 0) return super.complete(a, i);
                List<String> names = new ArrayList<>();
                for (Command c : Commands.BY_NAME.values()) if (c.listed() && Commands.allowed(c)) names.add(c.name);
                return names;
            }
        });

        Commands.register(new Command("time", "/time <set|add|query> <value>", "Set, advance or show the time of day. Set accepts day, noon, sunset, night, midnight, sunrise or ticks") {
            public void run(String[] a) {
                Level level = Ess.mc.level;
                if (a.length == 1 && a[0].equalsIgnoreCase("query")) {
                    long t = level.getLevelTime();
                    Ess.ok("Time of day: " + (t % 24000) + " (day " + (t / 24000 + 1) + ")");
                    return;
                }
                if (a.length != 2) throw usage();
                long now = level.getLevelTime();
                if (a[0].equalsIgnoreCase("set")) {
                    long v = named(a[1]);
                    long base = now - now % 24000;
                    long t = base + v;
                    if (t < now) t += 24000; // never go back in time: crops, furnaces etc. keep working
                    level.setLevelTime(t);
                    Ess.ok("Set the time to " + v);
                } else if (a[0].equalsIgnoreCase("add")) {
                    long v = parseInt(a[1], 0, 1_000_000);
                    level.setLevelTime(now + v);
                    Ess.ok("Added " + v + " ticks");
                } else throw usage();
            }

            private long named(String s) {
                switch (s.toLowerCase()) {
                    case "day": return 1000;
                    case "noon": return 6000;
                    case "sunset": return 12000;
                    case "night": return 13000;
                    case "midnight": return 18000;
                    case "sunrise": return 23000;
                    default: return parseInt(s, 0, 23999);
                }
            }

            public List<String> complete(String[] a, int i) {
                if (i == 0) return list("set", "add", "query");
                if (i == 1 && a[0].equalsIgnoreCase("set")) return list("day", "noon", "sunset", "night", "midnight", "sunrise");
                if (i == 1 && a[0].equalsIgnoreCase("add")) return list("1000", "6000", "12000");
                return super.complete(a, i);
            }
        });

        Commands.register(new Command("weather", "/weather <clear|rain|thunder> [seconds]", "Change the weather; /weather set <type> works too") {
            public void run(String[] a) {
                if (a.length > 0 && a[0].equalsIgnoreCase("set")) {
                    String[] b = new String[a.length - 1];
                    System.arraycopy(a, 1, b, 0, b.length);
                    a = b;
                }
                if (a.length < 1 || a.length > 2) throw usage();
                LevelProperties p = Ess.mc.level.getProperties();
                int ticks = a.length == 2 ? parseInt(a[1], 1, 1_000_000) * 20 : (300 + Ess.mc.level.rand.nextInt(600)) * 20;
                switch (a[0].toLowerCase()) {
                    case "clear":
                        p.setRaining(false);
                        p.setThundering(false);
                        break;
                    case "rain":
                        p.setRaining(true);
                        p.setThundering(false);
                        break;
                    case "thunder":
                        p.setRaining(true);
                        p.setThundering(true);
                        break;
                    default:
                        throw usage();
                }
                p.setRainTime(ticks);
                p.setThunderTime(ticks);
                Ess.ok("Weather set to " + a[0].toLowerCase() + " for " + ticks / 20 + " seconds");
            }

            public List<String> complete(String[] a, int i) {
                if (i == 0) return list("clear", "rain", "thunder", "set");
                if (i == 1 && a[0].equalsIgnoreCase("set")) return list("clear", "rain", "thunder");
                return super.complete(a, i);
            }
        });

        Commands.register(new Command("sethome", "/sethome [name]", "Save your current position as a home (default name: home)") {
            public void run(String[] a) {
                EssData d = Source.state();
                String name = a.length > 0 ? a[0] : "home";
                boolean existed = d.homes.containsKey(name);
                d.homes.put(name, Ess.here());
                Ess.ok((existed ? "Moved home '" : "Set home '") + name + "' at " + Ess.here().describe());
            }
        });

        Commands.register(new Command("delhome", "/delhome <name>", "Delete a home") {
            public void run(String[] a) {
                if (a.length != 1) throw usage();
                EssData d = Source.state();
                if (d.homes.remove(a[0]) == null) throw new Fail("No home called '" + a[0] + "'");
                Ess.ok("Deleted home '" + a[0] + "'");
            }

            public List<String> complete(String[] a, int i) {
                return i == 0 ? homeNames() : super.complete(a, i);
            }
        });

        Commands.register(new Command("renamehome", "/renamehome <old> <new>", "Rename a home") {
            public void run(String[] a) {
                if (a.length != 2) throw usage();
                EssData d = Source.state();
                if (!d.homes.containsKey(a[0])) throw new Fail("No home called '" + a[0] + "'");
                if (d.homes.containsKey(a[1])) throw new Fail("There is already a home called '" + a[1] + "'");
                // keep the order of the homes
                List<Map.Entry<String, Loc>> es = new ArrayList<>(d.homes.entrySet());
                d.homes.clear();
                for (Map.Entry<String, Loc> e : es) d.homes.put(e.getKey().equals(a[0]) ? a[1] : e.getKey(), e.getValue());
                Ess.ok("Renamed home '" + a[0] + "' to '" + a[1] + "'");
            }

            public List<String> complete(String[] a, int i) {
                return i == 0 ? homeNames() : super.complete(a, i);
            }
        });

        Commands.register(new Command("home", "/home [name]", "Teleport to a home; lists them when you have several", "homes") {
            public void run(String[] a) {
                EssData d = Source.state();
                if (d.homes.isEmpty()) throw new Fail("You have no homes yet. Use /sethome [name].");
                Loc l;
                if (a.length == 0) {
                    if (d.homes.size() > 1) {
                        Ess.msg("§eHomes:");
                        for (Map.Entry<String, Loc> e : d.homes.entrySet()) Ess.ok(" " + e.getKey() + " - " + e.getValue().describe());
                        Ess.ok("Use /home <name>");
                        return;
                    }
                    l = d.homes.values().iterator().next();
                } else {
                    l = d.homes.get(a[0]);
                    if (l == null) throw new Fail("No home called '" + a[0] + "'");
                }
                if (Ess.teleport(l, true)) Ess.ok("Teleported home");
            }

            public List<String> complete(String[] a, int i) {
                return i == 0 ? homeNames() : super.complete(a, i);
            }
        });

        Commands.register(new Command("back", "/back", "Return to where you were before your last teleport or death") {
            public void run(String[] a) {
                EssData d = Source.state();
                if (d.back == null) throw new Fail("Nowhere to go back to yet");
                Loc to = d.back;
                if (Ess.teleport(to, true)) Ess.ok("Returned to " + to.describe());
            }
        });

        Commands.register(new Command("gamemode", "/gamemode <survival|creative|spectator>", "Change your game mode", "gm") {
            public void run(String[] a) {
                if (a.length != 1) throw usage();
                GameMode m = GameMode.parse(a[0]);
                if (m == null) throw usage();
                Source.setMode(m);
                Ess.ok("Game mode set to " + m.display());
            }

            public List<String> complete(String[] a, int i) {
                return i == 0 ? list("survival", "creative", "spectator") : super.complete(a, i);
            }
        });

        Commands.register(new Command("heal", "/heal", "Restore full health, put out fire and refill air", "feed") {
            public void run(String[] a) {
                PlayerBase p = Source.actor();
                p.health = 20;
                p.fire = 0;
                p.air = 300;
                Ess.ok("Healed");
            }
        });

        Commands.register(new Command("flyspeed", "/flyspeed <1-10>", "Set your flying speed (1 is normal)") {
            public void run(String[] a) {
                if (a.length != 1) throw usage();
                Source.state().flySpeed = parseInt(a[0], 1, 10);
                Ess.ok("Fly speed set to " + Source.state().flySpeed);
            }

            public List<String> complete(String[] a, int i) {
                return i == 0 ? list("1", "2", "3", "4", "5", "6", "7", "8", "9", "10") : super.complete(a, i);
            }
        });

        Commands.register(new Command("gamerule", "/gamerule [rule] [value]", "Show or change this world's game rules") {
            public void run(String[] a) {
                if (a.length == 0) {
                    Ess.msg("\u00a7eGame rules:");
                    for (String n : GameRules.names()) Ess.ok(" " + n + " = " + GameRules.find(n).format(GameRules.value(n)));
                    return;
                }
                Setting r = GameRules.find(a[0]);
                if (r == null) throw new Fail("Unknown game rule '" + a[0] + "'");
                if (a.length == 1) {
                    Ess.ok(r.key + " = " + r.format(GameRules.value(r.key)) + " \u00a78(" + r.desc + ")");
                    return;
                }
                Integer v = r.parse(a[1]);
                if (v == null) throw new Fail(r.expected());
                Ess.data().rules.put(r.key, v);
                Ess.ok("Game rule " + r.key + " is now " + r.format(v));
            }

            public List<String> complete(String[] a, int i) {
                if (i == 0) return GameRules.names();
                if (i == 1) {
                    Setting r = GameRules.find(a[0]);
                    if (r == null || r.isSwitch) return list("true", "false");
                    if (r.isNumber()) return list(Integer.toString(r.def));
                    List<String> l = new ArrayList<>();
                    for (int v = r.min; v <= r.max; v++) l.add(r.format(v));
                    return l;
                }
                return super.complete(a, i);
            }
        });

        Commands.register(new Command("give", "/give <player> <item> [amount]", "Give a player an item by name or id (TAB completes names)", "i") {
            public void run(String[] a) {
                if (a.length < 2 || a.length > 3) throw usage();
                PlayerBase p = Players.player(a[0]);
                if (p == null) throw new Fail("No player called " + a[0] + " is in the game");
                ItemNames.Entry e = ItemNames.find(a[1]);
                if (e == null) throw new Fail("Unknown item '" + a[1] + "'");
                int count = a.length == 3 ? parseInt(a[2], 1, 64 * 36) : 1;
                int left = count;
                while (left > 0) {
                    int n = Math.min(left, e.maxStack());
                    ItemInstance st = e.stack(n);
                    if (!p.inventory.addStack(st) || st.count > 0) {
                        if (st.count > 0) p.dropItem(st, false);
                    }
                    left -= n;
                }
                PlayerBase me = Source.actor();
                if (me != null && me.name.equalsIgnoreCase(p.name)) {
                    Ess.ok("Gave " + count + " x " + e.display);
                } else {
                    Ess.ok("Gave " + count + " x " + e.display + " to " + p.name);
                }
            }

            public List<String> complete(String[] a, int i) {
                if (i == 0) return Players.online();
                return i == 1 ? ItemNames.names() : i == 2 ? list("1", "16", "32", "64") : super.complete(a, i);
            }
        });

        Commands.register(new Command("tp", "/tp <x> <y> <z>", "Teleport to coordinates (~ for relative)", "teleport") {
            public void run(String[] a) {
                if (a.length != 3) throw usage();
                Loc h = Ess.here();
                double x = parseCoord(a[0], h.x, true), y = parseCoord(a[1], h.y, false), z = parseCoord(a[2], h.z, true);
                if (y < -64 || y > 512) throw new Fail("y must be between -64 and 512");
                if (Ess.teleport(new Loc(x, y, z, h.yaw, h.pitch, h.dim), true)) Ess.ok("Teleported to " + (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z));
            }

            public List<String> complete(String[] a, int i) {
                return i < 3 ? list("~") : super.complete(a, i);
            }
        });

        Commands.register(new Command("spawn", "/spawn", "Teleport to the world spawn") {
            public void run(String[] a) {
                Level overworld = Source.world().dimension.id == 0 ? Source.world()
                        : new Level(Source.world(), net.minecraft.level.dimension.Dimension.getByID(0));
                Vec3i s = overworld.getSpawnPosition();
                int y = Ess.topY(overworld, s.x, s.z);
                if (y < 0) y = s.y;
                Loc h = Ess.here();
                if (Ess.teleport(new Loc(s.x + 0.5, y, s.z + 0.5, h.yaw, h.pitch, 0), true)) Ess.ok("Teleported to spawn");
            }
        });

        Commands.register(new Command("top", "/top", "Teleport to the highest block above you") {
            public void run(String[] a) {
                Loc h = Ess.here();
                int y = Ess.topY((int) Math.floor(h.x), (int) Math.floor(h.z));
                if (y < 0) throw new Fail("There are no blocks here");
                if (Ess.teleport(new Loc(Math.floor(h.x) + 0.5, y, Math.floor(h.z) + 0.5, h.yaw, h.pitch, h.dim), true)) Ess.ok("Teleported to the top");
            }
        });

        Commands.register(new Command("seed", "/seed", "Show the world seed") {
            public boolean needsCheats() {
                return false;
            }

            public void run(String[] a) {
                Ess.ok("Seed: §f" + Source.world().getProperties().getSeed());
            }
        });

        Commands.register(new Command("kill", "/kill", "Kill yourself") {
            public void run(String[] a) {
                Source.killActor();
            }
        });
    }

    private static List<String> homeNames() {
        EssData d = Source.state();
        return d == null ? new ArrayList<>() : new ArrayList<>(d.homes.keySet());
    }
}
