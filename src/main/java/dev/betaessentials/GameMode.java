package dev.betaessentials;

public enum GameMode {
    SURVIVAL("survival", "s", "0"),
    CREATIVE("creative", "c", "1"),
    SPECTATOR("spectator", "sp", "3");

    public final String id;
    private final String[] aliases;

    GameMode(String id, String... aliases) {
        this.id = id;
        this.aliases = aliases;
    }

    public static GameMode parse(String s) {
        for (GameMode m : values()) {
            if (m.id.equalsIgnoreCase(s)) return m;
            for (String a : m.aliases) if (a.equalsIgnoreCase(s)) return m;
        }
        return null;
    }

    public String display() {
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }
}
