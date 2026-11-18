package com.craftingdead.immerse.world.airdrop;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration for the rank airdrop gift system.
 *
 * <p>Each rank owns a gift package delivered as an airdrop. Players summon it with
 * {@code /rankdrop}; the crate spawns after the configured arrival delay and loot is
 * rolled from per-rank loot tables so quantity and quality depend on luck. Players get
 * a limited number of calls per day (more for high ranks), broadcasts are fully
 * server-controlled, and low-rank players enjoy coordinate-hiding newbie protection.
 */
public final class ImmerseAirdropConfig {

  public static final ImmerseAirdropConfig instance;
  public static final ModConfigSpec configSpec;

  public final ModConfigSpec.BooleanValue enableAirdrops;
  public final ModConfigSpec.BooleanValue broadcastEnabled;
  public final ModConfigSpec.IntValue arrivalDelaySeconds;
  public final ModConfigSpec.IntValue dailyUses;
  public final ModConfigSpec.IntValue highRankDailyUses;
  public final ModConfigSpec.IntValue highRankThreshold;
  public final ModConfigSpec.IntValue newbieProtectionMaxRank;
  public final ModConfigSpec.IntValue dropRadiusMin;
  public final ModConfigSpec.IntValue dropRadiusMax;

  static {
    var pair = new ModConfigSpec.Builder().configure(ImmerseAirdropConfig::new);
    configSpec = pair.getRight();
    instance = pair.getLeft();
  }

  private ImmerseAirdropConfig(ModConfigSpec.Builder builder) {
    this.enableAirdrops = builder
        .comment("Master switch for the rank airdrop gift system")
        .define("enableAirdrops", true);
    this.broadcastEnabled = builder
        .comment("Server-wide broadcast switch for airdrop announcements (players cannot opt out)")
        .define("broadcastEnabled", true);
    this.arrivalDelaySeconds = builder
        .comment("Delay in seconds between calling the airdrop and its arrival (broadcast "
            + "announces the target coordinates immediately so others can race there)")
        .defineInRange("arrivalDelaySeconds", 180, 0, 3600);
    this.dailyUses = builder
        .comment("Airdrop calls allowed per player per rolling real-time 24 hours")
        .defineInRange("dailyUses", 1, 1, 64);
    this.highRankDailyUses = builder
        .comment("Airdrop calls per day for players at or above highRankThreshold")
        .defineInRange("highRankDailyUses", 2, 1, 64);
    this.highRankThreshold = builder
        .comment("Rank level (1-19) at which players get highRankDailyUses calls per day")
        .defineInRange("highRankThreshold", 16, 1, 19);
    this.newbieProtectionMaxRank = builder
        .comment("Players at or below this rank level do NOT get their coordinates "
            + "broadcast (newbie protection); they are still told their own drop spot")
        .defineInRange("newbieProtectionMaxRank", 3, 1, 19);
    this.dropRadiusMin = builder
        .comment("Minimum random radius (in blocks) of the drop landing spot around the summoner")
        .defineInRange("dropRadiusMin", 5, 0, 1000);
    this.dropRadiusMax = builder
        .comment("Maximum random radius (in blocks) of the drop landing spot around the summoner")
        .defineInRange("dropRadiusMax", 25, 1, 2000);
  }
}
