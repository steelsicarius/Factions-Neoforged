package io.icker.factions.util;

import com.flowpowered.math.vector.Vector2i;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.util.WorldUtils;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.TeamColor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Read-only integration of Factions claims with Dynmap.
 * <p>
 * Listens to faction claim events and renders claims as coloured area markers on
 * the Dynmap web interface. Only claim polygons and faction home markers are
 * displayed; no write operations are performed on Factions data.
 */
public class DynmapWrapper extends DynmapCommonAPIListener {
    private MarkerAPI markerApi;
    private MarkerIcon claimIcon;

    public DynmapWrapper() {
        register(this);

        ClaimEvents.ADD.register((Claim claim) -> generateMarkers());
        ClaimEvents.REMOVE.register(
                (x, z, level, faction) -> generateMarkers());

        FactionEvents.SET_HOME.register(this::setHome);
        FactionEvents.MODIFY.register(faction -> generateMarkers());
        FactionEvents.MEMBER_JOIN.register((faction, user) -> generateMarkers());
        FactionEvents.MEMBER_LEAVE.register((faction, user) -> generateMarkers());
        FactionEvents.POWER_CHANGE.register((faction, oldPower) -> generateMarkers());
        FactionEvents.DISBAND.register((faction) -> generateMarkers());

        // Defer initial marker generation until the server is fully started and
        // worlds are loaded. Calling generateMarkers() from apiEnabled() fires too
        // early (during onServerAboutToStart) when WorldUtils.getWorld() returns null,
        // causing a crash in Faction's static initializer.
        NeoForge.EVENT_BUS.addListener(
                (ServerStartedEvent event) -> {
                    FactionsMod.LOGGER.info("[Dynmap] ServerStartedEvent fired, markerApi=" + markerApi);
                    if (markerApi != null) {
                        generateMarkers();
                    } else {
                        FactionsMod.LOGGER.warn("[Dynmap] ServerStartedEvent fired but markerApi is null - factions markers will not be generated");
                    }
                });
    }

    @Override
    public void apiEnabled(DynmapCommonAPI api) {
        markerApi = api.getMarkerAPI();
        if (markerApi == null) {
            FactionsMod.LOGGER.warn("Dynmap MarkerAPI not available - factions markers disabled");
            return;
        }

        claimIcon = markerApi.getMarkerIcon(MarkerIcon.DEFAULT);
        if (claimIcon == null) {
            claimIcon = markerApi.createMarkerIcon("factions-default", "Default", null);
        }
        // Do NOT call generateMarkers() unconditionally here — it fires too early during
        // server startup (onServerAboutToStart) when WorldUtils.getWorld() returns null,
        // causing a crash in Faction's static initializer. It is deferred to the
        // ServerStartedEvent listener above. If the server is already started (rare edge
        // case where apiEnabled fires after ServerStartedEvent), generate immediately.
        if (WorldUtils.hasWorlds()) {
            FactionsMod.LOGGER.info("[Dynmap] Worlds ready in apiEnabled, generating markers");
            generateMarkers();
        } else {
            FactionsMod.LOGGER.info("[Dynmap] apiEnabled called but worlds not ready yet, deferring to ServerStartedEvent");
        }
    }

    @Override
    public void apiDisabled(DynmapCommonAPI api) {
        markerApi = null;
        claimIcon = null;
        clearMarkers();
    }

