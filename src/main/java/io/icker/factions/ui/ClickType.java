package io.icker.factions.ui;

import net.minecraft.world.inventory.ContainerInput;

/**
 * Minimal replacement for sgui's {@code eu.pb4.sgui.api.ClickType}, covering only
 * the variants the original mod uses.
 *
 * <p>Mapped from vanilla's 26.1 input model: {@code ContainerInput#PICKUP} with {@code buttonNum} 0/1 is
 * left/right click, {@code ContainerInput#QUICK_MOVE} is shift-click, and
 * {@code ContainerInput#THROW} is the drop key.
 */
public enum ClickType {
    MOUSE_LEFT,
    MOUSE_RIGHT,
    DROP;

    public static ClickType fromVanilla(ContainerInput input, int buttonNum) {
        return switch (input) {
            case QUICK_MOVE, PICKUP -> buttonNum == 1 ? MOUSE_RIGHT : MOUSE_LEFT;
            case THROW -> DROP;
            default -> MOUSE_LEFT;
        };
    }
}
