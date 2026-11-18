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
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.capability.CapabilityUtil;
import com.craftingdead.core.util.ItemStackDataUtil;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.skin.Paint;
import com.craftingdead.core.world.item.gun.skin.Skin;
import com.craftingdead.core.world.item.gun.skin.Skins;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;

public class PaintItem extends Item {

  private static final String CUSTOM_COLOR_KEY = "craftingdead:paint_color";

  private final boolean multipaint;
  private final ResourceKey<Skin> skin;

  public PaintItem(Properties properties) {
    super(properties);
    this.multipaint = true;
    this.skin = null;
  }

  public PaintItem(ResourceKey<Skin> skin, Properties properties) {
    super(properties);
    this.multipaint = false;
    this.skin = skin;
  }

  public Optional<ResourceKey<Skin>> getSkinKey() {
    return Optional.ofNullable(this.skin);
  }

  public Paint asPaint(ItemStack itemStack) {
    var paintSkin = this.skin == null ? Skins.VULCAN : this.skin;
    return Paint.of(paintSkin,
        this.multipaint ? OptionalInt.of(this.getOrCreateColor(itemStack)) : OptionalInt.empty());
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack paintStack = player.getItemInHand(hand);
    InteractionHand gunHand = hand == InteractionHand.MAIN_HAND
        ? InteractionHand.OFF_HAND
        : InteractionHand.MAIN_HAND;
    ItemStack gunStack = player.getItemInHand(gunHand);

    if (!this.paintGun(level, player, gunHand, gunStack, paintStack)) {
      return InteractionResultHolder.pass(paintStack);
    }

    return InteractionResultHolder.sidedSuccess(paintStack, level.isClientSide());
  }

  private boolean paintGun(Level level, Player player, InteractionHand gunHand,
      ItemStack gunStack, ItemStack paintStack) {
    if (gunStack.isEmpty() || paintStack.isEmpty() || !Paint.isValid(gunStack, paintStack)) {
      return false;
    }

    var gun = Gun.get(gunStack).orElse(null);
    if (gun == null) {
      return false;
    }

    if (!level.isClientSide()) {
      var previousPaint = gun.getPaintStack();
      var newPaint = paintStack.copyWithCount(1);
      if (ItemStack.isSameItemSameComponents(previousPaint, newPaint)) {
        return false;
      }
      gun.setPaintStack(newPaint);

      if (!player.getAbilities().instabuild) {
        paintStack.shrink(1);
      }

      if (!previousPaint.isEmpty()) {
        var returnedPaint = previousPaint.copy();
        if (!player.getInventory().add(returnedPaint)) {
          player.drop(returnedPaint, false);
        }
      }

      GunItem.syncHeldGunState(level, player, gunHand, gunStack, gun);
      player.getInventory().setChanged();
    }

    return true;
  }

  private void bootstrapLegacyColor(ItemStack itemStack, @Nullable CompoundTag nbt) {
    if (ItemStackDataUtil.contains(itemStack, CUSTOM_COLOR_KEY) || nbt == null) {
      return;
    }

    if (nbt.contains(CUSTOM_COLOR_KEY, Tag.TAG_INT)) {
      ItemStackDataUtil.updateCustomData(itemStack, tag -> tag.putInt(CUSTOM_COLOR_KEY,
          nbt.getInt(CUSTOM_COLOR_KEY)));
      return;
    }

    if (nbt.contains("color", Tag.TAG_INT)) {
      ItemStackDataUtil.updateCustomData(itemStack, tag -> tag.putInt(CUSTOM_COLOR_KEY,
          nbt.getInt("color")));
    }
  }

  private int getOrCreateColor(ItemStack itemStack) {
    if (ItemStackDataUtil.contains(itemStack, CUSTOM_COLOR_KEY)) {
      return ItemStackDataUtil.getInt(itemStack, CUSTOM_COLOR_KEY, DyeColor.WHITE.getTextColor());
    }

    int color = DyeColor.values()[ThreadLocalRandom.current().nextInt(DyeColor.values().length)]
        .getTextColor();
    ItemStackDataUtil.updateCustomData(itemStack, tag -> tag.putInt(CUSTOM_COLOR_KEY, color));
    return color;
  }

    public void appendHoverText(ItemStack itemStack, TooltipContext context,
      List<Component> lines, TooltipFlag flag) {
    if (this.skin == null) {
      lines.add(Component.translatable("paint.multi_paint")
          .withStyle(ChatFormatting.GRAY));
      return;
    }

    Skin skin = null;
    try {
      skin = Skins.REGISTRY.getHolder(this.skin).map(net.minecraft.core.Holder::value).orElse(null);
    } catch (Exception ignored) {
      skin = Skins.REGISTRY.get(this.skin);
    }
    if (skin == null) {
      lines.add(Component.translatable("paint.accepted_guns")
          .withStyle(ChatFormatting.GRAY));
      return;
    }

    var skins = skin.getAcceptedGuns();
    if (skins == null || skins.isEmpty()) {
      lines.add(Component.translatable("paint.accepted_guns")
          .withStyle(ChatFormatting.GRAY));
      return;
    }
    lines.add(Component.translatable("paint.accepted_guns")
        .withStyle(ChatFormatting.GRAY));
    skins.stream()
        .map(BuiltInRegistries.ITEM::get)
        .filter(Objects::nonNull)
        .map(Item::getDescription)
        .map(text -> text.copy().withStyle(ChatFormatting.RED))
        .forEach(lines::add);
  }

}
