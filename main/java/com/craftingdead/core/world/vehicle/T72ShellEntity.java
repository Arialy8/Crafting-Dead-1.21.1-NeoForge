package com.craftingdead.core.world.vehicle;

import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.world.damagesource.ModDamageSource;
import com.craftingdead.core.world.entity.explosion.ModExplosionUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.sounds.SoundSource;
import com.craftingdead.core.sounds.ModSoundEvents;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Fast swept 125 mm projectile, retaining the source pack's HE/AP ammunition distinction. */
public final class T72ShellEntity extends ThrowableItemProjectile {

  private static final int MAX_LIFETIME = 160;
  private boolean ricochetedThisHit;
  private boolean flybyPlayed;

  public T72ShellEntity(EntityType<? extends T72ShellEntity> type, Level level) {
    super(type, level);
  }

  public T72ShellEntity(LivingEntity owner, Level level, ItemStack shell) {
    super(VehicleContent.T72_SHELL.get(), owner, level);
    this.setItem(shell.copyWithCount(1));
  }

  @Override
  protected Item getDefaultItem() {
    return VehicleContent.SHELL_125MM_HE.get();
  }

  @Override
  protected double getDefaultGravity() {
    return 0.012D;
  }

  public boolean isArmourPiercing() {
    return this.getItem().is(VehicleContent.SHELL_125MM_AP.get());
  }

  @Override
  public void tick() {
    super.tick();
    if (this.level().isClientSide()) {
      this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(),
          0.0D, 0.0D, 0.0D);
      Player listener = this.level().getNearestPlayer(this, 12.0D);
      if (!this.flybyPlayed && listener != null && this.getDeltaMovement().lengthSqr() > 1.0D
          && listener.distanceToSqr(this) < 64.0D) {
        this.flybyPlayed = true;
        this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
            ModSoundEvents.T72_SHELL_FLYBY.get(), SoundSource.AMBIENT, 1.35F,
            0.92F + this.random.nextFloat() * 0.12F, false);
      }
    }
    if (this.tickCount > MAX_LIFETIME) this.discard();
  }

  @Override
  protected void onHitEntity(EntityHitResult result) {
    if (!this.level().isClientSide() && result.getEntity() instanceof T72B3Entity tank) {
      this.ricochetedThisHit = tank.handleShellImpact(this);
    } else if (!this.level().isClientSide() && this.isArmourPiercing()) {
      Entity owner = this.getOwner();
      if (owner instanceof LivingEntity livingOwner) {
        result.getEntity().hurt(ModDamageSource.gun(livingOwner, false), 80.0F);
      }
    }
    super.onHitEntity(result);
  }

  @Override
  protected void onHit(HitResult result) {
    super.onHit(result);
    if (this.level().isClientSide()) return;
    if (this.ricochetedThisHit) {
      this.ricochetedThisHit = false;
      return;
    }
    LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
    if (owner == null) {
      this.discard();
      return;
    }
    this.emitImpactEffects(result);
    if (this.isArmourPiercing()) {
      ModExplosionUtil.explode(this.level(), this, this.getX(), this.getY(), this.getZ(), 1.25F,
          Explosion.BlockInteraction.DESTROY);
    } else {
      ModExplosionUtil.explode(this.level(), this,
          ModDamageSource.causeUnscaledExplosionDamage(owner), this.getX(), this.getY(), this.getZ(),
          3.3F, false, ServerConfig.instance.explosivesFragGrenadeExplosionMode.get());
    }
    this.discard();
  }

  private void emitImpactEffects(HitResult result) {
    if (!(this.level() instanceof ServerLevel server)) return;
    Vec3 hit = result.getLocation();
    server.sendParticles(ParticleTypes.FLASH, hit.x, hit.y, hit.z, 2,
        0.18D, 0.18D, 0.18D, 0.0D);
    server.sendParticles(ParticleTypes.LARGE_SMOKE, hit.x, hit.y, hit.z,
        this.isArmourPiercing() ? 9 : 22, 0.8D, 0.6D, 0.8D, 0.08D);
    server.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z,
        this.isArmourPiercing() ? 28 : 16, 0.9D, 0.7D, 0.9D, 0.32D);
    if (result instanceof BlockHitResult blockHit) {
      var state = this.level().getBlockState(blockHit.getBlockPos());
      if (!state.isAir()) {
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
            hit.x, hit.y, hit.z, this.isArmourPiercing() ? 32 : 52,
            1.2D, 0.8D, 1.2D, 0.24D);
      }
      if (!this.level().getFluidState(blockHit.getBlockPos()).isEmpty()) {
        server.sendParticles(ParticleTypes.SPLASH, hit.x, hit.y, hit.z,
            36, 1.6D, 1.0D, 1.6D, 0.35D);
        server.sendParticles(ParticleTypes.CLOUD, hit.x, hit.y + 0.5D, hit.z,
            18, 1.0D, 0.4D, 1.0D, 0.12D);
      }
    }
  }

  public void ricochet(Vec3 surfaceNormal) {
    Vec3 velocity = this.getDeltaMovement();
    Vec3 reflected = velocity.subtract(surfaceNormal.scale(2.0D * velocity.dot(surfaceNormal)))
        .scale(0.42D);
    this.setDeltaMovement(reflected);
    this.setPos(this.getX() + reflected.x * 0.08D, this.getY() + reflected.y * 0.08D,
        this.getZ() + reflected.z * 0.08D);
    this.hasImpulse = true;
  }
}
