/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.particle;

import com.craftingdead.core.CraftingDead;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import java.util.function.Supplier;

public class ModParticleTypes {

  public static final DeferredRegister<ParticleType<?>> deferredRegister =
      DeferredRegister.create(Registries.PARTICLE_TYPE, CraftingDead.ID);

  public static final Supplier<ParticleType<GrenadeSmokeParticleData>> GRENADE_SMOKE =
      deferredRegister.register("grenade_smoke",
          () -> create(true, GrenadeSmokeParticleData.CODEC,
              GrenadeSmokeParticleData.STREAM_CODEC));

  public static final Supplier<ParticleType<FlashParticleOptions>> RGB_FLASH =
      deferredRegister.register("rgb_flash",
          () -> create(true, FlashParticleOptions.CODEC, FlashParticleOptions.STREAM_CODEC));

  private static <T extends ParticleOptions> ParticleType<T> create(boolean alwaysShow,
      MapCodec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
    return new ParticleType<T>(alwaysShow) {
      @Override
      public MapCodec<T> codec() {
        return codec;
      }

      @Override
      public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
        return streamCodec;
      }
    };
  }
}
