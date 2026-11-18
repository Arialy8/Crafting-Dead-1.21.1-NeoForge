package com.craftingdead.survival.world;

import com.craftingdead.survival.CraftingDeadSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class SurvivalEnchantments {
  public static final ResourceKey<Enchantment> INFECTION = ResourceKey.create(
      Registries.ENCHANTMENT,
      ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID, "infection"));
  public static final ResourceKey<Enchantment> BURNING = key("burning");
  public static final ResourceKey<Enchantment> EXPLOSIVE = key("explosive");
  public static final ResourceKey<Enchantment> BLINDING = key("blinding");
  public static final ResourceKey<Enchantment> SLOWING = key("slowing");

  private static ResourceKey<Enchantment> key(String name) {
    return ResourceKey.create(Registries.ENCHANTMENT,
        ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID, name));
  }

  private SurvivalEnchantments() {}
}
