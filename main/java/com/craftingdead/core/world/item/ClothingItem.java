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

import com.craftingdead.core.util.RegistryNameUtil;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.world.action.item.ItemActionType;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.item.equipment.Clothing;
import com.craftingdead.core.world.item.equipment.Equipment.Slot;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.capability.CapabilityUtil;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.equipment.SimpleClothing;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.craftingdead.core.world.item.attribute.ModAttributeModifierIds;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
public class ClothingItem extends EquipmentItem {

  public enum ClothingType {
    CASUAL,
    UTILITY,
    MILITARY,
    HEAVY
  }

  public static final UUID ARMOR_MODIFIER_ID =
      UUID.fromString("4117e432-16f5-4eea-a4fe-127b54d39af1");

  private final Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers;
  private final boolean fireImmunity;
  private final boolean enhancesSwimming;
  private final Supplier<? extends ItemActionType<?>> itemActionType;
  private final ClothingType clothingType;

  public ClothingItem(Properties properties, Supplier<? extends ItemActionType<?>> itemActionType,
      ClothingType clothingType) {
    super(properties);
    this.attributeModifiers = properties.attributeModifiers.build();
    this.fireImmunity = properties.fireImmunity;
    this.enhancesSwimming = properties.enhancesSwimming;
    this.itemActionType = itemActionType;
    this.clothingType = clothingType;
  }

