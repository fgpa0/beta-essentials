package dev.betaessentials;

import net.minecraft.entity.Boat;
import net.minecraft.entity.EntityBase;
import net.minecraft.entity.Minecart;
import net.minecraft.entity.Painting;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemBase;
import net.minecraft.item.ItemInstance;
import net.minecraft.level.Level;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitType;

/** Middle click. Creative: select the block's item if it is already on the hotbar, else put one in the current slot. */
public final class PickBlock {
    private PickBlock() {}

    public static void pick(Level level, int x, int y, int z) {
        int[] it = itemFor(level.getTileId(x, y, z), level.getTileMeta(x, y, z));
        if (it == null) return;
        int item = it[0], dmg = it[1];
        PlayerInventory inv = Ess.mc.player.inventory;
        for (int i = 0; i < 9; i++) {
            ItemInstance s = inv.main[i];
            if (s != null && s.itemId == item && s.getDamage() == dmg) {
                inv.selectedHotbarSlot = i;
                return;
            }
        }
        inv.main[inv.selectedHotbarSlot] = new ItemInstance(item, 1, dmg);
    }

    /** The item id and damage value a block is picked as, or null if it has none. */
    public static int[] itemFor(int id, int meta) {
        if (id <= 0) return null;
        int item = id, dmg = 0;
        switch (id) {
            case 2: case 60: item = 3; break;          // grass, farmland -> dirt
            case 7: item = 7; break;
            case 9: case 8: case 10: case 11: case 51: case 90: case 34: case 36: return null; // fluids, fire, portal, piston parts
            case 6: case 17: case 18: dmg = meta & 3; break;
            case 31: case 35: case 44: dmg = meta; break;
            case 43: item = 44; dmg = meta; break;    // double slab -> slab
            case 26: item = 355; break;               // bed
            case 55: item = 331; break;               // redstone wire
            case 59: item = 295; break;               // crops -> seeds
            case 62: item = 61; break;                // lit furnace
            case 63: case 68: item = 323; break;      // sign
            case 64: item = 324; break;               // wooden door
            case 71: item = 330; break;               // iron door
            case 74: item = 73; break;                // lit redstone ore
            case 75: item = 76; break;                // redstone torch (off)
            case 83: item = 338; break;               // sugar cane
            case 92: item = 354; break;               // cake
            case 93: case 94: item = 356; break;      // repeater
            default: break;
        }
        if (item >= ItemBase.byId.length || ItemBase.byId[item] == null) return null;
        return new int[]{item, dmg};
    }
}
