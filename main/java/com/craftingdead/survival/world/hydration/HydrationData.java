package com.craftingdead.survival.world.hydration;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class HydrationData implements INBTSerializable<CompoundTag> {

  public static final int MAX_WATER = 20;

  private int water = MAX_WATER;
  private int tickCounter;

  public int getWater() {
    return this.water;
  }

  public int getTickCounter() {
    return this.tickCounter;
  }

  public void setTickCounter(int tickCounter) {
    this.tickCounter = Math.max(0, tickCounter);
  }

  public boolean addWater(int amount) {
    return this.setWater(this.water + amount);
  }

  public boolean consumeWater(int amount) {
    return this.setWater(this.water - amount);
  }

  public boolean setWater(int water) {
    int clamped = Math.max(0, Math.min(MAX_WATER, water));
    if (this.water == clamped) {
      return false;
    }
    this.water = clamped;
    return true;
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    return this.serializeNBT();
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
    this.deserializeNBT(tag);
  }

  public CompoundTag serializeNBT() {
    var tag = new CompoundTag();
    tag.putInt("water", this.water);
    tag.putInt("tickCounter", this.tickCounter);
    return tag;
  }

  public void deserializeNBT(CompoundTag tag) {
    if (tag == null) {
      return;
    }
    this.water = Math.max(0, Math.min(MAX_WATER, tag.getInt("water")));
    this.tickCounter = Math.max(0, tag.getInt("tickCounter"));
  }
}
