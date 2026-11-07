package dev.betaessentials.command;

import dev.betaessentials.*;
import net.minecraft.entity.player.PlayerBase;
import net.minecraft.level.Level;

/** The actor of a command; world permissions always come from the host. */
public interface Source {
    PlayerBase player();
    Level level();
    EssData data();
    void message(String text);
    boolean teleport(Loc to, boolean remember);
    void mode(GameMode mode);
    void kill();

    static Source current() { return Commands.source; }
    static PlayerBase actor() { return current() == null ? Ess.mc.player : current().player(); }
    static Level world() { return current() == null ? Ess.mc.level : current().level(); }
    static EssData state() { return current() == null ? Ess.data() : current().data(); }
    static void setMode(GameMode mode) {
        if (current() == null) PlayerState.setMode(mode);
        else current().mode(mode);
    }
    static void killActor() {
        if (current() == null) PlayerState.kill();
        else current().kill();
    }
}
