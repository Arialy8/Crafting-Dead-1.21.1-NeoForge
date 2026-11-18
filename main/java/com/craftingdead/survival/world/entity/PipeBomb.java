package com.craftingdead.survival.world.entity;

import com.craftingdead.core.world.entity.ExplosionSource;
import com.craftingdead.core.world.entity.explosion.ModExplosionUtil;
import com.craftingdead.core.world.entity.grenade.Grenade;
import com.craftingdead.core.world.item.GrenadeItem;
import com.craftingdead.survival.world.item.SurvivalItems;
import java.util.OptionalInt;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import com.craftingdead.survival.world.SurvivalGameRules;

public class PipeBomb extends Grenade implements ExplosionSource {

  private static final int FUSE_TICKS = 60;

  public PipeBomb(EntityType<? extends Grenade> type, Level level) {
    super(type, level);
  }

  public PipeBomb(Level level) {
    super(SurvivalEntityTypes.PIPE_BOMB.get(), level);
  }

  public PipeBomb(LivingEntity thrower, Level level) {
    super(SurvivalEntityTypes.PIPE_BOMB.get(), thrower, level);
  }

  @Override
  public OptionalInt getMinimumTicksUntilAutoActivation() {
    return OptionalInt.of(FUSE_TICKS);
  }

  @Override
  public void activatedChanged(boolean activated) {
    if (activated && !this.level().isClientSide()) {
      this.kill();
      ModExplosionUtil.explode(this.level(), this, this.createDamageSource(),
          this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 4.0F, false,
          this.level().getGameRules().getBoolean(SurvivalGameRules.EXPLOSION_TERRAIN_DAMAGE)
              ? Explosion.BlockInteraction.DESTROY_WITH_DECAY
              : Explosion.BlockInteraction.KEEP);
    }
  }

  @Override
  public void onGrenadeTick() {
    if (this.tickCount % 6 != 0) {
      return;
    }
    if (this.level().isClientSide()) {
      this.level().addParticle(DustParticleOptions.REDSTONE,
          this.getX(), this.getY() + 0.2D, this.getZ(), 0.0D, 0.0D, 0.0D);
    } else {
      float progress = Math.min(1.0F, this.tickCount / (float) FUSE_TICKS);
      this.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.2F,
          Mth.lerp(progress, 1.0F, 2.0F));
    }
  }

  @Override
  public boolean isAttracting() {
    return true;
  }

  @Override
  public GrenadeItem asItem() {
    return (GrenadeItem) SurvivalItems.ITEMS.get("pipe_bomb").get();
  }

  @Override
  public void onMotionStop(int stopsCount) {}

  @Override
  public float getDamageMultiplier() {
    return 1.0F;
  }

  @Override
  public double getKnockbackMultiplier() {
    return 1.0D;
  }
}
