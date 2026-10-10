package io.icker.factions.util;

import io.icker.factions.FactionsMod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Replacement for {@code me.lucko:fabric-permissions-api}, using NeoForge's {@link PermissionAPI}.
 *
 * <p>Node names are preserved exactly as the original Fabric mod used them (e.g.
 * {@code factions.claim.add}, including the historical {@code faction.safe} typo in
 * SafeCommand), so existing LuckPerms configurations keep working.
 *
 * <p>Semantics match the original {@code Permissions.check(source, permission, defaultValue)}:
 * player sources are checked against the permission node (falling back to the command-level
 * {@code defaultValue} when no permission handler provides a value), non-player sources fall
 * back to the vanilla command-level check.
 */
public final class Permissions {
    private static final Map<String, PermissionNode<Boolean>> NODES = new HashMap<>();

    private Permissions() {}

    public static boolean check(CommandSourceStack source, String permission, int defaultValue) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            return PermissionAPI.getPermission(player, node(permission, defaultValue));
        }

        return source.permissions()
                .hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(defaultValue)));
    }

    private static PermissionNode<Boolean> node(String permission, int defaultValue) {
        String cacheKey = permission + "\u0000" + defaultValue;
        synchronized (NODES) {
            return NODES.computeIfAbsent(cacheKey, key -> createNode(permission, defaultValue));
        }
    }

    private static PermissionNode<Boolean> createNode(String permission, int defaultValue) {
        // PermissionNode prefixes the node name with "<modID>." so, to reproduce the exact
        // original node string, the mod id and node name are split off the permission string.
        String modID;
        String nodeName;
        if (permission.startsWith(FactionsMod.MODID + ".")) {
            modID = FactionsMod.MODID;
            nodeName = permission.substring(FactionsMod.MODID.length() + 1);
        } else {
            int separator = permission.indexOf('.');
            modID = permission.substring(0, separator);
            nodeName = permission.substring(separator + 1);
        }

        return new PermissionNode<>(
                modID,
                nodeName,
                PermissionTypes.BOOLEAN,
                (player, uuid, contexts) ->
                        player != null
                                && player.permissions()
                                        .hasPermission(
                                                new Permission.HasCommandLevel(
                                                        PermissionLevel.byId(defaultValue))));
    }
}
