/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.world.item.crafting;

import com.craftingdead.core.capability.CapabilityUtil;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * 1.21.1 port note:
 * Recipes are no longer deserialized through fromJson/fromNetwork methods on a ForgeRegistryEntry.
 * They now expose a MapCodec for datapack JSON and a StreamCodec for network sync.
 */
public class UpgradeMagazineRecipe extends CustomRecipe {

  private static final int MIDDLE_SLOT_INDEX = 4;

  private final Ingredient magazine;
  private final ItemStack nextTier;

  public UpgradeMagazineRecipe(Ingredient magazine, ItemStack nextTier) {
    super(CraftingBookCategory.MISC);
    this.magazine = magazine;
    this.nextTier = nextTier;
  }

  public Ingredient magazine() {
    return this.magazine;
  }

  public ItemStack nextTier() {
    return this.nextTier;
  }

  @Override
  public boolean matches(CraftingInput input, Level level) {
    if (input.size() != 9) {
      return false;
    }

    for (int i = 0; i < input.size(); ++i) {
      ItemStack stack = input.getItem(i);
      switch (i) {
        case MIDDLE_SLOT_INDEX -> {
          if (!this.magazine.test(stack)) {
            return false;
          }
        }
        default -> {
          if (!stack.is(Tags.Items.NUGGETS_IRON)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
    ItemStack result = this.nextTier.copy();

    Magazine magazine = Magazine.get(input.getItem(MIDDLE_SLOT_INDEX)).orElse(null);
    Magazine resultMagazine = Magazine.get(result).orElse(null);
    if (magazine != null && resultMagazine != null) {
      resultMagazine.setSize(magazine.getSize());
    }

    return result;
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return width * height == 9;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ModRecipeSerializers.UPGRADE_MAGAZINE.get();
  }

  public static class Serializer implements RecipeSerializer<UpgradeMagazineRecipe> {

    public static final MapCodec<UpgradeMagazineRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Ingredient.CODEC.fieldOf("magazine").forGetter(UpgradeMagazineRecipe::magazine),
            ItemStack.CODEC.fieldOf("nextTier").forGetter(UpgradeMagazineRecipe::nextTier))
            .apply(instance, UpgradeMagazineRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeMagazineRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, UpgradeMagazineRecipe::magazine,
            ItemStack.STREAM_CODEC, UpgradeMagazineRecipe::nextTier,
            UpgradeMagazineRecipe::new);

    @Override
    public MapCodec<UpgradeMagazineRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, UpgradeMagazineRecipe> streamCodec() {
      return STREAM_CODEC;
    }
  }
}
