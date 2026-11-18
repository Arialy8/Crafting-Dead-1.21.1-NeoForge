/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Bridges Forge ItemStackHandler's 1.21.x registry-aware NBT methods. */
public final class ItemHandlerNbtUtil {

  private ItemHandlerNbtUtil() {}

  public static CompoundTag serialize(ItemStackHandler itemHandler) {
    return serialize(itemHandler, ItemStackNbtUtil.lookupProvider());
  }

  public static CompoundTag serialize(ItemStackHandler itemHandler, HolderLookup.Provider provider) {
    if (itemHandler == null) {
      return new CompoundTag();
    }
    try {
      return itemHandler.serializeNBT(provider == null ? ItemStackNbtUtil.lookupProvider() : provider);
    } catch (RuntimeException ex) {
      return new CompoundTag();
    }
  }

  public static void deserialize(ItemStackHandler itemHandler, CompoundTag tag) {
    deserialize(itemHandler, ItemStackNbtUtil.lookupProvider(), tag);
  }

  public static void deserialize(ItemStackHandler itemHandler, HolderLookup.Provider provider,
      CompoundTag tag) {
    if (itemHandler == null || tag == null || tag.isEmpty()) {
      return;
    }
    try {
      itemHandler.deserializeNBT(provider == null ? ItemStackNbtUtil.lookupProvider() : provider, tag);
    } catch (RuntimeException ignored) {
      // Bad legacy item handler tags should not prevent a world or stack from loading.
    }
  }
}
