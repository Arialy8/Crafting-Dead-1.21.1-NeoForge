/*
 * Crafting Dead 1.21.1 port helper.
 */
package com.craftingdead.core.util;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Compatibility helper for the Minecraft 1.20.5+ ItemStack data-component change.
 *
 * <p>Old Forge 1.18.2 code commonly used {@code ItemStack#getTag()} and
 * {@code ItemStack#getOrCreateTag()}. In 1.21.1, arbitrary stack NBT lives in
 * {@link DataComponents#CUSTOM_DATA}. This class keeps old Crafting Dead logic readable while
 * the gun/medical systems are migrated to proper typed DataComponentTypes later.</p>
 */
public final class ItemStackDataUtil {

  private ItemStackDataUtil() {}

  public static boolean hasCustomData(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    try {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      return customData != null && !customData.isEmpty();
    } catch (RuntimeException ex) {
      return false;
    }
  }

  public static CompoundTag getCustomDataCopy(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return new CompoundTag();
    }
    try {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      return customData == null ? new CompoundTag() : customData.copyTag();
    } catch (RuntimeException ex) {
      return new CompoundTag();
    }
  }

  public static void setCustomData(ItemStack stack, CompoundTag tag) {
    if (stack == null || stack.isEmpty()) {
      return;
    }
    try {
      CustomData.set(DataComponents.CUSTOM_DATA, stack, tag == null ? new CompoundTag() : tag.copy());
    } catch (RuntimeException ignored) {
      // Broken component payloads should not prevent a stack from being used or synced.
    }
  }

  public static void updateCustomData(ItemStack stack, Consumer<CompoundTag> updater) {
    if (stack == null || stack.isEmpty() || updater == null) {
      return;
    }
    try {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
        try {
          updater.accept(tag);
        } catch (RuntimeException ignored) {
          // Keep the original component data if the caller cannot update a legacy tag.
        }
      });
    } catch (RuntimeException ignored) {
      // Do not let malformed stack custom data abort capability hydration or client rendering.
    }
  }

  public static boolean contains(ItemStack stack, String key) {
    if (stack == null || stack.isEmpty() || key == null || key.isEmpty()) {
      return false;
    }
    try {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      return customData != null && customData.contains(key);
    } catch (RuntimeException ex) {
      return false;
    }
  }

  public static int getInt(ItemStack stack, String key, int fallback) {
    CompoundTag tag = getTagIfPresent(stack, key);
    if (tag == null) {
      return fallback;
    }
    try {
      return tag.getInt(key);
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static boolean getBoolean(ItemStack stack, String key, boolean fallback) {
    CompoundTag tag = getTagIfPresent(stack, key);
    if (tag == null) {
      return fallback;
    }
    try {
      return tag.getBoolean(key);
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static float getFloat(ItemStack stack, String key, float fallback) {
    CompoundTag tag = getTagIfPresent(stack, key);
    if (tag == null) {
      return fallback;
    }
    try {
      float value = tag.getFloat(key);
      return Float.isFinite(value) ? value : fallback;
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static String getString(ItemStack stack, String key, String fallback) {
    CompoundTag tag = getTagIfPresent(stack, key);
    if (tag == null) {
      return fallback;
    }
    try {
      String value = tag.getString(key);
      return value == null ? fallback : value;
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  private static CompoundTag getTagIfPresent(ItemStack stack, String key) {
    if (stack == null || stack.isEmpty() || key == null || key.isEmpty()) {
      return null;
    }
    try {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null || !customData.contains(key)) {
        return null;
      }
      return customData.copyTag();
    } catch (RuntimeException ex) {
      return null;
    }
  }
}
