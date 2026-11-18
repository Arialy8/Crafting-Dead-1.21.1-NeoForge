/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Compatibility wrapper for the 1.20.5+/1.21 item component serialization API.
 *
 * <p>Older Crafting Dead code used {@code ItemStack#serializeNBT()},
 * {@code ItemStack#save(CompoundTag)} and {@code ItemStack#of(CompoundTag)}. In
 * 1.21 these paths require a {@link HolderLookup.Provider}, so centralising the
 * calls keeps the remaining gun/inventory code readable while the full data
 * component migration is in progress.</p>
 */
public final class ItemStackNbtUtil {

  private ItemStackNbtUtil() {}

  public static HolderLookup.Provider lookupProvider() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    // During client-only rendering/tooltips or early data-fix migration paths there may be no
    // running integrated/dedicated server yet. The old 1.18.x code could still serialize plain
    // stack NBT in those situations, so use the empty registry access as a safe fallback and let
    // call-sites that need full dynamic registries pass an explicit provider.
    return server == null ? RegistryAccess.EMPTY : server.registryAccess();
  }

  public static CompoundTag save(ItemStack stack) {
    return save(stack, lookupProvider());
  }

  public static CompoundTag save(ItemStack stack, HolderLookup.Provider lookupProvider) {
    if (stack == null || stack.isEmpty()) {
      return new CompoundTag();
    }
    try {
      Tag tag = stack.saveOptional(lookupProvider == null ? ItemStackNbtUtil.lookupProvider() : lookupProvider);
      return tag instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
    } catch (RuntimeException ex) {
      return new CompoundTag();
    }
  }

  public static void saveInto(ItemStack stack, CompoundTag target) {
    if (target == null) {
      return;
    }
    CompoundTag saved = save(stack);
    for (String key : saved.getAllKeys()) {
      var tag = saved.get(key);
      if (tag != null) {
        target.put(key, tag);
      }
    }
  }

  public static ItemStack parseOptional(CompoundTag tag) {
    if (tag == null || tag.isEmpty()) {
      return ItemStack.EMPTY;
    }
    try {
      ItemStack parsed = ItemStack.parseOptional(lookupProvider(), tag);
      if (!parsed.isEmpty()) {
        return parsed;
      }
    } catch (RuntimeException ignored) {
    }
    return parseOptionalBasic(tag);
  }

  public static ItemStack parseOptional(HolderLookup.Provider lookupProvider, CompoundTag tag) {
    if (tag == null || tag.isEmpty()) {
      return ItemStack.EMPTY;
    }
    try {
      ItemStack parsed = ItemStack.parseOptional(
          lookupProvider == null ? ItemStackNbtUtil.lookupProvider() : lookupProvider, tag);
      if (!parsed.isEmpty()) {
        return parsed;
      }
    } catch (RuntimeException ignored) {
    }
    return parseOptionalBasic(tag);
  }

  /**
   * Registry-light fallback for network payloads that carry only id/count/custom_data.
   *
   * <p>This is intentionally narrow: it restores the item id, count and CUSTOM_DATA component,
   * which is exactly what Crafting Dead guns/magazines need for ammo HUD and renderer sync.
   * It avoids client-side RegistryAccess.EMPTY failures from turning synced mod stacks into AIR.</p>
   */
  private static ItemStack parseOptionalBasic(CompoundTag tag) {
    try {
      String id = tag.getString("id");
      if (id == null || id.isEmpty()) {
        return ItemStack.EMPTY;
      }

      var itemId = ResourceLocation.tryParse(id);
      if (itemId == null) {
        return ItemStack.EMPTY;
      }

      var item = BuiltInRegistries.ITEM.get(itemId);
      if (item == null || item == Items.AIR) {
        return ItemStack.EMPTY;
      }

      int count = 1;
      if (tag.contains("count", Tag.TAG_ANY_NUMERIC)) {
        count = Math.max(1, tag.getInt("count"));
      } else if (tag.contains("Count", Tag.TAG_ANY_NUMERIC)) {
        count = Math.max(1, tag.getInt("Count"));
      }

      ItemStack stack = new ItemStack(item, count);

      if (tag.contains("components", Tag.TAG_COMPOUND)) {
        CompoundTag components = tag.getCompound("components");
        if (components.contains("minecraft:custom_data", Tag.TAG_COMPOUND)) {
          CompoundTag customData = components.getCompound("minecraft:custom_data");
          CustomData.set(DataComponents.CUSTOM_DATA, stack, customData.copy());
        }
      }

      if (tag.contains("tag", Tag.TAG_COMPOUND)) {
        // Legacy/intermediate stack format used by earlier port steps.
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag.getCompound("tag").copy());
      }

      return stack;
    } catch (RuntimeException ex) {
      return ItemStack.EMPTY;
    }
  }
}
