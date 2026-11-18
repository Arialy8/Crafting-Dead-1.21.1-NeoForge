/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.world.item.gun.magazine;

import com.craftingdead.core.util.ItemStackDataUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Mirrors magazine capability state into ItemStack CUSTOM_DATA. */
public final class MagazineStackDataComponentUtil {

  public static final String ROOT_KEY = "craftingdead:magazine";
  public static final String LEGACY_KEY = "magazine";
  public static final String SIZE = "size";

  private MagazineStackDataComponentUtil() {}

  public static CompoundTag readMagazineState(ItemStack stack) {
    var tag = ItemStackDataUtil.getCustomDataCopy(stack);
    if (tag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
      return tag.getCompound(ROOT_KEY).copy();
    }
    if (tag.contains(LEGACY_KEY, Tag.TAG_COMPOUND)) {
      return tag.getCompound(LEGACY_KEY).copy();
    }
    var fallback = new CompoundTag();
    if (tag.contains(SIZE, Tag.TAG_INT)) {
      fallback.putInt(SIZE, tag.getInt(SIZE));
    }
    return fallback;
  }

  public static void writeMagazineState(ItemStack stack, CompoundTag magazineState) {
    if (stack == null || stack.isEmpty()) {
      return;
    }
    var state = magazineState == null ? new CompoundTag() : magazineState.copy();
    ItemStackDataUtil.updateCustomData(stack, tag -> {
      if (state.isEmpty()) {
        tag.remove(ROOT_KEY);
        tag.remove(LEGACY_KEY);
      } else {
        tag.put(ROOT_KEY, state);
        tag.put(LEGACY_KEY, state.copy());
      }
    });
  }

  public static int getSize(ItemStack stack, int fallback) {
    var state = readMagazineState(stack);
    return Math.max(0, state.contains(SIZE, Tag.TAG_INT) ? state.getInt(SIZE) : fallback);
  }

  public static void setSize(ItemStack stack, int size) {
    var state = readMagazineState(stack);
    state.putInt(SIZE, Math.max(0, size));
    writeMagazineState(stack, state);
  }
}
