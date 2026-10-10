package io.icker.factions.util;

import io.icker.factions.FactionsMod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Every permission node queried via {@link #check}, with its default command level.
     * NeoForge requires nodes to be registered via {@link PermissionGatherEvent.Nodes} before
     * being queried (it throws {@code UnregisteredPermissionException} otherwise), so this
     * list must cover all {@code Requires.hasPerms} call sites in the mod.
     */
    private static final String[][] PERMISSIONS = {
        {"faction.safe", "0"}, // (sic) historical typo kept for config compatibility
        {"factions.claim.access", "0"},
        {"factions.claim.access.decrease", "0"},
        {"factions.claim.access.increase", "0"},
        {"factions.claim.add", "0"},
        {"factions.claim.add.force", "0"},
        {"factions.claim.add.size", "0"},
        {"factions.claim.auto", "0"},
        {"factions.claim.list", "0"},
        {"factions.claim.remove", "0"},
        {"factions.claim.remove.all", "0"},
        {"factions.claim.remove.size", "0"},
        {"factions.create", "0"},
        {"factions.declare.ally", "0"},
        {"factions.declare.enemy", "0"},
        {"factions.declare.neutral", "0"},
        {"factions.disband", "0"},
        {"factions.home", "0"},
        {"factions.home.set", "0"},
        {"factions.info", "0"},
        {"factions.info.other", "0"},
        {"factions.invite.add", "0"},
        {"factions.invite.list", "0"},
        {"factions.invite.remove", "0"},
        {"factions.join", "0"},
        {"factions.kick", "0"},
        {"factions.leave", "0"},
        {"factions.list", "0"},
        {"factions.map", "0"},
        {"factions.members", "0"},
        {"factions.members.other", "0"},
        {"factions.modify.color", "0"},
        {"factions.modify.description", "0"},
        {"factions.modify.gui", "0"},
        {"factions.modify.motd", "0"},
        {"factions.modify.name", "0"},
        {"factions.modify.open", "0"},
        {"factions.permission", "0"},
        {"factions.permission.add", "0"},
        {"factions.permission.add.faction", "0"},
        {"factions.permission.add.guest", "0"},
        {"factions.permission.list", "0"},
        {"factions.permission.list.faction", "0"},
        {"factions.permission.list.guest", "0"},
        {"factions.permission.remove", "0"},
        {"factions.permission.remove.faction", "0"},
        {"factions.permission.remove.guest", "0"},
        {"factions.rank.demote", "0"},
        {"factions.rank.promote", "0"},
        {"factions.rank.transfer", "0"},
        {"factions.settings", "0"},
        {"factions.settings.chat", "0"},
        {"factions.settings.radar", "0"},
        {"factions.settings.sounds", "0"},
        {"factions.admin.gui", null},
        {"factions.admin.bypass", null},
        {"factions.admin.power", null},
        {"factions.admin.spoof", null},
        {"factions.admin.audit", null},
    };

    private Permissions() {}

    /**
     * Registers every permission node with NeoForge. Wired in FactionsMod's constructor;
     * the Nodes event fires when the permission handler initializes (server start).
     */
    public static void gatherNodes(PermissionGatherEvent.Nodes event) {
        List<PermissionNode<?>> nodes = new ArrayList<>(PERMISSIONS.length);
        for (String[] definition : PERMISSIONS) {
            int defaultValue =
                    definition[1] == null
                            ? FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL
                            : Integer.parseInt(definition[1]);
            nodes.add(node(definition[0], defaultValue));
        }
        event.addNodes(nodes);
    }

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
