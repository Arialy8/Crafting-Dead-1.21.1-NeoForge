package com.craftingdead.core.client.sounds;

import com.craftingdead.core.sounds.ModSoundEvents;
import com.craftingdead.core.world.vehicle.T72B3Entity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** Continuous positional diesel loop whose pitch and volume follow the synchronized engine. */
public final class T72EngineSoundInstance extends AbstractTickableSoundInstance {

  private final T72B3Entity tank;

  public T72EngineSoundInstance(T72B3Entity tank) {
    super(ModSoundEvents.T72_ENGINE.get(), SoundSource.BLOCKS,
        SoundInstance.createUnseededRandom());
    this.tank = tank;
    this.looping = true;
    this.delay = 0;
    this.volume = 0.0F;
    this.pitch = 0.72F;
    this.x = tank.getX();
    this.y = tank.getY();
    this.z = tank.getZ();
  }

  @Override
  public boolean canPlaySound() {
    return !this.tank.isSilent();
  }

  @Override
  public boolean canStartSilent() {
    return true;
  }

  @Override
  public void tick() {
    if (this.tank.isRemoved()) {
      this.stop();
      return;
    }
    this.x = this.tank.getX();
    this.y = this.tank.getY() + 0.8D;
    this.z = this.tank.getZ();
    if (!this.tank.isEngineRunning() || this.tank.isDestroyed()) {
      this.volume = Mth.lerp(0.18F, this.volume, 0.0F);
      this.pitch = Mth.lerp(0.12F, this.pitch, 0.72F);
      return;
    }
    float rpm = Mth.clamp(this.tank.getEngineRpm() / 8500.0F, 0.0F, 1.0F);
    float load = Mth.clamp(Math.abs(this.tank.getEnginePower()), 0.0F, 1.0F);
    float targetVolume = 0.48F + load * 0.34F;
    float targetPitch = 0.70F + rpm * 0.42F;
    this.volume = Mth.lerp(0.16F, this.volume, targetVolume);
    this.pitch = Mth.lerp(0.14F, this.pitch, targetPitch);
  }
}
