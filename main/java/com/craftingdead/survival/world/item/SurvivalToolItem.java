package com.craftingdead.survival.world.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SurvivalToolItem extends Item {

  public SurvivalToolItem(Properties properties) {
    super(properties);
  }

  @Override
  public boolean hasCraftingRemainingItem(ItemStack stack) {
    return true;
  }

  @Override
  public ItemStack getCraftingRemainingItem(ItemStack stack) {
    var remainingStack = stack.copy();
    remainingStack.setCount(1);
    remainingStack.setDamageValue(remainingStack.getDamageValue() + 1);
    return remainingStack.getDamageValue() >= remainingStack.getMaxDamage()
        ? ItemStack.EMPTY
        : remainingStack;
  }
}
