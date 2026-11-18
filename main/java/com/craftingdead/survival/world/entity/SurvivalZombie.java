package com.craftingdead.survival.world.entity;

import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.equipment.ClothingProtection;
import com.craftingdead.survival.world.effect.SurvivalMobEffects;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.ammoprovider.RefillableAmmoProvider;
import com.craftingdead.core.world.item.StorageItem;
import com.craftingdead.core.world.item.StorageStackDataComponentUtil;
import com.craftingdead.core.util.ItemHandlerNbtUtil;
import com.craftingdead.core.world.entity.ai.FollowAttractiveGrenadeGoal;
import com.craftingdead.survival.CraftingDeadSurvival;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class SurvivalZombie extends Zombie implements RangedAttackMob {

  private RangedAttackGoal gunAttackGoal;
  private long triggerReleaseTick;
  private long lastGunDamageTick = Long.MIN_VALUE;

  public SurvivalZombie(EntityType<? extends Zombie> type, Level level) {
    super(type, level);
  }

  @Override
  public boolean convertsInWater() {
    return false;
  }

  @Override
  protected boolean isSunSensitive() {
    return false;
  }

  @Override
  protected void registerGoals() {
    super.registerGoals();
    RangedCombatProfile rangedProfile = this.rangedCombatProfile();
    this.gunAttackGoal = new RangedAttackGoal(this, 1.0D,
        rangedProfile.attackInterval(), rangedProfile.attackRadius()) {
      @Override
      public boolean canUse() {
        return super.canUse() && !SurvivalZombie.this.specialAttack()
            && Gun.get(SurvivalZombie.this.getMainHandItem()).isPresent();
      }
    };
    if (!this.specialAttack()) this.goalSelector.addGoal(1, new FollowAttractiveGrenadeGoal(this, 1.15D));
    this.goalSelector.addGoal(2, this.gunAttackGoal);
  }

  @Override
  public void tick() {
    super.tick();
    if (!this.level().isClientSide() && this.isAlive() && !this.isNoAi()) this.tickSpecialAttack();
    if (!this.level().isClientSide() && this.tickCount % 20 == 0
        && !this.getTags().contains("craftingdead_loadout_v3")) ZombieLoadouts.equip(this, true);
    if (!this.level().isClientSide() && this.triggerReleaseTick > 0
        && this.level().getGameTime() >= this.triggerReleaseTick) {
      var extension = LivingExtension.get(this);
      if (extension != null) {
        extension.mainHandGun().ifPresent(gun ->
            gun.setNPCTriggerPressed(extension, false, true, this.gunAccuracy()));
      }
      this.triggerReleaseTick = 0;
    }
  }

  @Override
  public void performRangedAttack(LivingEntity target, float distanceFactor) {
    if (this.level().isClientSide()) {
      return;
    }
    var extension = LivingExtension.get(this);
    if (extension != null) {
      extension.mainHandGun().ifPresent(gun -> {
        gun.setNPCTriggerPressed(extension, true, true, this.gunAccuracy());
        this.triggerReleaseTick = this.level().getGameTime() + 1;
      });
    }
  }

  @Override
  public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
    if (this.specialAttack() && (this.getMainHandItem().getItem() instanceof com.craftingdead.core.world.item.GunItem
        || this.getMainHandItem().getItem() instanceof com.craftingdead.core.world.item.GrenadeItem)) return false;
    boolean hurt = super.doHurtTarget(target);
    if (!hurt || this.level().isClientSide() || !(target instanceof LivingEntity livingTarget)
        || livingTarget instanceof Zombie
        || livingTarget.hasEffect(SurvivalMobEffects.holder(SurvivalMobEffects.INFECTION))) {
      return hurt;
    }
    float biteProtection = 0.0F;
    if (livingTarget instanceof Player player) {
      var extension = LivingExtension.get(player);
      biteProtection = extension == null ? 0.0F
          : ClothingProtection.getBiteProtection(
              extension.getItemInSlot(Equipment.Slot.CLOTHING));
    }
    float chance = ClothingProtection.calculateInfectionChance(0.15F, biteProtection);
    if (this.random.nextFloat() < chance) {
      livingTarget.addEffect(new MobEffectInstance(
          SurvivalMobEffects.holder(SurvivalMobEffects.INFECTION), 20 * 60 * 5));
      if (livingTarget instanceof Player player) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
            "message.craftingdeadsurvival.infection.applied"), true);
      }
    }
    return hurt;
  }

  @Override
  public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
      MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
    SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
    if (spawnType == MobSpawnType.NATURAL) this.addTag("craftingdead_population");
    this.equipProfession();
    return result;
  }

  private void equipProfession() {
    ZombieLoadouts.equip(this, false);
  }

  @Override protected float getEquipmentDropChance(EquipmentSlot slot) {
    return ZombieLoadouts.dropChance(this, this.getItemBySlot(slot), null,
        super.getEquipmentDropChance(slot));
  }

  private final com.craftingdead.survival.world.FixedRocketLock rocketLock =
      new com.craftingdead.survival.world.FixedRocketLock();
  private long nextSpecialAttack;

  private boolean specialAttack() {
    String type = ZombieLoadouts.type(this);
    return type.equals("rocket_zombie") || type.equals("thrower_zombie");
  }

  private void tickSpecialAttack() {
    if (!this.specialAttack()) return;
    long now = this.level().getGameTime();
    if (rocketLock.point() != null) {
      this.getNavigation().stop();
      this.getLookControl().setLookAt(rocketLock.point().x, rocketLock.point().y, rocketLock.point().z);
      if (rocketLock.ready(now)) {
        var rocket = new com.craftingdead.core.world.entity.projectile.RpgRocket(this, this.level());
        var direction = rocketLock.point().subtract(rocket.position());
        rocket.shoot(direction.x, direction.y, direction.z, 1.5F, 0F);
        this.level().addFreshEntity(rocket);
        rocketLock.clear();
        nextSpecialAttack = now + 160;
      }
      return;
    }
    LivingEntity target = this.getTarget();
    if (!(target instanceof net.minecraft.server.level.ServerPlayer) || !target.isAlive()
        || now < nextSpecialAttack || !this.hasLineOfSight(target)) return;
    if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return;
    String type = ZombieLoadouts.type(this);
    if (type.equals("rocket_zombie") && this.distanceToSqr(target) <= 40 * 40
        && this.getMainHandItem().is(ModItems.RPG.get())) {
      rocketLock.arm(target.position().add(0, 0.5, 0), now);
      this.getNavigation().stop();
      if (target instanceof net.minecraft.server.level.ServerPlayer player)
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("你已被火箭筒僵尸锁定!")
            .withStyle(net.minecraft.ChatFormatting.RED), true);
    } else if (type.equals("thrower_zombie") && this.distanceToSqr(target) <= 10 * 10
        && this.getMainHandItem().getItem() instanceof com.craftingdead.core.world.item.GrenadeItem item) {
      var grenade = item.createForMob(this, this.level());
      if (grenade != null) {
        grenade.setPos(this.getX(), this.getEyeY(), this.getZ());
        var direction = target.position().add(0, 0.5, 0).subtract(grenade.position());
        grenade.shoot(direction.x, direction.y + direction.horizontalDistance() * 0.24,
            direction.z, 0.85F, 0F);
        this.level().addFreshEntity(grenade);
      }
      nextSpecialAttack = now + 100;
    }
  }

  @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
    boolean result = super.hurt(source, amount);
    if (result && !this.level().isClientSide() && this.isAlive()
        && this.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) >= 0.33)
      this.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
    return result;
  }

  private float gunAccuracy() {
    String type = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
    return switch (type) {
      case "sniper_zombie" -> 0.88F;
      case "soldier_zombie", "swat_zombie", "alfa_zombie" -> 0.72F;
      case "desert_raider_zombie", "pilot_zombie", "police_zombie" -> 0.62F;
      case "juggernaut_zombie", "giant_zombie" -> 0.5F;
      default -> 0.6F;
    };
  }

  public float consumeNpcGunDamage() {
    long now = this.level().getGameTime();
    int cooldown = this.isSniper() ? 100 : 40;
    if (this.lastGunDamageTick != Long.MIN_VALUE
        && now - this.lastGunDamageTick < cooldown) {
      return 0.0F;
    }
    this.lastGunDamageTick = now;
    return this.isSniper() ? 14.0F : 4.0F;
  }

  private boolean isSniper() {
    return BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath()
        .equals("sniper_zombie");
  }

  private RangedCombatProfile rangedCombatProfile() {
    String type = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
    return switch (type) {
      case "sniper_zombie" -> new RangedCombatProfile(100, 32.0F);
      case "alfa_zombie", "desert_raider_zombie", "soldier_zombie", "swat_zombie",
          "pilot_zombie", "scout_zombie", "juggernaut_zombie" ->
          new RangedCombatProfile(40, 24.0F);
      case "police_zombie", "bounty_hunter_zombie" ->
          new RangedCombatProfile(30, 16.0F);
      default -> new RangedCombatProfile(30, 20.0F);
    };
  }

  private record RangedCombatProfile(int attackInterval, float attackRadius) {}
}
