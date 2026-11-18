/*
 * Crafting Dead
 * Copyright (C) 2022 NexusNode LTD
 */
package com.craftingdead.core.world.item.enchantment;

import com.craftingdead.core.CraftingDead;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Forge/Minecraft 1.21 moved enchantments to the data-driven registry path.
 *
 * <p>The old 1.18.x code registered a concrete Enchantment instance through
 * DeferredRegister. That constructor/registry path no longer maps cleanly to 1.21.x.
 * This class keeps stable ids and small helper accessors so gun logic can compile while
 * the final datapack enchantment definition is added later.</p>
 */
public final class ModEnchantments {

  public static final ResourceKey<Enchantment> ARMOR_PENETRATION = ResourceKey.create(
      Registries.ENCHANTMENT,
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "armor_penetration"));

  private ModEnchantments() {}

  /**
   * Temporary migration accessor. The level lookup will be wired to registry-aware
   * HolderLookup once the 1.21 enchantment datapack is finalized.
   */
  public static int getArmorPenetrationLevel(ItemStack stack) {
    return 0;
  }
}
