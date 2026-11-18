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

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface TargetSelector<T extends LivingExtension<?, ?>> {

  TargetSelector<?> SELF_ONLY = (performer, target) -> Optional.of(performer);
  TargetSelector<?> OTHERS_ONLY = (performer, target) -> Optional.ofNullable(target);
  TargetSelector<?> SELF_OR_OTHERS =
      (performer, target) -> Optional.ofNullable(target == null ? performer : target);

  Optional<T> select(LivingExtension<?, ?> performer, @Nullable LivingExtension<?, ?> target);

  default TargetSelector<PlayerExtension<?>> players() {
    return (performer, target) -> this.select(performer, target)
        .filter(l -> l instanceof PlayerExtension)
        .map(l -> (PlayerExtension<?>) l);
  }

  @SuppressWarnings("unchecked")
  default <E extends LivingEntity> TargetSelector<LivingExtension<E, ?>> ofEntityType(
      EntityType<E> type) {
    if (type == null) {
      return (performer, target) -> Optional.empty();
    }
    return (performer, target) -> this.select(performer, target)
        .filter(living -> living != null && living.entity().getType() == type)
        .map(living -> (LivingExtension<E, ?>) living);
  }

  @SuppressWarnings("unchecked")
  default <E extends LivingEntity> TargetSelector<LivingExtension<E, ?>> ofEntityType(
      Class<E> clazz) {
    if (clazz == null) {
      return (performer, target) -> Optional.empty();
    }
    return (performer, target) -> this.select(performer, target)
        .filter(living -> living != null && clazz.isInstance(living.entity()))
        .map(living -> (LivingExtension<E, ?>) living);
  }

  default TargetSelector<?> hasEffect(Supplier<MobEffect> effect) {
    // Forge/Minecraft 1.21 stores active effects as Holder<MobEffect>. Older Crafting Dead
    // call-sites often still pass RegistryObject<MobEffect>/Supplier<MobEffect>, so compare the
    // underlying value instead of manufacturing a Holder.direct(...) that will not equal the
    // registered holder.
    return this.filter(living -> {
      var mobEffect = effect == null ? null : effect.get();
      return living != null && mobEffect != null && living.entity().getActiveEffects().stream()
          .anyMatch(instance -> instance.getEffect().value() == mobEffect);
    });
  }

  default TargetSelector<?> hasEffectHolder(Supplier<Holder<MobEffect>> effect) {
    return this.filter(living -> {
      var holder = effect == null ? null : effect.get();
      return living != null && holder != null && living.entity().hasEffect(holder);
    });
  }

  default TargetSelector<T> filter(Predicate<T> predicate) {
    if (predicate == null) {
      return (performer, target) -> Optional.empty();
    }
    return (performer, target) -> this.select(performer, target).filter(predicate);
  }
}
