/*
 * Crafting Dead - 1.21.1 migration helper
 */
package com.craftingdead.core.world.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Holder/value compatibility helpers for the 1.20.5+/1.21 MobEffect API.
 */
public final class MobEffectUtil {

  private MobEffectUtil() {}

  public static boolean hasEffectValue(LivingEntity entity, @Nullable MobEffect effect) {
    return getEffectValue(entity, effect) != null;
  }

  @Nullable
  public static MobEffectInstance getEffectValue(LivingEntity entity, @Nullable MobEffect effect) {
    if (effect == null) {
      return null;
    }
    return entity.getActiveEffects().stream()
        .filter(instance -> instance.getEffect().value() == effect)
        .findFirst()
        .orElse(null);
  }

  public static boolean removeEffectValue(LivingEntity entity, @Nullable MobEffect effect) {
    if (effect == null) {
      return false;
    }
    var holder = entity.getActiveEffects().stream()
        .map(MobEffectInstance::getEffect)
        .filter(active -> active.value() == effect)
        .findFirst()
        .orElse(null);
    return holder != null && entity.removeEffect(holder);
  }

  public static boolean hasEffect(LivingEntity entity, Holder<MobEffect> effect) {
    return entity.hasEffect(effect);
  }
}
