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

package com.craftingdead.core.world.inventory;

import org.jetbrains.annotations.NotNull;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.equipment.Equipment;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EquipmentSlot extends Slot {

  private static Container emptyInventory = new SimpleContainer(0);
  private final LivingExtension<?, ?> living;
  private final Equipment.Slot slot;

  public EquipmentSlot(LivingExtension<?, ?> living, Equipment.Slot slot, int xPosition,
      int yPosition) {
    super(emptyInventory, slot.getIndex(), xPosition, yPosition);
    this.living = living;
    this.slot = slot;
  }

  @Override
  public boolean mayPlace(@NotNull ItemStack stack) {
    if (this.slot == Equipment.Slot.GUN && stack.is(ModItems.RPG.get())) {
      return false;
    }
    return !stack.isEmpty() && Equipment.get(stack)
        .map(equipment -> equipment.isValidForSlot(this.slot))
        .orElse(false);
  }

  @Override
  @NotNull
  public ItemStack getItem() {
    return this.living.getItemInSlot(this.slot);
  }

  @Override
  public void set(@NotNull ItemStack stack) {
    this.living.setItemInSlot(this.slot, stack);
    this.setChanged();
  }

  @Override
  public void onQuickCraft(@NotNull ItemStack oldStackIn, @NotNull ItemStack newStackIn) {}

  @Override
  public int getMaxStackSize() {
    return 1;
  }

  @Override
  public int getMaxStackSize(@NotNull ItemStack stack) {
    return 1;
  }

  @Override
  public boolean mayPickup(Player playerIn) {
    return !this.living.getItemInSlot(this.slot).isEmpty();
  }

  @Override
  @NotNull
  public ItemStack remove(int amount) {
    return amount > 0 ? this.living.setItemInSlot(this.slot, ItemStack.EMPTY) : ItemStack.EMPTY;
  }
}
