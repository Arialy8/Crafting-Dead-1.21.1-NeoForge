/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.data.recipes;

import com.craftingdead.core.world.item.crafting.UpgradeMagazineRecipe;
import com.craftingdead.core.CraftingDead;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Minimal 1.21.1 data-gen adapter for upgrade-magazine recipes.
 *
 * The old FinishedRecipe/Advancement.Builder path was removed from modern recipe data-gen.
 * This builder now emits the actual recipe instance through RecipeOutput. Advancements can be
 * reintroduced later if desired; keeping them out for now reduces the 1.21.1 porting surface.
 */
public class UpgradeMagazineRecipeBuilder {

  private final Ingredient magazine;
  private final Item nextTier;

  private UpgradeMagazineRecipeBuilder(Ingredient magazine, Item nextTier) {
    this.magazine = magazine;
    this.nextTier = nextTier;
  }

  public static UpgradeMagazineRecipeBuilder create(Ingredient magazine, Item nextTier) {
    return new UpgradeMagazineRecipeBuilder(magazine, nextTier);
  }

  public UpgradeMagazineRecipeBuilder unlockedBy(String id, Object criterion) {
    // Advancement generation is intentionally deferred during the 1.21.1 core API migration.
    return this;
  }

  public UpgradeMagazineRecipeBuilder group(String group) {
    return this;
  }

  public void save(RecipeOutput output) {
    this.save(output, BuiltInRegistries.ITEM.getKey(this.nextTier));
  }

  public void save(RecipeOutput output, String id) {
    ResourceLocation location = ResourceLocation.tryParse(id);
    if (location == null) {
      location = ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, id == null ? "upgrade_magazine" : id.replaceAll("[^a-z0-9_./-]", "_"));
    }
    this.save(output, location);
  }

  public void save(RecipeOutput output, ResourceLocation id) {
    output.accept(id, new UpgradeMagazineRecipe(this.magazine, new ItemStack(this.nextTier)), null);
  }
}
