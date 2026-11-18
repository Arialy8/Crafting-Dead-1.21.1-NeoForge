package com.craftingdead.survival.world.effect;

import com.craftingdead.survival.CraftingDeadSurvival;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SurvivalMobEffects {

  public static final DeferredRegister<MobEffect> deferredRegister =
      DeferredRegister.create(Registries.MOB_EFFECT, CraftingDeadSurvival.ID);

  public static final Supplier<MobEffect> INFECTION =
      deferredRegister.register("infection", InfectionMobEffect::new);

  public static final Supplier<MobEffect> BROKEN_LEG =
      deferredRegister.register("broken_leg", BrokenLegMobEffect::new);

  public static Holder<MobEffect> holder(Supplier<MobEffect> effect) {
    var mobEffect = effect.get();
    var holder = BuiltInRegistries.MOB_EFFECT.getResourceKey(mobEffect)
        .flatMap(BuiltInRegistries.MOB_EFFECT::getHolder);
    return holder.isPresent() ? holder.get() : Holder.direct(mobEffect);
  }

  private SurvivalMobEffects() {}
}
