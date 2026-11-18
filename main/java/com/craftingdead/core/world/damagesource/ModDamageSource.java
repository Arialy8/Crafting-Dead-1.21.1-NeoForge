/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.world.damagesource;

import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.world.entity.grenade.Grenade;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 1.21.1 damage-source bridge.
 *
 * <p>Forge/Minecraft 1.18 allowed constructing {@link DamageSource} directly from a string and
 * mutating flags with methods such as {@code setProjectile()}, {@code setExplosion()} and
 * {@code bypassArmor()}. In 1.21.x those flags live on data-driven {@link DamageType}s and their
 * tags. Keep all custom Crafting Dead damage creation in this class so gun/grenade/trauma code can
 * migrate gradually.</p>
 */
public class ModDamageSource {

  public static final String BULLET_HEADSHOT_DAMAGE_TYPE = "bullet.headshot";
  public static final String BULLET_BODY_DAMAGE_TYPE = "bullet";
  public static final String BLEEDING = "bleeding";
  public static final String GRENADE = "grenade";
  public static final String UNSCALED_EXPLOSION = "unscaled_explosion";

  public static final ResourceKey<DamageType> BULLET_HEADSHOT = key(BULLET_HEADSHOT_DAMAGE_TYPE);
  public static final ResourceKey<DamageType> BULLET_BODY = key(BULLET_BODY_DAMAGE_TYPE);
  public static final ResourceKey<DamageType> BLEEDING_TYPE = key(BLEEDING);
  public static final ResourceKey<DamageType> GRENADE_TYPE = key(GRENADE);
  public static final ResourceKey<DamageType> UNSCALED_EXPLOSION_TYPE = key(UNSCALED_EXPLOSION);

  private static ResourceKey<DamageType> key(String path) {
    return ResourceKey.create(Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, path));
  }

  private static DamageSource source(Level level, ResourceKey<DamageType> key) {
    return new DamageSource(level.registryAccess()
        .registryOrThrow(Registries.DAMAGE_TYPE)
        .getHolderOrThrow(key));
  }

  private static DamageSource source(Level level, ResourceKey<DamageType> key,
      @Nullable Entity directEntity, @Nullable Entity causingEntity) {
    return new DamageSource(level.registryAccess()
        .registryOrThrow(Registries.DAMAGE_TYPE)
        .getHolderOrThrow(key), directEntity, causingEntity);
  }

  public static DamageSource gun(LivingEntity source, boolean headshot) {
    return source(source.level(), headshot ? BULLET_HEADSHOT : BULLET_BODY, source, source);
  }

  public static DamageSource grenade(Grenade grenade, @Nullable Entity thrower) {
    return source(grenade.level(), GRENADE_TYPE, grenade, thrower);
  }

  public static DamageSource bleeding(LivingEntity victim) {
    return source(victim.level(), BLEEDING_TYPE);
  }

  /**
   * Temporary migration helper for old call sites that have not yet been given an entity context.
   * Prefer {@link #bleeding(LivingEntity)} when editing nearby code.
   */
  @Deprecated(forRemoval = true)
  public static DamageSource bleeding() {
    throw new IllegalStateException("Use ModDamageSource.bleeding(LivingEntity) on 1.21.1");
  }

  /**
   * Creates an explosion damage source without difficulty scaling.
   */
  public static DamageSource causeUnscaledExplosionDamage(@Nullable LivingEntity source) {
    if (source == null) {
      throw new IllegalArgumentException("1.21.1 damage sources require level context");
    }
    return source(source.level(), UNSCALED_EXPLOSION_TYPE, source, source);
  }

  /**
   * Hurt an entity without doing knockback.
   */
  public static boolean hurtWithoutKnockback(Entity victim, DamageSource source, float amount) {
    if (victim instanceof LivingEntity livingHit) {
      var livingResistance = livingHit.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (livingResistance == null) {
        return livingHit.hurt(source, amount);
      }
      var previousResistance = livingResistance.getBaseValue();

      livingResistance.setBaseValue(Integer.MAX_VALUE);
      var attackResult = livingHit.hurt(source, amount);

      livingResistance.setBaseValue(previousResistance);
      return attackResult;
    }
    return victim.hurt(source, amount);
  }
}
