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

import net.minecraft.network.chat.Component;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.util.RayTraceUtil;
import com.craftingdead.core.world.action.ActionObserver;
import com.craftingdead.core.world.action.ProgressBar;
import com.craftingdead.core.world.effect.ModMobEffects;
import com.craftingdead.core.world.effect.MobEffectUtil;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

public final class EntityItemAction<T extends LivingExtension<?, ?>> extends ItemAction {

  private final EntityItemActionType<T> type;
  private final LivingExtension<?, ?> performer;

  @Nullable
  private final T selectedTarget;

  protected EntityItemAction(InteractionHand hand, EntityItemActionType<T> type,
      LivingExtension<?, ?> performer, T selectedTarget) {
    super(hand);
    this.type = type;
    this.performer = performer;
    this.selectedTarget = selectedTarget;
  }

  public T getSelectedTarget() {
    return this.selectedTarget;
  }

  @Override
  public boolean start(boolean simulate) {
    return this.selectedTarget != null && super.start(simulate);
  }

  @Override
  public ActionObserver createPerformerObserver() {
    return ActionObserver.create(this, ProgressBar.create(this.type(),
        this.performer == this.selectedTarget || this.selectedTarget == null ? null
            : Component.translatable("action.target",
                this.selectedTarget.entity().getDisplayName().getString()),
        this::getProgress));
  }

  @Override
  public ActionObserver createTargetObserver() {
    if (this.performer == null || this.performer.entity() == null) {
      return this.createPerformerObserver();
    }
    return ActionObserver.create(this, ProgressBar.create(this.type(),
        Component.translatable("action.performer",
            this.performer.entity().getDisplayName().getString()),
        this::getProgress));
  }

  @Override
  public boolean tick() {
    if (this.performer == null || this.selectedTarget == null) {
      return false;
    }
    if (this.selectedTarget != this.performer) {
      var result = RayTraceUtil.rayTraceLivingEntities(this.performer.entity()).orElse(null);
      if (result == null || result.getEntity() != this.selectedTarget.entity()) {
        this.performer.cancelAction(true);
        return false;
      }
    }
    return super.tick();
  }

  @Override
  public void stop(StopReason reason) {
    if (this.performer == null || this.selectedTarget == null) {
      super.stop(reason);
      return;
    }
    if (reason != null && reason.isCompleted()) {
      if (this.type.getCustomAction() != null
          && performer.entity().getRandom().nextFloat() < this.type.getCustomAction().chance()) {
        this.type.getCustomAction().consumer().accept(this.performer, this.selectedTarget);
      }

      // Apply medical effects based on item type and config
      this.applyMedicalEffects();

      for (var action : this.type.getEffects()) {
        if (performer.entity().getRandom().nextFloat() < action.chance()) {
          var effectInstance = action.effect().get();
          if (effectInstance.getEffect().value().isInstantenous()) {
            effectInstance.getEffect().value().applyInstantenousEffect(this.selectedTarget.entity(),
                this.selectedTarget.entity(),
                this.selectedTarget.entity(), effectInstance.getAmplifier(), 1.0D);
          } else {
            this.selectedTarget.entity().addEffect(new MobEffectInstance(effectInstance));
          }
        }
      }
    }

    super.stop(reason);
  }

  @Override
  public LivingExtension<?, ?> performer() {
    return this.performer;
  }

  @Override
  public Optional<LivingExtension<?, ?>> target() {
    return this.selectedTarget == this.performer
        ? Optional.empty()
        : Optional.ofNullable(this.selectedTarget);
  }

  @Override
  public ItemActionType<?> type() {
    return this.type;
  }

  private void applyMedicalEffects() {
    ItemStack itemStack = this.getItemStack();
    if (itemStack == null || itemStack.isEmpty() || this.selectedTarget == null
        || this.performer == null) {
      return;
    }
    var targetEntity = this.selectedTarget.entity();
    var random = this.performer.entity().getRandom();

    if (itemStack.is(ModItems.BANDAGE.get())) {
      // Bandage effects
      float baseChance = ServerConfig.instance.bandageBleedReductionChance.get().floatValue();
      float effectiveChance = this.calculateBleedingChance(targetEntity, baseChance);
      
      if (ServerConfig.instance.bandageRemovesBleeding.get() && random.nextFloat() < effectiveChance) {
        targetEntity.removeEffect(ModMobEffects.holder(ModMobEffects.BLEEDING));
      }
    } else if (itemStack.is(ModItems.FIRST_AID_KIT.get())) {
      // First Aid Kit effects
      if (ServerConfig.instance.firstAidKitRemovesBleeding.get()) {
        targetEntity.removeEffect(ModMobEffects.holder(ModMobEffects.BLEEDING));
      }
      
      // Infection reduction (remove infection effect with chance)
      // Use ResourceLocation to check for infection effect across modules
      var infectionEffectLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("craftingdead", "infection");
      var infectionEffect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(infectionEffectLocation);
      if (infectionEffect != null && MobEffectUtil.hasEffectValue(targetEntity, infectionEffect)
          && random.nextFloat() < ServerConfig.instance.firstAidKitInfectionReductionChance.get().floatValue()) {
        MobEffectUtil.removeEffectValue(targetEntity, infectionEffect);
      }

      // Trauma severity reduction - reduce trauma effects
      int severityReduction = ServerConfig.instance.firstAidKitTraumaSeverityReduction.get();
      if (severityReduction > 0) {
        this.reduceTraumaEffects(targetEntity, severityReduction);
      }
    } else if (itemStack.is(ModItems.CLEAN_RAG.get())) {
      // Clean Rag effects
      if (ServerConfig.instance.cleanRagRemovesBleeding.get()) {
        targetEntity.removeEffect(ModMobEffects.holder(ModMobEffects.BLEEDING));
      }
    }
  }

  private float calculateBleedingChance(net.minecraft.world.entity.LivingEntity entity, float baseChance) {
    if (entity == null) {
      return Math.max(0.0F, Math.min(baseChance, 1.0F));
    }
    // Apply adrenaline bleeding chance multiplier if adrenaline effect is active
    if (entity.hasEffect(ModMobEffects.holder(ModMobEffects.ADRENALINE))) {
      float multiplier = ServerConfig.instance.adrenalineBleedChanceMultiplier.get().floatValue();
      return Math.min(1.0f, baseChance * multiplier);
    }
    return Math.max(0.0F, Math.min(baseChance, 1.0F));
  }

  private void reduceTraumaEffects(net.minecraft.world.entity.LivingEntity entity, int levels) {
    if (entity == null || levels <= 0) {
      return;
    }
    // Reduce trauma-related effects by shortening duration or reducing amplifier
    var blindnessEffect = entity.getEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
    if (blindnessEffect != null) {
      int newDuration = Math.max(1, blindnessEffect.getDuration() - (levels * 20));
      entity.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
      entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 
          newDuration, blindnessEffect.getAmplifier()));
    }

    var nauseaEffect = entity.getEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
    if (nauseaEffect != null) {
      int newDuration = Math.max(1, nauseaEffect.getDuration() - (levels * 40));
      entity.removeEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
      entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 
          newDuration, nauseaEffect.getAmplifier()));
    }

    var slownessEffect = entity.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
    if (slownessEffect != null) {
      int newDuration = Math.max(1, slownessEffect.getDuration() - (levels * 30));
      entity.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
      entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 
          newDuration, slownessEffect.getAmplifier()));
    }
  }
}
