package dev.betaessentials.command;

/**
 * How other mods add chat commands, without Beta Essentials knowing about them. Declare an implementation as the
 * "beta-essentials:commands" entrypoint in fabric.mod.json; Beta Essentials calls it once, when its command table is built,
 * and the implementation calls {@link Commands#register(Command)} for each command. Commands run with
 * {@link Source#current()} set (null for the host); report results with {@code Ess.msg/ok/err}, throw
 * {@link Command.Fail} for usage errors.
 */
public interface CommandProvider {
    void registerCommands();
}
