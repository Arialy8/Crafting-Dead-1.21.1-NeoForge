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

package com.craftingdead.core.world.item.equipment;

import com.craftingdead.core.world.item.ClothingItem;
import com.craftingdead.core.world.item.GunItem;
import com.craftingdead.core.world.item.HatItem;
import com.craftingdead.core.world.item.MeleeWeaponItem;
import com.craftingdead.core.world.item.StorageItem;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import com.craftingdead.core.ModDataComponents;

@FunctionalInterface
public interface Equipment {

  static Optional<Equipment> get(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return Optional.empty();
    }

    var item = stack.getItem();
    if (item instanceof StorageItem storageItem) {
      return Optional.of(storageItem.createEquipment(stack));
    }
    if (item instanceof ClothingItem clothingItem) {
      return Optional.of(clothingItem.createEquipment());
    }
    if (item instanceof HatItem hatItem) {
      return Optional.of(hatItem.createEquipment());
    }
    if (item instanceof GunItem) {
      return Optional.of(forSlot(Slot.GUN));
    }
    if (item instanceof MeleeWeaponItem) {
      return Optional.of(forSlot(Slot.MELEE));
    }

    CompoundTag tag = stack.get(ModDataComponents.getEquipment());
    if (tag == null) return Optional.empty();
    return Optional.empty();
  }

  default Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers() {
    return ImmutableMultimap.of();
  }

  boolean isValidForSlot(Slot slot);

  static Equipment forSlot(Slot slot) {
    return slot::equals;
  }

  enum Slot {

    MELEE(0), GUN(1), HAT(2), CLOTHING(3), VEST(4), BACKPACK(5);

    private final int index;

    Slot(int index) {
      this.index = index;
    }

    public int getIndex() {
      return this.index;
    }
  }
}
