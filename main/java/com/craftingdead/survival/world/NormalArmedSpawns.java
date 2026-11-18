package com.craftingdead.survival.world;

import com.craftingdead.core.world.item.GunItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;

/** Ordinary population only; blood moon waves and explicit admin summons retain their rules. */
public final class NormalArmedSpawns {
  public static boolean allowed(boolean night, double distanceSquared, int nearbyArmed) {
    return night && distanceSquared >= 30 * 30 && nearbyArmed < 2;
  }
  public static boolean canArm(Zombie zombie) {
    if (!(zombie.level() instanceof ServerLevel level)
        || !zombie.getTags().contains("craftingdead_population")
        || BloodMoonManager.isBloodMoonDay(level)) return true;
    if (!BloodMoonManager.nightTime(level.getDayTime())) return false;
    for (var player : level.players()) {
      if (player.isCreative() || player.isSpectator()) continue;
      double dx = player.getX() - zombie.getX(), dz = player.getZ() - zombie.getZ();
      var area = new AABB(player.getX() - 48, level.getMinBuildHeight(), player.getZ() - 48,
          player.getX() + 48, level.getMaxBuildHeight(), player.getZ() + 48);
      int count = level.getEntitiesOfClass(Zombie.class, area,
          mob -> mob != zombie && mob.isAlive() && mob.getMainHandItem().getItem() instanceof GunItem).size();
      if (Math.abs(dx) <= 48 && Math.abs(dz) <= 48
          && !allowed(true, dx * dx + dz * dz, count)) return false;
    }
    return true;
  }
  private NormalArmedSpawns() {}
}
