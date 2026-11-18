/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.world.item.crafting;

import com.craftingdead.core.CraftingDead;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ModRecipeSerializers {

  public static final DeferredRegister<RecipeSerializer<?>> deferredRegister =
      DeferredRegister.create(Registries.RECIPE_SERIALIZER, CraftingDead.ID);

  public static final Supplier<SimpleCraftingRecipeSerializer<ChalkTransferRecipe>> CHALK_TRANSFER =
      deferredRegister.register("chalk_transfer", () -> new SimpleCraftingRecipeSerializer<>(ChalkTransferRecipe::new));

  public static final Supplier<RecipeSerializer<UpgradeMagazineRecipe>> UPGRADE_MAGAZINE =
      deferredRegister.register("upgrade_magazine", UpgradeMagazineRecipe.Serializer::new);

  public static final Supplier<SimpleCraftingRecipeSerializer<DuplicateMagazineRecipe>> DUPLICATE_MAGAZINE =
      deferredRegister.register("duplicate_magazine",
          () -> new SimpleCraftingRecipeSerializer<>(DuplicateMagazineRecipe::new));
}
