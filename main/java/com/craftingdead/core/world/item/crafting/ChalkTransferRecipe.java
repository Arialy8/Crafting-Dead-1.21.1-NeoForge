package com.craftingdead.core.world.item.crafting;
import com.craftingdead.core.world.item.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public final class ChalkTransferRecipe extends CustomRecipe {
  public ChalkTransferRecipe(CraftingBookCategory category) { super(category); }
  private boolean eligible(ItemStack stack) {
    return stack.getItem() instanceof GunItem || stack.getItem() instanceof MeleeWeaponItem
        || stack.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem || stack.getItem() instanceof FlamethrowerItem
        || stack.getItem() instanceof StorageItem || stack.getItem() instanceof net.minecraft.world.item.DiggerItem;
  }
  @Override public boolean matches(CraftingInput input, Level level) {
    int chalk = 0, target = 0;
    for (var stack : input.items()) {
      if (stack.isEmpty()) continue;
      if (stack.getItem() instanceof ChalkItem && stack.has(DataComponents.CUSTOM_NAME)) chalk++;
      else if (eligible(stack)) target++;
      else return false;
    }
    return chalk == 1 && target == 1;
  }
  @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
    ItemStack result = ItemStack.EMPTY;
    net.minecraft.network.chat.Component name = null;
    for (var stack : input.items()) {
      if (stack.getItem() instanceof ChalkItem) name = stack.get(DataComponents.CUSTOM_NAME);
      else if (eligible(stack)) result = stack.copyWithCount(1);
    }
    if (!result.isEmpty() && name != null) result.set(DataComponents.CUSTOM_NAME, name);
    return result;
  }
  @Override public boolean canCraftInDimensions(int w, int h) { return w * h >= 2; }
  @Override public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.CHALK_TRANSFER.get(); }
}
