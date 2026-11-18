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

package com.craftingdead.core.world.item;


import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.util.ItemHandlerNbtUtil;
import com.craftingdead.core.util.ItemStackDataUtil;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class StorageItem extends EquipmentItem {

  public static final int MAX_ROWS_TO_SHOW = 6;

  public static final UUID ARMOR_MODIFIER_ID =
      UUID.fromString("5900b64d-0e0b-4872-804b-0522bb87c33f");

  private final Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers;
  private final Equipment.Slot slot;
  private final int itemRows;
  private final ItemHandlerMenuConstructor menuConstructor;
  private final Component component;

  public StorageItem(Properties properties) {
    super(properties);
    this.attributeModifiers = properties.attributeModifiers.build();
    this.slot = properties.slot;
    this.itemRows = properties.itemRows;
    this.menuConstructor = properties.menuConstructor;
    this.component = properties.component;
  }

  public Equipment createEquipment(ItemStack itemStack) {
    return new Storage(itemStack);
  }

  public int getItemRows() {
    return this.itemRows;
  }

    public void appendHoverText(ItemStack itemStack, TooltipContext context, List<Component> lines,
      TooltipFlag tooltipFlag) {
    super.appendHoverText(itemStack, context, lines, tooltipFlag);

    if (this.component != null) {
      lines.add(this.component.copy().withStyle(ChatFormatting.GRAY));
    }

    IItemHandler itemHandler = Capabilities.ItemHandler.ITEM.getCapability(itemStack, null);
    if (itemHandler == null) {
      itemHandler = new ItemStackHandler(this.itemRows * 9);
      ItemHandlerNbtUtil.deserialize((ItemStackHandler) itemHandler,
          StorageStackDataComponentUtil.readStorageState(itemStack));
    }
    if (itemHandler != null) {
      int itemsBeyondLimit = 0;
      int itemsDisplayed = 0;

      for (int i = 0; i < itemHandler.getSlots(); i++) {
        var stack = itemHandler.getStackInSlot(i);
        if (!stack.isEmpty()) {
          if (itemsDisplayed++ >= MAX_ROWS_TO_SHOW) {
            itemsBeyondLimit++;
            continue;
          }
          var amountText = Component.literal(stack.getCount() + "x ")
              .withStyle(ChatFormatting.DARK_GRAY);
          var itemText = stack.getHoverName().plainCopy().withStyle(ChatFormatting.GRAY);
          if (itemsDisplayed == 1) {
            lines.add(Component.literal(" "));
            lines.add(Component.translatable("storage_item.contents")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
          }
          lines.add(amountText.append(itemText));
        }
      }

      if (itemsBeyondLimit > 0) {
        lines.add(Component.literal(". . . + " + itemsBeyondLimit)
            .withStyle(ChatFormatting.RED));
      }
    }
  }

  /**
   * Forge 1.21 removed the old Item#getShareTag hook. Kept as a migration helper
   * for explicit callers until this storage state is moved to data components or a
   * dedicated sync payload.
   */
  public CompoundTag getCraftingDeadShareTag(ItemStack stack) {
    var shareTag = ItemStackDataUtil.getCustomDataCopy(stack);
    Equipment.get(stack)
        .filter(Storage.class::isInstance)
        .map(Storage.class::cast)
        .map(Storage::itemHandler)
        .map(ItemHandlerNbtUtil::serialize)
        .filter(tag -> !tag.isEmpty())
        .ifPresent(tag -> {
          shareTag.put(StorageStackDataComponentUtil.LEGACY_STORAGE, tag.copy());
          shareTag.put(StorageStackDataComponentUtil.STORAGE, tag.copy());
          StorageStackDataComponentUtil.writeStorageState(stack, tag);
        });
    return shareTag;
  }

  /** See {@link #getCraftingDeadShareTag(ItemStack)}. */
  public void readCraftingDeadShareTag(ItemStack stack, @Nullable CompoundTag tag) {
    if (tag == null) {
      return;
    }
    CompoundTag storageTag = tag.contains(StorageStackDataComponentUtil.STORAGE, Tag.TAG_COMPOUND)
        ? tag.getCompound(StorageStackDataComponentUtil.STORAGE)
        : tag.getCompound(StorageStackDataComponentUtil.LEGACY_STORAGE);
    if (!storageTag.isEmpty()) {
      Equipment.get(stack)
          .filter(Storage.class::isInstance)
          .map(Storage.class::cast)
          .ifPresent(storage -> ItemHandlerNbtUtil.deserialize(storage.itemHandler(), storageTag));
      StorageStackDataComponentUtil.writeStorageState(stack, storageTag);
    }
  }

  public static class Properties extends Item.Properties {

    private final ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> attributeModifiers =
        ImmutableMultimap.builder();
    private Equipment.Slot slot;
    private int itemRows;
    private ItemHandlerMenuConstructor menuConstructor;
    private Component component;

    public Properties attributeModifier(Holder<Attribute> attribute, AttributeModifier modifier) {
      this.attributeModifiers.put(attribute, modifier);
      return this;
    }

    public Properties slot(Equipment.Slot slot) {
      this.slot = slot;
      return this;
    }

    public Properties itemRows(int itemRows) {
      this.itemRows = itemRows;
      return this;
    }

    public Properties menuConstructor(ItemHandlerMenuConstructor menuConstructor) {
      this.menuConstructor = menuConstructor;
      return this;
    }

    public Properties toolTip(Component component) {
      this.component = component;
      return this;
    }
  }

  @FunctionalInterface
  public interface ItemHandlerMenuConstructor {

    @Nullable
    AbstractContainerMenu createMenu(int windowId, Inventory inventory, IItemHandler itemHandler);
  }

  private class Storage implements Equipment, MenuConstructor {

    private final ItemStack itemStack;
    private final ItemStackHandler itemHandler;

    public Storage(ItemStack itemStack) {
      this.itemStack = itemStack;
      final var size = StorageItem.this.itemRows * 9;
      this.itemHandler = new ItemStackHandler(size) {
        @Override
        protected void onLoad() {
          if (this.getSlots() != size) {
            this.setSize(size);
          }
          this.persistToStack();
        }

        @Override
        protected void onContentsChanged(int slot) {
          super.onContentsChanged(slot);
          this.persistToStack();
        }

        private void persistToStack() {
          CompoundTag tag = ItemHandlerNbtUtil.serialize(this);
          StorageStackDataComponentUtil.writeStorageState(Storage.this.itemStack, tag);
        }
      };
      ItemHandlerNbtUtil.deserialize(this.itemHandler,
          StorageStackDataComponentUtil.readStorageState(itemStack));
    }

    private ItemStackHandler itemHandler() {
      return this.itemHandler;
    }

        public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
      return StorageItem.this.menuConstructor.createMenu(windowId, inventory, this.itemHandler);
    }

        public Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers() {
      return StorageItem.this.attributeModifiers;
    }

        public boolean isValidForSlot(Slot slot) {
      return slot == StorageItem.this.slot;
    }
  }
}
