package com.craftingdead.core.world.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/** Ingredient that only matches when the crafting slot contains the requested full stack. */
public record StackedIngredient(Item item, int count) implements ICustomIngredient {

  public static final MapCodec<StackedIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(
          BuiltInRegistries.ITEM.byNameCodec().fieldOf("item")
              .forGetter(StackedIngredient::item),
          Codec.intRange(1, 99).fieldOf("count").forGetter(StackedIngredient::count))
          .apply(instance, StackedIngredient::new));

  @Override
  public boolean test(ItemStack stack) {
    return stack.is(this.item) && stack.getCount() >= this.count;
  }

  @Override
  public Stream<ItemStack> getItems() {
    return Stream.of(new ItemStack(this.item, this.count));
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return VehicleContent.STACKED_INGREDIENT.get();
  }
}
