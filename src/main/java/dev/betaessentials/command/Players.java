package dev.betaessentials.command;

import dev.betaessentials.Ess;
import net.minecraft.entity.player.PlayerBase;

import java.util.ArrayList;
import java.util.List;

/** Players in the game by name, for commands that take a player (single player: only your own). */
final class Players {
    private Players() {}

    /** A player in the game by name (case-insensitive), else null. */
    static PlayerBase player(String name) {
        PlayerBase p = Ess.mc == null ? null : Ess.mc.player;
        return p != null && p.name.equalsIgnoreCase(name) ? p : null;
    }

    static List<String> online() {
        List<String> out = new ArrayList<>();
        if (Ess.mc != null && Ess.mc.player != null) out.add(Ess.mc.player.name);
        return out;
    }
}
