package com.craftingdead.immerse.world.inventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class BaseUtilityMenu extends ChestMenu {
  public BaseUtilityMenu(int id, Inventory inventory, Container container) {
    super(MenuType.GENERIC_9x1, id, inventory, container, 1);
    for (int index = 0; index < 9; index++) {
      final int slotIndex = index;
      Slot slot = new Slot(container, index, 8 + index * 18, 18) {
        @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(slotIndex, stack); }
      };
      slot.index = index;
      slots.set(index, slot);
    }
  }
}
