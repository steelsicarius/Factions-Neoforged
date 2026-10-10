package io.icker.factions.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Minimal replacement for sgui's {@code GuiElementBuilder} + {@code GuiElement}: builder and built
 * element merged into a single mutable class, so the mod only ever builds elements once
 * and before opening.
 */
public class GuiElement {
    /** Replacement for sgui's quad-arg callback; the unused parameters are dropped in the rewrite. */
    public interface ClickCallback {
        void onClick(int index, ClickType type);
    }

    private final ItemStack stack;
    private ClickCallback callback;

    public GuiElement(Item item) {
        this(new ItemStack(item));
    }

    public GuiElement(ItemStack stack) {
        this.stack = stack;
    }

    public static GuiElement from(ItemStack stack) {
        return new GuiElement(stack.copy());
    }

    public ItemStack getItemStack() {
        return this.stack;
    }

    public GuiElement setName(Component name) {
        this.stack.set(DataComponents.CUSTOM_NAME, name);
        return this;
    }

    public GuiElement setLore(List<Component> lines) {
        this.stack.set(DataComponents.LORE, new ItemLore(List.copyOf(lines)));
        return this;
    }

    public GuiElement addLore(Component line) {
        ItemLore lore = this.stack.get(DataComponents.LORE);
        List<Component> lines = lore == null ? new ArrayList<>() : new ArrayList<>(lore.lines());
        lines.add(line);
        return this.setLore(lines);
    }

    /** Hides the entire tooltip. */
    public GuiElement hideTooltip() {
        this.stack.set(
                DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
        return this;
    }

    /** Hides the vanilla item-type name line, but keeps custom name and lore. */
    public GuiElement hideDefaultTooltip() {
        this.stack.set(
                DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay.DEFAULT.withHidden(DataComponents.ITEM_NAME, true));
        return this;
    }

    /**
     * Applies a base64-encoded profile texture payload (the format shipped in {@code io.icker.factions.util.Icons})
     * to a player head item.
     */
    public GuiElement setProfileSkinTexture(String base64Payload) {
        // authlib 7.x (MC 26.x): GameProfile exposes its property map via the record-style
        // properties() accessor.
        GameProfile profile =
                new GameProfile(
                        UUID.nameUUIDFromBytes(base64Payload.getBytes(StandardCharsets.UTF_8)),
                        "factions_gui");
        profile.properties().put("textures", new Property("textures", base64Payload));
        return this.setProfile(profile);
    }

    /** Uses the real profile of the given player (the client resolves the skin from it). */
    public GuiElement setProfile(GameProfile profile) {
        this.stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        return this;
    }

    public GuiElement setCallback(ClickCallback callback) {
        this.callback = callback;
        return this;
    }

    public GuiElement setCallback(Runnable callback) {
        this.callback = (index, type) -> callback.run();
        return this;
    }

    void runCallback(int index, ClickType type) {
        if (this.callback != null) {
            this.callback.onClick(index, type);
        }
    }
}
