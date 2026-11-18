/*
 * Crafting Dead
 * Copyright (C) 2022 NexusNode LTD
 */
package com.craftingdead.core.world.item.enchantment;

import net.minecraft.world.item.ItemStack;

/** Compatibility shim for 1.18 enchantment helper calls during the 1.21.1 port. */
public final class EnchantmentLevelUtil {

  private EnchantmentLevelUtil() {}

  public static int unbreaking(ItemStack stack) {
    return 0;
  }

  public static int flame(ItemStack stack) {
    return 0;
  }

  public static int power(ItemStack stack) {
    return 0;
  }

  public static int armorPenetration(ItemStack stack) {
    return ModEnchantments.getArmorPenetrationLevel(stack);
  }
}
