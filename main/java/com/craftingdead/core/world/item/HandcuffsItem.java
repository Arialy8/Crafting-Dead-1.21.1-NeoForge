/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.world.item;

import java.util.function.Supplier;
import com.craftingdead.core.util.RayTraceUtil;
import com.craftingdead.core.world.action.Action;
import com.craftingdead.core.world.action.item.EntityItemActionType;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Instant handcuffs item for 1.21.1.
 *
 * <p>Do not bypass the old action system completely. The action path already contains the correct
 * target selector, consume/result handling and multiplayer capability logic. This item only changes
 * the timing: instead of registering a held action and waiting for the next ticks, it starts the
 * action and completes it immediately on the server.</p>
 */
public class HandcuffsItem extends ActionItem {

  public HandcuffsItem(Supplier<? extends EntityItemActionType<?>> actionType,
      Properties properties) {
    super(actionType, properties);
  }

  @Override
  public InteractionResult interactLivingEntity(ItemStack itemStack, Player player,
      LivingEntity targetEntity, InteractionHand hand) {
    if (player.level().isClientSide()) {
      return InteractionResult.CONSUME;
    }

    return this.performInstantHandcuff(player, targetEntity, hand)
        ? InteractionResult.CONSUME
        : InteractionResult.PASS;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var itemStack = player.getItemInHand(hand);
    if (level.isClientSide()) {
      return InteractionResultHolder.consume(itemStack);
    }

    var hitResult = RayTraceUtil.rayTraceLivingEntities(player).orElse(null);
    if (hitResult != null && hitResult.getEntity() instanceof LivingEntity targetEntity
        && this.performInstantHandcuff(player, targetEntity, hand)) {
      return InteractionResultHolder.consume(itemStack);
    }

    return InteractionResultHolder.pass(itemStack);
  }

  private boolean performInstantHandcuff(Player player, LivingEntity targetEntity,
      InteractionHand hand) {
    if (player == null || targetEntity == null || player == targetEntity) {
      return false;
    }

    var performer = PlayerExtension.get(player);
    var target = LivingExtension.get(targetEntity);
    if (performer == null || target == null) {
      return false;
    }

    var action = this.getActionType().createEntityAction(performer, target, hand).orElse(null);
    if (action == null || !action.start(false)) {
      return false;
    }

    try {
      action.stop(Action.StopReason.COMPLETED);
    } finally {
      performer.setHoldingRightClick(false);
      performer.setRightClickTicks(0);
      player.stopUsingItem();
    }

    return true;
  }
}
