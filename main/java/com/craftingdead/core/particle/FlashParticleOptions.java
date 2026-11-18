/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.particle;

import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.util.RegistryNameUtil;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record FlashParticleOptions(float red, float green, float blue, float scale)
    implements ParticleOptions {

  public static final MapCodec<FlashParticleOptions> CODEC =
      RecordCodecBuilder.mapCodec(instance -> instance
          .group(
              com.mojang.serialization.Codec.FLOAT.fieldOf("red").forGetter(FlashParticleOptions::red),
              com.mojang.serialization.Codec.FLOAT.fieldOf("green").forGetter(FlashParticleOptions::green),
              com.mojang.serialization.Codec.FLOAT.fieldOf("blue").forGetter(FlashParticleOptions::blue),
              com.mojang.serialization.Codec.FLOAT.fieldOf("scale").forGetter(FlashParticleOptions::scale))
          .apply(instance, FlashParticleOptions::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, FlashParticleOptions> STREAM_CODEC =
      StreamCodec.of((buf, value) -> value.writeToNetwork(buf), FlashParticleOptions::fromNetwork);

  public static FlashParticleOptions fromNetwork(FriendlyByteBuf packetBuffer) {
    return new FlashParticleOptions(
        clampColour(NetworkBufferUtil.readFloat(packetBuffer, 1.0F)),
        clampColour(NetworkBufferUtil.readFloat(packetBuffer, 1.0F)),
        clampColour(NetworkBufferUtil.readFloat(packetBuffer, 1.0F)),
        Math.max(0.0F, NetworkBufferUtil.readFloat(packetBuffer, 1.0F)));
  }

  private static float clampColour(float value) {
    return Math.max(0.0F, Math.min(value, 1.0F));
  }

  public void writeToNetwork(FriendlyByteBuf packetBuffer) {
    NetworkBufferUtil.writeFloat(packetBuffer, this.red, 0.0F, 1.0F);
    NetworkBufferUtil.writeFloat(packetBuffer, this.green, 0.0F, 1.0F);
    NetworkBufferUtil.writeFloat(packetBuffer, this.blue, 0.0F, 1.0F);
    NetworkBufferUtil.writeFloat(packetBuffer, this.scale, 0.0F, 64.0F);
  }

  public String writeToString() {
    return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f",
        RegistryNameUtil.particleType(this.getType()), this.red, this.green, this.blue, this.scale);
  }

  @Override
  public ParticleType<FlashParticleOptions> getType() {
    return ModParticleTypes.RGB_FLASH.get();
  }
}
