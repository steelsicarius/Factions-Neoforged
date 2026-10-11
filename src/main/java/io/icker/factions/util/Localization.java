package io.icker.factions.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Minimal replacement for {@code xyz.nucleoid:server-translations-api} (Fabric-only).
 *
 * <p>Clients usually do not have this mod installed (it is a server-side mod), so
 * {@code Component.translatable} keys from our lang files would render as raw keys
 * client-side. STAPI solved this by resolving translations server-side per player
 * language; this shim does the same:
 *
 * <ul>
 *   <li>{@link #raw} resolves a key to a plain string (used by the input GUI defaults
 *       and the placeholder wrappers);</li>
 *   <li>{@link #resolve} rewrites a component tree, replacing the translatable contents
 *       whose keys exist in our lang tables with literal (server-translated) text and
 *       leaving everything else untouched, so vanilla keys still translate on the client.</li>
 * </ul>
 *
 * <p>Falls back from the player's language to {@code en_us}, then keeps the component
 * translatable (vanilla behaviour for unknown keys).
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
        String value = template(language, key);
        return value != null ? value : key;
    }

    /** Resolves a component tree server-side in the player's language. */
    public static Component resolve(Component component, ServerPlayer player) {
        return resolve(component, player.getLanguage());
    }

    /**
     * Replaces translatable contents covered by our lang tables with literal translated
     * text; other components (including vanilla translatables the client can resolve
     * itself) are copied through unchanged.
     */
    public static Component resolve(Component component, String language) {
        if (!(component instanceof MutableComponent mutable)) {
            return component;
        }

        MutableComponent result = resolveContents(mutable, language);
        result.setStyle(resolveStyle(mutable.getStyle(), language));
        for (Component sibling : mutable.getSiblings()) {
            result.append(resolve(sibling, language));
        }
        return result;
    }

    /** Hover contents are components too (e.g. the /f map cell tooltips). */
    private static Style resolveStyle(Style style, String language) {
        HoverEvent hover = style.getHoverEvent();
        if (hover instanceof HoverEvent.ShowText showText) {
            Component resolved = resolve(showText.value(), language);
            if (resolved != showText.value()) {
                return style.withHoverEvent(new HoverEvent.ShowText(resolved));
            }
        }
        return style;
    }

    private static MutableComponent resolveContents(MutableComponent component, String language) {
        if (component.getContents() instanceof TranslatableContents contents) {
            String template = template(language, contents.getKey());
            if (template != null) {
                return format(template, contents.getArgs(), language, component.getStyle());
            }
        }
        // Component#copy() copies the siblings as well; resolve() re-appends every sibling
        // (resolved), so the copy must start empty — otherwise they render twice.
        MutableComponent copy = component.copy();
        copy.getSiblings().clear();
        return copy;
    }

    /**
     * Mirrors vanilla's translatable rendering: literal chunks become text and each
     * {@code %s}/{@code %n$s} placeholder inserts its argument; component arguments are
     * resolved recursively, other arguments are stringified.
     */
    /**
     * Mirrors vanilla's translatable rendering: literal chunks become text and each
     * placeholder inserts its argument; component arguments are resolved recursively,
     * other arguments are stringified. Supports the specifier grammar used by the mod's
     * lang files: {@code %[N$][.P][s|d|f]} plus {@code %%}:
     *
     * <ul>
     *   <li>{@code %s} / {@code %d} consume the next positional argument (stringified);</li>
     *   <li>{@code %1$s}, {@code %2$s}, ... select the argument by (1-based) index;</li>
     *   <li>{@code %f} formats a numeric argument with Java's default precision and
     *       {@code %.Pf} with precision P (e.g. {@code %.2f} for coordinates);</li>
     *   <li>{@code %%} renders a literal percent sign.</li>
     * </ul>
     */
    private static MutableComponent format(
            String template, Object[] args, String language, Style style) {
        MutableComponent result = Component.empty().withStyle(style);
        int positional = 0;
        int index = 0;
        while (index < template.length()) {
            int next = template.indexOf('%', index);
            if (next == -1) {
                result.append(template.substring(index));
                break;
            }
            if (next > index) {
                result.append(template.substring(index, next));
            }
            if (next + 1 >= template.length()) {
                result.append("%");
                break;
            }

            int cursor = next + 1;
            char first = template.charAt(cursor);

            if (first == '%') {
                result.append("%");
                index = cursor + 1;
                continue;
            }

            // Optional positional index: N$
            int argumentIndex = -1;
            if (first >= '1' && first <= '9') {
                int numberEnd = cursor;
                while (numberEnd < template.length()
                        && template.charAt(numberEnd) >= '0'
                        && template.charAt(numberEnd) <= '9') {
                    numberEnd++;
                }
                if (numberEnd < template.length() && template.charAt(numberEnd) == '$') {
                    argumentIndex = Integer.parseInt(template.substring(cursor, numberEnd)) - 1;
                    cursor = numberEnd + 1;
                }
            }

            // Optional precision: .P
            int precision = -1;
            if (cursor < template.length() && template.charAt(cursor) == '.') {
                int precisionEnd = cursor + 1;
                while (precisionEnd < template.length()
                        && template.charAt(precisionEnd) >= '0'
                        && template.charAt(precisionEnd) <= '9') {
                    precisionEnd++;
                }
                precision = Integer.parseInt(template.substring(cursor + 1, precisionEnd));
                cursor = precisionEnd;
            }

            if (cursor >= template.length()) {
                result.append("%");
                index = next + 1;
                continue;
            }
            char conversion = template.charAt(cursor);

            if (conversion == 's' || conversion == 'd' || conversion == 'f') {
                Object arg;
                if (argumentIndex >= 0) {
                    arg = argumentIndex < args.length ? args[argumentIndex] : null;
                } else {
                    arg = positional < args.length ? args[positional] : null;
                    positional++;
                }

                if (conversion == 'f') {
                    // Numeric formatting with precision (e.g. %.2f for coordinates);
                    // non-numeric arguments fall back to their string form.
                    if (arg instanceof Number number) {
                        String pattern = precision < 0 ? "%f" : "%." + precision + "f";
                        result.append(String.format(pattern, number.doubleValue()));
                    } else {
                        result.append(arg == null ? "null" : String.valueOf(arg));
                    }
                } else {
                    result.append(argument(arg, language));
                }
                index = cursor + 1;
            } else {
                // Unknown specifier: keep the percent sign, re-scan after it.
                result.append("%");
                index = next + 1;
            }
        }
        return result;
    }

    private static Component argument(Object argument, String language) {
        if (argument instanceof Component component) {
            return resolve(component, language);
        }
        return Component.literal(argument == null ? "null" : String.valueOf(argument));
    }

    private static String template(String language, String key) {
        String value = table(language).get(key);
        if (value == null) {
            value = table(FALLBACK_LANGUAGE).get(key);
        }
        return value;
    }

    private static Map<String, String> table(String language) {
        synchronized (TABLES) {
            return TABLES.computeIfAbsent(
                    language,
                    code -> {
                        try (InputStream stream =
                                Localization.class.getResourceAsStream(
                                        LANG_PATH.formatted(
                                                io.icker.factions.FactionsMod.MODID, code))) {
                            if (stream == null) {
                                return Map.of();
                            }
                            JsonObject json =
                                    JsonParser.parseReader(
                                                    new InputStreamReader(
                                                            stream, StandardCharsets.UTF_8))
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
