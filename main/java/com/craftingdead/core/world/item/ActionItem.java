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

import java.util.function.Supplier;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.util.RayTraceUtil;
import com.craftingdead.core.world.action.item.ItemActionType;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ActionItem extends Item {

  private final Supplier<? extends ItemActionType<?>> itemActionType;

  public ActionItem(Supplier<? extends ItemActionType<?>> itemActionType, Properties properties) {
    super(properties);
    this.itemActionType = itemActionType;
  }

  public ItemActionType<?> getActionType() {
    return this.itemActionType.get();
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    if (this.tryWashRag(context.getLevel(), context.getPlayer(), context.getHand(),
        context.getClickedPos())) {
      return InteractionResult.CONSUME;
    }

    if (context.getLevel().isClientSide() && context.getPlayer() != null) {
      context.getPlayer().startUsingItem(context.getHand());
      return InteractionResult.CONSUME;
    }

    if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
      var performer = PlayerExtension.get(context.getPlayer());
      if (performer != null && ActionTypes.WASH_RAG.get().createBlockAction(performer, context)
          .map(action -> this.performActionUse(performer, context.getHand(), action))
          .orElse(false)) {
        return InteractionResult.CONSUME;
      }
      if (performer != null && this.getActionType().createBlockAction(performer, context)
          .map(action -> this.performActionUse(performer, context.getHand(), action))
          .orElse(false)) {
        return InteractionResult.CONSUME;
      }
    }
    return InteractionResult.PASS;
  }

  @Override
  public InteractionResult interactLivingEntity(ItemStack itemStack, Player player,
      LivingEntity targetEntity, InteractionHand hand) {
    if (player.level().isClientSide()) {
      player.startUsingItem(hand);
      return InteractionResult.CONSUME;
    }

    if (!player.level().isClientSide()) {
      var performer = PlayerExtension.get(player);
      var target = LivingExtension.get(targetEntity);
      if (performer != null && target != null && this.getActionType().createEntityAction(performer, target, hand)
          .map(action -> this.performActionUse(performer, hand, action))
          .orElse(false)) {
        return InteractionResult.CONSUME;
      }
    }
    return InteractionResult.PASS;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (player.level().isClientSide()) {
      player.startUsingItem(hand);
      return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    if (!player.level().isClientSide()) {
      var performer = PlayerExtension.get(player);
      if (performer == null) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
      }
      // In multiplayer, right-clicking another player may arrive through the generic use path
      // instead of interactLivingEntity. Try entity-targeted actions first so medical items and
      // handcuffs can target other players reliably.
      var entityHitResult = RayTraceUtil.rayTraceLivingEntities(player).orElse(null);
      if (entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity targetEntity) {
        var target = LivingExtension.get(targetEntity);
        if (target != null && this.getActionType().createEntityAction(performer, target, hand)
            .map(action -> this.performActionUse(performer, hand, action))
            .orElse(false)) {
          return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
      }

      var hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
      if (hitResult.getType() == HitResult.Type.BLOCK
          && this.tryWashRag(level, player, hand,
              ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos())) {
        return InteractionResultHolder.consume(player.getItemInHand(hand));
      }

      if (hitResult.getType() == HitResult.Type.BLOCK
          && ActionTypes.WASH_RAG.get()
              .createBlockAction(performer, new UseOnContext(player, hand, hitResult))
              .map(action -> this.performActionUse(performer, hand, action))
              .orElse(false)) {
        return InteractionResultHolder.consume(player.getItemInHand(hand));
      }

      if (hitResult.getType() == HitResult.Type.BLOCK
          && this.getActionType()
              .createBlockAction(performer, new UseOnContext(player, hand, hitResult))
              .map(action -> this.performActionUse(performer, hand, action))
              .orElse(false)) {
        return InteractionResultHolder.consume(player.getItemInHand(hand));
      }

      if (this.getActionType().createAction(performer, hand)
          .map(action -> this.performActionUse(performer, hand, action))
          .orElse(false)) {
        return InteractionResultHolder.consume(player.getItemInHand(hand));
      }
    }

    return InteractionResultHolder.pass(player.getItemInHand(hand));
  }

  private boolean tryWashRag(Level level, Player player, InteractionHand hand,
      net.minecraft.core.BlockPos pos) {
    if (level == null || player == null || hand == null || pos == null) {
      return false;
    }
    ItemStack heldStack = player.getItemInHand(hand);
    if (!heldStack.is(ModItems.DIRTY_RAG.get())
        && !heldStack.is(ModItems.BLOODY_RAG.get())
        && !heldStack.is(ModItems.BANDAGE.get())) {
      return false;
    }
    if (!level.getFluidState(pos).is(FluidTags.WATER)
        && !level.getBlockState(pos).getFluidState().is(FluidTags.WATER)) {
      return false;
    }
    if (!level.isClientSide()) {
      if (!player.getAbilities().instabuild) {
        heldStack.shrink(1);
      }
      ItemStack cleanRag = new ItemStack(ModItems.CLEAN_RAG.get());
      if (heldStack.isEmpty()) {
        player.setItemInHand(hand, cleanRag);
      } else if (!player.getInventory().add(cleanRag)) {
        player.drop(cleanRag, false);
      }
    }
    return true;
  }

  private boolean performActionUse(PlayerExtension<?> performer, InteractionHand hand, com.craftingdead.core.world.action.Action action) {
    if (performer == null || action == null) {
      return false;
    }
    performer.setHoldingRightClick(true);
    if (performer.getRightClickTicks() <= 0) {
      performer.setRightClickTicks(1);
    }
    if (performer.entity() != null && hand != null) {
      performer.entity().startUsingItem(hand);
    }
    return performer.performAction(action, true);
  }

  @Override
  public int getUseDuration(ItemStack itemStack, LivingEntity livingEntity) {
    return this.getActionType().getDurationTicks();
  }
}
