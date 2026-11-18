/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.world.item;

import com.craftingdead.core.util.ItemStackDataUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Transitional storage item state bridge for Minecraft 1.20.5+/1.21 data components.
 *
 * <p>Storage inventories were historically carried only by Forge capability NBT/share tags.
 * Keeping a mirrored copy under CUSTOM_DATA lets tooltips, menu-opening code and capability
 * hydration recover item contents after stack copies cross paths where the old share tag hooks
 * are no longer invoked by vanilla.</p>
 */
public final class StorageStackDataComponentUtil {

  public static final String STORAGE = "craftingdead:storage";
  public static final String LEGACY_STORAGE = "storage";

  private StorageStackDataComponentUtil() {}

  public static CompoundTag readStorageState(ItemStack stack) {
    CompoundTag customData = ItemStackDataUtil.getCustomDataCopy(stack);
    if (customData.contains(STORAGE, Tag.TAG_COMPOUND)) {
      return customData.getCompound(STORAGE).copy();
    }
    if (customData.contains(LEGACY_STORAGE, Tag.TAG_COMPOUND)) {
      return customData.getCompound(LEGACY_STORAGE).copy();
    }
    return new CompoundTag();
  }

  public static void writeStorageState(ItemStack stack, CompoundTag storageState) {
    if (stack == null || stack.isEmpty()) {
      return;
    }
    CompoundTag customData = ItemStackDataUtil.getCustomDataCopy(stack);
    CompoundTag state = storageState == null ? new CompoundTag() : storageState.copy();
    if (state.isEmpty()) {
      customData.remove(STORAGE);
      customData.remove(LEGACY_STORAGE);
    } else {
      customData.put(STORAGE, state);
      // Keep the legacy key during the migration so older explicit share-tag readers and debug
      // NBT snapshots can still see the backpack inventory without knowing the namespaced key.
      customData.put(LEGACY_STORAGE, state.copy());
    }
    ItemStackDataUtil.setCustomData(stack, customData);
  }
}
