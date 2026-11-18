package com.craftingdead.core.world.entity.projectile;

import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.particle.GrenadeSmokeParticleData;
import com.craftingdead.core.world.damagesource.ModDamageSource;
import com.craftingdead.core.world.entity.ExplosionSource;
import com.craftingdead.core.world.entity.ModEntityTypes;
import com.craftingdead.core.world.entity.explosion.ModExplosionUtil;
import com.craftingdead.core.world.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class RpgRocket extends ThrowableItemProjectile implements ExplosionSource {

  private static final int MAX_LIFETIME_TICKS = 20 * 10;
  private static final float EXPLOSION_RADIUS = 4.5F;
  private static final GrenadeSmokeParticleData TRAIL_SMOKE =
      new GrenadeSmokeParticleData(0.72F, 0.74F, 0.76F, 1.25F);

  public RpgRocket(EntityType<? extends RpgRocket> type, Level level) {
    super(type, level);
  }

  public RpgRocket(LivingEntity owner, Level level) {
    super(ModEntityTypes.RPG_ROCKET.get(), owner, level);
  }

  @Override
  protected Item getDefaultItem() {
    return ModItems.ROCKET.get();
  }

  @Override
  protected double getDefaultGravity() {
    return 0.0D;
  }

  @Override
  public void tick() {
    super.tick();
    if (this.tickCount > MAX_LIFETIME_TICKS) {
      this.discard();
      return;
    }

    if (this.level().isClientSide()) {
      var movement = this.getDeltaMovement();
      var backwards = movement.lengthSqr() > 1.0E-7D
          ? movement.normalize().scale(-0.42D)
          : movement;
      var tailX = this.getX() + backwards.x;
      var tailY = this.getY() + backwards.y;
      var tailZ = this.getZ() + backwards.z;

      if ((this.tickCount & 1) == 0) {
        this.level().addParticle(TRAIL_SMOKE, tailX, tailY, tailZ,
            0.0D, 0.0D, 0.0D);
      }
      if ((this.tickCount & 3) == 0) {
        this.level().addParticle(ParticleTypes.FLAME, tailX, tailY, tailZ,
            0.0D, 0.0D, 0.0D);
      }
    }
  }

  @Override
  protected void onHit(HitResult hitResult) {
    super.onHit(hitResult);
    if (this.level().isClientSide()) {
      return;
    }

    var owner = this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
    if (owner == null) {
      this.discard();
      return;
    }

    ModExplosionUtil.explode(this.level(), this,
        ModDamageSource.causeUnscaledExplosionDamage(owner),
        this.getX(), this.getY(), this.getZ(), EXPLOSION_RADIUS, false,
        ServerConfig.instance.explosivesFragGrenadeExplosionMode.get());
    this.discard();
  }

  @Override
  public float getDamageMultiplier() {
    return 1.25F;
  }

  @Override
  public double getKnockbackMultiplier() {
    return 1.0D;
  }
}
