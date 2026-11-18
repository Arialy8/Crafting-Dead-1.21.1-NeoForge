package com.craftingdead.survival.world;

import java.util.Collection;
import java.util.List;
import net.minecraft.world.Difficulty;

/** One-based world days, independent of player age and local regional difficulty. */
public final class ZombieProgression {
  public static final List<String> EARLY = List.of("weak_zombie", "doctor_zombie",
      "miner_zombie", "firefighter_zombie");
  private static final List<String> LATE = List.of("alfa_zombie", "juggernaut_zombie",
      "tank_zombie", "giant_zombie", "rocket_zombie", "thrower_zombie", "sniper_zombie");
  public static long cycle(long time) { return Math.floorDiv(Math.max(0, time), 168000L); }
  public static long day(long time) { return Math.floorMod(Math.floorDiv(Math.max(0, time), 24000L), 7) + 1; }
  public static boolean early(long time, Difficulty difficulty) {
    return day(time) <= 3;
  }
  public static List<String> roster(Collection<String> names, long time, Difficulty difficulty) {
    if (early(time, difficulty)) return EARLY;
    long lateDay = 7;
    return names.stream().filter(name -> day(time) >= lateDay || !LATE.contains(name)).toList();
  }
  public static int meleePercent(long time, Difficulty difficulty) {
    return switch (difficulty) {
      case PEACEFUL -> 0;
      case EASY -> early(time, difficulty) ? 15 : 25;
      case NORMAL -> early(time, difficulty) ? 30 : 40;
      case HARD -> early(time, difficulty) ? 45 : 60;
    };
  }
  public static int armorPercent(long time, Difficulty difficulty) {
    return switch (difficulty) {
      case PEACEFUL -> 0;
      case EASY -> early(time, difficulty) ? 2 : 8;
      case NORMAL -> early(time, difficulty) ? 5 : 20;
      case HARD -> early(time, difficulty) ? 10 : 35;
    };
  }
  public static int fullSetPercent(long time, Difficulty difficulty) {
    if (early(time, difficulty)) return 0;
    return switch (difficulty) {
      case PEACEFUL -> 0;
      case EASY -> 3;
      case NORMAL -> 10;
      case HARD -> 20;
    };
  }
  private ZombieProgression() {}
}
