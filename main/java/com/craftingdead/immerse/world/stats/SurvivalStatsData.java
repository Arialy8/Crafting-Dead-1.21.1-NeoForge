package com.craftingdead.immerse.world.stats;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class SurvivalStatsData implements INBTSerializable<CompoundTag> {

  public static final int TICKS_PER_DAY = 24000;

  private int survivalTicks;
  private int zombieKills;
  private int playerKills;
  private long rankAdjustment;

  public long getRankScore() {
    return Math.max(0, RankDefinitions.scoreOf(this.survivalTicks,
        this.zombieKills, this.playerKills) + this.rankAdjustment);
  }

  public void setRankScore(long score) {
    this.rankAdjustment = Math.max(0, score) - RankDefinitions.scoreOf(
        this.survivalTicks, this.zombieKills, this.playerKills);
  }

  public long deductRankScore(int penalty) {
    long before = this.getRankScore();
    this.setRankScore(Math.max(0, before - penalty));
    return before - this.getRankScore();
  }
  private long airdropDayStamp;
  public static final long AIRDROP_WINDOW_MILLIS = 86_400_000L;
  private int airdropUsesToday;

  public int getSurvivalTicks() {
    return this.survivalTicks;
  }

  public int getDaysSurvived() {
    return this.survivalTicks / TICKS_PER_DAY;
  }

  public int getZombieKills() {
    return this.zombieKills;
  }

  public int getPlayerKills() {
    return this.playerKills;
  }

  public void tick() {
    if (this.survivalTicks < Integer.MAX_VALUE) {
      this.survivalTicks++;
    }
  }

  public void incrementZombieKills() {
    if (this.zombieKills < Integer.MAX_VALUE) {
      this.zombieKills++;
    }
  }

  public void incrementPlayerKills() {
    if (this.playerKills < Integer.MAX_VALUE) {
      this.playerKills++;
    }
  }

  public void set(int survivalTicks, int zombieKills, int playerKills) {
    this.survivalTicks = Math.max(0, survivalTicks);
    this.zombieKills = Math.max(0, zombieKills);
    this.playerKills = Math.max(0, playerKills);
  }

  public void reset() {
    this.set(0, 0, 0);
    this.rankAdjustment = 0;
    // Airdrop daily usage is deliberately NOT reset on death so players cannot cheat
    // the daily limit by dying.
  }

  /**
   * The argument is epoch milliseconds. The first claim starts a rolling 24-hour window;
   * offline time counts and subsequent claims do not move the deadline.
   */
  public void recordAirdropUse(long currentDay) {
    if (this.airdropDayStamp == 0 || currentDay - this.airdropDayStamp >= AIRDROP_WINDOW_MILLIS) {
      this.airdropDayStamp = currentDay;
      this.airdropUsesToday = 0;
    }
    this.airdropUsesToday++;
  }

  /**
   * Returns calls in the active real-time window, or zero after its deadline.
   */
  public int airdropUsesOn(long currentDay) {
    return this.airdropDayStamp != 0 && currentDay - this.airdropDayStamp < AIRDROP_WINDOW_MILLIS
        ? this.airdropUsesToday : 0;
  }

  public long airdropResetRemaining(long now) {
    return this.airdropDayStamp == 0 ? 0
        : Math.max(0, this.airdropDayStamp + AIRDROP_WINDOW_MILLIS - now);
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    var tag = new CompoundTag();
    tag.putInt("survivalTicks", this.survivalTicks);
    tag.putInt("zombieKills", this.zombieKills);
    tag.putInt("playerKills", this.playerKills);
    tag.putLong("rankAdjustment", this.rankAdjustment);
    tag.putLong("airdropWindowStartMillis", this.airdropDayStamp);
    tag.putInt("airdropUsesToday", this.airdropUsesToday);
    return tag;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
    if (tag != null) {
      this.set(tag.getInt("survivalTicks"), tag.getInt("zombieKills"),
          tag.getInt("playerKills"));
      this.airdropDayStamp = tag.getLong("airdropWindowStartMillis");
      this.rankAdjustment = tag.getLong("rankAdjustment");
      this.airdropUsesToday = Math.max(0, tag.getInt("airdropUsesToday"));
    }
  }
}
