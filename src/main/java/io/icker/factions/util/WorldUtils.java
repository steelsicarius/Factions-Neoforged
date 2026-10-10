package io.icker.factions.util;

import io.icker.factions.api.event.Event;
import io.icker.factions.api.event.EventFactory;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class WorldUtils {
    public static MinecraftServer server;

    public static final Event<Ready> ON_READY =
            EventFactory.createArrayBacked(
                    Ready.class,
                    callbacks ->
                            () -> {
                                for (Ready callback : callbacks) {
                                    callback.onReady();
                                }
                            });

    public static void register() {
        // Fabric original: ServerLifecycleEvents.SERVER_STARTING. On NeoForge the closest
        // event is ServerAboutToStartEvent: vanilla DedicatedServer#initServer calls
        // saveEverything -> saveAllChunks (which triggers Faction's lazy DB load through
        // our saveAllChunks mixin) AFTER handleServerAboutToStart and loadLevel, so the
        // server field must be set in AboutToStart, before any of that. ServerStartingEvent
        // fires too late (after initServer), which crashed the server on the first boot
        // with existing faction data.
        NeoForge.EVENT_BUS.addListener(WorldUtils::serverAboutToStart);
    }

    private static void serverAboutToStart(ServerAboutToStartEvent event) {
        WorldUtils.server = event.getServer();
        ON_READY.invoker().onReady();
    }

    public static boolean isReady() {
        return server != null;
    }

    public static boolean hasWorlds() {
        return !WorldUtils.server.levelKeys().isEmpty();
    }

    public static boolean isValid(String level) {
        return WorldUtils.server.levelKeys().stream()
                .anyMatch(key -> Objects.equals(key.identifier(), Identifier.parse(level)));
    }

    @Nullable
    public static ServerLevel getWorld(String level) {
        Optional<ResourceKey<Level>> key =
                WorldUtils.server.levelKeys().stream()
                        .filter(
                                testKey ->
                                        Objects.equals(
                                                testKey.identifier(), Identifier.parse(level)))
                        .findAny();

        if (key.isEmpty()) {
            return null;
        } else {
            return server.getLevel(key.get());
        }
    }

    @FunctionalInterface
    public interface Ready {
        void onReady();
    }
}
