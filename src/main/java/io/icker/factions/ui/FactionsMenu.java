package io.icker.factions.ui;

import io.icker.factions.util.Localization;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * Minimal replacement for sgui's {@code eu.pb4.sgui.api.gui.SimpleGui}.
 *
 * <p>The stateful GUI object implements {@link MenuProvider}; the actual
 * {@link AbstractContainerMenu} is created lazily on {@code open()} (vanilla's
 * {@code AbstractContainerMenu#containerId} is final and must be assigned by
 * {@code ServerPlayer#openMenu}). Slots are backed by a dummy container and
 * synchronized by index, so the vanilla chest/hopper menu on the client side
 * renders them as-is.
 *
 * <p>All interaction is locked (the original always passed
 * {@code manipulateInventory = false}): clicking an element only runs its
 * callback, and nothing can ever be picked up, moved or dropped, matching
 * sgui's behaviour for this mod.
 */
public class FactionsMenu implements MenuProvider {
    /** Implemented by the inner menus so other GUI classes can mark the currently open one. */
    protected interface MarkableClose {
        void markProgrammaticClose();
    }

    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 18;

    public final ServerPlayer player;
    private final MenuType<?> type;
    private final int rows;
    private final GuiElement[] elements;
    @Nullable
    private Menu activeMenu;
    private Component title = Component.empty();

    protected FactionsMenu(MenuType<?> type, ServerPlayer player, int slots) {
        this.type = type;
        this.player = player;
        this.elements = new GuiElement[slots];
        this.rows = (slots + 8) / 9;
    }

    @Override
    public Component getDisplayName() {
        // Resolved server-side: the client does not have this mod's lang files.
        return Localization.resolve(this.title, this.player);
    }

    public void setTitle(Component title) {
        this.title = title;
    }

    public void setSlot(int index, GuiElement element) {
        this.elements[index] = element;
        if (this.activeMenu != null) {
            this.activeMenu.syncElement(index, element);
        }
    }

    /** Appends to the first free slot, exactly like sgui's addSlot. */
    public int addSlot(GuiElement element) {
        for (int index = 0; index < this.elements.length; index++) {
            if (this.elements[index] == null) {
                this.setSlot(index, element);
                return index;
            }
        }
        return -1;
    }

    @Nullable
    public GuiElement getGuiElement(int index) {
        return index >= 0 && index < this.elements.length ? this.elements[index] : null;
    }

    /** Re-sends every element stack to the open menu. */
    protected void updateDisplay() {
        if (this.activeMenu != null) {
            this.activeMenu.syncAll();
        }
    }

    public void refreshOpen() {
        this.updateDisplay();
        this.open();
    }

    public boolean open() {
        this.markCurrentlyOpenMenu();
        this.player.openMenu(this);
        return true;
    }

    /** Closes without firing the close callback. */
    public void close() {
        this.close(false);
    }

    /**
     * Mirrors sgui: {@code close(true)} additionally fires {@link #onManualClose()} after
     * closing (used by the back/close navigation buttons).
     */
    public void close(boolean runCloseCallback) {
        if (this.activeMenu != null) {
            this.activeMenu.markProgrammaticClose();
        }
        this.player.closeContainer();
        if (runCloseCallback) {
            this.onManualClose();
        }
    }

    /** sgui hook: called when the player closed the screen themselves. */
    public void onManualClose() {}

    /**
     * Marks the currently open menu (if it is one of ours) so that opening a new
     * menu does not look like a manual close to it. Vanilla's
     * {@code ServerPlayer#openMenu} closes the previous menu first.
     */
    private void markCurrentlyOpenMenu() {
        if (this.player.containerMenu instanceof MarkableClose menu) {
            menu.markProgrammaticClose();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        this.activeMenu = new Menu(containerId, inventory);
        return this.activeMenu;
    }

    private class Menu extends AbstractContainerMenu implements MarkableClose {
        private final SimpleContainer backing;
        private boolean programmaticClose = false;

        private Menu(int containerId, Inventory inventory) {
            super(FactionsMenu.this.type, containerId);
            this.backing = new SimpleContainer(FactionsMenu.this.elements.length);

            for (int y = 0; y < FactionsMenu.this.rows; y++) {
                for (int x = 0; x < 9; x++) {
                    this.addSlot(
                            new Slot(this.backing, x + y * 9, GRID_LEFT + x * 18, GRID_TOP + y * 18));
                }
            }
            this.addStandardInventorySlots(
                    inventory, GRID_LEFT, GRID_TOP + FactionsMenu.this.rows * 18 + 13);

            this.syncAll();
        }

        private void syncAll() {
            for (int index = 0; index < FactionsMenu.this.elements.length; index++) {
                this.syncElement(index, FactionsMenu.this.elements[index]);
            }
        }

        private void syncElement(int index, @Nullable GuiElement element) {
            this.getSlot(index).set(element == null ? ItemStack.EMPTY : element.getItemStack());
            this.broadcastChanges();
        }

        @Override
        public void markProgrammaticClose() {
            this.programmaticClose = true;
        }

        @Override
        public boolean stillValid(Player player) {
            return player.isAlive() && !player.isRemoved();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            if (!this.programmaticClose) {
                FactionsMenu.this.onManualClose();
            }
        }

        @Override
        public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
            if (slotIndex >= 0 && slotIndex < FactionsMenu.this.elements.length) {
                @Nullable GuiElement element = FactionsMenu.this.elements[slotIndex];
                if (element != null) {
                    element.runCallback(slotIndex, ClickType.fromVanilla(input, buttonNum));
                }
            }
        }
    }
}
