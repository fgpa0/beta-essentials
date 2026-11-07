package dev.betaessentials;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.AbstractClientPlayer;
import net.minecraft.entity.player.PlayerBase;
import net.minecraft.level.Level;
import net.minecraft.level.chunk.ChunkCache;

public final class Ess {
    private Ess() {}

    public static Minecraft mc;
    public static void started(Minecraft minecraft) {
        mc = minecraft;
    }

    /** Singleplayer world loaded (in multiplayer everything stays vanilla). */
    public static boolean active() {
        return mc != null && mc.level != null && mc.player != null && !mc.level.isServerSide;
    }

    public static EssData data() {
        if (mc == null || mc.level == null || mc.level.isServerSide || mc.level.getProperties() == null) return null;
        return ((EssProperties) mc.level.getProperties()).ess$data();
    }

    public static boolean isLocalPlayer(Object o) {
        return mc != null && o != null && o == mc.player && active();
    }

    public static GameMode mode() {
        EssData d = data();
        return d == null ? GameMode.SURVIVAL : d.mode;
    }

    public static boolean creative() {
        return active() && mode() == GameMode.CREATIVE;
    }

    public static boolean spectator() {
        return active() && mode() == GameMode.SPECTATOR;
    }

    /** Creative or spectator: no damage, flight allowed, mobs ignore you. */
    public static boolean invulnerable() {
        return active() && mode() != GameMode.SURVIVAL;
    }

    public static void LOG(String s) {
        System.out.println("[Beta Essentials] " + s);
    }

    public static void msg(String s) {
        if (dev.betaessentials.command.Source.current() != null) {
            dev.betaessentials.command.Source.current().message(s);
            return;
        }
        if (mc != null && mc.overlay != null) mc.overlay.addChatMessage(s);
    }

    public static void ok(String s) {
        msg("§7" + s);
    }

    public static void err(String s) {
        msg("§c" + s);
    }

    public static Loc here() {
        PlayerBase p = dev.betaessentials.command.Source.actor();
        return new Loc(p.x, p.boundingBox.minY, p.z, p.yaw, p.pitch, p.dimensionId);
    }

    public static void rememberBack(Loc l) {
        EssData d = dev.betaessentials.command.Source.state();
        if (d != null) d.back = l;
    }

    /** Teleport the player (feet position). Remembers the old position for /back. Loads the destination dimension when necessary. */
    public static boolean teleport(Loc to, boolean rememberBack) {
        if (dev.betaessentials.command.Source.current() != null)
            return dev.betaessentials.command.Source.current().teleport(to, rememberBack);
        AbstractClientPlayer p = mc.player;
        if (to.dim != 0 && to.dim != -1) return false;
        if (rememberBack) rememberBack(here());
        if (to.dim != p.dimensionId) {
            Level destination = new Level(mc.level, net.minecraft.level.dimension.Dimension.getByID(to.dim));
            mc.level.removeEntity(p);
            p.removed = false;
            p.dimensionId = to.dim;
            p.level = destination;
            p.setPositionAndAngles(to.x, to.y, to.z, to.yaw, to.pitch);
            mc.showLevelProgress(destination, "Teleporting", p);
        }
        Level level = mc.level;
        int cx = (int) Math.floor(to.x) >> 4, cz = (int) Math.floor(to.z) >> 4;
        if (level.getCache() instanceof ChunkCache) ((ChunkCache) level.getCache()).method_1242(cx, cz);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) level.getChunkFromCache(cx + dx, cz + dz);
        }
        if (p.vehicle != null) p.startRiding(null);
        p.velocityX = p.velocityY = p.velocityZ = 0;
        ((dev.betaessentials.mixin.EntityAccessor) p).ess$setFallDistance(0);
        p.setPositionAndAngles(to.x, to.y, to.z, to.yaw, to.pitch);
        return true;
    }

    /** Highest standing position at x/z (feet y), or -1 when the column is empty. */
    public static int topY(int x, int z) {
        return topY(dev.betaessentials.command.Source.world(), x, z);
    }

    public static int topY(Level level, int x, int z) {
        for (int y = 127; y >= 0; y--) {
            int id = level.getTileId(x, y, z);
            if (id != 0 && net.minecraft.block.BlockBase.BY_ID[id] != null
                    && net.minecraft.block.BlockBase.BY_ID[id].material.blocksMovement()) return y + 1;
        }
        return -1;
    }
}
