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

package com.craftingdead.core.world.item.gun.skin;

import com.craftingdead.core.util.RegistryNameUtil;
import com.craftingdead.core.world.item.PaintItem;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import com.craftingdead.core.ModDataComponents;

public interface Paint {

  static Optional<Paint> get(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return Optional.empty();
    }

    if (stack.getItem() instanceof PaintItem paintItem) {
      return Optional.of(paintItem.asPaint(stack));
    }

    CompoundTag tag = null;
    try {
      if (ModDataComponents.getPaint() != null) {
        tag = stack.get(ModDataComponents.getPaint());
      }
    } catch (RuntimeException ignored) {
    }
    if (tag == null) {
      return Optional.empty();
    }

    Holder<Skin> skin = SkinLookupUtil.load(tag);
    if (skin == null) {
      return Optional.empty();
    }
    OptionalInt color = tag.contains("color") ? OptionalInt.of(tag.getInt("color")) : OptionalInt.empty();
    return Optional.of(new Paint() {
      @Override
      public Holder<Skin> getSkin() {
        return skin;
      }

      @Override
      public OptionalInt getColor() {
        return color;
      }
    });
  }

  Holder<Skin> getSkin();

  default OptionalInt getColor() {
    return OptionalInt.empty();
  }

  static Paint of(ResourceKey<Skin> skin) {
    return () -> SkinLookupUtil.holder(skin.location());
  }

  static Paint of(ResourceKey<Skin> skin, int color) {
    return of(skin, OptionalInt.of(color));
  }

  static Paint of(ResourceKey<Skin> skin, OptionalInt color) {
    return new Paint() {

      @Override
      public OptionalInt getColor() {
        return color;
      }

      @Override
      public Holder<Skin> getSkin() {
        return SkinLookupUtil.holder(skin.location());
      }
    };
  }

  static boolean isValid(ItemStack gunStack, ItemStack itemStack) {
    return isValid(RegistryNameUtil.item(gunStack.getItem()), itemStack);
  }

  static boolean isValid(ResourceLocation gunName, ItemStack itemStack) {
    return Paint.get(itemStack)
        .map(Paint::getSkin)
        .map(Holder::value)
        .filter(skin -> skin.getAcceptedGuns().contains(gunName))
        .isPresent();
  }
}
