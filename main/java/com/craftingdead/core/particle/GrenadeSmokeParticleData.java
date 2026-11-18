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

public class GrenadeSmokeParticleData implements ParticleOptions {

  public static final MapCodec<GrenadeSmokeParticleData> CODEC =
      RecordCodecBuilder.mapCodec(instance -> instance
          .group(com.mojang.serialization.Codec.FLOAT.fieldOf("red").forGetter(GrenadeSmokeParticleData::getRed),
              com.mojang.serialization.Codec.FLOAT.fieldOf("green").forGetter(GrenadeSmokeParticleData::getGreen),
              com.mojang.serialization.Codec.FLOAT.fieldOf("blue").forGetter(GrenadeSmokeParticleData::getBlue),
              com.mojang.serialization.Codec.FLOAT.fieldOf("scale").forGetter(GrenadeSmokeParticleData::getScale))
          .apply(instance, GrenadeSmokeParticleData::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, GrenadeSmokeParticleData> STREAM_CODEC =
      StreamCodec.of((buf, value) -> value.writeToNetwork(buf), GrenadeSmokeParticleData::fromNetwork);

  public static GrenadeSmokeParticleData fromNetwork(FriendlyByteBuf packetBuffer) {
    return new GrenadeSmokeParticleData(
        clampColour(NetworkBufferUtil.readFloat(packetBuffer, 1.0F)),
        clampColour(NetworkBufferUtil.readFloat(packetBuffer, 1.0F)),
        clampColour(NetworkBufferUtil.readFloat(packetBuffer, 1.0F)),
        Math.max(0.0F, NetworkBufferUtil.readFloat(packetBuffer, 1.0F)));
  }

  private static float clampColour(float value) {
    return Math.max(0.0F, Math.min(value, 1.0F));
  }

  private final float red;
  private final float green;
  private final float blue;
  private final float scale;

  public GrenadeSmokeParticleData(float red, float green, float blue, float scale) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.scale = scale;
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
  public ParticleType<GrenadeSmokeParticleData> getType() {
    return ModParticleTypes.GRENADE_SMOKE.get();
  }

  public float getRed() {
    return this.red;
  }

  public float getGreen() {
    return this.green;
  }

  public float getBlue() {
    return this.blue;
  }

  public float getScale() {
    return this.scale;
  }
}
