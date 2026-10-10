package io.icker.factions.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;

/**
 * Minimal replacement for {@code xyz.nucleoid:server-translations-api} (Fabric-only).
 *
 * <p>The mod only uses {@code Localization.raw(key, ...)} without format arguments, so this shim
 * resolves the key against the language tables shipped in {@code assets/factions/lang/} on the
 * server side and returns a literal component — the same observable behaviour as STAPI's
 * {@code raw} for these calls. Falls back to {@code en_us}, then to the raw key.
 */
public final class Localization {
    private static final String LANG_PATH = "/assets/%s/lang/%s.json";
    private static final String FALLBACK_LANGUAGE = "en_us";
    private static final Map<String, Map<String, String>> TABLES = new HashMap<>();

    private Localization() {}

    /** Translates {@code key} in the language last reported by the player's client. */
    public static String raw(String key, ServerPlayer player) {
        return raw(key, player.getLanguage());
    }

    /** Translates {@code key} in the given language (e.g. {@code FactionsMod.CONFIG.LANGUAGE}). */
    public static String raw(String key, String language) {
        String value = table(language).get(key);
        if (value == null) {
            value = table(FALLBACK_LANGUAGE).get(key);
        }
        return value != null ? value : key;
    }

    private static Map<String, String> table(String language) {
        synchronized (TABLES) {
            return TABLES.computeIfAbsent(
                    language,
                    code -> {
                        try (InputStream stream =
                                Localization.class.getResourceAsStream(
                                        LANG_PATH.formatted(io.icker.factions.FactionsMod.MODID, code))) {
                            if (stream == null) {
                                return Map.of();
                            }
                            JsonObject json =
                                    JsonParser.parseReader(
                                                    new InputStreamReader(stream, StandardCharsets.UTF_8))
                                            .getAsJsonObject();
                            Map<String, String> table = new HashMap<>();
                            for (var entry : json.entrySet()) {
                                table.put(entry.getKey(), entry.getValue().getAsString());
                            }
                            return table;
                        } catch (Exception e) {
                            io.icker.factions.FactionsMod.LOGGER.warn(
                                    "Failed to load language table ({})", code, e);
                            return Map.of();
                        }
                    });
        }
    }
}
