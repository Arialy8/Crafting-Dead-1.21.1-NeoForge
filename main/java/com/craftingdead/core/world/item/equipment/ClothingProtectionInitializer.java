/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.world.item.equipment;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.world.item.ClothingItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Initializes Crafting Dead clothing protection data on stacks that enter normal gameplay.
 *
 * <p>1.18.x used {@code EntityJoinWorldEvent}. Forge 1.20+/1.21.x renamed the level lifecycle
 * event to {@link EntityJoinLevelEvent}, so this class is kept as the central compatibility point
 * for loot drops, crafted items and picked up stacks.</p>
 */
@EventBusSubscriber(modid = CraftingDead.ID)
public final class ClothingProtectionInitializer {

  private ClothingProtectionInitializer() {}

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide()) {
      return;
    }

    if (event.getEntity() instanceof ItemEntity itemEntity) {
      initializeClothingProtection(itemEntity.getItem());
    }
  }

  @SubscribeEvent
  public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
    initializeClothingProtection(event.getCrafting());
  }

  @SubscribeEvent
  public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
    initializeClothingProtection(event.getItemEntity().getItem());
  }

  private static void initializeClothingProtection(ItemStack itemStack) {
    if (itemStack.isEmpty() || !(itemStack.getItem() instanceof ClothingItem clothingItem)) {
      return;
    }

    if (ClothingProtection.hasProtectionAttributes(itemStack)) {
      return;
    }

    ClothingProtectionPresets.applyByType(itemStack, clothingItem.getClothingType());
  }
}
