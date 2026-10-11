package io.icker.factions.util;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.placeholder.PlaceholderManager;

import java.util.function.Function;

/**
 * Optional TAB integration.
 *
 * <p>TAB's NeoForge platform has no eu.pb4 PlaceholderAPI hook (unlike its Fabric and old
 * Forge platforms, which ship a PlaceholderAPIHook), so the placeholders registered by
 * {@link PlaceholdersWrapper} are invisible to it. TAB's public API instead allows
 * registering placeholders directly, which this wrapper does — mirroring the same keys
 * and values as PlaceholdersWrapper (legacy section-coded strings, which TAB converts).
 */
public class TabWrapper {
    private static final int REFRESH_MS = 500;

    public TabWrapper() {
        PlaceholderManager manager = TabAPI.getInstance().getPlaceholderManager();

        manager.registerPlayerPlaceholder(
                "%factions:name%", REFRESH_MS, player -> resolve(player, TabWrapper::name));
        manager.registerPlayerPlaceholder(
                "%factions:colorless_name%", REFRESH_MS, player -> resolve(player, TabWrapper::colorlessName));
        manager.registerPlayerPlaceholder(
                "%factions:chat%", REFRESH_MS, player -> resolve(player, TabWrapper::chat));
        manager.registerPlayerPlaceholder(
                "%factions:rank%", REFRESH_MS, player -> resolve(player, TabWrapper::rank));
        manager.registerPlayerPlaceholder(
                "%factions:color%", REFRESH_MS, player -> resolve(player, TabWrapper::color));
        manager.registerPlayerPlaceholder(
                "%factions:description%", REFRESH_MS, player -> resolve(player, TabWrapper::description));
        manager.registerPlayerPlaceholder(
                "%factions:state%", REFRESH_MS, player -> resolve(player, TabWrapper::state));
        manager.registerPlayerPlaceholder(
                "%factions:power%", REFRESH_MS, player -> resolve(player, TabWrapper::power));
        manager.registerPlayerPlaceholder(
                "%factions:power_formatted%", REFRESH_MS, player -> resolve(player, TabWrapper::power));
        manager.registerPlayerPlaceholder(
                "%factions:max_power%", REFRESH_MS, player -> resolve(player, TabWrapper::maxPower));
        manager.registerPlayerPlaceholder(
                "%factions:player_power%", REFRESH_MS, player -> resolve(player, TabWrapper::playerPower));
        manager.registerPlayerPlaceholder(
                "%factions:required_power%", REFRESH_MS, player -> resolve(player, TabWrapper::requiredPower));
        manager.registerPlayerPlaceholder(
                "%factions:required_power_formatted%", REFRESH_MS, player -> resolve(player, TabWrapper::requiredPower));
    }

    private static String resolve(TabPlayer player, Function<User, String> handler) {
        try {
            return handler.apply(User.get(player.getUniqueId()));
        } catch (Exception e) {
            return "";
        }
    }

    private static String name(User user) {
        Faction faction = user.getFaction();
        return faction == null ? "" : faction.getColor().toString() + faction.getName();
    }

    private static String colorlessName(User user) {
        Faction faction = user.getFaction();
        return faction == null ? "" : faction.getName();
    }

    private static String chat(User user) {
        if (user.chat == User.ChatMode.GLOBAL || !user.isInFaction()) {
            return Localization.raw("factions.papi.chat.global", FactionsMod.CONFIG.LANGUAGE);
        }
        return Localization.raw("factions.papi.chat.faction", FactionsMod.CONFIG.LANGUAGE);
    }

    private static String rank(User user) {
        return user.isInFaction() ? user.getRankName() : "";
    }

    private static String color(User user) {
        return user.isInFaction() ? user.getFaction().getColor().name() : "reset";
    }

    private static String description(User user) {
        Faction faction = user.getFaction();
        return faction == null ? "" : faction.getDescription();
    }

    private static String state(User user) {
        Faction faction = user.getFaction();
        return faction == null ? "" : String.valueOf(faction.isOpen());
    }

    private static String power(User user) {
        Faction faction = user.getFaction();
        return faction == null ? "" : String.valueOf(faction.getPower());
    }

    private static String maxPower(User user) {
        Faction faction = user.getFaction();
        return faction == null ? "" : String.valueOf(faction.calculateMaxPower());
    }

    private static String playerPower(User user) {
        return String.valueOf(FactionsMod.CONFIG.POWER.MEMBER);
    }

    private static String requiredPower(User user) {
        Faction faction = user.getFaction();
        return faction == null
                ? ""
                : String.valueOf(faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT);
    }
}
