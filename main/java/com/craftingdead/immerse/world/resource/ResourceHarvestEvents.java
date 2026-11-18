package com.craftingdead.immerse.world.resource;

import com.craftingdead.core.world.item.ModAxeItem;
import com.craftingdead.core.world.item.ModPickaxeItem;
import com.craftingdead.immerse.world.base.BaseSecurity;
import com.craftingdead.immerse.world.item.ImmerseItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class ResourceHarvestEvents {

  private static final String WOOD = "wood";
  private static final String STONE = "stone";

  @SubscribeEvent
  public void handleBlockBreak(BlockEvent.BreakEvent event) {
    if (!(event.getLevel() instanceof ServerLevel level)
        || !(event.getPlayer() instanceof ServerPlayer player)) {
      return;
    }

    BlockState state = event.getState();
    String resourceKind = getResourceKind(state);
    if (resourceKind == null) {
      return;
    }

    ItemStack tool = player.getMainHandItem();
    boolean customTool = WOOD.equals(resourceKind)
        ? tool.getItem() instanceof ModAxeItem
        : tool.getItem() instanceof ModPickaxeItem;
    if (!customTool || player.getAbilities().instabuild) {
      DepletableResourceData.remove(level, event.getPos());
      return;
    }

    // Do not award resources if another base rule rejects this break.
    if (!BaseSecurity.canBreak(level, event.getPos(), player)) {
      return;
    }

    event.setCanceled(true);
    DepletableResourceData.HarvestResult result = DepletableResourceData.harvest(
        level, event.getPos(), resourceKind, level.getRandom());
    if (result.amount() > 0) {
      ItemStack output = new ItemStack(ImmerseItems.ITEMS.get(
          WOOD.equals(resourceKind) ? ImmerseItems.SPLIT_LOG : ImmerseItems.STONE_PEBBLE).get(),
          result.amount());
      if (!player.addItem(output)) {
        player.drop(output, false);
      }
      tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }
    if (result.exhausted()) {
      player.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.resource_depleted"), true);
    }
  }

  private static String getResourceKind(BlockState state) {
    if (state.is(BlockTags.LOGS)) {
      return WOOD;
    }
    if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
      return STONE;
    }
    return null;
  }
}
