package com.craftingdead.immerse.world.airdrop;

import com.craftingdead.core.ModAttachmentTypes;
import com.craftingdead.immerse.CraftingDeadImmerse;
import com.craftingdead.immerse.world.stats.RankDefinitions;
import com.craftingdead.immerse.world.stats.SurvivalStatsData;
import com.craftingdead.survival.world.item.SupplyDropRadioItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * Core logic of the rank airdrop gift system.
 *
 * <p>Every rank owns a gift package delivered as a {@code SupplyDrop} airdrop: the player
 * calls it with {@code /rankdrop}, loot comes from per-rank loot tables
 * ({@code craftingdeadimmerse:rank_drops/rankN}) so its quantity and quality are luck
 * based. Each player gets a limited number of calls per in-game day (high ranks get a
 * larger allowance) within a rolling real-time 24-hour window. The crate arrives after a configurable delay; the broadcast goes out
 * immediately when the drop is called, announcing the XZ target so others can race over.
 * Broadcasts are fully server-controlled; players cannot opt out. Players of the first
 * few ranks enjoy newbie protection: their coordinates are never broadcast publicly.
 */
public final class RankAirdrop {

  public enum Result {
    SUMMONED,
    NO_USES_LEFT,
    DISABLED,
    NO_SPACE
  }

  /** Outcome of a summon attempt, carrying the landing spot when successful. */
  public record SummonResult(Result result, @Nullable BlockPos landing) {}

  private static final String LOOT_TABLE_PATH = "rank_drops/rank";

  private RankAirdrop() {}

  /** Resolves the loot table key for the given rank (1-based per-rank tables). */
  public static ResourceKey<LootTable> lootTableFor(RankDefinitions.Rank rank) {
    return ResourceKey.create(Registries.LOOT_TABLE,
        ResourceLocation.fromNamespaceAndPath(CraftingDeadImmerse.ID,
            LOOT_TABLE_PATH + (rank.ordinal() + 1)));
  }

  /** Computes the current rank score of a player (same formula as the rank commands). */
  public static long scoreOf(ServerPlayer player) {
    var stats = player.getData(ModAttachmentTypes.getSurvivalStats());
    return stats.getRankScore();
  }

  public static int rankLevelOf(ServerPlayer player) {
    return RankDefinitions.levelOf(scoreOf(player));
  }

  /** Daily call allowance for the player's current rank. */
  public static int dailyUsesFor(ServerPlayer player) {
    int level = rankLevelOf(player);
    return level >= ImmerseAirdropConfig.instance.highRankThreshold.get()
        ? ImmerseAirdropConfig.instance.highRankDailyUses.get()
        : ImmerseAirdropConfig.instance.dailyUses.get();
  }

  /** Calls already used in the current real-time 24-hour window. */
  public static int usesUsedToday(ServerPlayer player) {
    long day = System.currentTimeMillis();
    return player.getData(ModAttachmentTypes.getSurvivalStats()).airdropUsesOn(day);
  }

  /** Remaining calls for today, at least zero. */
  public static int usesRemaining(ServerPlayer player) {
    return Math.max(0, dailyUsesFor(player) - usesUsedToday(player));
  }

  /** Epoch milliseconds used for real-time allowance tracking. */
  private static long currentDay(ServerLevel level) {
    return System.currentTimeMillis();
  }

