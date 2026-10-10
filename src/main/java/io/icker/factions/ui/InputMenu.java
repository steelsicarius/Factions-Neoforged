package io.icker.factions.ui;

import net.minecraft.core.component.DataComponents;
import io.icker.factions.util.Localization;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

/**
 * Minimal replacement for sgui's {@code eu.pb4.sgui.api.gui.AnvilInputGui}: uses the vanilla anvil
 * screen client-side as a free text input, with the ingredient/result slots repurposed as
 * return/confirm buttons (slots 1 and 2, same indices sgui used).
 *
 * <p>The vanilla rename field is pre-filled from the input item's custom name
 * (AnvilScreen#slotChanged), so {@link #setDefaultInputValue} keeps its behaviour.
 */
public class InputMenu implements MenuProvider {
    public final ServerPlayer player;
    public GuiElement returnBtn;
    public GuiElement confirmBtn;

    private Component title = Component.empty();
    @Nullable
    private String defaultValue = null;
    private String inputText = "";
    @Nullable
    private Menu activeMenu;

    public InputMenu(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public Component getDisplayName() {
        // Resolved server-side: the client does not have this mod's lang files.
        return Localization.resolve(this.title, this.player);
    }

    public void setTitle(Component title) {
        this.title = title;
    }

    public void setDefaultInputValue(String value) {
        this.defaultValue = value;
    }

    public String getInput() {
        return this.inputText;
    }

    /** Mirrors sgui's getGuiElement: index 1 is the return button, index 2 the confirm button. */
    @Nullable
    public GuiElement getGuiElement(int index) {
        return switch (index) {
            case AnvilMenu.ADDITIONAL_SLOT -> this.returnBtn;
            case AnvilMenu.RESULT_SLOT -> this.confirmBtn;
            default -> null;
        };
    }

    public boolean open() {
        if (this.player.containerMenu instanceof FactionsMenu.MarkableClose menu) {
            menu.markProgrammaticClose();
        }
        this.player.openMenu(this);
        return true;
    }

    public void close() {
        if (this.activeMenu != null) {
            this.activeMenu.markProgrammaticClose();
        }
        this.player.closeContainer();
    }

    /** sgui hook: called when the player closed the screen themselves. */
    public void onManualClose() {}

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        this.activeMenu = new Menu(containerId, inventory);
        return this.activeMenu;
    }

    private class Menu extends AnvilMenu {
        private boolean programmaticClose = false;

        private Menu(int containerId, Inventory inventory) {
            super(containerId, inventory, ContainerLevelAccess.NULL);
            this.setCost(0);

            ItemStack inputItem = new ItemStack(Items.PAPER);
            if (InputMenu.this.defaultValue != null) {
                inputItem.set(DataComponents.CUSTOM_NAME, Component.literal(InputMenu.this.defaultValue));
            }
            this.getSlot(INPUT_SLOT).set(inputItem);
            this.getSlot(ADDITIONAL_SLOT).set(resolveButton(InputMenu.this.returnBtn));
            this.getSlot(RESULT_SLOT).set(resolveButton(InputMenu.this.confirmBtn));
        }

        private void markProgrammaticClose() {
            this.programmaticClose = true;
        }

        /**
         * Resolves the button's name server-side, in place (NOT a copy: InputGui's
         * showErrorMessage mutates this same stack later to flash error messages).
         */
        private ItemStack resolveButton(GuiElement button) {
            if (button == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = button.getItemStack();
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            if (name != null) {
                stack.set(
                        DataComponents.CUSTOM_NAME,
                        Localization.resolve(name, InputMenu.this.player.getLanguage()));
            }
            return stack;
        }

        @Override
        public boolean stillValid(Player player) {
            // Vanilla's is tied to ContainerLevelAccess (NULL is invalid); the menu stays open
            // while the player is connected, like sgui's AnvilInputGui.
            return player.isAlive() && !player.isRemoved();
        }

        @Override
        protected void createResultInternal() {
            // Keep the custom return/confirm buttons in the result slot instead of the renamed
            // item and skip all repair/cost logic.
        }

        @Override
        public boolean setItemName(String name) {
            InputMenu.this.inputText = name;
            return super.setItemName(name);
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            if (!this.programmaticClose) {
                InputMenu.this.onManualClose();
            }
        }

        @Override
        public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
            GuiElement element = InputMenu.this.getGuiElement(slotIndex);
            if (element != null) {
                element.runCallback(slotIndex, ClickType.fromVanilla(input, buttonNum));
            }
        }
    }
}
