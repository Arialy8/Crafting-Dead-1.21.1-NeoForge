package com.craftingdead.survival.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class LootGeneratorParticle extends TextureSheetParticle {

  private LootGeneratorParticle(ClientLevel level, double x, double y, double z, SpriteSet sprite) {
    super(level, x, y, z);
    this.lifetime = 40;
    this.quadSize = 0.45F;
    this.hasPhysics = false;
    this.pickSprite(sprite);
  }

  @Override
  public ParticleRenderType getRenderType() {
    return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
  }

  @Override
  public void tick() {
    super.tick();
    this.alpha = 1.0F - ((float) this.age / (float) this.lifetime);
  }

  public static class Factory implements ParticleProvider<SimpleParticleType> {

    private final SpriteSet sprite;

    public Factory(SpriteSet sprite) {
      this.sprite = sprite;
    }

    @Override
    public Particle createParticle(SimpleParticleType particleType, ClientLevel level,
        double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
      return new LootGeneratorParticle(level, x, y, z, this.sprite);
    }
  }
}
