package com.craftingdead.survival.world;

import net.minecraft.world.level.GameRules;

public final class SurvivalGameRules {
  public static final GameRules.Key<GameRules.BooleanValue> EXPLOSION_TERRAIN_DAMAGE =
      GameRules.register("craftingDeadExplosionTerrainDamage", GameRules.Category.MISC,
          GameRules.BooleanValue.create(false));
  public static final GameRules.Key<GameRules.BooleanValue> NATURAL_SPECIAL_ZOMBIES =
      GameRules.register("craftingDeadNaturalSpecialZombies", GameRules.Category.SPAWNING,
          GameRules.BooleanValue.create(true));
  public static final GameRules.Key<GameRules.IntegerValue> SPECIAL_ZOMBIE_RATE =
      GameRules.register("craftingDeadSpecialZombieRate", GameRules.Category.SPAWNING,
          GameRules.IntegerValue.create(50));
  public static final GameRules.Key<GameRules.BooleanValue> RARE_SPECIAL_ZOMBIES =
      GameRules.register("craftingDeadRareSpecialZombies", GameRules.Category.SPAWNING,
          GameRules.BooleanValue.create(true));
  public static final GameRules.Key<GameRules.BooleanValue> NIGHT_HORDES =
      GameRules.register("craftingDeadNightHordes", GameRules.Category.SPAWNING,
          GameRules.BooleanValue.create(true));
  public static final GameRules.Key<GameRules.IntegerValue> NIGHT_HORDE_CHANCE =
      GameRules.register("craftingDeadNightHordeChance", GameRules.Category.SPAWNING,
          GameRules.IntegerValue.create(10));
  public static final GameRules.Key<GameRules.BooleanValue> BLOOD_MOON =
      GameRules.register("craftingDeadBloodMoon", GameRules.Category.SPAWNING,
          GameRules.BooleanValue.create(true));
  public static final GameRules.Key<GameRules.IntegerValue> BLOOD_MOON_CYCLE_DAYS =
      GameRules.register("craftingDeadBloodMoonCycleDays", GameRules.Category.SPAWNING,
          GameRules.IntegerValue.create(7));
  public static final GameRules.Key<GameRules.IntegerValue> BLOOD_MOON_OMEN_CHANCE =
      GameRules.register("craftingDeadBloodMoonOmenChance", GameRules.Category.SPAWNING,
          GameRules.IntegerValue.create(25));

  public static void init() {}
  public static final GameRules.Key<GameRules.IntegerValue> BLOOD_MOON_RANDOM_CHANCE =
      GameRules.register("craftingDeadBloodMoonRandomChance", GameRules.Category.SPAWNING,
          GameRules.IntegerValue.create(0));
  public static final GameRules.Key<GameRules.IntegerValue> BLOOD_MOON_WAVE_SECONDS =
      GameRules.register("craftingDeadBloodMoonWaveSeconds", GameRules.Category.SPAWNING,
          GameRules.IntegerValue.create(90));
  public static final GameRules.Key<GameRules.BooleanValue> BLOOD_MOON_NO_SLEEP =
      GameRules.register("craftingDeadBloodMoonNoSleep", GameRules.Category.PLAYER,
          GameRules.BooleanValue.create(true));

  private SurvivalGameRules() {}
}