  /**
   * Summons a rank airdrop for the given player, honouring the master switch and the
   * per-player daily usage limit. On success the crate spawns after the configured
   * arrival delay, and the broadcast goes out immediately.
   */
  public static SummonResult summon(ServerPlayer player) {
    if (!ImmerseAirdropConfig.instance.enableAirdrops.get()) {
      return new SummonResult(Result.DISABLED, null);
    }
    ServerLevel level = player.serverLevel();
    if (usesRemaining(player) <= 0) {
      return new SummonResult(Result.NO_USES_LEFT, null);
    }
    SurvivalStatsData stats = player.getData(ModAttachmentTypes.getSurvivalStats());
    int min = ImmerseAirdropConfig.instance.dropRadiusMin.get();
    int max = Math.max(min, ImmerseAirdropConfig.instance.dropRadiusMax.get());
    BlockPos target = randomTarget(player.blockPosition(), min, max, level.getRandom());
    ResourceKey<LootTable> lootTable = lootTableFor(
        RankDefinitions.fromScore(scoreOf(player)));
    BlockPos landing = level.getHeightmapPos(
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
    RankDropSchedule.get(level.getServer()).schedule(level, target, lootTable,
        ImmerseAirdropConfig.instance.arrivalDelaySeconds.get(), player,
        rankLevelOf(player) <= ImmerseAirdropConfig.instance.newbieProtectionMaxRank.get());
    // Record the use immediately so the daily allowance cannot be raced by re-running
    // the command before the crate arrives.
    stats.recordAirdropUse(currentDay(level));
    broadcast(level, player, landing);
    return new SummonResult(Result.SUMMONED, landing);
  }

  private static void spawn(ServerLevel level, BlockPos target,
      ResourceKey<LootTable> lootTable) {
    SupplyDropRadioItem.spawnSupplyDrop(level, target, lootTable);
  }

  /**
   * Broadcasts the airdrop announcement to every online player. Broadcasts are fully
   * server-controlled: the per-player toggle was removed. Players within the newbie
   * protection range never get coordinates published - they are only told that someone
   * called an airdrop; the summoner always learns their own exact target.
   */
  public static void broadcast(ServerLevel level, ServerPlayer summoner, BlockPos landing) {
    if (!ImmerseAirdropConfig.instance.broadcastEnabled.get()) {
      return;
    }
    int summonerLevel = rankLevelOf(summoner);
    boolean hideCoords = summonerLevel <= ImmerseAirdropConfig.instance
        .newbieProtectionMaxRank.get();
    Component coords = coordsComponent(landing);
    Component message = hideCoords
        ? Component.translatable(
            "command.craftingdeadimmerse.rankdrop.broadcast_hidden",
            summoner.getDisplayName(),
            Component.translatable(RankDefinitions.fromScore(scoreOf(summoner)).nameKey()))
            .withStyle(ChatFormatting.GOLD)
        : Component.translatable(
            "command.craftingdeadimmerse.rankdrop.broadcast",
            summoner.getDisplayName(),
            Component.translatable(RankDefinitions.fromScore(scoreOf(summoner)).nameKey()),
            landing.getX(), landing.getZ()).withStyle(ChatFormatting.GOLD);
    message = message.copy().append(Component.literal("，预计 "
        + duration(ImmerseAirdropConfig.instance.arrivalDelaySeconds.get())
        + " 后开始降落（" + level.dimension().location() + "）"));
    for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
      player.displayClientMessage(message, false);
    }
    // The summoner always gets the exact target coordinates (XZ only).
    MutableComponent self = Component.translatable(
        "command.craftingdeadimmerse.rankdrop.self_coords",
        landing.getX(), landing.getZ()).withStyle(ChatFormatting.GOLD);
    summoner.displayClientMessage(self, false);
  }

  public static String duration(int seconds) {
    return seconds >= 60 ? seconds / 60 + "分钟"
        + (seconds % 60 == 0 ? "" : seconds % 60 + "秒") : seconds + "秒";
  }

  public static void announce(MinecraftServer server, java.util.UUID caller, String name,
      String dimension, BlockPos landing, boolean hideCoords, String status) {
    if (ImmerseAirdropConfig.instance.broadcastEnabled.get()) {
      server.getPlayerList().broadcastSystemMessage(Component.literal("[军衔空投] " + name
          + " 的补给" + status + (hideCoords ? "（新手保护，坐标保密）"
              : "，维度 " + dimension + "，X: " + landing.getX() + " Z: " + landing.getZ()))
          .withStyle(ChatFormatting.GOLD), false);
    }
    ServerPlayer owner = caller == null ? null : server.getPlayerList().getPlayer(caller);
    if (owner != null && (hideCoords || !ImmerseAirdropConfig.instance.broadcastEnabled.get()))
      owner.sendSystemMessage(Component.literal("[你的军衔空投] " + status
          + "，X: " + landing.getX() + " Z: " + landing.getZ()).withStyle(ChatFormatting.YELLOW));
  }

  /** Formats the XZ coordinates in gold, without the Y coordinate. */
  public static Component coordsComponent(BlockPos landing) {
    return Component.translatable("command.craftingdeadimmerse.rankdrop.coords",
        landing.getX(), landing.getZ()).withStyle(ChatFormatting.GOLD);
  }

  private static BlockPos randomTarget(BlockPos origin, int minRadius, int maxRadius,
      net.minecraft.util.RandomSource random) {
    double angle = random.nextDouble() * Math.PI * 2.0D;
    double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
    int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
    int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
    return new BlockPos(x, origin.getY(), z);
  }
}
