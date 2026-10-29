package io.icker.factions.util;

import com.flowpowered.math.vector.Vector2i;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import net.minecraft.ChatFormatting;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

        generateMarkers();
    }

    @Override
    public void apiDisabled(DynmapCommonAPI api) {
        markerApi = null;
        claimIcon = null;
        clearMarkers();
    }

    private void generateMarkers() {
        if (markerApi == null) {
            return;
        }

        clearMarkers();

        MarkerSet set = markerApi.getMarkerSet("factions");
        if (set == null) {
            set = markerApi.createMarkerSet("factions", "Factions Claims", null, true);
        }
        final MarkerSet factionSet = set;
        factionSet.setHideByDefault(false);
        factionSet.setLabelShow(true);

        for (Faction faction : Faction.all()) {
            String info = getInfo(faction);

            ClaimGrouper.separateClaimsByLevel(faction).forEach((level, claims) -> {
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

                        Color factionColor = new Color(getColorRgb(faction));
                        int fillRGBA = factionColor.getRGB() | 0x40000000; // ~25% opacity fill
                        int lineRGBA = factionColor.getRGB() | 0xFF000000; // solid outline

                        AreaMarker area = factionSet.createAreaMarker(
                                UUID.randomUUID().toString(),
                                faction.getName(),
                                true, // markup
                                level,
                                xs, zs,
                                true); // persistent

                        area.setLineStyle(2, 1.0, lineRGBA);
                        area.setFillStyle(0.25, fillRGBA);
                        area.setDescription(info);
                        area.setLabel(faction.getName(), true);
                    }
                });
            });

            // Home marker
            Home home = faction.getHome();
            if (home != null) {
                setHome(faction, home);
            }
        }
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
            set.deleteMarkerSet();
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

        String homeId = faction.getID().toString() + "-home";
        org.dynmap.markers.Marker existing = set.findMarker(homeId);
        if (existing != null) {
            existing.setLocation(home.level, home.x, 64.0, home.z);
            existing.setLabel(faction.getName() + "'s Home", true);
            existing.setDescription(getInfo(faction));
            return;
        }

        set.createMarker(
                homeId,
                faction.getName() + "'s Home",
                true, // markup
                home.level,
                home.x, 64.0, home.z,
                claimIcon,
                true); // persistent
    }

    private static int getColorRgb(Faction faction) {
        ChatFormatting formatting = ChatFormatting.getByName(faction.getColor().name().toLowerCase());
        Integer rgb = formatting == null ? null : formatting.getColor();
        return rgb == null ? 0x00FFFFFF : rgb; // default white with alpha
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
