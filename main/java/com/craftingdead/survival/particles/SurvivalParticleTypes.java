package com.craftingdead.survival.particles;

import com.craftingdead.survival.CraftingDeadSurvival;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SurvivalParticleTypes {

  public static final DeferredRegister<ParticleType<?>> deferredRegister =
      DeferredRegister.create(Registries.PARTICLE_TYPE, CraftingDeadSurvival.ID);

  public static final Supplier<SimpleParticleType> MILITARY_LOOT_GEN =
      register("military_loot_gen");

  public static final Supplier<SimpleParticleType> MEDIC_LOOT_GEN =
      register("medic_loot_gen");

  public static final Supplier<SimpleParticleType> CIVILIAN_LOOT_GEN =
      register("civilian_loot_gen");

  public static final Supplier<SimpleParticleType> CIVILIAN_RARE_LOOT_GEN =
      register("civilian_rare_loot_gen");

  public static final Supplier<SimpleParticleType> POLICE_LOOT_GEN =
      register("police_loot_gen");

  private static Supplier<SimpleParticleType> register(String name) {
    return deferredRegister.register(name, () -> new SimpleParticleType(true));
  }

  private SurvivalParticleTypes() {}
}
