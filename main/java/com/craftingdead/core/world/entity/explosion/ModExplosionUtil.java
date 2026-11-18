package com.craftingdead.core.world.entity.explosion;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

/**
 * Central adapter for explosion calls during the 1.18.2 -> 1.21.1 migration.
 */
public final class ModExplosionUtil {

  private ModExplosionUtil() {}

  private static Level.ExplosionInteraction toExplosionInteraction(
      Explosion.BlockInteraction blockInteraction) {
    if (blockInteraction == null) {
      return Level.ExplosionInteraction.NONE;
    }
    return switch (blockInteraction) {
      case DESTROY -> Level.ExplosionInteraction.BLOCK;
      case DESTROY_WITH_DECAY -> Level.ExplosionInteraction.TNT;
      case TRIGGER_BLOCK -> Level.ExplosionInteraction.TRIGGER;
      case KEEP -> Level.ExplosionInteraction.NONE;
    };
  }

  public static void explode(Level level, Entity source, DamageSource damageSource,
      double x, double y, double z, float radius, boolean fire,
      Explosion.BlockInteraction blockInteraction) {
    level.explode(source, damageSource, null, x, y, z, radius, fire,
        toExplosionInteraction(blockInteraction));
  }

  public static void explode(Level level, Entity source,
      double x, double y, double z, float radius,
      Explosion.BlockInteraction blockInteraction) {
    level.explode(source, x, y, z, radius, toExplosionInteraction(blockInteraction));
  }
}
