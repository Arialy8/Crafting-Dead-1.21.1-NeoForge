package com.craftingdead.survival.world.inventory;

import com.craftingdead.survival.world.item.GeneratorAreaSelectorItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class GeneratorSelectorMenu extends AbstractContainerMenu {
  private final Inventory inventory;
  private final int[] values = new int[6];

  public GeneratorSelectorMenu(int id, Inventory inventory) {
    super(SurvivalMenuTypes.GENERATOR_SELECTOR.get(), id);
    this.inventory = inventory;
    for (int i = 0; i < this.values.length; i++) {
      this.addDataSlot(DataSlot.shared(this.values, i));
    }
    this.refreshData();
  }

  public int getValue(int index) {
    return this.values[index];
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }

  private ItemStack selector() {
    return this.inventory.player.getMainHandItem();
  }

  private void refreshData() {
    ItemStack stack = this.selector();
    this.setData(0, GeneratorAreaSelectorItem.getCount(stack));
    for (int i = 0; i < 5; i++) {
      this.setData(i + 1, GeneratorAreaSelectorItem.getWeight(stack, i));
    }
  }

  @Override
  public boolean clickMenuButton(Player player, int id) {
    ItemStack stack = this.selector();
    if (!(stack.getItem() instanceof GeneratorAreaSelectorItem)) {
      return false;
    }
    if (id >= 1000) {
      int encoded = id - 1000;
      int field = encoded / 501;
      int value = encoded % 501;
      if (field == 0) {
        GeneratorAreaSelectorItem.setCount(stack, value);
      } else if (field <= GeneratorAreaSelectorItem.TYPE_COUNT
          && !GeneratorAreaSelectorItem.setWeight(stack, field - 1, value)) {
        return false;
      } else if (field > GeneratorAreaSelectorItem.TYPE_COUNT) {
        return false;
      }
    } else if (id == 0 || id == 1) {
      GeneratorAreaSelectorItem.setCount(stack,
          GeneratorAreaSelectorItem.getCount(stack) + (id == 0 ? -5 : 5));
    } else if (id >= 2 && id < 12) {
      int index = (id - 2) / 2;
      int delta = (id - 2) % 2 == 0 ? -5 : 5;
      GeneratorAreaSelectorItem.adjustWeight(stack, index, delta);
    } else {
      return false;
    }
    this.refreshData();
    return true;
  }

  @Override
  public boolean stillValid(Player player) {
    return player.getMainHandItem().getItem() instanceof GeneratorAreaSelectorItem;
  }
}
