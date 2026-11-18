package com.craftingdead.survival.world;

import com.craftingdead.survival.world.effect.SurvivalMobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public final class SurvivalFractureEvents {

  private static final int FRACTURE_DURATION = 20 * 60 * 60;

  @SubscribeEvent
  public void onPlayerLanded(LivingFallEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)
        || player.isCreative()
        || player.isSpectator()
        || player.hasEffect(SurvivalMobEffects.holder(SurvivalMobEffects.BROKEN_LEG))) {
      return;
    }

    float distance = event.getDistance();
    float chance;
    if (distance >= 15.0F) {
      chance = 1.0F;
    } else if (distance >= 12.0F) {
      chance = 0.9F;
    } else if (distance >= 8.0F) {
      chance = 0.6F;
    } else if (distance >= 3.0F) {
      chance = 0.1F;
    } else {
      return;
    }

    if (player.getRandom().nextFloat() < chance) {
      player.addEffect(new MobEffectInstance(
          SurvivalMobEffects.holder(SurvivalMobEffects.BROKEN_LEG),
          FRACTURE_DURATION, 0, false, true, true));
    }
  }
}
