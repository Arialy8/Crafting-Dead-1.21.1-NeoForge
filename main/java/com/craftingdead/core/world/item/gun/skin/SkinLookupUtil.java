/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.world.item.gun.skin;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Small compatibility layer for storing gun skins without relying on
 * FriendlyByteBuf#writeWithCodec/readWithCodec for a custom static registry.
 *
 * <p>Forge 1.21.x is stricter about registry-aware codecs. For Crafting Dead's
 * built-in skin registry, a ResourceLocation key is enough for persistent NBT
 * and network sync, and it is easier to validate during the port.</p>
 */
public final class SkinLookupUtil {

  public static final String NBT_KEY = "skin";

  private SkinLookupUtil() {}

  public static ResourceLocation id(Holder<Skin> holder) {
    if (holder == null) {
      return Skins.VULCAN.location();
    }
    return holder.unwrapKey()
        .map(ResourceKey::location)
        .orElseGet(() -> holder.value() == null ? Skins.VULCAN.location() : holder.value().getName());
  }

  public static Holder<Skin> holder(ResourceLocation id) {
    if (id == null) {
      id = Skins.VULCAN.location();
    }
    var key = ResourceKey.create(Skins.SKINS, id);
    return Skins.REGISTRY.getHolder(key)
        .or(() -> Skins.REGISTRY.getHolder(Skins.VULCAN))
        .orElseThrow();
  }

  public static void save(CompoundTag tag, Holder<Skin> skin) {
    if (skin != null) {
      tag.putString(NBT_KEY, id(skin).toString());
    }
  }

  public static Holder<Skin> load(CompoundTag tag) {
    if (tag == null || !tag.contains(NBT_KEY)) {
      return null;
    }
    var id = ResourceLocation.tryParse(tag.getString(NBT_KEY));
    return id == null ? null : holder(id);
  }
}