    private void generateMarkers() {
        if (markerApi == null) {
            FactionsMod.LOGGER.warn("[Dynmap] generateMarkers called but markerApi is null");
            return;
        }

        FactionsMod.LOGGER.info("[Dynmap] generateMarkers() called");

        // Obtener o crear el set de marcadores "factions" (mantenerlo persistente para que dynmap
        // escriba el archivo JSON de marcadores y los clientes puedan consultarlo en cualquier momento)
        final MarkerSet factionSet;
        MarkerSet existingSet = markerApi.getMarkerSet("factions");
        if (existingSet == null) {
            factionSet = markerApi.createMarkerSet("factions", "Factions Claims", null, true);
        } else {
            factionSet = existingSet;
        }
        factionSet.setHideByDefault(false);
        factionSet.setLabelShow(true);

        // Eliminar todos los marcadores de área existentes del set (manteniendo el set
        // para evitar churn que impide a dynmap escribir el JSON de marcadores).
        // Se recrearán abajo con los datos actuales de las facciones.
        for (AreaMarker existing : new HashSet<>(factionSet.getAreaMarkers())) {
            existing.deleteMarker();
        }

        AtomicInteger factionCount = new AtomicInteger(0);
        AtomicInteger markerCount = new AtomicInteger(0);

        for (Faction faction : Faction.all()) {
            factionCount.incrementAndGet();
            String info = getInfo(faction);

            ClaimGrouper.separateClaimsByLevel(faction).forEach((level, claims) -> {
                String dynmapWorld = getDynmapWorldName(level);
                if (dynmapWorld == null) {
                    FactionsMod.LOGGER.warn("[Dynmap] Skipping claims in unknown world: " + level);
                    return;
                }
                FactionsMod.LOGGER.info("[Dynmap] Faction {} claims in world '{}': {} claims", faction.getName(), dynmapWorld, claims.size());

                ClaimGrouper.convertClaimsToLineSegmentGroups(claims).forEach(group -> {
                    List<List<Vector2i>> outlines =
                            ClaimGrouper.convertLineSegmentsToOutlines(group);

                    for (List<Vector2i> outline : outlines) {
                        if (outline.isEmpty()) {
                            continue;
                        }

                        double[] xs = new double[outline.size()];
                        double[] zs = new double[outline.size()];
                        for (int i = 0; i < outline.size(); i++) {
                            xs[i] = outline.get(i).getX();
                            zs[i] = outline.get(i).getY();
                        }

                        // Dynmap's MarkerAPI expects 24-bit RGB colors (0xRRGGBB) with the
                        // opacity conveyed via the setLineStyle/setFillStyle parameters.
                        // Passing 32-bit ARGB values (e.g. 0xFFFFAA00 from ChatFormatting
                        // through java.awt.Color#getRGB) leaks the alpha byte into the
                        // "#%06X" hex produced for the marker JSON ("#FFFFAA00"), which
                        // browsers parse as #RRGGBBAA => alpha 0x00 => fully transparent
                        // (invisible) claim areas on the web map.
                        int rgb = getColorRgb(faction) & 0xFFFFFF;

                        FactionsMod.LOGGER.info("[Dynmap] Creating area marker for faction {} in world '{}' with {} points (color #{}): xs={} zs={}",
                                faction.getName(), dynmapWorld, outline.size(),
                                String.format("%06X", rgb),
                                java.util.Arrays.toString(xs), java.util.Arrays.toString(zs));

                        AreaMarker area = factionSet.createAreaMarker(
                                UUID.randomUUID().toString(),
                                faction.getName(),
                                false, // markup (false = regular marker visible on HD maps)
                                dynmapWorld,
                                xs, zs,
                                true); // persistent

                        area.setLineStyle(2, 1.0, rgb); // solid outline (opacity 1.0)
                        area.setFillStyle(0.25, rgb);   // ~25% opacity fill
                        area.setDescription(info);
                        area.setLabel(faction.getName(), true);
                        markerCount.incrementAndGet();
                    }
                });
            });

            // Home marker
            Home home = faction.getHome();
            if (home != null) {
                FactionsMod.LOGGER.info("[Dynmap] Home marker for faction {} at world={} x={} z={}",
                        faction.getName(), home.level, home.x, home.z);
                setHome(faction, home);
            }
        }

        FactionsMod.LOGGER.info("[Dynmap] generateMarkers() complete: " + factionCount.get() + " factions, " + markerCount.get() + " area markers");
    }

