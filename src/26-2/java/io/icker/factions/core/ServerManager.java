package io.icker.factions.core;

import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

public class ServerManager {
    public static void register() {
        // Fabric original: ServerPlayConnectionEvents.JOIN
        NeoForge.EVENT_BUS.addListener(ServerManager::playerJoin);
        MiscEvents.ON_SAVE.register(ServerManager::save);
    }

    private static void save(MinecraftServer server) {
        // Defensive: the persistents load lazily on first touch; before WorldUtils has
        // the server reference their (inventory) deserializer cannot resolve registries.
        if (!WorldUtils.isReady()) {
            return;
        }
        Claim.save();
        Faction.save();
        User.save();
    }

    private static void playerJoin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        User user = User.get(player.getUUID());

        if (user.isInFaction()) {
            Faction faction = user.getFaction();
            new Message(
                            Component.translatable(
                                    "factions.events.member_returns", player.getName().getString()))
                    .send(player, false);
            new Message(faction.getMOTD()).prependFaction(faction).send(player, false);
        }
    }
}
