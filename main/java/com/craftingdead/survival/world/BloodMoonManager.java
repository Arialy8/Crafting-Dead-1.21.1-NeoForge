package com.craftingdead.survival.world;

import com.craftingdead.core.network.message.play.SyncBloodMoonMessage;
import java.util.Random;
import net.minecraft.server.level.ServerLevel;

/**
 * 血月（Blood Moon）逻辑管理器。
 *
 * <p>血月以固定天数为一个周期，周期的最后一天为“血月天”，当天夜晚必定触发尸潮；
 * 周期内其余每天有概率成为“预兆日”，预兆日白天天空会变暗。</p>
 *
 * <p>预兆日通过（世界种子 ^ 天数）作为随机种子确定性判定，服务端重启或所有玩家重进
 * 都不会改变结果，保证同一天内所有玩家看到一致的天色。</p>
 */
public final class BloodMoonManager {

  public static final int TICKS_PER_DAY = 24000;
  public static final int MIN_CYCLE_DAYS = 2;
  public static final int MAX_CYCLE_DAYS = 31;
  private static final long OMEN_SEED_MULTIPLIER = 0x9E3779B97F4A7C15L;

  private BloodMoonManager() {}

  public static float doubledChance(float chance) {
    return Math.clamp(chance * 2F, 0F, 1F);
  }

  public static float dropChance(net.minecraft.world.entity.Entity entity, float chance) {
    return entity instanceof net.minecraft.world.entity.monster.Zombie
        && entity.level() instanceof ServerLevel level && isBloodMoonDay(level)
        ? doubledChance(chance) : chance;
  }

  /**
   * 血月功能是否开启（对应 gamerule craftingDeadBloodMoon）。
   */
  public static boolean isEnabled(ServerLevel level) {
    return level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
        && level.getGameRules().getBoolean(SurvivalGameRules.BLOOD_MOON);
  }

  /**
   * 血月周期天数（对应 gamerule craftingDeadBloodMoonCycleDays），始终限制在合法范围内。
   */
  public static int cycleDays(ServerLevel level) {
    return Math.clamp(level.getGameRules().getInt(SurvivalGameRules.BLOOD_MOON_CYCLE_DAYS),
        MIN_CYCLE_DAYS, MAX_CYCLE_DAYS);
  }

  /**
   * 当前世界已经经过的天数（从世界创建起）。
   */
  public static long currentDay(ServerLevel level) {
    return level.getDayTime() / TICKS_PER_DAY;
  }

  /**
   * 当前处于周期中的第几天（0 到 cycleDays - 1）。
   */
  public static int dayOfCycle(ServerLevel level) {
    return Math.floorMod(currentDay(level), cycleDays(level));
  }

  /**
   * 今天是否为血月天（周期的最后一天）。
   */
  public static boolean isBloodMoonDay(ServerLevel level) {
    if (!isEnabled(level)) return false;
    long day = currentDay(level);
    BloodMoonState state = BloodMoonState.get(level);
    if (state.stoppedDay == day) return false;
    int chance = Math.clamp(level.getGameRules().getInt(
        SurvivalGameRules.BLOOD_MOON_RANDOM_CHANCE), 0, 100);
    return state.forcedDay == day || scheduled(level.getSeed(), day, cycleDays(level), chance);
  }

  public static boolean scheduled(long seed, long day, int cycle, int chance) {
    return Math.floorMod(day, cycle) == cycle - 1
        || new Random(seed ^ day * OMEN_SEED_MULTIPLIER ^ 0x42B10D).nextInt(100) < chance;
  }

  public static boolean nightTime(long time) {
    long tick = Math.floorMod(time, TICKS_PER_DAY);
    return tick >= 13000 && tick < 23000;
  }

  /**
   * 当前是否处于血月夜（血月天的夜晚时段）。
   */
  public static boolean isBloodMoonNight(ServerLevel level) {
    return isBloodMoonDay(level) && nightTime(level.getDayTime());
  }

  /**
   * 距离下一个血月天还有几天；血月当天为 0，功能关闭时返回 -1。
   */
  public static int daysUntilBloodMoon(ServerLevel level) {
    if (!isEnabled(level)) {
      return -1;
    }
    if (isBloodMoonDay(level)) return 0;
    int chance = Math.clamp(level.getGameRules().getInt(
        SurvivalGameRules.BLOOD_MOON_RANDOM_CHANCE), 0, 100);
    for (int days = 1; days <= cycleDays(level); days++)
      if (scheduled(level.getSeed(), currentDay(level) + days, cycleDays(level), chance)) return days;
    return cycleDays(level);
  }

  /**
   * 今天是否为预兆日（血月前天空变暗的日子）。
   *
   * <p>只有非血月天会判定预兆；每天用（世界种子 ^ 天数）做随机源，按预兆概率
   * （gamerule craftingDeadBloodMoonOmenChance，百分比）掷一次骰，结果确定且可复现。</p>
   */
  public static boolean isOmenDay(ServerLevel level) {
    if (!isEnabled(level) || isBloodMoonDay(level)) {
      return false;
    }
    int chance = Math.clamp(
        level.getGameRules().getInt(SurvivalGameRules.BLOOD_MOON_OMEN_CHANCE), 0, 100);
    if (chance <= 0) {
      return false;
    }
    long day = currentDay(level);
    long seed = level.getSeed() ^ (day * OMEN_SEED_MULTIPLIER);
    return new Random(seed).nextInt(100) < chance;
  }

  /**
   * 构造发送给客户端的血月状态同步包。
   */
  public static SyncBloodMoonMessage createSyncMessage(ServerLevel level) {
    return new SyncBloodMoonMessage(isBloodMoonDay(level), isOmenDay(level),
        daysUntilBloodMoon(level));
  }
}
