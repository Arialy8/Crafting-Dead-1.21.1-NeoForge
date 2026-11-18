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

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.AttachmentLike;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public class AttachmentItem extends Item implements AttachmentLike {

  private final Supplier<Attachment> attachment;

  public AttachmentItem(Supplier<Attachment> attachment, Properties properties) {
    super(properties);
    this.attachment = attachment;
  }

  @Override
  public String getDescriptionId() {
    return this.attachment.get().getDescriptionId();
  }


  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack attachmentStack = player.getItemInHand(hand);
    InteractionHand gunHand = hand == InteractionHand.MAIN_HAND
        ? InteractionHand.OFF_HAND
        : InteractionHand.MAIN_HAND;
    ItemStack gunStack = player.getItemInHand(gunHand);

    if (!this.attachToGun(level, player, gunHand, gunStack, attachmentStack)) {
      return InteractionResultHolder.pass(attachmentStack);
    }

    return InteractionResultHolder.sidedSuccess(attachmentStack, level.isClientSide());
  }

  private boolean attachToGun(Level level, Player player, InteractionHand gunHand,
      ItemStack gunStack, ItemStack attachmentStack) {
    if (gunStack.isEmpty() || attachmentStack.isEmpty()) {
      return false;
    }

    var attachment = this.asAttachment();
    var slot = attachment.getInventorySlot();
    if (slot == null) {
      return false;
    }

    var gun = Gun.get(gunStack).orElse(null);
    if (gun == null || !gun.isAcceptedAttachment(attachmentStack)) {
      return false;
    }

    if (!level.isClientSide()) {
      var attachments = new HashMap<>(gun.getAttachments());
      var previousAttachment = attachments.get(slot);
      if (previousAttachment == attachment) {
        return false;
      }
      attachments.put(slot, attachment);
      gun.setAttachments(attachments);

      if (!player.getAbilities().instabuild) {
        attachmentStack.shrink(1);
      }

      if (previousAttachment != null && previousAttachment != attachment) {
        var previousStack = new ItemStack(previousAttachment);
        if (!player.getInventory().add(previousStack)) {
          player.drop(previousStack, false);
        }
      }

      GunItem.syncHeldGunState(level, player, gunHand, gunStack, gun);
      player.getInventory().setChanged();
    }

    return true;
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context,
      List<Component> lines, TooltipFlag tooltipFlag) {
    super.appendHoverText(stack, context, lines, tooltipFlag);

    for (Entry<Attachment.MultiplierType, Float> entry : this.asAttachment()
        .getMultipliers().entrySet()) {
      lines.add(Component.translatable(entry.getKey().getTranslationKey())
          .withStyle(ChatFormatting.GRAY)
          .append(Component.literal(" " + entry.getValue() + "x")
              .withStyle(ChatFormatting.RED)));
    }
  }

  @Override
  public Attachment asAttachment() {
    return this.attachment.get();
  }
}
