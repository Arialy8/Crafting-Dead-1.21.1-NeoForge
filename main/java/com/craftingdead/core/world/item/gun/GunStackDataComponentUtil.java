/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.world.item.gun;

import com.craftingdead.core.util.ItemStackDataUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Stores gun state in the 1.20.5+/1.21 {@code DataComponents.CUSTOM_DATA} component.
 *
 * <p>The old 1.18.2 code serialised a gun capability directly into item NBT. During the
 * port we keep the same compound shape, but place it under a namespaced root in the stack's
 * CUSTOM_DATA component. Readers also accept the legacy top-level keys so existing worlds and
 * intermediate migration builds remain loadable.</p>
 */
public final class GunStackDataComponentUtil {

  public static final String ROOT_KEY = "craftingdead:gun";

  public static final String AMMO_PROVIDER_TYPE = "ammoProviderType";
  public static final String AMMO_PROVIDER = "ammoProvider";
  public static final String ATTACHMENTS = "attachments";
  public static final String PAINT_STACK = "paintStack";
  public static final String SKIN = "skin";

  private GunStackDataComponentUtil() {}

  public static boolean hasGunState(ItemStack stack) {
    var tag = ItemStackDataUtil.getCustomDataCopy(stack);
    return tag.contains(ROOT_KEY, Tag.TAG_COMPOUND)
        || tag.contains(AMMO_PROVIDER_TYPE, Tag.TAG_STRING)
        || tag.contains(ATTACHMENTS, Tag.TAG_LIST)
        || tag.contains(SKIN, Tag.TAG_STRING);
  }

  public static CompoundTag readGunState(ItemStack stack) {
    var tag = ItemStackDataUtil.getCustomDataCopy(stack);
    if (tag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
      return tag.getCompound(ROOT_KEY).copy();
    }

    // Legacy fallback: older port steps and 1.18 worlds may have written gun values at the top
    // level of the item stack tag. Copy only the keys this system owns so unrelated custom data
    // remains untouched.
    var legacy = new CompoundTag();
    copyIfPresent(tag, legacy, AMMO_PROVIDER_TYPE);
    copyIfPresent(tag, legacy, AMMO_PROVIDER);
    copyIfPresent(tag, legacy, ATTACHMENTS);
    copyIfPresent(tag, legacy, PAINT_STACK);
    copyIfPresent(tag, legacy, SKIN);
    return legacy;
  }

  public static void writeGunState(ItemStack stack, CompoundTag gunState) {
    if (stack == null || stack.isEmpty()) {
      return;
    }
    var state = gunState == null ? new CompoundTag() : gunState.copy();
    ItemStackDataUtil.updateCustomData(stack, tag -> {
      clearOwnedKeys(tag);
      if (state.isEmpty()) {
        return;
      }
      tag.put(ROOT_KEY, state);

      // Mirror key identity markers at top-level for one transition cycle. This keeps older
      // Crafting Dead builds from losing state if a user downgrades during testing.
      copyIfPresent(state, tag, AMMO_PROVIDER_TYPE);
      copyIfPresent(state, tag, AMMO_PROVIDER);
      copyIfPresent(state, tag, ATTACHMENTS);
      copyIfPresent(state, tag, PAINT_STACK);
      copyIfPresent(state, tag, SKIN);
    });
  }

  public static void clearGunState(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return;
    }
    ItemStackDataUtil.updateCustomData(stack, GunStackDataComponentUtil::clearOwnedKeys);
  }

  private static void clearOwnedKeys(CompoundTag tag) {
    tag.remove(ROOT_KEY);
    tag.remove(AMMO_PROVIDER_TYPE);
    tag.remove(AMMO_PROVIDER);
    tag.remove(ATTACHMENTS);
    tag.remove(PAINT_STACK);
    tag.remove(SKIN);
  }

  private static void copyIfPresent(CompoundTag source, CompoundTag target, String key) {
    if (source.contains(key)) {
      target.put(key, source.get(key).copy());
    }
  }
}
