/*
 * Crafting Dead
 * Copyright (C) 2022 NexusNode LTD
 */
package com.craftingdead.core.world.item.enchantment;

import com.craftingdead.core.world.item.GunItem;
import net.minecraft.world.item.Item;

/**
 * Legacy 1.18 enchantment category shim. 1.21 enchantment targeting is data-driven,
 * so this is kept only for migration-time intent checks and documentation.
 */
public final class ModEnchantmentTypes {

  private ModEnchantmentTypes() {}

  public static boolean isGunItem(Item item) {
    return item instanceof GunItem;
  }
}
