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

package com.craftingdead.core.world.action.item;

import com.craftingdead.core.world.entity.extension.PlayerExtension;
import java.util.Optional;
import com.craftingdead.core.world.action.Action;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class ItemAction implements Action {

  private final InteractionHand hand;
  private ItemStack originalStack;
  public float progress;

  public ItemAction(InteractionHand hand) {
    this.hand = hand;
  }

  public InteractionHand getHand() {
    return this.hand;
  }

  public ItemStack getItemStack() {
    return this.performer() == null || this.performer().entity() == null || this.hand == null
        ? ItemStack.EMPTY
        : this.performer().entity().getItemInHand(this.hand);
  }

  public int getTicksUsingItem() {
    return this.performer() == null || this.performer().entity() == null
        ? 0
        : this.performer().entity().getTicksUsingItem();
  }

  private boolean checkHeldItem() {
    return this.type() != null && this.type().getHeldItemPredicate() != null
        && this.type().getHeldItemPredicate().test(this.getItemStack());
  }

  @Override
  public boolean start(boolean simulate) {
    if (this.performer() == null || this.performer().entity() == null || this.hand == null) {
      return false;
    }
    if (this.checkHeldItem()) {
      if (!simulate) {
        this.originalStack = getItemStack();
        var extension = this.performer().entity() instanceof Player player
            ? PlayerExtension.get(player)
            : null;
        if (extension != null) {
          extension.setHoldingRightClick(true);
          if (extension.getRightClickTicks() <= 0) {
            extension.setRightClickTicks(1);
          }
        }
        this.performer().entity().startUsingItem(this.hand);
      }
      return true;
    }
    return false;
  }

  @Override
  public void stop(StopReason reason) {
    if (this.performer() == null || this.performer().entity() == null || this.type() == null) {
      return;
    }
    var extension = this.performer().entity() instanceof Player player
        ? PlayerExtension.get(player)
        : null;
    this.performer().entity().stopUsingItem();
    if (reason == null || !reason.isCompleted()) {
      return;
    }

    final var heldStack = this.getItemStack();

    final var resultStack = this.getResultItem(this.performer())
        .map(Item::getDefaultInstance)
        .orElse(ItemStack.EMPTY);

    if (!this.shouldConsumeItem(this.performer())
        && this.type().getUsageDamage() >= 1) {
      if (!heldStack.isEmpty() && heldStack.isDamageableItem()) {
        heldStack.hurtAndBreak(this.type().getUsageDamage(), this.performer().entity(),
            this.hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
      }
    }

    if (this.shouldConsumeItem(this.performer())) {
      heldStack.shrink(1);
    }

    if (!resultStack.isEmpty()) {
      if (heldStack.isEmpty()) {
        this.performer().entity().setItemInHand(this.hand, resultStack);
      } else if (this.performer().entity() instanceof Player player
          && player.getInventory().add(resultStack)) {
        this.performer().entity().spawnAtLocation(resultStack);
      }
    }

    if (extension != null) {
      extension.setRightClickTicks(0);
      extension.setHoldingRightClick(false);
    }
    this.type().getFinishSound().ifPresent(
        sound -> this.performer().entity().playSound(sound, 1.0F, 1.0F));
  }

  protected boolean shouldConsumeItem(LivingExtension<?, ?> performer) {
    if (performer == null || performer.entity() == null || this.type() == null) {
      return false;
    }
    return this.type().shouldConsumeItem() &&
        !(performer.entity() instanceof Player player && player.isCreative())
        || this.type().shouldConsumeItemInCreative();
  }

  protected Optional<Item> getResultItem(LivingExtension<?, ?> performer) {
    if (performer == null || performer.entity() == null || this.type() == null) {
      return Optional.empty();
    }
    return (!this.type().useResultItemInCreative()
        && performer.entity() instanceof Player player
        && player.isCreative()) ? Optional.empty() : this.type().getResultItem();
  }

  @Override
  public boolean tick() {
    if (this.performer() == null || this.performer().entity() == null || this.type() == null) {
      return false;
    }
    var extension = this.performer().entity() instanceof Player player
        ? PlayerExtension.get(player)
        : null;
    if (extension == null || !extension.isHoldingRightClick()
        || !this.isSameHeldActionStack()) {
      this.performer().cancelAction(true);
      if (extension != null) {
        extension.setRightClickTicks(0);
      }
      return false;
    }

    if (this.type().isFreezeMovement()) {
      this.performer().setMovementBlocked(true);
    }

    int durationTicks = Math.max(1, this.type().getDurationTicks());
    int rightClickTicks = extension == null ? 0 : extension.getRightClickTicks();
    int ticksUsingItem = Math.max(this.performer().entity().getTicksUsingItem(), rightClickTicks);
    return ticksUsingItem >= durationTicks
        || this.performer().entity().getUseItemRemainingTicks() == 1;
  }

  public float getProgress(float partialTicks) {
    if (this.performer() == null || this.performer().entity() == null || this.type() == null) {
      return 0.0F;
    }
    int durationTicks = Math.max(1, this.type().getDurationTicks());
    int ticksUsingItem = this.performer().entity().getTicksUsingItem();
    if (this.performer().entity() instanceof Player player) {
      var extension = PlayerExtension.get(player);
      if (extension != null) {
        ticksUsingItem = Math.max(ticksUsingItem, extension.getRightClickTicks());
      }
    }
    this.progress = Math.max(0.0F, Math.min((ticksUsingItem + partialTicks) / durationTicks, 1.0F));
    return this.progress;
  }

  private boolean isSameHeldActionStack() {
    ItemStack currentStack = this.getItemStack();
    return this.originalStack == currentStack
        || (this.originalStack != null
            && !this.originalStack.isEmpty()
            && currentStack != null
            && !currentStack.isEmpty()
            && currentStack.is(this.originalStack.getItem()));
  }

  @Override
  public abstract ItemActionType<?> type();
}
