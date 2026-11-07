package dev.betaessentials;

import dev.betaessentials.settings.Setting;
import dev.betaessentials.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Per-world game rules: the world settings of {@link Settings}, stored in level.dat. */
public final class GameRules {
    private GameRules() {}

    /** Groups in the Game Rules tab (declared first: the rules below use them while the class initialises). */
    private static final String PLAYER = "Player", WORLD = "World", MOBS = "Mobs";

    public static final String KEEP_INVENTORY = rule("keepInventory", PLAYER, "Keep Inventory", false, "Keep your items and armour when you die");
    public static final String MOB_GRIEFING = rule("mobGriefing", MOBS, "Mob Griefing", true, "Creepers and ghast fireballs destroy blocks");
    public static final String DO_FIRE_TICK = rule("doFireTick", WORLD, "Fire Spread", true, "Fire spreads and burns out");
    public static final String ENABLE_NIGHTMARES = rule("enableNightmares", MOBS, "Nightmares", true, "Monsters can spawn by your bed and wake you up");
    public static final String ALLOW_SPRINTING = rule("allowSprinting", PLAYER, "Sprinting", false, "Double-tap forward or hold Ctrl to sprint");
    public static final String TREE_CAPACITOR = rule("treeCapacitor", PLAYER, "Tree Felling", false, "Axes fell whole trees (sneak to mine one log)");
    public static final String DO_DAYLIGHT_CYCLE = rule("doDaylightCycle", WORLD, "Daylight Cycle", true, "Time of day advances");
    public static final String DO_WEATHER_CYCLE = rule("doWeatherCycle", WORLD, "Weather Cycle", true, "Weather changes on its own");
    public static final String DO_MOB_SPAWNING = rule("doMobSpawning", MOBS, "Mob Spawning", true, "Animals and monsters spawn naturally");
    public static final String FALL_DAMAGE = rule("fallDamage", PLAYER, "Fall Damage", true, "Falling hurts the player");
    public static final String BREAK_DELAY = rule("breakDelay", PLAYER, "Break Delay", true, "Vanilla 5 tick pause between breaking blocks (off = none)");

    private static String rule(String key, String group, String title, boolean def, String desc) {
        Settings.toggle(key, Setting.Scope.WORLD, Settings.GAME_RULES, title, def, desc).group(group);
        return key;
    }

    /** Rules chosen on the Create World screen, applied to the next new world. */
    public static Map<String, Integer> pending;
    /** Allow Cheats chosen on the Create World screen. */
    public static boolean pendingCheats;
    /** Game mode chosen on the Create World screen. */
    public static GameMode pendingMode = GameMode.SURVIVAL;

    public static int value(String name) {
        Setting s = Settings.ALL.get(name);
        return s == null ? 0 : s.value();
    }

    public static boolean get(String name) {
        return value(name) != 0;
    }

    /** A world setting by name, ignoring case, or null. */
    public static Setting find(String name) {
        Setting s = Settings.find(name);
        return s != null && s.scope == Setting.Scope.WORLD ? s : null;
    }

    public static List<String> names() {
        List<String> l = new ArrayList<>();
        for (Setting s : Settings.of(Setting.Scope.WORLD)) l.add(s.key);
        Collections.sort(l, String.CASE_INSENSITIVE_ORDER);
        return l;
    }
}
