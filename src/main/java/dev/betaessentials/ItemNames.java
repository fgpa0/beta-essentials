package dev.betaessentials;

import net.minecraft.block.BlockBase;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.item.ItemBase;
import net.minecraft.item.ItemInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Every obtainable block and item variant, with a command-friendly name ("diamond_pickaxe", "wool_red"...). */
public final class ItemNames {
    private ItemNames() {}

    public static final class Entry {
        public final int id, meta;
        public final String name, display;

        Entry(int id, int meta, String name, String display) {
            this.id = id;
            this.meta = meta;
            this.name = name;
            this.display = display;
        }

        public ItemInstance stack(int count) {
            return new ItemInstance(id, count, meta);
        }

        public int maxStack() {
            return ItemBase.byId[id].getMaxStackSize();
        }
    }

    /** Technical blocks with no sensible item form. */
    private static final int[] HIDDEN = {8, 10, 26, 34, 36, 51, 55, 59, 62, 63, 64, 68, 71, 74, 75, 83, 90, 92, 93, 94};

    private static List<Entry> entries;
    private static Map<String, Entry> byName;

    private static int variants(int id) {
        switch (id) {
            case 6: case 17: case 18: case 31: return 3;
            case 35: case 351: return 16;
            case 43: case 44: return 4;
            case 263: return 2;
            default: return 1;
        }
    }

    public static synchronized List<Entry> all() {
        if (entries != null) return entries;
        List<Entry> list = new ArrayList<>();
        Map<String, Entry> names = new LinkedHashMap<>();
        Set<Integer> hidden = new HashSet<>();
        for (int h : HIDDEN) hidden.add(h);
        TranslationStorage ts = TranslationStorage.getInstance();
        for (int id = 1; id < ItemBase.byId.length; id++) {
            ItemBase item = ItemBase.byId[id];
            if (item == null) continue;
            if (id < 256 && (BlockBase.BY_ID[id] == null || hidden.contains(id))) continue;
            int n = variants(id);
            for (int meta = 0; meta < n; meta++) {
                ItemInstance st = new ItemInstance(id, 1, meta);
                String display;
                try {
                    String key = st.getTranslationKey();
                    display = ts.translate(key + ".name");
                    if (display == null || display.isEmpty() || display.endsWith(".name")) display = key;
                } catch (Throwable t) {
                    display = "item" + id;
                }
                String name = display.toLowerCase().replace(' ', '_').replaceAll("[^a-z0-9_]", "");
                if (name.startsWith("tile.") || name.startsWith("item.")) name = name.substring(5);
                if (name.isEmpty()) name = "item" + id;
                if (names.containsKey(name)) name = name + "_" + meta;
                if (names.containsKey(name)) name = name + "_" + id;
                Entry e = new Entry(id, meta, name, display);
                list.add(e);
                names.put(name, e);
            }
        }
        entries = list;
        byName = names;
        return list;
    }

    /** By name, "id" or "id:meta". */
    public static Entry find(String s) {
        all();
        Entry e = byName.get(s.toLowerCase());
        if (e != null) return e;
        try {
            int colon = s.indexOf(':');
            int id = Integer.parseInt(colon < 0 ? s : s.substring(0, colon));
            int meta = colon < 0 ? 0 : Integer.parseInt(s.substring(colon + 1));
            if (id > 0 && id < ItemBase.byId.length && ItemBase.byId[id] != null) {
                for (Entry x : entries) if (x.id == id && x.meta == meta) return x;
                return new Entry(id, meta, s, s);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public static List<String> names() {
        all();
        return new ArrayList<>(byName.keySet());
    }
}
