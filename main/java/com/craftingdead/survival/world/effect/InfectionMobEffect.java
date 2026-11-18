package com.craftingdead.survival.world.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class InfectionMobEffect extends MobEffect {

  private static final int BASE_TICK_DELAY = 20 * 8;
  private static final int MIN_TICK_DELAY = 20 * 2;

  public InfectionMobEffect() {
    super(MobEffectCategory.HARMFUL, 0x4C8A35);
  }

  @Override
  public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
    if (livingEntity.getHealth() > 1.0F) {
      livingEntity.hurt(livingEntity.damageSources().magic(), 1.0F);
    }
    return true;
  }

  @Override
  public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    int delay = Math.max(MIN_TICK_DELAY, BASE_TICK_DELAY - amplifier * 20);
    return duration % delay == 0;
  }
}
