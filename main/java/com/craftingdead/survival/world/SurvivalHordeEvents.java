package com.craftingdead.survival.world;

import com.craftingdead.survival.world.entity.SurvivalEntityTypes;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class SurvivalHordeEvents {

  private static final int CHECK_INTERVAL = 20 * 60 * 20;
  private static final List<String> HORDE_TYPES = List.of(
      "weak_zombie", "advanced_zombie", "fast_zombie");

  /**
   * 在玩家周围生成一波尸潮。测试指令与正常夜晚判定共用。
   *
   * @param bloodMoonNight 是否为血月级尸潮（规模更大、精英概率更高）
   */
  public static void spawnHorde(ServerLevel level, ServerPlayer player, boolean bloodMoonNight) {
    if (bloodMoonNight) {
      spawnBloodWave(level, player, 7 + player.getRandom().nextInt(8));
      return;
    }
    int count = (bloodMoonNight ? 6 : 2) + player.getRandom().nextInt(bloodMoonNight ? 5 : 4);
    for (int i = 0; i < count; i++) {
      String name = HORDE_TYPES.get(player.getRandom().nextInt(HORDE_TYPES.size()));
      spawnZombie(level, player, name);
    }
    float soldierChance = bloodMoonNight ? 0.01F : 0.005F;
    if (player.getRandom().nextFloat() < soldierChance) {
      spawnZombie(level, player, "soldier_zombie");
    }
    float sniperChance = bloodMoonNight ? 0.002F : 0.001F;
    if (player.getRandom().nextFloat() < sniperChance) {
      spawnZombie(level, player, "sniper_zombie");
    }
  }

  public static int spawnBloodWave(ServerLevel level, ServerPlayer player, int requested) {
    if (level.getDifficulty() == Difficulty.PEACEFUL
        || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) return 0;
    int loaded = 0;
    for (var entity : level.getAllEntities())
      if (entity.isAlive() && entity.getTags().contains("craftingdead_bloodmoon")) loaded++;
    int allowed = Math.min(Math.clamp(requested, 1, 100), Math.max(0, 240 - loaded));
    var names = List.copyOf(SurvivalEntityTypes.ZOMBIES.keySet());
    int spawned = 0;
    // Retry blocked terrain, but never generate chunks or place inside solid blocks.
    for (int attempt = 0; attempt < allowed * 16 && spawned < allowed; attempt++) {
      if (spawnZombie(level, player, names.get(player.getRandom().nextInt(names.size())), true)) spawned++;
    }
    return spawned;
  }

  private static boolean spawnZombie(ServerLevel level, ServerPlayer player, String name) {
    return spawnZombie(level, player, name, false);
  }

  private static boolean spawnZombie(ServerLevel level, ServerPlayer player, String name, boolean blood) {
      if (level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(64)).size() >= 80)
        return false;
      EntityType<Zombie> type = SurvivalEntityTypes.ZOMBIES.get(name).get();
      Zombie zombie = type.create(level);
      if (zombie == null) {
        return false;
      }
      double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
      int distance = 24 + player.getRandom().nextInt(13);
      int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
      int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
      if (!level.hasChunkAt(new net.minecraft.core.BlockPos(x, player.getBlockY(), z))) return false;
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      zombie.moveTo(x + 0.5D, y, z + 0.5D,
          player.getRandom().nextFloat() * 360.0F, 0.0F);
      if (!level.getWorldBorder().isWithinBounds(zombie.blockPosition())
          || !level.noCollision(zombie)
          || !level.getFluidState(zombie.blockPosition()).isEmpty()
          || !level.getBlockState(zombie.blockPosition().below()).isFaceSturdy(
              level, zombie.blockPosition().below(), net.minecraft.core.Direction.UP)) return false;
      zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()),
          MobSpawnType.EVENT, null);
      if (blood) {
        zombie.addTag("craftingdead_bloodmoon");
        zombie.setTarget(player);

      }
      return level.addFreshEntity(zombie);
  }

  private static void boost(Zombie zombie,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      double multiplier) {
    var value = zombie.getAttribute(attribute);
    if (value != null) value.setBaseValue(value.getBaseValue() * multiplier);
  }
}