    /**
     * Converts a factions claim level (dimension ID, e.g. "minecraft:overworld") to the
     * corresponding dynmap world name (e.g. "Mundo nuevo", "DIM-1", "DIM1").
     * Uses the same naming logic as {@code NeoForgeWorld.getWorldName()}.
     *
     * @param level the factions claim level/dimension ID
     * @return the dynmap world name, or null if the level cannot be mapped to a loaded world
     */
    private static String getDynmapWorldName(String level) {
        ServerLevel world = WorldUtils.getWorld(level);
        if (world == null) {
            FactionsMod.LOGGER.warn("[Dynmap] getDynmapWorldName: level='{}' not found in loaded worlds", level);
            return null;
        }

        ResourceKey<Level> dim = world.dimension();
        String result;
        if (dim == Level.OVERWORLD) {
            result = WorldUtils.server.getWorldData().getLevelName();
        } else if (dim == Level.END) {
            result = "DIM1";
        } else if (dim == Level.NETHER) {
            result = "DIM-1";
        } else {
            result = dim.identifier().getNamespace() + "_" + dim.identifier().getPath();
        }
        FactionsMod.LOGGER.info("[Dynmap] getDynmapWorldName: level='{}' -> dim='{}' -> dynmapWorld='{}'", level, dim, result);
        return result;
    }

    private void clearMarkers() {
        if (markerApi == null) {
            return;
        }
        MarkerSet set = markerApi.getMarkerSet("factions");
        if (set != null) {
            // Remove all area markers by iterating over a copy
            for (AreaMarker am : new HashSet<>(set.getAreaMarkers())) {
                am.deleteMarker();
            }
            // Remove all regular markers (e.g. homes)
            Set<org.dynmap.markers.Marker> allMarkers = new HashSet<>();
            allMarkers.addAll(set.getMarkers());
            for (org.dynmap.markers.Marker m : allMarkers) {
                m.deleteMarker();
            }
            // Keep the MarkerSet itself so dynmap can persist _markers_ JSON.
            // Do NOT call set.deleteMarkerSet() — that would wipe the set and
            // prevent dynmap from writing the marker JSON file clients read.
        }
    }

    private void setHome(Faction faction, Home home) {
        if (markerApi == null || home == null) {
            return;
        }

        MarkerSet set = markerApi.getMarkerSet("factions");
        if (set == null) {
            return;
        }

        String dynmapWorld = getDynmapWorldName(home.level);
        FactionsMod.LOGGER.info("[Dynmap] Home world mapping: level='{}' -> dynmapWorld='{}'", home.level, dynmapWorld);
        if (dynmapWorld == null) {
            FactionsMod.LOGGER.warn("[Dynmap] Skipping home marker for unknown world: " + home.level);
            return;
        }

        String homeId = faction.getID().toString() + "-home";
        org.dynmap.markers.Marker existing = set.findMarker(homeId);
        if (existing != null) {
            existing.setLocation(dynmapWorld, home.x, 64.0, home.z);
            existing.setLabel(faction.getName() + "'s Home", true);
            existing.setDescription(getInfo(faction));
            return;
        }

        set.createMarker(
                homeId,
                faction.getName() + "'s Home",
                false, // markup (false = regular marker visible on HD maps)
                dynmapWorld,
                home.x, 64.0, home.z,
                claimIcon,
                true); // persistent
    }

    private static int getColorRgb(Faction faction) {
        TeamColor teamColor = TeamColor.byName(faction.getColor().name().toLowerCase());
        Integer rgb = teamColor == null ? null : teamColor.rgb();
        // TeamColor colors are 24-bit RGB (0xRRGGBB); mask anyway to guarantee
        // no alpha byte ever reaches the Dynmap marker API.
        return rgb == null ? 0xFFFFFF : (rgb & 0xFFFFFF); // default white
    }

    private String getInfo(Faction faction) {
        return "Name: " + faction.getName()
                + "<br>"
                + "Description: " + faction.getDescription()
                + "<br>"
                + "Power: " + faction.getPower()
                + "<br>"
                + "Members: " + faction.getUsers().size();
    }
}
