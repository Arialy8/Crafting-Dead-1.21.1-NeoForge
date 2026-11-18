/*
 * Crafting Dead - 1.21.1 migration helper
 */
package com.craftingdead.core.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Bridge for legacy Crafting Dead classes that still expose the old
 * no-registry NBT methods while Forge/Minecraft 1.21.x passes a
 * {@link HolderLookup.Provider} to serializers.
 *
 * <p>Keep the old methods as the internal implementation point until the
 * individual systems are fully converted to registry-aware codecs/data
 * components.
 */
public interface LegacyCompoundSerializable extends INBTSerializable<CompoundTag> {

  CompoundTag serializeNBT();

  void deserializeNBT(CompoundTag tag);

  @Override
  default CompoundTag serializeNBT(HolderLookup.Provider provider) {
    return this.serializeNBT();
  }

  @Override
  default void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
    this.deserializeNBT(tag);
  }
}
