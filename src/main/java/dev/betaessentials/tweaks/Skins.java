package dev.betaessentials.tweaks;

import dev.betaessentials.Ess;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Player Skins: Beta asks Mojang's old Amazon storage for skins and capes by player name, which is gone. These old
 * addresses are turned into the player's current texture address from Mojang's API (name, then profile). Runs in
 * vanilla's image download thread, never on the game thread. Modern 64x64 skins work: Beta's model uses the top
 * 64x32 half, which has the same layout (slim "Alex" arms are 3 pixels wide and show a thin stripe).
 */
public final class Skins {
    private Skins() {}

    public static final dev.betaessentials.settings.Setting PLAYER_SKINS = dev.betaessentials.settings.Settings.toggle("playerSkins",
            dev.betaessentials.settings.Setting.Scope.CLIENT, dev.betaessentials.settings.Settings.CLIENT, "Player Skins", true,
            "Load skins and capes from Mojang (Beta's own skin server is gone)");

    /** Registers the setting (called once at startup, before the settings file is read). */
    public static void register() {}

    private static final String OLD_SKINS = "http://s3.amazonaws.com/MinecraftSkins/", OLD_CAPES = "http://s3.amazonaws.com/MinecraftCloaks/";
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final Pattern TEXTURES = Pattern.compile("\"name\"\\s*:\\s*\"textures\"\\s*,\\s*\"value\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");

    /** Texture JSON (decoded) per player name, so skin and cape take one lookup; "" when the player has none. */
    private static final Map<String, String> profiles = new ConcurrentHashMap<>();

    /** The address to download instead of an old skin or cape address; any other address is returned unchanged. */
    public static String redirect(String url) {
        if (url == null || !PLAYER_SKINS.on()) return url;
        boolean skin = url.startsWith(OLD_SKINS), cape = url.startsWith(OLD_CAPES);
        if (!skin && !cape) return url;
        String name = url.substring((skin ? OLD_SKINS : OLD_CAPES).length()).replace(".png", "");
        String textures = profiles.computeIfAbsent(name, Skins::lookup);
        String found = textureUrl(textures, skin ? "SKIN" : "CAPE");
        return found != null ? found : url; // nothing: let vanilla fail quietly as before
    }

    private static String lookup(String name) {
        try {
            if (!name.matches("[A-Za-z0-9_]{1,16}")) return "";
            String user = get("https://api.mojang.com/users/profiles/minecraft/" + name);
            Matcher id = user == null ? null : ID.matcher(user);
            if (id == null || !id.find()) return "";
            String profile = get("https://sessionserver.mojang.com/session/minecraft/profile/" + id.group(1));
            return profile == null ? "" : texturesOf(profile);
        } catch (Exception e) {
            Ess.LOG("Skin lookup for " + name + " failed: " + e);
            return "";
        }
    }

    /** The decoded "textures" property of a session server profile, or "". */
    public static String texturesOf(String profileJson) {
        Matcher m = TEXTURES.matcher(profileJson);
        return m.find() ? new String(Base64.getDecoder().decode(m.group(1)), StandardCharsets.UTF_8) : "";
    }

    /** The url of SKIN or CAPE in a decoded textures JSON, or null. */
    public static String textureUrl(String textures, String kind) {
        if (textures == null) return null;
        Matcher m = Pattern.compile("\"" + kind + "\"\\s*:\\s*\\{\\s*\"url\"\\s*:\\s*\"([^\"]+)\"").matcher(textures);
        return m.find() ? m.group(1) : null;
    }

    private static String get(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        try {
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in.transferTo(out);
                return out.toString(StandardCharsets.UTF_8);
            }
        } finally {
            c.disconnect();
        }
    }
}
