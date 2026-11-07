package dev.betaessentials;

import net.minecraft.block.BlockBase;
import net.minecraft.entity.player.PlayerBase;
import net.minecraft.item.ItemInstance;
import net.minecraft.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** treeCapacitor: an axe fells the whole tree, then the orphaned leaves decay quickly. */
public final class TreeFeller {
    private TreeFeller() {}

    private static final int LOG = 17, LEAVES = 18, MAX_LOGS = 256;
    /** Leaves waiting to decay, per level (the player can switch dimensions while leaves are still decaying). */
    private static final java.util.Map<Level, List<long[]>> queues = new java.util.IdentityHashMap<>();

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** Called after the player broke a log at x/y/z (already air). */
    public static void fell(Level level, PlayerBase player, int x0, int y0, int z0, int meta, boolean creative) {
        List<int[]> logs = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{x0, y0, z0});
        seen.add(key(x0, y0, z0));
        int leaves = 0;
        while (!q.isEmpty() && logs.size() < MAX_LOGS) {
            int[] c = q.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = c[0] + dx, y = c[1] + dy, z = c[2] + dz;
                        if (y < 0 || y > 127 || !seen.add(key(x, y, z))) continue;
                        int id = level.getTileId(x, y, z);
                        if (id == LEAVES) leaves++;
                        else if (id == LOG && (level.getTileMeta(x, y, z) & 3) == (meta & 3)) {
                            int[] p = {x, y, z};
                            logs.add(p);
                            q.add(p);
                        }
                    }
                }
            }
        }
        // only real trees: a log cabin has no leaves touching its logs
        if (logs.isEmpty() || leaves < 4) return;
        logs.sort((a, b) -> Integer.compare(a[1], b[1]));
        int minX = x0, maxX = x0, minY = y0, maxY = y0, minZ = z0, maxZ = z0;
        BlockBase log = BlockBase.BY_ID[LOG];
        int n = 0;
        for (int[] p : logs) {
            ItemInstance held = player.getHeldItem();
            if (!creative && (held == null || !(held.getType() instanceof net.minecraft.item.tool.Hatchet))) break;
            int m = level.getTileMeta(p[0], p[1], p[2]);
            if (n++ % 4 == 0) level.playLevelEvent(2001, p[0], p[1], p[2], LOG + m * 256);
            if (!level.setTile(p[0], p[1], p[2], 0)) continue;
            if (!creative) {
                log.afterBreak(level, player, p[0], p[1], p[2], m);
                held.postMine(LOG, p[0], p[1], p[2], player);
                if (held.count <= 0) {
                    held.unusedEmptyMethod1(player);
                    player.breakHeldItem();
                }
            }
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
            minZ = Math.min(minZ, p[2]);
            maxZ = Math.max(maxZ, p[2]);
        }
        queueLeaves(level, minX - 5, minY, minZ - 5, maxX + 5, Math.min(127, maxY + 5), maxZ + 5);
        decay(level, Integer.MAX_VALUE); // all at once
    }

    private static void queueLeaves(Level level, int x0, int y0, int z0, int x1, int y1, int z1) {
        List<long[]> found = new ArrayList<>();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (level.getTileId(x, y, z) == LEAVES) found.add(new long[]{x, y, z});
                }
            }
        }
        Collections.shuffle(found, level.rand);
        queues.computeIfAbsent(level, k -> new ArrayList<>()).addAll(found);
    }

    /** Once per tick: decay a batch of queued leaves that no longer have a log nearby. */
    public static void tick(Level level) {
        List<long[]> queue = queues.get(level);
        if (queue == null) return;
        if (queue.isEmpty()) {
            queues.remove(level);
            return;
        }
        decay(level, Math.max(6, queue.size() / 25));
    }

    private static void decay(Level level, int batch) {
        BlockBase leaves = BlockBase.BY_ID[LEAVES];
        List<long[]> leafQueue = queues.get(level);
        if (leafQueue == null) return;
        for (int i = 0; i < batch && !leafQueue.isEmpty(); i++) {
            long[] p = leafQueue.remove(leafQueue.size() - 1);
            int x = (int) p[0], y = (int) p[1], z = (int) p[2];
            if (level.getTileId(x, y, z) != LEAVES || hasLogNearby(level, x, y, z)) continue;
            int meta = level.getTileMeta(x, y, z);
            leaves.drop(level, x, y, z, meta);
            level.setTile(x, y, z, 0);
            if (level.rand.nextInt(4) == 0) level.playLevelEvent(2001, x, y, z, LEAVES + meta * 256);
        }
    }

    private static boolean hasLogNearby(Level level, int x, int y, int z) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (level.getTileId(x + dx, y + dy, z + dz) == LOG) return true;
                }
            }
        }
        return false;
    }
}
