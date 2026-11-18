/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.core.world.item.gun.magazine;

import com.craftingdead.core.network.Synched;
import net.minecraft.nbt.CompoundTag;
import com.craftingdead.core.util.LegacyCompoundSerializable;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import com.craftingdead.core.ModDataComponents;

public interface Magazine extends LegacyCompoundSerializable, Synched {
  
  static Optional<Magazine> get(ItemStack stack) {
    if (stack == null || stack.isEmpty()
        || !(stack.getItem() instanceof com.craftingdead.core.world.item.MagazineItem magazineItem)) {
      return Optional.empty();
    }
    return Optional.of(magazineItem.createComponentBackedMagazine(stack));
  }

  float getArmorPenetration();

  int getSize();

  void setSize(int size);

  void refill();

  default boolean isEmpty() {
    return this.getSize() == 0;
  }

  int decrementSize();

  int getMaxSize();
}
