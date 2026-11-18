/*
 * Crafting Dead
 * Copyright (C) 2022 NexusNode LTD
 */
package com.craftingdead.core.world.item.enchantment;

/**
 * Legacy marker kept so old references and documentation still resolve during the
 * 1.21.1 port. Minecraft 1.21 enchantments are data-driven and should be declared
 * through datapack JSON instead of subclassing Enchantment like 1.18.x.
 */
@Deprecated(forRemoval = false)
public final class ArmorPenetrationEnchantment {
  public static final int MAX_LEVEL = 255;

  private ArmorPenetrationEnchantment() {}
}
