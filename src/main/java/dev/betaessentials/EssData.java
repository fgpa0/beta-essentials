package dev.betaessentials;

import dev.betaessentials.settings.Setting;
import dev.betaessentials.settings.Settings;
import net.minecraft.util.io.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;

/** Everything Beta Essentials stores per world, kept in level.dat under "BetaEssentials". */
public final class EssData {
    public static final String TAG = "BetaEssentials", OLD_TAG = "Essentials";

    public final Map<String, Integer> rules = new LinkedHashMap<>();
    public GameMode mode = GameMode.SURVIVAL;
    public boolean flying;
    public int flySpeed = 1;
    public final Map<String, Loc> homes = new LinkedHashMap<>();
    public Loc back;
    /** Allow Cheats: commands only work when this is on. */
    public boolean cheats;

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        CompoundTag r = new CompoundTag();
        for (Map.Entry<String, Integer> e : rules.entrySet()) {
            Setting rule = Settings.ALL.get(e.getKey());
            if (rule != null && !rule.isSwitch) r.put(e.getKey(), (int) e.getValue());
            else r.put(e.getKey(), e.getValue() != 0);
        }
        t.put("rules", r);
        t.put("mode", mode.id);
        t.put("flying", flying);
        t.put("flySpeed", flySpeed);
        StringBuilder names = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Loc> e : homes.entrySet()) {
            if (i > 0) names.append('\n');
            names.append(e.getKey());
            t.put("home" + i, e.getValue().toTag());
            i++;
        }
        t.put("homeNames", names.toString());
        if (back != null) t.put("back", back.toTag());
        t.put("cheats", cheats);
        return t;
    }

    public static EssData fromTag(CompoundTag t) {
        EssData d = new EssData();
        if (t == null) return d;
        CompoundTag r = t.getCompoundTag("rules");
        for (Setting rule : Settings.of(Setting.Scope.WORLD)) {
            if (r.containsKey(rule.key)) d.rules.put(rule.key, rule.isSwitch ? (r.getBoolean(rule.key) ? 1 : 0) : r.getInt(rule.key));
        }
        GameMode m = GameMode.parse(t.getString("mode"));
        if (m != null) d.mode = m;
        d.flying = t.getBoolean("flying");
        if (t.containsKey("flySpeed")) d.flySpeed = Math.max(1, Math.min(10, t.getInt("flySpeed")));
        String names = t.getString("homeNames");
        if (names != null && !names.isEmpty()) {
            String[] n = names.split("\n");
            for (int i = 0; i < n.length; i++) {
                if (t.containsKey("home" + i)) d.homes.put(n[i], Loc.fromTag(t.getCompoundTag("home" + i)));
            }
        }
        if (t.containsKey("back")) d.back = Loc.fromTag(t.getCompoundTag("back"));
        // worlds from before the Cheats setting were played with commands: keep them working
        d.cheats = !t.containsKey("cheats") || t.getBoolean("cheats");
        return d;
    }
}
