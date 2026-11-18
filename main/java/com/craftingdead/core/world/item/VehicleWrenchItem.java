package com.craftingdead.core.world.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Iron vehicle wrench that survives repair recipes while consuming one durability point. */
public final class VehicleWrenchItem extends MeleeWeaponItem {

  public VehicleWrenchItem(int attackDamage, double attackSpeed, Item.Properties properties) {
    super(attackDamage, attackSpeed, properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
      TooltipFlag tooltipFlag) {
    super.appendHoverText(stack, context, lines, tooltipFlag);
    lines.add(Component.translatable("tooltip.craftingdead.iron_wrench")
        .withStyle(ChatFormatting.GRAY));
  }

  @Override
  public boolean hasCraftingRemainingItem(ItemStack stack) {
    return true;
  }

  @Override
  public ItemStack getCraftingRemainingItem(ItemStack stack) {
    ItemStack remaining = stack.copy();
    remaining.setCount(1);
    remaining.setDamageValue(remaining.getDamageValue() + 1);
    return remaining.getDamageValue() >= remaining.getMaxDamage() ? ItemStack.EMPTY : remaining;
  }
}
