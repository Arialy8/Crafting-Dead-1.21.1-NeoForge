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

package com.craftingdead.core.world.action;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import com.craftingdead.core.network.message.play.SyncLivingMessage;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.tags.ModItemTags;
import com.craftingdead.core.world.action.item.BlockItemActionType;
import com.craftingdead.core.world.action.item.EntityItemActionType;
import com.craftingdead.core.world.action.reload.MagazineReloadAction;
import com.craftingdead.core.world.action.reload.RefillableReloadAction;
import com.craftingdead.core.world.effect.ModMobEffects;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.survival.world.effect.SurvivalMobEffects;
import com.craftingdead.survival.world.item.SurvivalItems;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class ActionTypes {

  public static final ResourceKey<Registry<ActionType<?>>> REGISTRY_KEY =
      ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "action_type"));

  public static final DeferredRegister<ActionType<?>> deferredRegister =
      DeferredRegister.create(REGISTRY_KEY, CraftingDead.ID);

  @SuppressWarnings("unchecked")
  public static final Registry<ActionType<?>> REGISTRY =
      deferredRegister.makeRegistry(builder -> {});

  public static final Supplier<ActionType<?>> MAGAZINE_RELOAD =
      deferredRegister.register("magazine_reload",
          () -> new SimpleActionType<>(MagazineReloadAction::new, true));

  public static final Supplier<ActionType<?>> REFILLABLE_RELOAD =
      deferredRegister.register("refillable_reload",
          () -> new SimpleActionType<>(RefillableReloadAction::new, true));

  public static final Supplier<ActionType<?>> REMOVE_MAGAZINE =
      deferredRegister.register("remove_magazine",
          () -> new SimpleActionType<>(RemoveMagazineAction::new, true));

  public static final Supplier<EntityItemActionType<?>> SHRED_CLOTHING =
      deferredRegister.register("shred_clothing",
          () -> EntityItemActionType.builder(TargetSelector.SELF_ONLY)
              .forItem(itemStack -> itemStack.is(ModItemTags.CLOTHING))
              .customAction((performer, target) -> {
                var random = target.random();
                int randomRagAmount = random.nextInt(3) + 3;

                for (int i = 0; i < randomRagAmount; i++) {
                  ItemStack ragStack = new ItemStack(random.nextBoolean()
                      ? ModItems.CLEAN_RAG.get()
                      : ModItems.DIRTY_RAG.get());

                  if (target.entity() instanceof Player player) {
                    if (!player.getInventory().add(ragStack)) {
                      player.drop(ragStack, false);
                    }
                  } else {
                    target.entity().spawnAtLocation(ragStack);
                  }
                }
              }, 1.0F)
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_SYRINGE =
      deferredRegister.register("use_syringe",
          () -> EntityItemActionType
              .builder((performer, target) -> {
                if (target == null
                    || performer == target
                    || target.entity() instanceof Skeleton) {
                  return Optional.empty();
                }

                var targetEntity = target.entity();
                if (targetEntity.getHealth() > 4) {
                  return Optional.of(target);
                }

                if (performer.entity() instanceof Player player) {
                  player.displayClientMessage(
                      Component.translatable("message.low_health",
                          targetEntity.getDisplayName()).withStyle(ChatFormatting.RED),
                      true);
                }

                return Optional.empty();
              })
              .forItem(ModItems.SYRINGE)
              .duration(16)
              .customAction((performer, target) -> target.entity().hurt(
                  target.entity().damageSources().mobAttack(target.entity()), 2.0F), 1.0F)
              .resultItem(ModItems.BLOOD_SYRINGE)
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_FIRST_AID_KIT =
      deferredRegister.register("use_first_aid_kit",
          () -> EntityItemActionType.builder(TargetSelector.SELF_OR_OTHERS)
              .forItem(ModItems.FIRST_AID_KIT)
              .duration(32)
              .customAction((performer, target) -> target.entity().heal(
                  ServerConfig.instance.firstAidKitHealAmount.get().floatValue()), 1.0F)
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_ADRENALINE_SYRINGE =
      deferredRegister.register("use_adrenaline_syringe",
          () -> EntityItemActionType.builder(TargetSelector.SELF_OR_OTHERS)
              .forItem(ModItems.ADRENALINE_SYRINGE)
              .duration(16)
              .resultItem(ModItems.SYRINGE)
              .useResultItemInCreative(false)
              .effect(() -> new MobEffectInstance(ModMobEffects.holder(ModMobEffects.ADRENALINE), 
                  ServerConfig.instance.adrenalineDurationTicks.get(), 1))
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_BLOOD_SYRINGE =
      deferredRegister.register("use_blood_syringe",
          () -> EntityItemActionType.builder(TargetSelector.SELF_OR_OTHERS)
              .forItem(ModItems.BLOOD_SYRINGE)
              .duration(32)
              .resultItem(ModItems.SYRINGE)
              .useResultItemInCreative(false)
              .effect(() -> new MobEffectInstance(MobEffects.HEAL, 1, 
                  (int) Math.max(0, ServerConfig.instance.bloodSyringeHealAmount.get().floatValue() - 1)))
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_BANDAGE =
      deferredRegister.register("use_bandage",
          () -> EntityItemActionType.builder(TargetSelector.SELF_OR_OTHERS)
              .forItem(ModItems.BANDAGE)
              .duration(32)
              .customAction((performer, target) -> target.entity().heal(
                  ServerConfig.instance.bandageHealAmount.get().floatValue()), 1.0F)
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_CLEAN_RAG =
      deferredRegister.register("use_clean_rag",
          () -> EntityItemActionType
              .builder(TargetSelector.SELF_OR_OTHERS.hasEffect(ModMobEffects.BLEEDING))
              .forItem(ModItems.CLEAN_RAG)
              .duration(32)
              .resultItem(ModItems.BLOODY_RAG)
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_SPLINT =
      deferredRegister.register("use_splint",
          () -> EntityItemActionType.builder(TargetSelector.SELF_OR_OTHERS)
              .forItem(() -> SurvivalItems.ITEMS.get("splint").get())
              .duration(32)
              .customAction((performer, target) ->
                  target.entity().removeEffect(SurvivalMobEffects.holder(SurvivalMobEffects.BROKEN_LEG)))
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_MORPHINE_SYRINGE =
      deferredRegister.register("use_morphine_syringe",
          () -> EntityItemActionType.builder(TargetSelector.SELF_OR_OTHERS)
              .forItem(() -> SurvivalItems.ITEMS.get("morphine_syringe").get())
              .duration(32)
              .resultItem(ModItems.SYRINGE)
              .useResultItemInCreative(false)
              .customAction((performer, target) -> {
                target.entity().removeEffect(SurvivalMobEffects.holder(SurvivalMobEffects.BROKEN_LEG));
                target.entity().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                target.entity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 0));
              })
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_CURE_SYRINGE =
      deferredRegister.register("use_cure_syringe",
          () -> EntityItemActionType.builder(TargetSelector.SELF_OR_OTHERS)
              .forItem(() -> SurvivalItems.ITEMS.get("cure_syringe").get())
              .duration(32)
              .resultItem(ModItems.SYRINGE)
              .useResultItemInCreative(false)
              .customAction((performer, target) -> {
                boolean removed = target.entity().removeEffect(
                    SurvivalMobEffects.holder(SurvivalMobEffects.INFECTION));
                if (removed && target.entity() instanceof net.minecraft.world.entity.player.Player player) {
                  player.displayClientMessage(Component.translatable(
                      "message.craftingdeadsurvival.infection.cured"), true);
                }
              })
              .build());

  public static final Supplier<EntityItemActionType<?>> USE_RBI_SYRINGE =
      deferredRegister.register("use_rbi_syringe",
          () -> EntityItemActionType.builder(TargetSelector.SELF_OR_OTHERS)
              .forItem(() -> SurvivalItems.ITEMS.get("rbi_syringe").get())
              .duration(32)
              .resultItem(ModItems.SYRINGE)
              .useResultItemInCreative(false)
              .customAction((performer, target) -> {
                target.entity().addEffect(new MobEffectInstance(
                    SurvivalMobEffects.holder(SurvivalMobEffects.INFECTION), 20 * 60 * 10, 0));
                if (target.entity() instanceof net.minecraft.world.entity.player.Player player) {
                  player.displayClientMessage(Component.translatable(
                      "message.craftingdeadsurvival.infection.injected"), true);
                }
              })
              .build());

  public static final Supplier<BlockItemActionType> WASH_RAG =
      deferredRegister.register("wash_rag",
          () -> BlockItemActionType.builder()
              .forItem(itemStack -> itemStack.is(ModItems.DIRTY_RAG.get())
                  || itemStack.is(ModItems.BLOODY_RAG.get())
                  || itemStack.is(ModItems.BANDAGE.get()))
              .resultItem(ModItems.CLEAN_RAG)
              .consumeItemInCreative(true)
              .finishSound(SoundEvents.BUCKET_FILL)
              .forFluid(FluidTags.WATER)
              .build());

  public static final Supplier<EntityItemActionType<?>> APPLY_HANDCUFFS =
      deferredRegister.register("apply_handcuffs",
          () -> EntityItemActionType.builder(TargetSelector.OTHERS_ONLY
              .players()
              .filter(((Predicate<PlayerExtension<?>>) PlayerExtension::isHandcuffed).negate()))
              .forItem(ModItems.HANDCUFFS)
              .duration(1)
              .customAction((performer, target) -> {
                target.setHandcuffs(performer.mainHandItem().copy());

                // The server-side handcuffed state is stored in PlayerExtension SynchedData.
                // Force a full SyncLivingMessage immediately so every tracking client can render
                // the handcuffs model in the same tick, instead of waiting for a later dirty sync.
                if (!target.level().isClientSide()) {
                  FriendlyByteBuf data = new RegistryFriendlyByteBuf(
                      Unpooled.buffer(), target.entity().registryAccess());
                  target.encode(data, true);
                  PacketDistributor.sendToPlayersTrackingEntityAndSelf(target.entity(),
                      new SyncLivingMessage(target.entity().getId(), data));
                }

                target.entity().displayClientMessage(
                    Component.translatable("handcuffs.handcuffed",
                        performer.entity().getDisplayName())
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true);
              }, 1.0F)
              .build());
}