  public Equipment createEquipment() {
    return new Clothing() {
      @Override
      public ResourceLocation getTexture(String skinType) {
        var itemId = RegistryNameUtil.item(ClothingItem.this);
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(),
            "textures/clothing/" + itemId.getPath() + "_" + skinType + ".png");
      }

      @Override
      public boolean fireImmunity() {
        return ClothingItem.this.fireImmunity;
      }

      @Override
      public boolean enhancesSwimming() {
        return ClothingItem.this.enhancesSwimming;
      }

      @Override
      public Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers() {
        return ClothingItem.this.createProfiledAttributeModifiers();
      }
    };
  }

  private Multimap<Holder<Attribute>, AttributeModifier> createProfiledAttributeModifiers() {
    var result = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
    result.putAll(this.attributeModifiers);
    String name = RegistryNameUtil.item(this).getPath();
    var random = new java.util.Random(0xC4A7F1D2L ^ name.hashCode());

    int health = switch (this.clothingType) {
      case CASUAL -> randomBetween(random, 2, 6);
      case UTILITY -> randomBetween(random, 8, 12);
      case MILITARY -> randomBetween(random, 14, 20);
      case HEAVY -> randomBetween(random, 20, 25);
    };
    int armor = switch (this.clothingType) {
      case CASUAL -> randomBetween(random, 1, 2);
      case UTILITY -> randomBetween(random, 2, 4);
      case MILITARY -> randomBetween(random, 5, 7);
      case HEAVY -> randomBetween(random, 8, 10);
    };
    int toughness = switch (this.clothingType) {
      case CASUAL -> 0;
      case UTILITY -> randomBetween(random, 0, 1);
      case MILITARY -> randomBetween(random, 1, 3);
      case HEAVY -> randomBetween(random, 3, 5);
    };

    if (containsAny(name, "medic", "doctor", "nurse")) {
      health = Math.max(health, this.clothingType == ClothingType.MILITARY ? 19 : 12);
      armor = Math.min(armor, 4);
      toughness = Math.min(toughness, 1);
    } else if (containsAny(name, "swat", "combat", "army", "soldier", "spetsnaz")) {
      armor = Math.max(armor, 7);
      toughness = Math.max(toughness, 3);
      health = Math.min(health, 17);
    } else if (containsAny(name, "fireman", "hazmat", "scuba")) {
      armor = Math.max(armor, 4);
      toughness = Math.max(toughness, 2);
      health = Math.max(health, 11);
    }

    result.put(Attributes.ARMOR, new AttributeModifier(ModAttributeModifierIds.CLOTHING_ARMOR,
        armor, AttributeModifier.Operation.ADD_VALUE));
    result.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
        ModAttributeModifierIds.CLOTHING_ARMOR_TOUGHNESS, toughness,
        AttributeModifier.Operation.ADD_VALUE));
    result.put(Attributes.MAX_HEALTH, new AttributeModifier(
        ModAttributeModifierIds.CLOTHING_MAX_HEALTH, health,
        AttributeModifier.Operation.ADD_VALUE));
    if (this.clothingType == ClothingType.HEAVY) {
      result.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
          ModAttributeModifierIds.CLOTHING_MOVEMENT_SPEED, -0.15D,
          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    return result.build();
  }

  private static int randomBetween(java.util.Random random, int minimum, int maximum) {
    return minimum + random.nextInt(maximum - minimum + 1);
  }

  private static boolean containsAny(String value, String... fragments) {
    for (String fragment : fragments) {
      if (value.contains(fragment)) return true;
    }
    return false;
  }

  public ItemActionType<?> getActionType() {
    return this.itemActionType.get();
  }

  public ClothingType getClothingType() {
    return this.clothingType;
  }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag tooltipFlag) {
    super.appendHoverText(stack, context, lines, tooltipFlag);
    switch (this.clothingType) {
      case CASUAL -> lines.add(Component.translatable("clothing.protection.level")
          .withStyle(ChatFormatting.GRAY)
          .append(" ")
          .append(Component.translatable("clothing.casual")
              .withStyle(ChatFormatting.RED)));
      case UTILITY -> lines.add(Component.translatable("clothing.protection.level")
          .withStyle(ChatFormatting.GRAY)
          .append(" ")
          .append(Component.translatable("clothing.utility")
              .withStyle(ChatFormatting.RED)));
      case MILITARY -> lines.add(Component.translatable("clothing.protection.level")
          .withStyle(ChatFormatting.GRAY)
          .append(" ")
          .append(Component.translatable("clothing.military")
              .withStyle(ChatFormatting.RED)));
    }
    if (this.fireImmunity) {
      lines.add(Component.translatable("clothing.immune_to_fire")
          .withStyle(ChatFormatting.GRAY));
    }
  }

    public @NotNull InteractionResult useOn(UseOnContext context) {
    if (context.getPlayer() != null) {
      context.getPlayer().startUsingItem(context.getHand());
      return InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
  }

    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack itemStack,
      Player player, @NotNull LivingEntity targetEntity, @NotNull InteractionHand hand) {
    player.startUsingItem(hand);
    return InteractionResult.CONSUME;
  }

    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
      @NotNull InteractionHand hand) {
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(player.getItemInHand(hand));
  }

    @Override
  public @NotNull ItemStack finishUsingItem(@NotNull ItemStack itemStack,
      @NotNull Level level, @NotNull LivingEntity livingEntity) {
    if (!level.isClientSide() && livingEntity instanceof Player player
        && !itemStack.isEmpty() && itemStack.getItem() instanceof ClothingItem) {
      this.shredClothing(itemStack, player);
    }
    return itemStack;
  }

    @Override
  public @NotNull UseAnim getUseAnimation(@NotNull ItemStack itemStack) {
    return UseAnim.BOW;
  }

    public int getUseDuration(@NotNull ItemStack itemStack, LivingEntity livingEntity) {
    return this.getActionType().getDurationTicks();
  }

  private void shredClothing(@NotNull ItemStack itemStack, @NotNull Player player) {
    int ragAmount = player.getRandom().nextInt(3) + 3;
    if (!player.getAbilities().instabuild) {
      itemStack.shrink(1);
    }

    for (int i = 0; i < ragAmount; i++) {
      ItemStack ragStack = new ItemStack(player.getRandom().nextBoolean()
          ? ModItems.CLEAN_RAG.get()
          : ModItems.DIRTY_RAG.get());
      if (!player.getInventory().add(ragStack)) {
        player.drop(ragStack, false);
      }
    }
  }

  public static class Properties extends Item.Properties {

    private final ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> attributeModifiers =
        ImmutableMultimap.builder();
    private boolean fireImmunity;
    private boolean enhancesSwimming;

    public Properties attributeModifier(Holder<Attribute> attribute, AttributeModifier modifier) {
      this.attributeModifiers.put(attribute, modifier);
      return this;
    }

    public Properties fireImmunity() {
      this.fireImmunity = true;
      return this;
    }

    public Properties enhancesSwimming() {
      this.enhancesSwimming = true;
      return this;
    }
  }

  public static ClothingItem getClothingItem(Player player) {
    var playerExtension = PlayerExtension.get(player);
    if (playerExtension != null && !playerExtension.getItemInSlot(Slot.CLOTHING).isEmpty()
        && playerExtension.getItemInSlot(Slot.CLOTHING).getItem() instanceof ClothingItem clothingItem) {
      return clothingItem;
    }
    return null;
  }

  public float calculateDamage(float damage) {
    float reductionFactor = switch (this.clothingType) {
      case CASUAL -> 1.00F - ServerConfig.instance.
          casualClothingDamageReduction.get().floatValue();
      case UTILITY -> 1.00F - ServerConfig.instance.
          utilityClothingDamageReduction.get().floatValue();
      case MILITARY, HEAVY -> 1.00F - ServerConfig.instance.
          militaryClothingDamageReduction.get().floatValue();
    };
    return damage * reductionFactor;
  }

  public float calculateBleedAndInfectionChance(float baseChance) {
    float reductionFactor = switch (this.clothingType) {
      case CASUAL -> 1.00F - ServerConfig.instance.
          casualClothingBleedAndInfectionReduction.get().floatValue();
      case UTILITY -> 1.00F - ServerConfig.instance.
          utilityClothingBleedAndInfectionReduction.get().floatValue();
      case MILITARY, HEAVY -> 1.00F - ServerConfig.instance.
          militaryClothingBleedAndInfectionReduction.get().floatValue();
    };
    return baseChance * reductionFactor;
  }
}
