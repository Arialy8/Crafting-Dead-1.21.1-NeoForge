package com.craftingdead.survival.world;

import com.craftingdead.survival.world.entity.SurvivalEntityTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Keeps Crafting Dead zombie selection separate from vanilla biome spawn weights.
 */
public final class SurvivalZombieSpawning {

  @SubscribeEvent
  public void maintainPopulation(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
    if (event.getServer().getTickCount() % 100 != 0) return;
    var level = event.getServer().overworld();
    var state = BloodMoonState.get(level);
    long cycle = ZombieProgression.cycle(level.getDayTime());
    if (ZombieProgression.day(level.getDayTime()) >= 4 && state.progressionAnnouncedCycle != cycle) {
      state.progressionAnnouncedCycle = cycle;
      state.setDirty();
      event.getServer().getPlayerList().broadcastSystemMessage(
          net.minecraft.network.chat.Component.literal("僵尸难度上升：第 " + (cycle + 1) + " 周期已进入第 4 天后的生存阶段！")
              .withStyle(net.minecraft.ChatFormatting.RED), false);
    }
    if (BloodMoonManager.isBloodMoonDay(level) || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL
        || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) return;
    for (var player : level.players()) {
      if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
      int count = nearbyCount(level, player.position());
      if (count >= 10) continue;
      int wanted = 10 + level.random.nextInt(21);
      for (int attempt = 0; attempt < wanted * 12 && count < wanted; attempt++) {
        int x = player.getBlockX() + level.random.nextInt(97) - 48;
        int z = player.getBlockZ() + level.random.nextInt(97) - 48;
        var check = new net.minecraft.core.BlockPos(x, player.getBlockY(), z);
        if (player.distanceToSqr(x, player.getY(), z) < 16 * 16 || !level.hasChunkAt(check)) continue;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        var pos = new net.minecraft.core.BlockPos(x, y, z);
        if (Math.abs(y - player.getY()) > 32 || !level.getWorldBorder().isWithinBounds(pos)) continue;
        boolean fullNeighbor = level.players().stream().anyMatch(other ->
            !other.isCreative() && !other.isSpectator()
                && Math.abs(other.getX() - x) <= 48 && Math.abs(other.getZ() - z) <= 48
                && nearbyCount(level, other.position()) >= 30);
        if (fullNeighbor) continue;
        var names = ZombieProgression.roster(SurvivalEntityTypes.ZOMBIES.keySet(),
            level.getDayTime(), level.getDifficulty());
        var zombie = SurvivalEntityTypes.ZOMBIES.get(names.get(level.random.nextInt(names.size()))).get().create(level);
        if (zombie == null) continue;
        zombie.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360, 0);
        if (!level.noCollision(zombie) || !level.getFluidState(pos).isEmpty()
            || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) continue;
        zombie.addTag("craftingdead_population");
        zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        if (level.addFreshEntity(zombie)) count++;
      }
    }
  }

  private static int nearbyCount(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 pos) {
    return level.getEntitiesOfClass(Zombie.class, new net.minecraft.world.phys.AABB(
        pos.x - 48, level.getMinBuildHeight(), pos.z - 48,
        pos.x + 48, level.getMaxBuildHeight(), pos.z + 48), Zombie::isAlive).size();
  }

  private static final List<String> BASE_ZOMBIES = List.of(
      "weak_zombie", "advanced_zombie", "fast_zombie");

  private static final List<String> SPECIAL_ZOMBIES = List.of(
      "bounty_hunter_zombie", "desert_raider_zombie", "doctor_zombie",
      "firefighter_zombie", "hazmat_zombie", "miner_zombie", "ninja_zombie",
      "pilot_zombie", "police_zombie", "scout_zombie", "soldier_zombie",
      "swat_zombie", "alfa_zombie", "sniper_zombie", "juggernaut_zombie",
      "tank_zombie", "giant_zombie", "thrower_zombie", "rocket_zombie");

  private static final List<String> RARE_ZOMBIES = List.of(
      "alfa_zombie", "sniper_zombie", "juggernaut_zombie",
      "tank_zombie", "giant_zombie");

  @SubscribeEvent
  public void replaceNaturalZombie(FinalizeSpawnEvent event) {
    if (event.getSpawnType() == MobSpawnType.NATURAL && event.getEntity() instanceof Zombie
        && !BloodMoonManager.isBloodMoonDay(event.getLevel().getLevel())) {
      var serverLevel = event.getLevel().getLevel();
      for (var player : serverLevel.players()) {
        if (Math.abs(player.getX() - event.getX()) <= 48 && Math.abs(player.getZ() - event.getZ()) <= 48
            && nearbyCount(serverLevel, player.position()) >= 30) {
          event.setSpawnCancelled(true);
          return;
        }
      }
    }
    boolean ordinaryModZombie = event.getEntity() instanceof com.craftingdead.survival.world.entity.SurvivalZombie
        && !BloodMoonManager.isBloodMoonDay(event.getLevel().getLevel());
    if ((event.getEntity().getType() != EntityType.ZOMBIE && !ordinaryModZombie)
        || event.getSpawnType() != MobSpawnType.NATURAL) {
      return;
    }

    var level = event.getLevel();
    var rules = level.getLevel().getGameRules();
    var random = level.getRandom();
    List<String> candidates = BASE_ZOMBIES;

    int specialRate = Mth.clamp(rules.getInt(SurvivalGameRules.SPECIAL_ZOMBIE_RATE), 0, 100);
    if (rules.getBoolean(SurvivalGameRules.NATURAL_SPECIAL_ZOMBIES)
        && random.nextInt(100) < specialRate) {
      candidates = new ArrayList<>(SPECIAL_ZOMBIES);
      if (!rules.getBoolean(SurvivalGameRules.RARE_SPECIAL_ZOMBIES)) {
        candidates.removeAll(RARE_ZOMBIES);
      }
    }

    if (!BloodMoonManager.isBloodMoonDay(level.getLevel())) {
      candidates = ZombieProgression.roster(candidates, level.getLevel().getDayTime(),
          level.getLevel().getDifficulty());
      if (candidates.isEmpty()) candidates = ZombieProgression.EARLY;
    }
    String selectedName = candidates.get(random.nextInt(candidates.size()));
    var selectedType = SurvivalEntityTypes.ZOMBIES.get(selectedName);
    if (selectedType == null) {
      return;
    }

    Zombie replacement = selectedType.get().create(level.getLevel());
    if (replacement == null) {
      return;
    }

    var original = event.getEntity();
    replacement.moveTo(event.getX(), event.getY(), event.getZ(),
        original.getYRot(), original.getXRot());
    event.setSpawnCancelled(true);
    replacement.addTag("craftingdead_population");
    replacement.finalizeSpawn(level, event.getDifficulty(), MobSpawnType.EVENT, null);
    level.addFreshEntity(replacement);
  }
}
