/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.world.item.crafting;

import com.craftingdead.core.world.item.gun.magazine.Magazine;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

public class DuplicateMagazineRecipe extends CustomRecipe {

  public DuplicateMagazineRecipe(CraftingBookCategory category) {
    super(category);
  }

  public DuplicateMagazineRecipe() {
    this(CraftingBookCategory.MISC);
  }

  @Override
  public boolean matches(CraftingInput input, Level level) {
    if (input.size() != 9) {
      return false;
    }

    for (int i = 0; i < input.size(); i++) {
      ItemStack stack = input.getItem(i);
      switch (i) {
        case 4 -> {
          if (!Magazine.get(stack).isPresent()) {
            return false;
          }
        }
        default -> {
          if (!stack.is(Tags.Items.INGOTS_IRON)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
    NonNullList<ItemStack> remainingItems = NonNullList.withSize(input.size(), ItemStack.EMPTY);

    for (int i = 0; i < remainingItems.size(); ++i) {
      ItemStack item = input.getItem(i);
      if (Magazine.get(item).isPresent()) {
        remainingItems.set(i, item.copy());
      } else if (item.hasCraftingRemainingItem()) {
        remainingItems.set(i, item.getCraftingRemainingItem());
      }
    }

    return remainingItems;
  }

  @Override
  public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
    ItemStack result = input.getItem(4).copy();
    Magazine.get(result).ifPresent(magazine -> magazine.setSize(0));
    return result;
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return width * height == 9;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ModRecipeSerializers.DUPLICATE_MAGAZINE.get();
  }
}
