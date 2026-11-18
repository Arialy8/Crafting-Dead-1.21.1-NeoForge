package com.craftingdead.core.world.vehicle;

import com.mojang.logging.LogUtils;
import com.craftingdead.core.sounds.ModSoundEvents;
import com.craftingdead.core.util.RayTraceUtil;
import com.craftingdead.core.world.damagesource.ModDamageSource;
import com.craftingdead.core.world.entity.grenade.SmokeGrenadeEntity;
import com.craftingdead.core.world.entity.explosion.ModExplosionUtil;
import com.craftingdead.core.world.item.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.ClipContext;
import org.slf4j.Logger;

/**
 * A deliberately small, server-authoritative tracked vehicle foundation.
 * Advanced weapons, storage and module fitting will build on this entity after its
 * spawn/mount/movement lifecycle is proven in-game.
 */
public final class T72B3Entity extends Entity implements MenuProvider {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final int MAX_INTEGRITY = 200;
  private static final int MAX_FUEL = 15000;
  private static final int MAIN_RELOAD_TICKS = 140;
  private static final int COAX_FIRE_DELAY = 2;
  private static final int SMOKE_FIRE_DELAY = 80;
  private static final int COAX_CAPACITY = 400;
  private static final int COAX_RELOAD_TICKS = 120;
  private static final int SMOKE_CAPACITY = 8;
  private static final int SMOKE_RELOAD_TICKS = 15;
  private static final int ACTION_MAIN_GUN = 1;
  private static final int ACTION_COAX = 2;
  private static final int ACTION_SMOKE = 4;
  private static final int ACTION_SWITCH_AMMO = 8;
  private static final int ACTION_RELOAD = 16;
  private static final int ACTION_HEADLIGHTS = 32;
  private static final int ACTION_TOGGLE_GUN_CONTROL = 64;
  private static final int ACTION_TOGGLE_EXPOSURE = 128;
  // These match the scaled original LAV hull. Block collision is split into tapered sections so
  // the long vehicle does not inherit the false corner collisions of one giant vanilla AABB.
  private static final double HULL_HALF_WIDTH = 2.40D;
  private static final double HULL_HALF_LENGTH = 4.60D;
  private static final double HULL_DECK_HEIGHT = 1.0D;
  // Entity/player collision intentionally remains one block high, but walls must be tested
  // against the visible upper vehicle too or the turret and canopy appear to phase through them.
  private static final double VEHICLE_BLOCK_COLLISION_HEIGHT = 3.35D;
  private static final double HULL_CLEARANCE = 0.08D;
  private static final double MAX_STEP_HEIGHT = 2.02D;
  private static final double MAX_FORWARD_SPEED_RATE = 1.25D;
  private static final double MAX_BACKWARD_SPEED_RATE = 0.75D;
  private static final float POWER_INCREMENT = 0.0026F;
  private static final float POWER_DECREMENT = 0.002F;
  private static final float STEERING_SPEED = 0.1F;
  private static final double TRACKED_GRAVITY = 0.13D;
  private static final double VERTICAL_AIR_DRAG = 0.99D;
  private static final double MAX_FALL_SPEED = -3.20D;
  private static final int ENGINE_IDLE_RPM = 1000;
  private static final int ENGINE_MAX_RPM = 8500;
  private static final int ENGINE_SAFE_RPM = 6400;
  private static final int ENGINE_REV_LIMIT_RPM = 5810;
  private static final int ENGINE_STALL_RPM = 325;
  private static final int ENGINE_SHIFT_TICKS = 5;
  private static final double[] FORWARD_GEAR_RATIOS = {0.0D, 3.0D, 1.55D, 1.0D, 0.71D, 0.56D};
  private static final HullSection[] HULL_SECTIONS = {
      new HullSection(0.0D, 3.65D, 1.88D, 0.95D),
      new HullSection(0.0D, 2.05D, 2.25D, 0.85D),
      new HullSection(0.0D, 0.15D, 2.40D, 1.15D),
      new HullSection(0.0D, -2.00D, 2.35D, 1.10D),
      new HullSection(0.0D, -3.75D, 2.18D, 0.72D)
  };
  /** Exact body cells from the original LAV block-collision group, in rendered world scale. */
  private static final SourceHullBox[] SOURCE_HULL_BOXES = {
      new SourceHullBox(-1.11128D, 1.43924D, 3.60343D, 0.60000D, 0.60000D),
      new SourceHullBox(-0.01825D, 1.43924D, 3.60343D, 0.60000D, 0.60000D),
      new SourceHullBox(1.10569D, 1.43924D, 3.60343D, 0.60000D, 0.60000D),
      new SourceHullBox(-1.11261D, 1.64249D, 2.45816D, 0.60000D, 0.60000D),
      new SourceHullBox(1.10569D, 1.64249D, 2.45816D, 0.60000D, 0.60000D),
      new SourceHullBox(-2.34054D, 1.63819D, 4.46265D, 0.60000D, 0.42000D),
      new SourceHullBox(-2.34054D, 1.76992D, 3.25571D, 0.60000D, 0.42000D),
      new SourceHullBox(-2.34054D, 1.67969D, 2.04637D, 0.60000D, 0.50400D),
      new SourceHullBox(-2.33616D, 1.73366D, 0.83731D, 0.60000D, 0.50400D),
      new SourceHullBox(-2.34054D, 1.73366D, -0.36963D, 0.60000D, 0.50400D),
      new SourceHullBox(-2.34054D, 1.73366D, -1.57896D, 0.60000D, 0.50400D),
      new SourceHullBox(-2.34054D, 1.73366D, -2.78949D, 0.60000D, 0.50400D),
      new SourceHullBox(-2.34054D, 1.73366D, -4.01266D, 0.60000D, 0.50400D),
      new SourceHullBox(-1.11705D, 1.64249D, -1.76900D, 0.60000D, 0.60000D),
      new SourceHullBox(0.00251D, 1.64249D, -1.76900D, 0.60000D, 0.60000D),
      new SourceHullBox(1.10569D, 1.64249D, -1.76900D, 0.60000D, 0.60000D),
      new SourceHullBox(-1.11705D, 1.69296D, -2.99089D, 0.60000D, 0.60000D),
      new SourceHullBox(0.00251D, 1.69296D, -2.99089D, 0.60000D, 0.60000D),
      new SourceHullBox(1.10569D, 1.69296D, -2.99089D, 0.60000D, 0.60000D),
      new SourceHullBox(-1.11705D, 1.69296D, -4.20794D, 0.60000D, 0.60000D),
      new SourceHullBox(0.00251D, 1.69296D, -4.20794D, 0.60000D, 0.60000D),
      new SourceHullBox(1.10569D, 1.69296D, -4.20794D, 0.60000D, 0.60000D),
      new SourceHullBox(0.00869D, 0.92159D, -2.50921D, 1.62000D, 0.26611D),
      new SourceHullBox(0.00869D, 0.92159D, -0.10314D, 1.62000D, 0.26611D),
      new SourceHullBox(0.00869D, 0.92159D, 2.43010D, 1.62000D, 0.26611D),
      new SourceHullBox(2.32545D, 1.63819D, 4.46265D, 0.60000D, 0.42000D),
      new SourceHullBox(2.32545D, 1.76992D, 3.25571D, 0.60000D, 0.42000D),
      new SourceHullBox(2.30860D, 1.67969D, 2.04637D, 0.60000D, 0.50400D),
      new SourceHullBox(2.31296D, 1.73366D, 0.83731D, 0.60000D, 0.50400D),
      new SourceHullBox(2.30860D, 1.73366D, -0.36963D, 0.60000D, 0.50400D),
      new SourceHullBox(2.30860D, 1.73366D, -1.57896D, 0.60000D, 0.50400D),
      new SourceHullBox(2.30860D, 1.73366D, -2.78949D, 0.60000D, 0.50400D),
      new SourceHullBox(2.30860D, 1.73366D, -4.01266D, 0.60000D, 0.50400D)
  };
  private static final int CARGO_SIZE = 27;
  private static final EntityDataAccessor<Integer> INTEGRITY = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> TURRET_PITCH = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Integer> DRIVER_ID = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> LEFT_REAR_ID = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> RIGHT_REAR_ID = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> WEAPON_CONTROLLER_ID = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> LEFT_REAR_EXPOSED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Boolean> RIGHT_REAR_EXPOSED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Float> ENGINE_POWER = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> LEFT_TRACK_ROTATION = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> RIGHT_TRACK_ROTATION = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Boolean> LEFT_TRACK_INSTALLED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Boolean> RIGHT_TRACK_INSTALLED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Float> BODY_PITCH = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> BODY_ROLL = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> SUSPENSION_OFFSET = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Boolean> ENGINE_INSTALLED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Boolean> TURRET_INSTALLED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Boolean> SLAT_ARMOUR_INSTALLED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> MAIN_RELOAD = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> SELECTED_AMMO = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> ENGINE_HEALTH = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> TURRET_HEALTH = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> LEFT_TRACK_HEALTH = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> RIGHT_TRACK_HEALTH = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> SLAT_HEALTH = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Float> RECOIL = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> HE_AMMO = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> AP_AMMO = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> COAX_AMMO = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> SMOKE_AMMO = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> HEADLIGHTS = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Integer> COAX_LOADED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> COAX_RELOAD = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> SMOKE_LOADED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> SMOKE_RELOAD = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> ENGINE_RPM = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> ENGINE_GEAR = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> ENGINE_TEMPERATURE = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> BATTERY = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> ENGINE_RUNNING = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Float> ENGINE_CLUTCH = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Boolean> DESTROYED = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Integer> WRECK_AGE = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Float> WRECK_TURRET_YAW = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> WRECK_TURRET_ROLL = SynchedEntityData.defineId(
      T72B3Entity.class, EntityDataSerializers.FLOAT);

  private SimpleContainer cargo = new SimpleContainer(CARGO_SIZE);
  private float driverThrottle;
  private float driverTurn;
  private float driverLookYaw;
  private float driverLookPitch;
  private int driverActions;
  private int previousDriverActions;
  private int coaxCooldown;
  private int smokeCooldown;
  private boolean engineWasRunning;
  private float lastTurretYawForSound;
  private boolean turretWasMoving;
  private final Set<Integer> gunControlToggleHeld = new HashSet<>();
  private final Set<Integer> exposureToggleHeld = new HashSet<>();
  private final Set<Integer> internalDamageVictims = new HashSet<>();
  private final Map<Integer, Long> lastCollisionDamageTick = new HashMap<>();
  private long lastDriveBlockNotice = -100L;
  private long lastDriverInputTick = -100L;
  private long lastWeaponInputTick = -100L;
  private boolean reportedDriverInput;
  private boolean reportedDriverMovement;
  private double driveVelocity;
  private double verticalVelocity;
  private float deltaRotation;
  private int steeringHoldTicks;
  private int automaticGear = 1;
  private int shiftTicks;
  private float engineRpmState;
  private int clientInterpolationSteps;
  private double clientTargetX;
  private double clientTargetY;
  private double clientTargetZ;
  private float clientTargetYaw;
  private float clientTargetPitch;
  private double suspensionOffset;
  private double suspensionVelocity;

  public T72B3Entity(EntityType<? extends T72B3Entity> type, Level level) {
    super(type, level);
    this.blocksBuilding = true;
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(INTEGRITY, MAX_INTEGRITY);
    builder.define(TURRET_YAW, 0.0F);
    builder.define(TURRET_PITCH, 0.0F);
    builder.define(DRIVER_ID, -1);
    builder.define(LEFT_REAR_ID, -1);
    builder.define(RIGHT_REAR_ID, -1);
    builder.define(WEAPON_CONTROLLER_ID, -1);
    builder.define(LEFT_REAR_EXPOSED, false);
    builder.define(RIGHT_REAR_EXPOSED, false);
    builder.define(ENGINE_POWER, 0.0F);
    builder.define(LEFT_TRACK_ROTATION, 0.0F);
    builder.define(RIGHT_TRACK_ROTATION, 0.0F);
    builder.define(LEFT_TRACK_INSTALLED, false);
    builder.define(RIGHT_TRACK_INSTALLED, false);
    builder.define(BODY_PITCH, 0.0F);
    builder.define(BODY_ROLL, 0.0F);
    builder.define(SUSPENSION_OFFSET, 0.0F);
    builder.define(ENGINE_INSTALLED, true);
    builder.define(TURRET_INSTALLED, true);
    builder.define(SLAT_ARMOUR_INSTALLED, false);
    builder.define(FUEL, MAX_FUEL);
    builder.define(MAIN_RELOAD, 0);
    builder.define(SELECTED_AMMO, 0);
    builder.define(ENGINE_HEALTH, 50);
    builder.define(TURRET_HEALTH, 400);
    builder.define(LEFT_TRACK_HEALTH, 70);
    builder.define(RIGHT_TRACK_HEALTH, 70);
    builder.define(SLAT_HEALTH, 300);
    builder.define(RECOIL, 0.0F);
    builder.define(SKIN, 0);
    builder.define(HE_AMMO, 0);
    builder.define(AP_AMMO, 0);
    builder.define(COAX_AMMO, 0);
    builder.define(SMOKE_AMMO, 0);
    builder.define(HEADLIGHTS, false);
    builder.define(COAX_LOADED, 0);
    builder.define(COAX_RELOAD, 0);
    builder.define(SMOKE_LOADED, 0);
    builder.define(SMOKE_RELOAD, 0);
    builder.define(ENGINE_RPM, 0);
    builder.define(ENGINE_GEAR, 0);
    builder.define(ENGINE_TEMPERATURE, 20);
    builder.define(BATTERY, 100);
    builder.define(ENGINE_RUNNING, false);
    builder.define(ENGINE_CLUTCH, 1.0F);
    builder.define(DESTROYED, false);
    builder.define(WRECK_AGE, 0);
    builder.define(WRECK_TURRET_YAW, 0.0F);
    builder.define(WRECK_TURRET_ROLL, 78.0F);
  }

  @Override
  public InteractionResult interact(Player player, InteractionHand hand) {
    return this.handleInteraction(player, hand, null);
  }

  @Override
  public InteractionResult interactAt(Player player, Vec3 location, InteractionHand hand) {
    return this.handleInteraction(player, hand, location);
  }

  private InteractionResult handleInteraction(Player player, InteractionHand hand, Vec3 hitLocation) {
    if (this.isDestroyed()) {
      if (player.getItemInHand(hand).is(VehicleContent.IRON_WRENCH.get())) {
        return this.salvageWreck(player, hand);
      }
      return InteractionResult.PASS;
    }
    if (player.getItemInHand(hand).is(VehicleContent.TRACK_S.get())) {
      return this.installTrack(player, hand, hitLocation);
    }
    if (player.getItemInHand(hand).is(VehicleContent.ENGINE_VTA.get())) {
      return this.installModule(player, hand, ENGINE_INSTALLED, ENGINE_HEALTH, 50,
          "message.craftingdead.t72b3.engine_installed");
    }
    if (player.getItemInHand(hand).is(VehicleContent.T72B3_TURRET.get())) {
      return this.installModule(player, hand, TURRET_INSTALLED, TURRET_HEALTH, 400,
          "message.craftingdead.t72b3.turret_installed");
    }
    if (player.getItemInHand(hand).is(VehicleContent.RESHETKA_EPTA.get())) {
      return this.installModule(player, hand, SLAT_ARMOUR_INSTALLED, SLAT_HEALTH, 300,
          "message.craftingdead.t72b3.slat_installed");
    }
    if (player.getItemInHand(hand).is(VehicleContent.BLOWTORCH.get())) {
      return this.repairVehicle(player, hand);
    }
    if (player.getItemInHand(hand).is(VehicleContent.DIESEL_CAN.get())
        || player.getItemInHand(hand).is(Items.COAL)
        || player.getItemInHand(hand).is(Items.CHARCOAL)) {
      return this.refuel(player, hand);
    }
    if (player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.DyeItem dye) {
      return this.applySkin(player, hand, dye.getDyeColor().getId());
    }
    if (player.isSecondaryUseActive()) {
      if (!this.level().isClientSide()) {
        player.openMenu(this);
      }
      return InteractionResult.sidedSuccess(this.level().isClientSide());
    }
    // Tank entry is intentionally not available from the ground. The player has to climb onto
    // the upper hull, matching the physical vehicle layout rather than a generic mount entity.
    if (!this.isOnHullDeck(player)) {
      return InteractionResult.PASS;
    }
    if (!this.level().isClientSide()) {
      EntityDataAccessor<Integer> seat = this.selectSeat(hitLocation);
      if (this.entityData.get(seat) != -1) {
        return InteractionResult.FAIL;
      }
      this.entityData.set(seat, player.getId());
      this.setSeatExposed(seat, seat != DRIVER_ID);
      if (!player.startRiding(this)) {
        this.entityData.set(seat, -1);
        this.setSeatExposed(seat, false);
        return InteractionResult.FAIL;
      }
      this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_HATCH_OPEN.get(),
          SoundSource.BLOCKS, 0.9F, 1.0F);
      if (seat == DRIVER_ID) {
        player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.controls"),
            false);
      } else {
        player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.rear_controls"),
            false);
      }
    }
    return InteractionResult.sidedSuccess(this.level().isClientSide());
  }

  @Override
  public void tick() {
    super.tick();
    if (this.level().isClientSide()) {
      this.tickClientInterpolation();
    }
    if (!this.level().isClientSide()) {
      this.releaseEmptySeats();
      if (this.tickCount <= 2) {
        this.snapToGroundSurface();
      }
    }
    if (this.isDestroyed()) {
      if (!this.level().isClientSide()) this.tickWreck();
      return;
    }
    LivingEntity driver = this.getControllingPassenger();
    LivingEntity weaponController = this.getWeaponController();
    if (!this.level().isClientSide()) {
      if (driver == null || this.level().getGameTime() - this.lastDriverInputTick > 10L) {
        this.driverThrottle = 0.0F;
        this.driverTurn = 0.0F;
      }
      if (weaponController != null) {
      if (this.level().getGameTime() - this.lastWeaponInputTick > 5L) {
        this.driverActions = 0;
      }
      float targetTurretYaw = Mth.wrapDegrees(this.driverLookYaw - this.getYRot());
      if (this.isTurretOperational()) {
        this.entityData.set(TURRET_YAW, Mth.rotLerp(0.18F, this.getTurretYaw(), targetTurretYaw));
        float targetPitch = Mth.clamp(-this.driverLookPitch, -4.0F, 13.0F);
        this.entityData.set(TURRET_PITCH, Mth.lerp(0.18F, this.getTurretPitch(), targetPitch));
      }
      this.tickTurretSound();
        this.tickWeapons(weaponController);
      } else {
        this.driverActions = 0;
        this.tickWeaponCooldowns();
      }
      this.runTrackedEngine(this.driverThrottle, this.driverTurn, driver, true);
    }
    if (!this.level().isClientSide()) {
      this.updateGroundAttitude();
      this.keepLivingEntitiesOutsideHull(driver);
      this.tickHeadlightBeams();
      this.tickEngineExhaust();
      this.tickDamageEffects();
      if (this.tickCount % 10 == 0) this.updateAmmoCounts();
    }
  }

  private void tickHeadlightBeams() {
    if (!this.areHeadlightsOn() || !(this.level() instanceof ServerLevel server)
        || this.tickCount % 3 != 0) return;
    float yaw = this.getYRot() * Mth.DEG_TO_RAD;
    Vec3 forward = new Vec3(-Mth.sin(yaw), -0.035D, Mth.cos(yaw));
    Vec3 right = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
    Vec3 origin = this.position().add(0.0D, 0.72D, 0.0D);
    for (double distance = 3.2D; distance <= 42.0D; distance += 2.4D) {
      double spread = 0.28D + distance * 0.035D;
      for (double side : new double[] {-1.25D, 1.25D}) {
        Vec3 point = origin.add(forward.scale(distance)).add(right.scale(side));
        HitResult obstruction = this.level().clip(new ClipContext(origin, point,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (obstruction.getType() != HitResult.Type.MISS
            && obstruction.getLocation().distanceToSqr(origin) + 0.25D
                < point.distanceToSqr(origin)) {
          continue;
        }
        server.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1,
            spread, spread * 0.35D, spread, 0.005D);
      }
    }
  }

  private void tickEngineExhaust() {
    if (!this.isEngineRunning() || !(this.level() instanceof ServerLevel server)
        || this.tickCount % 3 != 0) return;
    float yaw = this.getYRot() * Mth.DEG_TO_RAD;
    Vec3 rear = this.position().add(Mth.sin(yaw) * 3.65D, 1.32D,
        -Mth.cos(yaw) * 3.65D);
    double load = Math.abs(this.entityData.get(ENGINE_POWER));
    server.sendParticles(load > 0.72D ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
        rear.x, rear.y, rear.z, load > 0.72D ? 3 : 1,
        0.18D, 0.12D, 0.18D, 0.018D + load * 0.025D);
    if (load > 0.92D && this.tickCount % 18 == 0) {
      server.sendParticles(ParticleTypes.FLAME, rear.x, rear.y, rear.z, 2,
          0.08D, 0.06D, 0.08D, 0.025D);
    }
  }

  private void tickDamageEffects() {
    if (!(this.level() instanceof ServerLevel server) || this.tickCount % 6 != 0) return;
    float yaw = this.getYRot() * Mth.DEG_TO_RAD;
    if (this.isEngineInstalled() && this.getEngineHealth() < 22) {
      Vec3 engine = this.position().add(Mth.sin(yaw) * 2.75D, 1.18D,
          -Mth.cos(yaw) * 2.75D);
      server.sendParticles(this.getEngineHealth() < 9 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
          engine.x, engine.y, engine.z, 2, 0.32D, 0.18D, 0.32D, 0.025D);
    }
    if (this.isTurretInstalled() && this.getTurretHealth() < 145) {
      server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 2.65D,
          this.getZ(), 1, 0.38D, 0.12D, 0.38D, 0.018D);
    }
    if ((this.isLeftTrackInstalled() && this.getLeftTrackHealth() < 22)
        || (this.isRightTrackInstalled() && this.getRightTrackHealth() < 22)) {
      server.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.35D,
          this.getZ(), 2, 1.75D, 0.18D, 2.8D, 0.04D);
    }
  }

  private void tickWreck() {
    this.entityData.set(WRECK_AGE, this.entityData.get(WRECK_AGE) + 1);
    this.driverThrottle = 0.0F;
    this.driverTurn = 0.0F;
    this.driverActions = 0;
    this.entityData.set(ENGINE_RUNNING, false);
    this.applyTrackedGravity(false, null);
    if (this.level() instanceof ServerLevel server && this.tickCount % 8 == 0) {
      server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 2.0D,
          this.getZ(), 3, 0.55D, 0.25D, 0.55D, 0.02D);
      if (this.getWreckAge() < 120) {
        server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.4D,
            this.getZ(), 2, 0.7D, 0.35D, 0.7D, 0.02D);
      }
    }
  }

  /** Smooths server snapshots over several client ticks, as the original vehicle base does. */
  private void tickClientInterpolation() {
    if (this.isControlledByLocalInstance()) {
      // The local driver is predicted after the world tick; do not pull it backwards toward an
      // older server snapshot. Only recover from a genuine large desynchronisation/teleport.
      if (this.clientInterpolationSteps > 0
          && this.distanceToSqr(this.clientTargetX, this.clientTargetY, this.clientTargetZ) > 9.0D) {
        this.setPos(this.clientTargetX, this.clientTargetY, this.clientTargetZ);
        this.setYRot(this.clientTargetYaw);
        this.setXRot(this.clientTargetPitch);
      }
      this.clientInterpolationSteps = 0;
      return;
    }
    if (this.clientInterpolationSteps <= 0) return;
    double divisor = this.clientInterpolationSteps;
    this.setPos(this.getX() + (this.clientTargetX - this.getX()) / divisor,
        this.getY() + (this.clientTargetY - this.getY()) / divisor,
        this.getZ() + (this.clientTargetZ - this.getZ()) / divisor);
    this.setYRot(this.getYRot()
        + Mth.wrapDegrees(this.clientTargetYaw - this.getYRot()) / (float) divisor);
    this.setXRot(this.getXRot()
        + Mth.wrapDegrees(this.clientTargetPitch - this.getXRot()) / (float) divisor);
    this.clientInterpolationSteps--;
  }

  @Override
  public void lerpTo(double x, double y, double z, float yaw, float pitch,
      int interpolationSteps) {
    if (!this.level().isClientSide()) {
      super.lerpTo(x, y, z, yaw, pitch, interpolationSteps);
      return;
    }
    this.clientTargetX = x;
    this.clientTargetY = y;
    this.clientTargetZ = z;
    this.clientTargetYaw = yaw;
    this.clientTargetPitch = pitch;
    // Three ticks is enough to hide 20 Hz packet stepping without the heavy input lag of a long
    // generic entity interpolation window.
    this.clientInterpolationSteps = 3;
  }

  /**
   * Port of the original Superb Warfare tracked-engine state machine used by VVP's M1A2. The
   * engine keeps a signed power value, applies ground drag before thrust, and uses differential
   * track motion for skid steering instead of teleporting at a fixed speed.
   */
  private void runTrackedEngine(float throttle, float turn, LivingEntity driver,
      boolean authoritative) {
    float power = this.entityData.get(ENGINE_POWER);
    // The source vehicle has two independent ground_tread slots. A bare wheel set cannot apply
    // engine force: both left and right tread assemblies must be fitted first.
    if (!this.hasCompleteTrackSet() || !this.isEngineOperational() || this.getFuel() <= 0) {
      throttle = 0.0F;
      turn = 0.0F;
      power *= 0.82F;
      if (Math.abs(power) < 0.0001F) power = 0.0F;
    }
    boolean forwardInput = throttle > 0.01F;
    boolean backInput = throttle < -0.01F;
    boolean rightInput = turn > 0.01F;
    boolean leftInput = turn < -0.01F;
    boolean engineRunning = driver != null && this.hasCompleteTrackSet()
        && this.isEngineOperational() && this.getFuel() > 0
        && this.entityData.get(BATTERY) > 0;
    if (authoritative && engineRunning && !this.engineWasRunning) {
      this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_ENGINE_START.get(),
          SoundSource.BLOCKS, 1.4F, 1.0F);
    }
    if (authoritative) {
      this.engineWasRunning = engineRunning;
      this.entityData.set(ENGINE_RUNNING, engineRunning);
      this.updateEngineInstruments(engineRunning, power, throttle);
    }
    if (authoritative && engineRunning && this.tickCount % 5 == 0 && this.getFuel() > 0) {
      int consumption = Math.max(1, Math.round(this.engineRpmState / 2600.0F));
      this.entityData.set(FUEL, Math.max(0, this.getFuel() - consumption));
    }
    double traction = this.getSurfaceTraction();

    if (forwardInput && !backInput) {
      power = Math.min(power
          + (power < 0.0F ? POWER_INCREMENT * 2.0F : POWER_INCREMENT)
          * (1.0F - Math.abs(power) / 1.02F), 1.0F);
    }
    if (backInput) {
      power = Math.max(power
          - (power > 0.0F ? POWER_DECREMENT * 4.0F : POWER_DECREMENT)
          * (1.0F - Math.abs(power) / 1.02F), -1.0F);
    }
    if (!forwardInput && !backInput) {
      power *= 0.96F;
      if (Math.abs(power) < 0.0001F) power = 0.0F;
    }
    if (rightInput || leftInput) {
      power *= 0.995F;
      this.steeringHoldTicks = Math.min(this.steeringHoldTicks + 1, 10);
      float steeringImpulse = (float) (STEERING_SPEED * 0.12F * this.steeringHoldTicks
          * traction);
      if (backInput) {
        this.deltaRotation += rightInput ? steeringImpulse : -steeringImpulse;
      } else {
        this.deltaRotation += rightInput ? -steeringImpulse : steeringImpulse;
      }
    } else {
      this.steeringHoldTicks = 0;
    }

    // Original ground model: strong lateral/rolling resistance, followed by engine thrust.
    this.driveVelocity *= 0.79D;
    double targetSpeed = power >= 0.0F ? MAX_FORWARD_SPEED_RATE : MAX_BACKWARD_SPEED_RATE;
    double clutch = this.entityData.get(ENGINE_CLUTCH);
    double gearTorque = power < 0.0F ? 1.10D
        : 0.72D + FORWARD_GEAR_RATIOS[Math.clamp(this.automaticGear, 1, 5)] * 0.13D;
    this.driveVelocity += 0.15D * targetSpeed * power * traction * clutch * gearTorque;
    if (Math.abs(this.driveVelocity) < 1.0E-4D) this.driveVelocity = 0.0D;

    this.deltaRotation *= Math.max(0.76D - 0.1D * Math.abs(this.driveVelocity), 0.3D);
    float oldYaw = this.getYRot();
    float requestedYaw = Mth.wrapDegrees(oldYaw - 8.0F * this.deltaRotation);
    boolean mayDestroy = authoritative && this.level().getGameRules()
        .getBoolean(VehicleGameRules.TANK_BLOCK_DESTRUCTION);
    if (Math.abs(Mth.wrapDegrees(requestedYaw - oldYaw)) > 1.0E-4F) {
      double rotatedY = this.resolveHullY(this.getX(), this.getY(), this.getZ(), requestedYaw,
          mayDestroy, driver);
      if (!Double.isNaN(rotatedY)) {
        this.setYRot(requestedYaw);
        this.setPos(this.getX(), rotatedY, this.getZ());
      } else {
        this.deltaRotation *= 0.25F;
      }
    }
    this.yRotO = this.getYRot();

    float yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    Vec3 movement = new Vec3(-Mth.sin(yawRadians) * this.driveVelocity, 0.0D,
        Mth.cos(yawRadians) * this.driveVelocity);
    double oldX = this.getX();
    double oldY = this.getY();
    double oldZ = this.getZ();
    boolean moved = this.moveSweptHull(movement, mayDestroy, driver);
    if (!moved && movement.horizontalDistanceSqr() > 1.0E-6D) {
      this.driveVelocity *= 0.12D;
      power *= 0.7F;
    }
    this.applyTrackedGravity(mayDestroy, driver);

    double travelled = Math.copySign(Math.sqrt(Mth.square(this.getX() - oldX)
        + Mth.square(this.getZ() - oldZ)), this.driveVelocity);
    double yawTravel = Mth.wrapDegrees(this.getYRot() - oldYaw) * Mth.DEG_TO_RAD
        * HULL_HALF_WIDTH * 0.78D;
    float leftTrack = wrapTrackRotation(this.entityData.get(LEFT_TRACK_ROTATION)
        - (float) ((travelled - yawTravel) * 83.0D));
    float rightTrack = wrapTrackRotation(this.entityData.get(RIGHT_TRACK_ROTATION)
        - (float) ((travelled + yawTravel) * 83.0D));
    this.entityData.set(ENGINE_POWER, power);
    this.entityData.set(LEFT_TRACK_ROTATION, leftTrack);
    this.entityData.set(RIGHT_TRACK_ROTATION, rightTrack);
    this.setDeltaMovement(this.getX() - oldX, this.getY() - oldY, this.getZ() - oldZ);
    this.hasImpulse = moved || Math.abs(this.getY() - oldY) > 1.0E-5D;
    if (authoritative && moved) {
      this.emitTrackGroundEffects(Math.abs(travelled) + Math.abs(yawTravel));
    }

    if (authoritative && moved && !this.reportedDriverMovement) {
      this.reportedDriverMovement = true;
      LOGGER.info("T-72B3 {} tracked engine moved from ({}, {}) to ({}, {})", this.getId(),
          oldX, oldZ, this.getX(), this.getZ());
    }
  }

  private void updateEngineInstruments(boolean running, float power, float throttle) {
    double speed = Math.abs(this.driveVelocity);
    if (!running) {
      this.engineRpmState = Math.max(0.0F, this.engineRpmState - 10.0F);
      this.entityData.set(ENGINE_RPM, Math.round(this.engineRpmState));
      this.entityData.set(ENGINE_GEAR, 0);
      this.entityData.set(ENGINE_CLUTCH, 0.0F);
      return;
    }
    int gear = power < -0.01F ? -1 : this.automaticGear;
    double ratio = gear < 0 ? 2.2D : FORWARD_GEAR_RATIOS[Math.clamp(gear, 1, 5)];
    int wheelRpm = (int) (speed * 1180.0D);
    int targetRpm = Math.clamp((int) (ENGINE_IDLE_RPM + wheelRpm * ratio * 2.25D
        + Math.abs(throttle) * 1500.0F), ENGINE_STALL_RPM, ENGINE_MAX_RPM);
    if (this.shiftTicks == 0 && gear > 0 && targetRpm > ENGINE_REV_LIMIT_RPM
        && this.automaticGear < 5) {
      this.automaticGear++;
      this.shiftTicks = ENGINE_SHIFT_TICKS;
    } else if (this.shiftTicks == 0 && gear > 1 && targetRpm < 1350) {
      this.automaticGear--;
      this.shiftTicks = ENGINE_SHIFT_TICKS;
    }
    float clutch = 1.0F;
    if (this.shiftTicks > 0) {
      this.shiftTicks--;
      clutch = 1.0F - Mth.sin((ENGINE_SHIFT_TICKS - this.shiftTicks)
          / (float) ENGINE_SHIFT_TICKS * Mth.PI);
      targetRpm = Math.max(ENGINE_IDLE_RPM, (int) (targetRpm * 0.72F));
    }
    this.entityData.set(ENGINE_CLUTCH, Mth.clamp(clutch, 0.12F, 1.0F));
    this.engineRpmState = Mth.lerp(0.22F, this.engineRpmState, targetRpm);
    if (this.engineRpmState > ENGINE_REV_LIMIT_RPM && this.tickCount % 10 < 2) {
      this.engineRpmState -= 420.0F;
    }
    this.entityData.set(ENGINE_RPM, Math.round(this.engineRpmState));
    this.entityData.set(ENGINE_GEAR, gear);
    int temperature = this.entityData.get(ENGINE_TEMPERATURE);
    int targetTemperature = running ? 62 + (int) (Math.abs(power) * 34.0F) : 20;
    if (this.tickCount % 10 == 0) {
      temperature += Integer.compare(targetTemperature, temperature);
      this.entityData.set(ENGINE_TEMPERATURE, Math.clamp(temperature, 0, 125));
    }
    int battery = this.entityData.get(BATTERY);
    if (this.tickCount % 20 == 0) {
      if (running) battery = Math.min(100, battery + 1);
      else if (this.areHeadlightsOn()) battery = Math.max(0, battery - 1);
      this.entityData.set(BATTERY, battery);
    }
    if ((temperature > 112 || this.engineRpmState > ENGINE_SAFE_RPM) && this.tickCount % 40 == 0) {
      this.entityData.set(ENGINE_HEALTH, Math.max(0, this.entityData.get(ENGINE_HEALTH) - 1));
    }
  }

  private static float wrapTrackRotation(float rotation) {
    return Mth.wrapDegrees(rotation);
  }

  private void tickWeaponCooldowns() {
    if (this.entityData.get(MAIN_RELOAD) > 0) {
      int remaining = this.entityData.get(MAIN_RELOAD) - 1;
      this.entityData.set(MAIN_RELOAD, remaining);
      if (remaining == 120) {
        this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_SHELL_OPEN.get(),
            SoundSource.BLOCKS, 1.0F, 1.0F);
      } else if (remaining == 20) {
        this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_SHELL_CLOSE.get(),
            SoundSource.BLOCKS, 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel server) {
          float yaw = (this.getYRot() + this.getTurretYaw()) * Mth.DEG_TO_RAD;
          Vec3 right = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
          Vec3 eject = this.position().add(0.0D, 2.72D, 0.0D).add(right.scale(1.15D));
          server.sendParticles(ParticleTypes.POOF, eject.x, eject.y, eject.z, 7,
              0.16D, 0.12D, 0.16D, 0.04D);
        }
      }
      if (remaining == 0) {
        this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_RELOAD.get(),
            SoundSource.PLAYERS, 1.1F, 1.0F);
      }
    }
    if (this.coaxCooldown > 0) this.coaxCooldown--;
    if (this.smokeCooldown > 0) this.smokeCooldown--;
    this.tickMagazineReload(COAX_RELOAD, COAX_LOADED, VehicleContent.BULLET_762.get(),
        COAX_CAPACITY, ModSoundEvents.T72_COAX_RELOAD.get());
    this.tickMagazineReload(SMOKE_RELOAD, SMOKE_LOADED, VehicleContent.SMOKE_40MM.get(),
        SMOKE_CAPACITY, ModSoundEvents.T72_SMOKE_RELOAD.get());
    this.entityData.set(RECOIL, Math.max(0.0F, this.entityData.get(RECOIL) - 0.16F));
  }

  private void tickTurretSound() {
    float currentYaw = this.getTurretYaw();
    boolean moving = Math.abs(Mth.wrapDegrees(currentYaw - this.lastTurretYawForSound)) > 0.035F;
    if (moving && !this.turretWasMoving) {
      this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_TURRET_START.get(),
          SoundSource.BLOCKS, 0.7F, 1.0F);
    } else if (moving && this.tickCount % 18 == 0) {
      this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_TURRET_MOVE.get(),
          SoundSource.BLOCKS, 0.55F, 1.0F);
    } else if (!moving && this.turretWasMoving) {
      this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_TURRET_STOP.get(),
          SoundSource.BLOCKS, 0.65F, 1.0F);
    }
    this.lastTurretYawForSound = currentYaw;
    this.turretWasMoving = moving;
  }

  private void tickMagazineReload(EntityDataAccessor<Integer> reload,
      EntityDataAccessor<Integer> loaded, net.minecraft.world.item.Item ammo, int capacity,
      net.minecraft.sounds.SoundEvent sound) {
    int ticks = this.entityData.get(reload);
    if (ticks <= 0) return;
    ticks--;
    this.entityData.set(reload, ticks);
    if (ticks == 0) {
      int needed = capacity - this.entityData.get(loaded);
      int loadedNow = this.consumeCargoItems(ammo, needed);
      this.entityData.set(loaded, this.entityData.get(loaded) + loadedNow);
      if (loadedNow > 0) {
        this.level().playSound(null, this.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, 1.0F);
      }
    }
  }

  private void tickWeapons(LivingEntity driver) {
    this.tickWeaponCooldowns();
    int rising = this.driverActions & ~this.previousDriverActions;
    if ((rising & ACTION_SWITCH_AMMO) != 0) {
      int next = 1 - this.entityData.get(SELECTED_AMMO);
      this.entityData.set(SELECTED_AMMO, next);
      if (driver instanceof Player player) {
        player.displayClientMessage(Component.translatable(next == 0
            ? "message.craftingdead.t72b3.ammo_he" : "message.craftingdead.t72b3.ammo_ap"), true);
      }
    }
    if ((rising & ACTION_RELOAD) != 0) {
      this.startCoaxReload();
      this.startSmokeReload();
    }
    if ((rising & ACTION_RELOAD) != 0 && this.entityData.get(MAIN_RELOAD) > 0) {
      if (driver instanceof Player player) {
        player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.reloading",
            this.entityData.get(MAIN_RELOAD) / 20.0F), true);
      }
    }
    if ((rising & ACTION_HEADLIGHTS) != 0) {
      this.entityData.set(HEADLIGHTS, !this.entityData.get(HEADLIGHTS));
    }
    if ((this.driverActions & ACTION_MAIN_GUN) != 0 && this.entityData.get(MAIN_RELOAD) == 0) {
      this.fireMainGun(driver);
    }
    if ((this.driverActions & ACTION_COAX) != 0 && this.coaxCooldown == 0) {
      this.fireCoax(driver);
    }
    if ((rising & ACTION_SMOKE) != 0 && this.smokeCooldown == 0) {
      this.fireSmoke(driver);
    }
    this.previousDriverActions = this.driverActions;
  }

  private Vec3 getTurretDirection() {
    float yaw = (this.getYRot() + this.getTurretYaw()) * Mth.DEG_TO_RAD;
    float pitch = this.getTurretPitch() * Mth.DEG_TO_RAD;
    double horizontal = Mth.cos(pitch);
    return new Vec3(-Mth.sin(yaw) * horizontal, Mth.sin(pitch),
        Mth.cos(yaw) * horizontal).normalize();
  }

  private Vec3 getMuzzlePosition(double forwardDistance, double height) {
    return this.position().add(0.0D, height, 0.0D)
        .add(this.getTurretDirection().scale(forwardDistance));
  }

  private void fireMainGun(LivingEntity driver) {
    if (!this.isTurretOperational()) return;
    ItemStack ammo = this.entityData.get(SELECTED_AMMO) == 0
        ? new ItemStack(VehicleContent.SHELL_125MM_HE.get())
        : new ItemStack(VehicleContent.SHELL_125MM_AP.get());
    if (!this.consumeCargoItem(ammo)) {
      if (driver instanceof Player player) {
        player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.no_main_ammo"),
            true);
      }
      this.entityData.set(MAIN_RELOAD, 10);
      return;
    }
    Vec3 direction = this.getTurretDirection();
    T72ShellEntity shell = new T72ShellEntity(driver, this.level(), ammo);
    Vec3 muzzle = this.getMuzzlePosition(8.75D, 2.75D);
    shell.setPos(muzzle.x, muzzle.y, muzzle.z);
    shell.setDeltaMovement(direction.scale(4.0D));
    shell.setYRot((float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG));
    this.level().addFreshEntity(shell);
    this.entityData.set(MAIN_RELOAD, MAIN_RELOAD_TICKS);
    this.entityData.set(RECOIL, 1.0F);
    this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_MAIN_GUN.get(),
        SoundSource.PLAYERS, 4.0F, 0.96F + this.random.nextFloat() * 0.08F);
    this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_MAIN_GUN_DISTANT.get(),
        SoundSource.PLAYERS, 6.0F, 0.98F + this.random.nextFloat() * 0.04F);
    this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_MAIN_GUN_INTERNAL.get(),
        SoundSource.PLAYERS, 1.8F, 1.0F);
    if (this.level() instanceof ServerLevel server) {
      server.sendParticles(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 2,
          0.15D, 0.15D, 0.15D, 0.0D);
      server.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 14,
          0.45D, 0.35D, 0.45D, 0.06D);
    }
  }

  private void fireCoax(LivingEntity driver) {
    if (!this.isTurretOperational()) return;
    if (driver instanceof Player player && !this.canUseCoax(player)) return;
    if (this.entityData.get(COAX_LOADED) <= 0) {
      this.startCoaxReload();
      this.coaxCooldown = 10;
      return;
    }
    this.entityData.set(COAX_LOADED, this.entityData.get(COAX_LOADED) - 1);
    Vec3 direction = this.getTurretDirection();
    Vec3 start = this.getMuzzlePosition(4.2D, 2.72D);
    Vec3 end = start.add(direction.scale(180.0D));
    HitResult blockHit = this.level().clip(new ClipContext(start, end,
        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    double maxDistanceSqr = blockHit.getType() == HitResult.Type.MISS
        ? start.distanceToSqr(end) : start.distanceToSqr(blockHit.getLocation());
    EntityHitResult entityHit = RayTraceUtil.getEntityHitResult(this, start, end,
        direction.scale(180.0D), candidate -> candidate != this && candidate.getVehicle() != this
            && EntitySelector.ENTITY_STILL_ALIVE.test(candidate), maxDistanceSqr);
    Vec3 impact = blockHit.getLocation();
    if (entityHit != null) {
      impact = entityHit.getLocation();
      entityHit.getEntity().hurt(ModDamageSource.gun(driver, false), 6.0F);
    }
    this.coaxCooldown = COAX_FIRE_DELAY;
    if (this.entityData.get(COAX_LOADED) == 0) this.startCoaxReload();
    this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_COAX.get(),
        SoundSource.PLAYERS, 1.7F, 0.95F + this.random.nextFloat() * 0.1F);
    if (this.level() instanceof ServerLevel server) {
      server.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 3,
          0.08D, 0.08D, 0.08D, 0.02D);
    }
  }

  private void fireSmoke(LivingEntity driver) {
    if (!this.isTurretOperational()) return;
    if (this.entityData.get(SMOKE_LOADED) <= 0) {
      this.startSmokeReload();
      return;
    }
    this.entityData.set(SMOKE_LOADED, this.entityData.get(SMOKE_LOADED) - 1);
    Vec3 direction = this.getTurretDirection();
    SmokeGrenadeEntity smoke = new SmokeGrenadeEntity(driver, this.level());
    Vec3 muzzle = this.getMuzzlePosition(2.8D, 3.05D);
    smoke.setPos(muzzle.x, muzzle.y, muzzle.z);
    smoke.setDeltaMovement(direction.scale(1.25D).add(0.0D, 0.34D, 0.0D));
    this.level().addFreshEntity(smoke);
    this.smokeCooldown = SMOKE_FIRE_DELAY;
    if (this.entityData.get(SMOKE_LOADED) == 0) this.startSmokeReload();
    this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_SMOKE_LAUNCH.get(),
        SoundSource.PLAYERS, 1.5F, 1.0F);
  }

  private boolean consumeCargoItem(ItemStack wanted) {
    for (int slot = 0; slot < this.cargo.getContainerSize(); slot++) {
      ItemStack stack = this.cargo.getItem(slot);
      if (stack.is(wanted.getItem())) {
        stack.shrink(1);
        this.cargo.setChanged();
        return true;
      }
    }
    return false;
  }

  private int consumeCargoItems(net.minecraft.world.item.Item wanted, int maximum) {
    int consumed = 0;
    for (int slot = 0; slot < this.cargo.getContainerSize() && consumed < maximum; slot++) {
      ItemStack stack = this.cargo.getItem(slot);
      if (!stack.is(wanted)) continue;
      int amount = Math.min(stack.getCount(), maximum - consumed);
      stack.shrink(amount);
      consumed += amount;
    }
    if (consumed > 0) this.cargo.setChanged();
    return consumed;
  }

  private void startCoaxReload() {
    if (this.entityData.get(COAX_RELOAD) == 0
        && this.entityData.get(COAX_LOADED) < COAX_CAPACITY
        && this.countCargoItem(VehicleContent.BULLET_762.get()) > 0) {
      this.entityData.set(COAX_RELOAD, COAX_RELOAD_TICKS);
    }
  }

  private void startSmokeReload() {
    if (this.entityData.get(SMOKE_RELOAD) == 0
        && this.entityData.get(SMOKE_LOADED) < SMOKE_CAPACITY
        && this.countCargoItem(VehicleContent.SMOKE_40MM.get()) > 0) {
      this.entityData.set(SMOKE_RELOAD, SMOKE_RELOAD_TICKS);
    }
  }

  private void updateAmmoCounts() {
    this.entityData.set(HE_AMMO, this.countCargoItem(VehicleContent.SHELL_125MM_HE.get()));
    this.entityData.set(AP_AMMO, this.countCargoItem(VehicleContent.SHELL_125MM_AP.get()));
    this.entityData.set(COAX_AMMO, this.countCargoItem(VehicleContent.BULLET_762.get()));
    this.entityData.set(SMOKE_AMMO, this.countCargoItem(VehicleContent.SMOKE_40MM.get()));
  }

  private int countCargoItem(net.minecraft.world.item.Item item) {
    int count = 0;
    for (int slot = 0; slot < this.cargo.getContainerSize(); slot++) {
      ItemStack stack = this.cargo.getItem(slot);
      if (stack.is(item)) count += stack.getCount();
    }
    return count;
  }

  /** Sweeps the full segmented hull in small increments so high speed cannot tunnel through walls. */
  private boolean moveSweptHull(Vec3 movement, boolean mayDestroy, LivingEntity driver) {
    double distance = movement.horizontalDistance();
    if (distance < 1.0E-6D) return false;
    int steps = Math.max(1, Mth.ceil(distance / 0.16D));
    double stepX = movement.x / steps;
    double stepZ = movement.z / steps;
    boolean moved = false;
    for (int step = 0; step < steps; step++) {
      double candidateX = this.getX() + stepX;
      double candidateZ = this.getZ() + stepZ;
      double candidateY = this.resolveHullY(candidateX, this.getY(), candidateZ, this.getYRot(),
          mayDestroy, driver);
      if (Double.isNaN(candidateY)) break;
      this.setPos(candidateX, candidateY, candidateZ);
      this.verticalVelocity = 0.0D;
      moved = true;
    }
    return moved;
  }

  /**
   * Resolves one candidate pose. A one-block obstacle is treated as a tread step; anything taller
   * blocks the complete hull unless the tank-block-destruction gamerule is enabled.
   */
  private double resolveHullY(double x, double baseY, double z, float yaw, boolean mayDestroy,
      LivingEntity driver) {
    List<CollisionHit> hits = this.collectHullBlockCollisions(x, baseY, z, yaw);
    if (hits.isEmpty()) return baseY;

    double stepY = baseY;
    boolean allStepHeight = true;
    for (CollisionHit hit : hits) {
      stepY = Math.max(stepY, hit.box.maxY);
      if (hit.box.maxY > baseY + MAX_STEP_HEIGHT + 1.0E-4D) {
        allStepHeight = false;
      }
    }
    if (allStepHeight && stepY > baseY + 1.0E-4D
        && this.collectHullBlockCollisions(x, stepY, z, yaw).isEmpty()) {
      return stepY;
    }

    if (!mayDestroy) return Double.NaN;
    Set<BlockPos> destroyed = new HashSet<>();
    for (CollisionHit hit : hits) {
      BlockState state = this.level().getBlockState(hit.pos);
      if (state.getDestroySpeed(this.level(), hit.pos) >= 0.0F && destroyed.add(hit.pos)) {
        this.level().destroyBlock(hit.pos, true, driver);
      }
    }
    hits = this.collectHullBlockCollisions(x, baseY, z, yaw);
    if (hits.isEmpty()) return baseY;
    stepY = baseY;
    for (CollisionHit hit : hits) {
      stepY = Math.max(stepY, hit.box.maxY);
      if (hit.box.maxY > baseY + MAX_STEP_HEIGHT + 1.0E-4D) return Double.NaN;
    }
    return this.collectHullBlockCollisions(x, stepY, z, yaw).isEmpty()
        ? stepY : Double.NaN;
  }

  private List<CollisionHit> collectHullBlockCollisions(double x, double baseY, double z,
      float yaw) {
    double radians = yaw * Mth.DEG_TO_RAD;
    double cosine = Mth.cos((float) radians);
    double sine = Mth.sin((float) radians);
    double xExtent = Math.abs(cosine) * HULL_HALF_WIDTH
        + Math.abs(sine) * HULL_HALF_LENGTH;
    double zExtent = Math.abs(sine) * HULL_HALF_WIDTH
        + Math.abs(cosine) * HULL_HALF_LENGTH;
    int minX = Mth.floor(x - xExtent - 0.05D);
    int maxX = Mth.floor(x + xExtent + 0.05D);
    int minY = Mth.floor(baseY - 0.05D);
    int maxY = Mth.floor(baseY + VEHICLE_BLOCK_COLLISION_HEIGHT + 0.05D);
    int minZ = Mth.floor(z - zExtent - 0.05D);
    int maxZ = Mth.floor(z + zExtent + 0.05D);
    List<CollisionHit> hits = new ArrayList<>();
    for (int blockX = minX; blockX <= maxX; blockX++) {
      for (int blockY = minY; blockY <= maxY; blockY++) {
        for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
          BlockPos pos = new BlockPos(blockX, blockY, blockZ);
          BlockState state = this.level().getBlockState(pos);
          if (state.isAir()) continue;
          var shape = state.getCollisionShape(this.level(), pos);
          if (shape.isEmpty()) continue;
          for (AABB localBox : shape.toAabbs()) {
            AABB box = localBox.move(pos);
            if (box.maxY <= baseY + 0.045D
                || box.minY >= baseY + VEHICLE_BLOCK_COLLISION_HEIGHT - 0.015D) {
              continue;
            }
            for (SourceHullBox sourceBox : SOURCE_HULL_BOXES) {
              if (box.maxY <= baseY + sourceBox.centerY - sourceBox.halfHeight
                  || box.minY >= baseY + sourceBox.centerY + sourceBox.halfHeight) {
                continue;
              }
              HullSection section = new HullSection(sourceBox.centerX, sourceBox.centerZ,
                  sourceBox.halfWidth, sourceBox.halfWidth);
              if (intersectsSection(box, x, z, cosine, sine, section)) {
                hits.add(new CollisionHit(pos, box));
                break;
              }
            }
          }
        }
      }
    }
    return hits;
  }

  /** Separating-axis test between a rotated hull section and an axis-aligned block box. */
  private static boolean intersectsSection(AABB box, double tankX, double tankZ, double cosine,
      double sine, HullSection section) {
    double sectionX = tankX + cosine * section.centerX - sine * section.centerZ;
    double sectionZ = tankZ + sine * section.centerX + cosine * section.centerZ;
    double boxCenterX = (box.minX + box.maxX) * 0.5D;
    double boxCenterZ = (box.minZ + box.maxZ) * 0.5D;
    double dx = boxCenterX - sectionX;
    double dz = boxCenterZ - sectionZ;
    double boxHalfX = (box.maxX - box.minX) * 0.5D;
    double boxHalfZ = (box.maxZ - box.minZ) * 0.5D;
    double absCos = Math.abs(cosine);
    double absSin = Math.abs(sine);
    double epsilon = 1.0E-5D;
    if (Math.abs(dx) >= boxHalfX + section.halfWidth * absCos
        + section.halfLength * absSin - epsilon) return false;
    if (Math.abs(dz) >= boxHalfZ + section.halfWidth * absSin
        + section.halfLength * absCos - epsilon) return false;
    double localX = cosine * dx + sine * dz;
    double localZ = -sine * dx + cosine * dz;
    if (Math.abs(localX) >= section.halfWidth + boxHalfX * absCos
        + boxHalfZ * absSin - epsilon) return false;
    return Math.abs(localZ) < section.halfLength + boxHalfX * absSin
        + boxHalfZ * absCos - epsilon;
  }

  private void applyTrackedGravity(boolean mayDestroy, LivingEntity driver) {
    double supportY = this.findHullSupportY(this.getX(), this.getY(), this.getZ(), this.getYRot());
    if (!Double.isNaN(supportY) && supportY >= this.getY() - 0.05D) {
      if (supportY > this.getY() + 0.01D) {
        double resolved = this.resolveHullY(this.getX(), supportY, this.getZ(), this.getYRot(),
            mayDestroy, driver);
        if (!Double.isNaN(resolved)) this.setPos(this.getX(), resolved, this.getZ());
      }
      this.applyLandingImpulse(this.verticalVelocity);
      this.verticalVelocity = 0.0D;
      return;
    }
    // Do not clamp a multi-ton vehicle to the old 0.65 block/tick floaty terminal speed. Keep
    // accelerating and use a wide downward support sweep so the faster fall cannot tunnel.
    this.verticalVelocity = Math.max((this.verticalVelocity - TRACKED_GRAVITY)
        * VERTICAL_AIR_DRAG, MAX_FALL_SPEED);
    double nextY = this.getY() + this.verticalVelocity;
    if (!Double.isNaN(supportY) && nextY <= supportY) {
      nextY = supportY;
      this.applyLandingImpulse(this.verticalVelocity);
      this.verticalVelocity = 0.0D;
    }
    this.setPos(this.getX(), nextY, this.getZ());
  }

  private double findHullSupportY(double x, double baseY, double z, float yaw) {
    double radians = yaw * Mth.DEG_TO_RAD;
    double cosine = Mth.cos((float) radians);
    double sine = Mth.sin((float) radians);
    double highest = Double.NaN;
    double[] localXs = {-1.95D, 0.0D, 1.95D};
    double[] localZs = {-3.85D, -1.8D, 0.25D, 2.3D, 4.0D};
    for (double localX : localXs) {
      for (double localZ : localZs) {
        double sampleX = x + cosine * localX - sine * localZ;
        double sampleZ = z + sine * localX + cosine * localZ;
        int blockX = Mth.floor(sampleX);
        int blockZ = Mth.floor(sampleZ);
        double scanDepth = Math.max(4.0D, -this.verticalVelocity + 1.0D);
        for (int blockY = Mth.floor(baseY + 0.15D);
            blockY >= Mth.floor(baseY - scanDepth); blockY--) {
          BlockPos pos = new BlockPos(blockX, blockY, blockZ);
          var shape = this.level().getBlockState(pos).getCollisionShape(this.level(), pos);
          if (shape.isEmpty()) continue;
          for (AABB localBox : shape.toAabbs()) {
            AABB box = localBox.move(pos);
            if (sampleX >= box.minX && sampleX <= box.maxX
                && sampleZ >= box.minZ && sampleZ <= box.maxZ
                && box.maxY <= baseY + 0.16D) {
              highest = Double.isNaN(highest) ? box.maxY : Math.max(highest, box.maxY);
            }
          }
          if (!Double.isNaN(highest) && highest >= blockY) break;
        }
      }
    }
    return highest;
  }

  /**
   * Four-point ground-device approximation of the source runtime. Front/rear height differences
   * drive pitch, left/right differences drive roll, and an impact excites a damped suspension
   * spring. This is visual orientation only; the conservative upright collision volume remains
   * authoritative so a tilted model cannot tunnel through a wall.
   */
  private void updateGroundAttitude() {
    double frontLeft = this.findSupportAt(1.90D, 3.65D);
    double frontRight = this.findSupportAt(-1.90D, 3.65D);
    double rearLeft = this.findSupportAt(1.90D, -3.55D);
    double rearRight = this.findSupportAt(-1.90D, -3.55D);
    double front = averageAvailable(frontLeft, frontRight);
    double rear = averageAvailable(rearLeft, rearRight);
    double left = averageAvailable(frontLeft, rearLeft);
    double right = averageAvailable(frontRight, rearRight);
    float targetPitch = Double.isNaN(front) || Double.isNaN(rear) ? 0.0F
        : (float) -Math.toDegrees(Math.atan2(front - rear, 7.20D));
    float targetRoll = Double.isNaN(left) || Double.isNaN(right) ? 0.0F
        : (float) Math.toDegrees(Math.atan2(left - right, 3.80D));
    targetPitch = Mth.clamp(targetPitch, -11.0F, 11.0F);
    targetRoll = Mth.clamp(targetRoll, -12.0F, 12.0F);
    this.entityData.set(BODY_PITCH, Mth.rotLerp(0.22F,
        this.entityData.get(BODY_PITCH), targetPitch));
    this.entityData.set(BODY_ROLL, Mth.rotLerp(0.22F,
        this.entityData.get(BODY_ROLL), targetRoll));

    this.suspensionVelocity += -this.suspensionOffset * 0.28D;
    this.suspensionVelocity *= 0.62D;
    this.suspensionOffset = Mth.clamp(this.suspensionOffset + this.suspensionVelocity,
        -0.18D, 0.10D);
    if (Math.abs(this.suspensionOffset) < 1.0E-4D
        && Math.abs(this.suspensionVelocity) < 1.0E-4D) {
      this.suspensionOffset = 0.0D;
      this.suspensionVelocity = 0.0D;
    }
    this.entityData.set(SUSPENSION_OFFSET, (float) this.suspensionOffset);
  }

  private void applyLandingImpulse(double impactVelocity) {
    if (impactVelocity < -0.06D) {
      this.suspensionVelocity -= Mth.clamp(-impactVelocity * 0.20D, 0.025D, 0.16D);
    }
    if (impactVelocity >= -0.55D || !(this.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    double impact = -impactVelocity;
    this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND,
        SoundSource.BLOCKS, (float) Mth.clamp(0.45D + impact * 0.28D, 0.6D, 1.6D),
        (float) Mth.clamp(1.05D - impact * 0.10D, 0.65D, 1.0D));
    BlockState ground = this.level().getBlockState(
        BlockPos.containing(this.getX(), this.getY() - 0.08D, this.getZ()));
    if (!ground.isAir()) {
      serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
          this.getX(), this.getY() + 0.12D, this.getZ(),
          Mth.clamp(Mth.ceil(impact * 14.0D), 8, 42), 2.0D, 0.12D, 2.8D, 0.12D);
    }
    if (impact > 0.90D) {
      this.damageIntegrity(Mth.ceil((impact - 0.90D) * 18.0D));
    }
  }

  /** track_s defines a -0.2 friction modifier for both snow and ice. */
  private double getSurfaceTraction() {
    double left = this.getTractionAt(1.85D, 0.0D);
    double right = this.getTractionAt(-1.85D, 0.0D);
    return (left + right) * 0.5D;
  }

  private double getTractionAt(double localX, double localZ) {
    BlockState state = this.getGroundStateAt(localX, localZ);
    return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
        || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.SNOW)
        || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW) ? 0.80D : 1.0D;
  }

  private BlockState getGroundStateAt(double localX, double localZ) {
    float yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double cosine = Mth.cos(yawRadians);
    double sine = Mth.sin(yawRadians);
    double x = this.getX() + cosine * localX - sine * localZ;
    double z = this.getZ() + sine * localX + cosine * localZ;
    for (int y = Mth.floor(this.getY() + 0.05D); y >= Mth.floor(this.getY() - 2.0D); y--) {
      BlockPos pos = BlockPos.containing(x, y, z);
      BlockState state = this.level().getBlockState(pos);
      if (!state.getCollisionShape(this.level(), pos).isEmpty()) return state;
    }
    return Blocks.AIR.defaultBlockState();
  }

  private void emitTrackGroundEffects(double trackTravel) {
    if (trackTravel < 0.018D || !this.hasCompleteTrackSet()
        || !(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 3 != 0) {
      return;
    }
    for (double localX : new double[] {-1.85D, 1.85D}) {
      BlockState ground = this.getGroundStateAt(localX, -3.05D);
      if (ground.isAir()) continue;
      float yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
      double x = this.getX() + Mth.cos(yawRadians) * localX
          - Mth.sin(yawRadians) * -3.05D;
      double z = this.getZ() + Mth.sin(yawRadians) * localX
          + Mth.cos(yawRadians) * -3.05D;
      BlockPos effectPos = BlockPos.containing(x, this.getY() + 0.05D, z);
      if (!this.level().getFluidState(effectPos).isEmpty()) {
        serverLevel.sendParticles(ParticleTypes.SPLASH, x, this.getY() + 0.20D, z,
            6, 0.35D, 0.08D, 0.42D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.BUBBLE, x, this.getY() + 0.05D, z,
            4, 0.25D, 0.05D, 0.32D, 0.08D);
      } else {
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
            x, this.getY() + 0.08D, z, 3, 0.20D, 0.06D, 0.25D, 0.025D);
      }
    }
    if (this.tickCount % 9 == 0) {
      BlockPos groundPos = BlockPos.containing(this.getX(), this.getY() - 0.08D, this.getZ());
      BlockState ground = this.level().getBlockState(groundPos);
      if (!ground.isAir()) {
        this.level().playSound(null, groundPos,
            ground.getSoundType(this.level(), groundPos, this).getStepSound(),
            SoundSource.BLOCKS, 0.65F, 0.72F);
      }
    }
  }

  private double findSupportAt(double localX, double localZ) {
    float yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double cosine = Mth.cos(yawRadians);
    double sine = Mth.sin(yawRadians);
    double sampleX = this.getX() + cosine * localX - sine * localZ;
    double sampleZ = this.getZ() + sine * localX + cosine * localZ;
    double highest = Double.NaN;
    for (int blockY = Mth.floor(this.getY() + 0.20D);
        blockY >= Mth.floor(this.getY() - 2.5D); blockY--) {
      BlockPos pos = BlockPos.containing(sampleX, blockY, sampleZ);
      var shape = this.level().getBlockState(pos).getCollisionShape(this.level(), pos);
      if (shape.isEmpty()) continue;
      for (AABB localBox : shape.toAabbs()) {
        AABB box = localBox.move(pos);
        if (sampleX >= box.minX && sampleX <= box.maxX
            && sampleZ >= box.minZ && sampleZ <= box.maxZ
            && box.maxY <= this.getY() + 0.21D) {
          highest = Double.isNaN(highest) ? box.maxY : Math.max(highest, box.maxY);
        }
      }
      if (!Double.isNaN(highest)) break;
    }
    return highest;
  }

  private static double averageAvailable(double first, double second) {
    if (Double.isNaN(first)) return second;
    if (Double.isNaN(second)) return first;
    return (first + second) * 0.5D;
  }

  /**
   * Vanilla entities are pushable rather than solid by default. The tank needs a true obstacle
   * footprint, and its original hull is rectangular rather than the square EntityType hitbox.
   */
  private void keepLivingEntitiesOutsideHull(LivingEntity driver) {
    AABB searchBox = this.createHullBoundingBox().inflate(0.5D, 0.15D, 0.5D);
    double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double cosine = Mth.cos((float) yawRadians);
    double sine = Mth.sin((float) yawRadians);

    for (Entity entity : this.level().getEntities(this, searchBox,
        candidate -> candidate instanceof LivingEntity && candidate != driver)) {
      if (entity.getVehicle() == this || entity.getBoundingBox().maxY <= this.getY()
          || entity.getBoundingBox().minY >= this.getY() + this.getBbHeight()) {
        continue;
      }

      double worldX = entity.getX() - this.getX();
      double worldZ = entity.getZ() - this.getZ();
      double localX = cosine * worldX + sine * worldZ;
      double localZ = -sine * worldX + cosine * worldZ;
      double entityRadius = Math.max(0.3D, entity.getBbWidth() * 0.5D);
      double widthLimit = HULL_HALF_WIDTH + entityRadius;
      double lengthLimit = HULL_HALF_LENGTH + entityRadius;
      if (Math.abs(localX) >= widthLimit || Math.abs(localZ) >= lengthLimit) {
        continue;
      }

      // A jumping entity that clears the front armour lands on the upper hull. This gives the
      // tank a real walkable deck while still denying horizontal movement through its body.
      if (entity.getY() >= this.getY() + HULL_DECK_HEIGHT - 0.35D) {
        double deckY = this.getY() + HULL_DECK_HEIGHT;
        if (entity.getY() < deckY || entity.getDeltaMovement().y < 0.0D) {
          entity.setPos(entity.getX(), deckY, entity.getZ());
          entity.setDeltaMovement(entity.getDeltaMovement().x, 0.0D,
              entity.getDeltaMovement().z);
        }
        continue;
      }

      double impactSpeed = Math.abs(this.driveVelocity);
      long gameTime = this.level().getGameTime();
      long lastImpact = this.lastCollisionDamageTick.getOrDefault(entity.getId(), -100L);
      if (driver != null && impactSpeed >= 0.12D && gameTime - lastImpact >= 10L
          && entity instanceof LivingEntity target) {
        float collisionDamage = Mth.clamp((float) (2.0D + impactSpeed * 20.0D), 3.0F, 26.0F);
        if (target.hurt(target.damageSources().mobAttack(driver), collisionDamage)) {
          this.lastCollisionDamageTick.put(entity.getId(), gameTime);
        }
      }

      // Move toward the closest edge in the tank's local coordinate system.
      if (widthLimit - Math.abs(localX) <= lengthLimit - Math.abs(localZ)) {
        localX = Math.copySign(widthLimit + HULL_CLEARANCE,
            localX == 0.0D ? 1.0D : localX);
      } else {
        localZ = Math.copySign(lengthLimit + HULL_CLEARANCE,
            localZ == 0.0D ? 1.0D : localZ);
      }

      double correctedX = this.getX() + cosine * localX - sine * localZ;
      double correctedZ = this.getZ() + sine * localX + cosine * localZ;
      entity.setPos(correctedX, entity.getY(), correctedZ);
      entity.setDeltaMovement(Vec3.ZERO);
    }
  }

  /**
   * Breaks only the row of blocks the hull is travelling into. Forward movement clears the front;
   * reverse movement clears the rear. The gamerule is deliberately off by default.
   */
  private boolean prepareTravelPath(Vec3 movement, LivingEntity driver) {
    if (movement.horizontalDistanceSqr() < 1.0E-6D) {
      return true;
    }

    boolean mayDestroy = this.level().getGameRules()
        .getBoolean(VehicleGameRules.TANK_BLOCK_DESTRUCTION);
    double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double forwardX = -Mth.sin((float) yawRadians);
    double forwardZ = Mth.cos((float) yawRadians);
    double direction = movement.x * forwardX + movement.z * forwardZ >= 0.0D ? 1.0D : -1.0D;
    double rightX = Mth.cos((float) yawRadians);
    double rightZ = Mth.sin((float) yawRadians);
    double leadingDistance = direction * (HULL_HALF_LENGTH + 0.25D);

    boolean pathClear = true;
    double stepSurfaceY = this.getY();
    for (double lateral = -2.25D; lateral <= 2.25D; lateral += 0.75D) {
      double x = this.getX() + forwardX * leadingDistance + rightX * lateral + movement.x;
      double z = this.getZ() + forwardZ * leadingDistance + rightZ * lateral + movement.z;
      for (int yOffset = 0; yOffset <= 1; yOffset++) {
        BlockPos blockPos = BlockPos.containing(x, this.getY() + yOffset, z);
        var blockState = this.level().getBlockState(blockPos);
        var collisionShape = blockState.getCollisionShape(this.level(), blockPos);
        if (blockState.isAir() || collisionShape.isEmpty()) {
          continue;
        }
        // A block directly below the entity commonly shares the same integer Y coordinate as the
        // tank's feet. Its collision top is the supporting ground, not a wall in the travel path.
        AABB collisionBounds = collisionShape.bounds().move(blockPos);
        if (collisionBounds.maxY <= this.getY() + 0.05D
            || collisionBounds.minY >= this.getY() + HULL_DECK_HEIGHT) {
          continue;
        }
        // Tracked vehicles can climb a one-block ledge. This also corrects old tanks that were
        // placed one block into the terrain before their physical hull was implemented.
        if (collisionBounds.maxY <= this.getY() + 1.05D) {
          stepSurfaceY = Math.max(stepSurfaceY, collisionBounds.maxY);
          continue;
        }
        if (mayDestroy && blockState.getDestroySpeed(this.level(), blockPos) >= 0.0F) {
          this.level().destroyBlock(blockPos, true, driver);
          blockState = this.level().getBlockState(blockPos);
        }
        if (!blockState.isAir()
            && !blockState.getCollisionShape(this.level(), blockPos).isEmpty()) {
          pathClear = false;
        }
      }
    }
    if (pathClear && stepSurfaceY > this.getY() + 0.01D) {
      this.setPos(this.getX(), stepSurfaceY, this.getZ());
    }
    return pathClear;
  }

  /**
   * With destruction disabled, only blocks above the one-block tread step may stop the tank.
   * Sampling at hull shoulder height avoids treating the supporting terrain as a wall.
   */
  private boolean isUpperTravelPathClear(Vec3 movement) {
    double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double forwardX = -Mth.sin((float) yawRadians);
    double forwardZ = Mth.cos((float) yawRadians);
    double direction = movement.x * forwardX + movement.z * forwardZ >= 0.0D ? 1.0D : -1.0D;
    double rightX = Mth.cos((float) yawRadians);
    double rightZ = Mth.sin((float) yawRadians);
    double leadingDistance = direction * (HULL_HALF_LENGTH + 0.1D);
    for (double lateral = -3.0D; lateral <= 3.0D; lateral += 1.0D) {
      double x = this.getX() + forwardX * leadingDistance + rightX * lateral + movement.x;
      double z = this.getZ() + forwardZ * leadingDistance + rightZ * lateral + movement.z;
      for (double height : new double[] {1.10D, 1.85D}) {
        BlockPos blockPos = BlockPos.containing(x, this.getY() + height, z);
        if (!this.level().getBlockState(blockPos).getCollisionShape(this.level(), blockPos).isEmpty()) {
          return false;
        }
      }
    }
    return true;
  }

  /** Raises legacy/sunken placements onto the highest nearby supporting collision surface. */
  private void snapToGroundSurface() {
    double currentY = this.getY();
    double surfaceY = currentY;
    int topY = Mth.floor(currentY + 1.05D);
    for (int y = topY; y >= Mth.floor(currentY) - 2; y--) {
      BlockPos pos = BlockPos.containing(this.getX(), y, this.getZ());
      var shape = this.level().getBlockState(pos).getCollisionShape(this.level(), pos);
      if (shape.isEmpty()) continue;
      double candidateY = shape.bounds().move(pos).maxY;
      if (candidateY <= currentY + 1.05D && candidateY > surfaceY) {
        surfaceY = candidateY;
      }
    }
    if (surfaceY > currentY + 0.01D) {
      this.setPos(this.getX(), surfaceY, this.getZ());
    }
  }

  @Override
  public boolean canAddPassenger(Entity passenger) {
    return !this.isDestroyed() && this.getPassengers().size() < 3 && passenger instanceof Player;
  }

  @Override
  public LivingEntity getControllingPassenger() {
    int driverId = this.entityData.get(DRIVER_ID);
    return this.getPassengers().stream()
        .filter(passenger -> passenger.getId() == driverId)
        .findFirst()
        .filter(LivingEntity.class::isInstance)
        .map(LivingEntity.class::cast)
        .orElse(null);
  }

  public LivingEntity getWeaponController() {
    int explicitId = this.entityData.get(WEAPON_CONTROLLER_ID);
    if (explicitId != -1) {
      LivingEntity explicit = this.getPassengerById(explicitId);
      if (explicit != null) return explicit;
    }
    return this.getControllingPassenger();
  }

  private LivingEntity getPassengerById(int entityId) {
    return this.getPassengers().stream()
        .filter(passenger -> passenger.getId() == entityId)
        .findFirst()
        .filter(LivingEntity.class::isInstance)
        .map(LivingEntity.class::cast)
        .orElse(null);
  }

  public boolean isWeaponController(Entity passenger) {
    return passenger != null && this.getWeaponController() == passenger;
  }

  public boolean isRearPassenger(Entity passenger) {
    if (passenger == null) return false;
    int id = passenger.getId();
    return id == this.entityData.get(LEFT_REAR_ID) || id == this.entityData.get(RIGHT_REAR_ID);
  }

  public boolean isPassengerExposed(Entity passenger) {
    if (passenger == null) return false;
    int id = passenger.getId();
    if (id == this.entityData.get(LEFT_REAR_ID)) return this.entityData.get(LEFT_REAR_EXPOSED);
    if (id == this.entityData.get(RIGHT_REAR_ID)) return this.entityData.get(RIGHT_REAR_EXPOSED);
    return false;
  }

  public boolean canUseCoax(Entity passenger) {
    return !this.isRearPassenger(passenger) || this.isPassengerExposed(passenger);
  }

  /** Allows armour penetration/spall to bypass the ordinary closed-hatch projectile shield. */
  public boolean isApplyingInternalDamage(Entity passenger) {
    return passenger != null && this.internalDamageVictims.contains(passenger.getId());
  }

  /** Original turret optic anchor, transformed into the imported model's world scale. */
  public Vec3 getGunnerOpticPosition(float partialTick) {
    float worldYaw = (this.getYRot() + this.getTurretYaw()) * Mth.DEG_TO_RAD;
    // b3_turret camera [0.5299, 1.1, 0.9] plus its installation anchor.
    double localRight = 0.5299D * 1.25D;
    double localForward = (0.9D + 0.1968D) * 1.25D;
    double offsetX = Mth.cos(worldYaw) * localRight - Mth.sin(worldYaw) * localForward;
    double offsetZ = Mth.sin(worldYaw) * localRight + Mth.cos(worldYaw) * localForward;
    return this.getPosition(partialTick).add(offsetX,
        (1.7185D + 1.1D) * 1.25D, offsetZ);
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  public boolean canBeCollidedWith() {
    return this.isAlive();
  }

  @Override
  public boolean isPickable() {
    return this.isAlive();
  }

  private boolean isOnHullDeck(Player player) {
    if (player.getY() < this.getY() + HULL_DECK_HEIGHT - 0.15D
        || player.getY() > this.getY() + HULL_DECK_HEIGHT + 1.5D) {
      return false;
    }
    double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double cosine = Mth.cos((float) yawRadians);
    double sine = Mth.sin((float) yawRadians);
    double worldX = player.getX() - this.getX();
    double worldZ = player.getZ() - this.getZ();
    double localX = cosine * worldX + sine * worldZ;
    double localZ = -sine * worldX + cosine * worldZ;
    return Math.abs(localX) <= HULL_HALF_WIDTH && Math.abs(localZ) <= HULL_HALF_LENGTH;
  }

  private AABB createHullBoundingBox() {
    double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double cosine = Math.abs(Mth.cos((float) yawRadians));
    double sine = Math.abs(Mth.sin((float) yawRadians));
    double xExtent = cosine * HULL_HALF_WIDTH + sine * HULL_HALF_LENGTH;
    double zExtent = sine * HULL_HALF_WIDTH + cosine * HULL_HALF_LENGTH;
    return new AABB(this.getX() - xExtent, this.getY(), this.getZ() - zExtent,
        this.getX() + xExtent, this.getY() + HULL_DECK_HEIGHT, this.getZ() + zExtent);
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (this.level().isClientSide() || this.isDestroyed() || this.isInvulnerableTo(source)) {
      return true;
    }
    int damage = Math.max(1, Mth.ceil(amount));
    if (this.isSlatArmourInstalled() && this.entityData.get(SLAT_HEALTH) > 0) {
      int absorbed = Math.max(1, Mth.ceil(damage * 0.55F));
      this.damageModule(SLAT_HEALTH, absorbed, SLAT_ARMOUR_INSTALLED);
      damage -= Math.min(damage, absorbed);
    }
    Entity direct = source.getDirectEntity();
    if (direct != null) {
      double localY = direct.getY() - this.getY();
      double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
      double dx = direct.getX() - this.getX();
      double dz = direct.getZ() - this.getZ();
      double localX = Mth.cos((float) yawRadians) * dx + Mth.sin((float) yawRadians) * dz;
      double localZ = -Mth.sin((float) yawRadians) * dx + Mth.cos((float) yawRadians) * dz;
      if (localY > 1.25D && this.isTurretInstalled()) {
        this.damageModule(TURRET_HEALTH, damage, TURRET_INSTALLED);
      } else if (localZ < -1.4D && this.isEngineInstalled()) {
        this.damageModule(ENGINE_HEALTH, damage, ENGINE_INSTALLED);
      } else if (Math.abs(localX) > 1.45D) {
        if (localX > 0.0D && this.isLeftTrackInstalled()) {
          this.damageModule(LEFT_TRACK_HEALTH, damage, LEFT_TRACK_INSTALLED);
        } else if (localX < 0.0D && this.isRightTrackInstalled()) {
          this.damageModule(RIGHT_TRACK_HEALTH, damage, RIGHT_TRACK_INSTALLED);
        }
      }
    }
    this.damageIntegrity(damage);
    return true;
  }

  /** Resolves local armour thickness and obliquity for a 125 mm shell impact. */
  public boolean handleShellImpact(T72ShellEntity shell) {
    Vec3 incoming = shell.getDeltaMovement().normalize();
    Vec3 delta = shell.position().subtract(this.position());
    float hullYaw = this.getYRot() * Mth.DEG_TO_RAD;
    double cosine = Mth.cos(hullYaw);
    double sine = Mth.sin(hullYaw);
    double localX = cosine * delta.x + sine * delta.z;
    double localZ = -sine * delta.x + cosine * delta.z;
    boolean turretZone = delta.y > 1.30D;
    boolean sideZone = Math.abs(localX / HULL_HALF_WIDTH) > Math.abs(localZ / HULL_HALF_LENGTH);
    Vec3 localNormal;
    int armour;
    if (turretZone) {
      armour = localZ >= 0.0D ? 520 : 110;
      localNormal = Math.abs(localX) > 1.15D
          ? new Vec3(Math.signum(localX), 0.0D, 0.0D)
          : new Vec3(0.0D, 0.0D, Math.signum(localZ == 0.0D ? 1.0D : localZ));
    } else if (sideZone) {
      armour = 120;
      localNormal = new Vec3(Math.signum(localX), 0.0D, 0.0D);
    } else if (localZ >= 0.0D) {
      armour = 430;
      localNormal = new Vec3(0.0D, 0.0D, 1.0D);
    } else {
      armour = 70;
      localNormal = new Vec3(0.0D, 0.0D, -1.0D);
    }
    Vec3 normal = new Vec3(cosine * localNormal.x - sine * localNormal.z, 0.0D,
        sine * localNormal.x + cosine * localNormal.z).normalize();
    double incidence = Math.max(0.08D, Math.abs(incoming.dot(normal)));
    int penetration = shell.isArmourPiercing() ? 560 : 95;
    double effectiveArmour = armour / incidence;
    if (this.isSlatArmourInstalled() && this.entityData.get(SLAT_HEALTH) > 0
        && !shell.isArmourPiercing()) {
      effectiveArmour += 180.0D;
      this.damageModule(SLAT_HEALTH, 45, SLAT_ARMOUR_INSTALLED);
    }
    if (penetration < effectiveArmour) {
      this.damageIntegrity(shell.isArmourPiercing() ? 4 : 9);
      if (incidence < 0.48D || shell.isArmourPiercing()) {
        shell.ricochet(normal);
        this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND,
            SoundSource.BLOCKS, 1.7F, 1.55F);
        if (this.level() instanceof ServerLevel server) {
          server.sendParticles(ParticleTypes.CRIT, shell.getX(), shell.getY(), shell.getZ(), 18,
              0.35D, 0.30D, 0.35D, 0.18D);
        }
        return true;
      }
      return false;
    }
    int internalDamage = shell.isArmourPiercing() ? 72 : 38;
    if (turretZone && this.isTurretInstalled()) {
      this.damageModule(TURRET_HEALTH, internalDamage * 2, TURRET_INSTALLED);
    } else if (localZ < -1.3D && this.isEngineInstalled()) {
      this.damageModule(ENGINE_HEALTH, internalDamage, ENGINE_INSTALLED);
    } else if (sideZone && Math.abs(localX) > 1.45D) {
      if (localX > 0.0D && this.isLeftTrackInstalled()) {
        this.damageModule(LEFT_TRACK_HEALTH, internalDamage, LEFT_TRACK_INSTALLED);
      } else if (localX < 0.0D && this.isRightTrackInstalled()) {
        this.damageModule(RIGHT_TRACK_HEALTH, internalDamage, RIGHT_TRACK_INSTALLED);
      }
    }
    LivingEntity shellOwner = shell.getOwner() instanceof LivingEntity living ? living : null;
    if (shellOwner != null) {
      for (Entity passenger : this.getPassengers()) {
        if (passenger instanceof LivingEntity livingPassenger) {
          this.internalDamageVictims.add(livingPassenger.getId());
          try {
            livingPassenger.hurt(ModDamageSource.gun(shellOwner, false),
                8.0F + this.random.nextFloat() * internalDamage * 0.30F);
          } finally {
            this.internalDamageVictims.remove(livingPassenger.getId());
          }
        }
      }
    }
    boolean ammoRackZone = sideZone && localZ > -1.4D && localZ < 1.1D;
    if (ammoRackZone && (this.entityData.get(HE_AMMO) + this.entityData.get(AP_AMMO)) > 0
        && this.random.nextFloat() < (shell.isArmourPiercing() ? 0.28F : 0.12F)) {
      this.damageIntegrity(MAX_INTEGRITY);
      if (this.level() instanceof ServerLevel server) {
        server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.6D,
            this.getZ(), 45, 1.8D, 1.0D, 1.8D, 0.16D);
      }
      return false;
    }
    this.damageIntegrity(internalDamage);
    return false;
  }

  private void damageModule(EntityDataAccessor<Integer> health, int damage,
      EntityDataAccessor<Boolean> installed) {
    int remaining = Math.max(0, this.entityData.get(health) - Math.max(0, damage));
    this.entityData.set(health, remaining);
    if (remaining == 0) {
      this.entityData.set(installed, false);
      this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_BREAK,
          SoundSource.BLOCKS, 1.0F, 0.7F);
    }
  }

  private void damageIntegrity(int amount) {
    int remaining = Math.max(0, this.entityData.get(INTEGRITY) - Math.max(0, amount));
    this.entityData.set(INTEGRITY, remaining);
    if (remaining == 0 && !this.isDestroyed()) {
      this.entityData.set(DESTROYED, true);
      this.entityData.set(WRECK_AGE, 0);
      this.entityData.set(WRECK_TURRET_YAW, this.random.nextFloat() * 70.0F - 35.0F);
      this.entityData.set(WRECK_TURRET_ROLL,
          (65.0F + this.random.nextFloat() * 50.0F) * (this.random.nextBoolean() ? 1.0F : -1.0F));
      this.ejectPassengers();
      this.dropCargo();
      ModExplosionUtil.explode(this.level(), this, this.getX(), this.getY() + 1.2D, this.getZ(),
          4.0F, Explosion.BlockInteraction.KEEP);
      this.setRemainingFireTicks(160);
    }
  }

  public boolean isDestroyed() { return this.entityData.get(DESTROYED); }

  public int getWreckAge() { return this.entityData.get(WRECK_AGE); }

  public float getWreckTurretYaw() { return this.entityData.get(WRECK_TURRET_YAW); }

  public float getWreckTurretRoll() { return this.entityData.get(WRECK_TURRET_ROLL); }

  public int getIntegrity() {
    return this.entityData.get(INTEGRITY);
  }

  public int getMaxIntegrity() {
    return MAX_INTEGRITY;
  }

  public int getSkin() {
    return this.entityData.get(SKIN);
  }

  public float getTurretYaw() {
    return this.entityData.get(TURRET_YAW);
  }

  public float getTurretPitch() {
    return this.entityData.get(TURRET_PITCH);
  }

  public float getRecoil() {
    return this.entityData.get(RECOIL);
  }

  public int getFuel() {
    return this.entityData.get(FUEL);
  }

  public int getMaxFuel() {
    return MAX_FUEL;
  }

  public boolean isHighExplosiveSelected() {
    return this.entityData.get(SELECTED_AMMO) == 0;
  }

  public boolean areHeadlightsOn() {
    return this.entityData.get(HEADLIGHTS);
  }

  public int getSelectedMainAmmoCount() {
    return this.entityData.get(this.isHighExplosiveSelected() ? HE_AMMO : AP_AMMO);
  }

  public int getCoaxAmmoCount() {
    return this.entityData.get(COAX_AMMO);
  }

  public int getCoaxLoaded() {
    return this.entityData.get(COAX_LOADED);
  }

  public int getCoaxReloadTicks() {
    return this.entityData.get(COAX_RELOAD);
  }

  public int getSmokeAmmoCount() {
    return this.entityData.get(SMOKE_AMMO);
  }

  public int getSmokeLoaded() {
    return this.entityData.get(SMOKE_LOADED);
  }

  public int getReloadTicks() {
    return this.entityData.get(MAIN_RELOAD);
  }

  public boolean isEngineInstalled() {
    return this.entityData.get(ENGINE_INSTALLED);
  }

  public boolean isTurretInstalled() {
    return this.entityData.get(TURRET_INSTALLED);
  }

  public boolean isSlatArmourInstalled() {
    return this.entityData.get(SLAT_ARMOUR_INSTALLED);
  }

  public boolean isEngineOperational() {
    return this.isEngineInstalled() && this.entityData.get(ENGINE_HEALTH) > 0;
  }

  public boolean isTurretOperational() {
    return this.isTurretInstalled() && this.entityData.get(TURRET_HEALTH) > 0;
  }

  public float getEnginePower() {
    return this.entityData.get(ENGINE_POWER);
  }

  public int getEngineRpm() { return this.entityData.get(ENGINE_RPM); }

  public int getEngineGear() { return this.entityData.get(ENGINE_GEAR); }

  public int getEngineTemperature() { return this.entityData.get(ENGINE_TEMPERATURE); }

  public int getBatteryLevel() { return this.entityData.get(BATTERY); }

  public boolean isEngineRunning() { return this.entityData.get(ENGINE_RUNNING); }

  public int getEngineHealth() { return this.entityData.get(ENGINE_HEALTH); }

  public int getTurretHealth() { return this.entityData.get(TURRET_HEALTH); }

  public int getLeftTrackHealth() { return this.entityData.get(LEFT_TRACK_HEALTH); }

  public int getRightTrackHealth() { return this.entityData.get(RIGHT_TRACK_HEALTH); }

  public float getLeftTrackRotation() {
    return this.entityData.get(LEFT_TRACK_ROTATION);
  }

  public float getRightTrackRotation() {
    return this.entityData.get(RIGHT_TRACK_ROTATION);
  }

  public boolean isLeftTrackInstalled() {
    return this.entityData.get(LEFT_TRACK_INSTALLED);
  }

  public boolean isRightTrackInstalled() {
    return this.entityData.get(RIGHT_TRACK_INSTALLED);
  }

  public boolean hasCompleteTrackSet() {
    return this.isLeftTrackInstalled() && this.isRightTrackInstalled();
  }

  public float getBodyPitch() {
    return this.entityData.get(BODY_PITCH);
  }

  public float getBodyRoll() {
    return this.entityData.get(BODY_ROLL);
  }

  public float getSuspensionOffset() {
    return this.entityData.get(SUSPENSION_OFFSET);
  }

  private InteractionResult installTrack(Player player, InteractionHand hand, Vec3 hitLocation) {
    if (hitLocation == null) {
      return InteractionResult.PASS;
    }
    double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double localX = Mth.cos((float) yawRadians) * hitLocation.x
        + Mth.sin((float) yawRadians) * hitLocation.z;
    boolean left = localX >= 0.0D;
    if (this.level().isClientSide()) {
      return InteractionResult.SUCCESS;
    }
    EntityDataAccessor<Boolean> target = left ? LEFT_TRACK_INSTALLED : RIGHT_TRACK_INSTALLED;
    if (this.entityData.get(target)) {
      player.displayClientMessage(Component.translatable(
          left ? "message.craftingdead.t72b3.left_track_present"
              : "message.craftingdead.t72b3.right_track_present"), true);
      return InteractionResult.CONSUME;
    }
    this.entityData.set(target, true);
    this.entityData.set(left ? LEFT_TRACK_HEALTH : RIGHT_TRACK_HEALTH, 70);
    if (!player.getAbilities().instabuild) {
      player.getItemInHand(hand).shrink(1);
    }
    this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE,
        SoundSource.BLOCKS, 0.8F, 1.15F);
    player.displayClientMessage(Component.translatable(
        left ? "message.craftingdead.t72b3.left_track_installed"
            : "message.craftingdead.t72b3.right_track_installed"), true);
    return InteractionResult.CONSUME;
  }

  private InteractionResult installModule(Player player, InteractionHand hand,
      EntityDataAccessor<Boolean> installed, EntityDataAccessor<Integer> health, int maxHealth,
      String messageKey) {
    if (this.level().isClientSide()) return InteractionResult.SUCCESS;
    if (this.entityData.get(installed) && this.entityData.get(health) > 0) {
      player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.module_present"),
          true);
      return InteractionResult.CONSUME;
    }
    this.entityData.set(installed, true);
    this.entityData.set(health, maxHealth);
    if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
    this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE,
        SoundSource.BLOCKS, 0.9F, 1.05F);
    player.displayClientMessage(Component.translatable(messageKey), true);
    return InteractionResult.CONSUME;
  }

  private InteractionResult repairVehicle(Player player, InteractionHand hand) {
    if (this.level().isClientSide()) return InteractionResult.SUCCESS;
    int before = this.getIntegrity() + this.entityData.get(ENGINE_HEALTH)
        + this.entityData.get(TURRET_HEALTH) + this.entityData.get(LEFT_TRACK_HEALTH)
        + this.entityData.get(RIGHT_TRACK_HEALTH) + this.entityData.get(SLAT_HEALTH);
    this.entityData.set(INTEGRITY, Math.min(MAX_INTEGRITY, this.getIntegrity() + 25));
    if (this.isEngineInstalled()) this.entityData.set(ENGINE_HEALTH,
        Math.min(50, this.entityData.get(ENGINE_HEALTH) + 15));
    if (this.isTurretInstalled()) this.entityData.set(TURRET_HEALTH,
        Math.min(400, this.entityData.get(TURRET_HEALTH) + 25));
    if (this.isLeftTrackInstalled()) this.entityData.set(LEFT_TRACK_HEALTH,
        Math.min(70, this.entityData.get(LEFT_TRACK_HEALTH) + 15));
    if (this.isRightTrackInstalled()) this.entityData.set(RIGHT_TRACK_HEALTH,
        Math.min(70, this.entityData.get(RIGHT_TRACK_HEALTH) + 15));
    if (this.isSlatArmourInstalled()) this.entityData.set(SLAT_HEALTH,
        Math.min(300, this.entityData.get(SLAT_HEALTH) + 20));
    int after = this.getIntegrity() + this.entityData.get(ENGINE_HEALTH)
        + this.entityData.get(TURRET_HEALTH) + this.entityData.get(LEFT_TRACK_HEALTH)
        + this.entityData.get(RIGHT_TRACK_HEALTH) + this.entityData.get(SLAT_HEALTH);
    if (after > before) {
      if (!player.getAbilities().instabuild) {
        player.getItemInHand(hand).hurtAndBreak(1, player,
            hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
      }
      this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR,
          SoundSource.BLOCKS, 0.8F, 1.0F);
      player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.repaired"), true);
    } else {
      player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.no_repairs"), true);
    }
    return InteractionResult.CONSUME;
  }

  private InteractionResult refuel(Player player, InteractionHand hand) {
    if (this.level().isClientSide()) return InteractionResult.SUCCESS;
    if (this.getFuel() >= MAX_FUEL) {
      player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.fuel_full"), true);
      return InteractionResult.CONSUME;
    }
    int fuelAdded = player.getItemInHand(hand).is(VehicleContent.DIESEL_CAN.get()) ? 5000 : 1200;
    this.entityData.set(FUEL, Math.min(MAX_FUEL, this.getFuel() + fuelAdded));
    if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
    player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.refueled",
        this.getFuel(), MAX_FUEL), true);
    return InteractionResult.CONSUME;
  }

  private InteractionResult applySkin(Player player, InteractionHand hand, int dyeId) {
    int skin = switch (dyeId) {
      case 0 -> 2;  // white: Z recognition marking
      case 4 -> 3;  // yellow: V recognition marking
      case 14 -> 4; // red: X recognition marking
      case 11, 12, 13 -> 0; // blue/green/brown: standard green
      default -> 1; // remaining dyes: desert tan
    };
    if (this.level().isClientSide()) return InteractionResult.SUCCESS;
    this.entityData.set(SKIN, skin);
    if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
    player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.skin_applied"), true);
    return InteractionResult.CONSUME;
  }

  private InteractionResult salvageWreck(Player player, InteractionHand hand) {
    if (this.level().isClientSide()) return InteractionResult.SUCCESS;
    this.spawnAtLocation(new ItemStack(ModItems.SCRAP_METAL.get(), 8 + this.random.nextInt(9)));
    if (this.isEngineInstalled()) {
      this.spawnAtLocation(new ItemStack(VehicleContent.DAMAGED_ENGINE_VTA.get()));
    }
    int damagedTracks = 0;
    if (this.isLeftTrackInstalled() && this.random.nextFloat() < 0.85F) damagedTracks++;
    if (this.isRightTrackInstalled() && this.random.nextFloat() < 0.85F) damagedTracks++;
    if (damagedTracks > 0) {
      this.spawnAtLocation(new ItemStack(VehicleContent.DAMAGED_TRACK_S.get(), damagedTracks));
    }
    if (this.isTurretInstalled() && this.random.nextFloat() < 0.22F) {
      this.spawnAtLocation(new ItemStack(VehicleContent.DAMAGED_T72B3_TURRET.get()));
    }
    if (this.isSlatArmourInstalled() && this.random.nextFloat() < 0.28F) {
      this.spawnAtLocation(new ItemStack(VehicleContent.DAMAGED_RESHETKA_EPTA.get()));
    }
    if (!player.getAbilities().instabuild) {
      player.getItemInHand(hand).hurtAndBreak(4, player,
          hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }
    this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE,
        SoundSource.BLOCKS, 1.0F, 0.72F);
    player.displayClientMessage(Component.translatable("message.craftingdead.t72b3.salvaged"), true);
    this.discard();
    return InteractionResult.CONSUME;
  }

  /** Accepts movement from the driver and weapon input from the currently selected turret seat. */
  public void acceptPassengerInput(Player player, float throttle, float turn, float lookYaw,
      float lookPitch, int actions) {
    if (this.level().isClientSide()) {
      return;
    }
    if (!this.hasPassenger(player) || !Float.isFinite(throttle) || !Float.isFinite(turn)
        || !Float.isFinite(lookYaw) || !Float.isFinite(lookPitch)) return;
    boolean toggleDown = (actions & ACTION_TOGGLE_GUN_CONTROL) != 0;
    if (toggleDown && this.gunControlToggleHeld.add(player.getId())) {
      int explicitId = this.entityData.get(WEAPON_CONTROLLER_ID);
      this.entityData.set(WEAPON_CONTROLLER_ID, explicitId == player.getId() ? -1 : player.getId());
      player.displayClientMessage(Component.translatable(
          this.isWeaponController(player) ? "message.craftingdead.t72b3.gun_control_on"
              : "message.craftingdead.t72b3.gun_control_off"), true);
    } else if (!toggleDown) {
      this.gunControlToggleHeld.remove(player.getId());
    }
    boolean exposureDown = (actions & ACTION_TOGGLE_EXPOSURE) != 0;
    if (exposureDown && this.exposureToggleHeld.add(player.getId())
        && this.isRearPassenger(player)) {
      boolean exposed = !this.isPassengerExposed(player);
      this.setPassengerExposed(player, exposed);
      if (!exposed) {
        this.entityData.set(WEAPON_CONTROLLER_ID, player.getId());
      }
      this.level().playSound(null, this.blockPosition(), exposed
              ? ModSoundEvents.T72_HATCH_OPEN.get() : ModSoundEvents.T72_HATCH_CLOSE.get(),
          SoundSource.BLOCKS, 0.9F, 1.0F);
      player.displayClientMessage(Component.translatable(exposed
          ? "message.craftingdead.t72b3.exposed" : "message.craftingdead.t72b3.retracted"), true);
    } else if (!exposureDown) {
      this.exposureToggleHeld.remove(player.getId());
    }
    if (this.getControllingPassenger() == player) {
      this.driverThrottle = Mth.clamp(throttle, -1.0F, 1.0F);
      this.driverTurn = Mth.clamp(turn, -1.0F, 1.0F);
      this.lastDriverInputTick = this.level().getGameTime();
    }
    if (this.isWeaponController(player)) {
      this.driverLookYaw = Mth.wrapDegrees(lookYaw);
      this.driverLookPitch = Mth.clamp(lookPitch, -90.0F, 90.0F);
      this.driverActions = actions & 0x3F;
      this.lastWeaponInputTick = this.level().getGameTime();
    }
    if (this.getControllingPassenger() == player
        && (Math.abs(this.driverThrottle) > 0.01F || Math.abs(this.driverTurn) > 0.01F)
        && this.level().getGameTime() - this.lastDriveBlockNotice >= 20L) {
      Component reason = null;
      if (!this.hasCompleteTrackSet()) {
        reason = Component.translatable("message.craftingdead.t72b3.drive_no_tracks");
      } else if (!this.isEngineOperational()) {
        reason = Component.translatable("message.craftingdead.t72b3.drive_no_engine");
      } else if (this.getFuel() <= 0) {
        reason = Component.translatable("message.craftingdead.t72b3.drive_no_fuel");
      }
      if (reason != null) {
        player.displayClientMessage(reason, true);
        this.lastDriveBlockNotice = this.level().getGameTime();
      }
    }
    if (this.getControllingPassenger() == player && !this.reportedDriverInput
        && (Math.abs(this.driverThrottle) > 0.01F || Math.abs(this.driverTurn) > 0.01F)) {
      this.reportedDriverInput = true;
      LOGGER.info("T-72B3 {} received driver WASD input (throttle={}, turn={})", this.getId(),
          this.driverThrottle, this.driverTurn);
    }
  }

  /** Local prediction keeps the controlling client visually aligned with server-authoritative input. */
  public void applyClientPrediction(float throttle, float turn) {
    if (!this.level().isClientSide()) {
      return;
    }
    this.runTrackedEngine(Mth.clamp(throttle, -1.0F, 1.0F),
        Mth.clamp(turn, -1.0F, 1.0F), this.getControllingPassenger(), false);
  }

  @Override
  protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
    if (!this.hasPassenger(passenger)) {
      return;
    }
    // Original T-72B3 definition places the controller at [0, 0.9, 2.0]. After applying the
    // imported OBJ's origin and scale, that is a seated point just inside the upper hull.
    double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double cosine = Mth.cos((float) yawRadians);
    double sine = Mth.sin((float) yawRadians);
    int passengerId = passenger.getId();
    if (passengerId == this.entityData.get(DRIVER_ID)) {
      // Original LAV controller seat [0, 0.9, 2.0], converted through the 1.25 render scale.
      double seatX = this.getX() - sine * 2.45D;
      double seatZ = this.getZ() + cosine * 2.45D;
      moveFunction.accept(passenger, seatX, this.getY() + 0.18D, seatZ);
    } else {
      // Both rear stations are mounted on the turret ring, so their world position traverses with
      // the turret just like the hatches and optics in the original vehicle definition.
      double turretRadians = (this.getYRot() + this.getTurretYaw()) * Mth.DEG_TO_RAD;
      double turretCosine = Mth.cos((float) turretRadians);
      double turretSine = Mth.sin((float) turretRadians);
      double ringForward = 0.246D;
      double ringX = this.getX() - sine * ringForward;
      double ringZ = this.getZ() + cosine * ringForward;
      // Inset both stations from the hatch rims. The asymmetric right lid requires more clearance.
      double relativeX = passengerId == this.entityData.get(LEFT_REAR_ID) ? -0.72D : 0.68D;
      double relativeZ = -0.82D;
      double seatX = ringX + turretCosine * relativeX - turretSine * relativeZ;
      double seatZ = ringZ + turretSine * relativeX + turretCosine * relativeZ;
      boolean exposed = this.isPassengerExposed(passenger);
      double seatY = this.getY() + (exposed
          ? (passengerId == this.entityData.get(LEFT_REAR_ID) ? 2.06D : 2.08D)
          : 0.62D);
      moveFunction.accept(passenger, seatX, seatY, seatZ);
    }
  }

  @Override
  public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
    float yaw = this.getYRot() * Mth.DEG_TO_RAD;
    Vec3 right = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
    Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
    double side = passenger.getId() == this.entityData.get(LEFT_REAR_ID) ? 1.0D : -1.0D;
    return this.position().add(right.scale(side * 3.35D)).add(forward.scale(0.7D))
        .add(0.0D, 2.15D, 0.0D);
  }

  private EntityDataAccessor<Integer> selectSeat(Vec3 hitLocation) {
    if (hitLocation == null) {
      if (this.entityData.get(DRIVER_ID) == -1) return DRIVER_ID;
      if (this.entityData.get(LEFT_REAR_ID) == -1) return LEFT_REAR_ID;
      return RIGHT_REAR_ID;
    }
    double yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
    double cosine = Mth.cos((float) yawRadians);
    double sine = Mth.sin((float) yawRadians);
    double localX = cosine * hitLocation.x + sine * hitLocation.z;
    double localZ = -sine * hitLocation.x + cosine * hitLocation.z;
    double driverDistance = Mth.square(localX) + Mth.square(localZ - 2.45D);
    double turretRadians = (this.getYRot() + this.getTurretYaw()) * Mth.DEG_TO_RAD;
    double turretCosine = Mth.cos((float) turretRadians);
    double turretSine = Mth.sin((float) turretRadians);
    double ringWorldX = -sine * 0.246D;
    double ringWorldZ = cosine * 0.246D;
    double rearX = turretCosine * (hitLocation.x - ringWorldX)
        + turretSine * (hitLocation.z - ringWorldZ);
    double rearZ = -turretSine * (hitLocation.x - ringWorldX)
        + turretCosine * (hitLocation.z - ringWorldZ);
    double leftDistance = Mth.square(rearX + 0.88D) + Mth.square(rearZ + 0.82D);
    double rightDistance = Mth.square(rearX - 0.98D) + Mth.square(rearZ + 0.82D);
    if (driverDistance <= leftDistance && driverDistance <= rightDistance) {
      return DRIVER_ID;
    }
    return leftDistance <= rightDistance ? LEFT_REAR_ID : RIGHT_REAR_ID;
  }

  private void releaseEmptySeats() {
    this.releaseSeatIfMissing(DRIVER_ID);
    this.releaseSeatIfMissing(LEFT_REAR_ID);
    this.releaseSeatIfMissing(RIGHT_REAR_ID);
  }

  private void releaseSeatIfMissing(EntityDataAccessor<Integer> seat) {
    int passengerId = this.entityData.get(seat);
    if (passengerId != -1
        && this.getPassengers().stream().noneMatch(passenger -> passenger.getId() == passengerId)) {
      this.entityData.set(seat, -1);
      this.gunControlToggleHeld.remove(passengerId);
      this.exposureToggleHeld.remove(passengerId);
      if (this.entityData.get(WEAPON_CONTROLLER_ID) == passengerId) {
        this.entityData.set(WEAPON_CONTROLLER_ID, -1);
      }
      this.setSeatExposed(seat, false);
      this.level().playSound(null, this.blockPosition(), ModSoundEvents.T72_HATCH_CLOSE.get(),
          SoundSource.BLOCKS, 0.85F, 1.0F);
    }
  }

  public boolean isDriverSeatOccupied() {
    return this.entityData.get(DRIVER_ID) != -1;
  }

  public boolean isLeftRearSeatOccupied() {
    return this.entityData.get(LEFT_REAR_ID) != -1;
  }

  public boolean isLeftRearSeatExposed() {
    return this.isLeftRearSeatOccupied() && this.entityData.get(LEFT_REAR_EXPOSED);
  }

  public boolean isRightRearSeatOccupied() {
    return this.entityData.get(RIGHT_REAR_ID) != -1;
  }

  public boolean isRightRearSeatExposed() {
    return this.isRightRearSeatOccupied() && this.entityData.get(RIGHT_REAR_EXPOSED);
  }

  private void setPassengerExposed(Entity passenger, boolean exposed) {
    int id = passenger.getId();
    if (id == this.entityData.get(LEFT_REAR_ID)) this.entityData.set(LEFT_REAR_EXPOSED, exposed);
    if (id == this.entityData.get(RIGHT_REAR_ID)) this.entityData.set(RIGHT_REAR_EXPOSED, exposed);
  }

  private void setSeatExposed(EntityDataAccessor<Integer> seat, boolean exposed) {
    if (seat == LEFT_REAR_ID) this.entityData.set(LEFT_REAR_EXPOSED, exposed);
    if (seat == RIGHT_REAR_ID) this.entityData.set(RIGHT_REAR_EXPOSED, exposed);
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("container.craftingdead.t_72b3");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return ChestMenu.threeRows(id, inventory, this.cargo);
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    this.entityData.set(INTEGRITY, tag.contains("Integrity")
        ? Math.clamp(tag.getInt("Integrity"), 0, MAX_INTEGRITY) : MAX_INTEGRITY);
    this.entityData.set(LEFT_TRACK_INSTALLED, tag.getBoolean("LeftTrackInstalled"));
    this.entityData.set(RIGHT_TRACK_INSTALLED, tag.getBoolean("RightTrackInstalled"));
    // Turret and engine were built-in defaults before modular damage was ported. Missing legacy
    // keys therefore mean installed, avoiding silently bricking existing worlds.
    this.entityData.set(ENGINE_INSTALLED,
        !tag.contains("EngineInstalled") || tag.getBoolean("EngineInstalled"));
    this.entityData.set(TURRET_INSTALLED,
        !tag.contains("TurretInstalled") || tag.getBoolean("TurretInstalled"));
    this.entityData.set(SLAT_ARMOUR_INSTALLED, tag.getBoolean("SlatArmourInstalled"));
    this.entityData.set(FUEL, tag.contains("Fuel") ? Math.clamp(tag.getInt("Fuel"), 0, MAX_FUEL)
        : MAX_FUEL);
    this.entityData.set(SELECTED_AMMO, Math.clamp(tag.getInt("SelectedAmmo"), 0, 1));
    this.entityData.set(MAIN_RELOAD, Math.max(0, tag.getInt("MainReload")));
    this.entityData.set(ENGINE_HEALTH, tag.contains("EngineHealth")
        ? Math.clamp(tag.getInt("EngineHealth"), 0, 50) : 50);
    this.entityData.set(TURRET_HEALTH, tag.contains("TurretHealth")
        ? Math.clamp(tag.getInt("TurretHealth"), 0, 400) : 400);
    this.entityData.set(LEFT_TRACK_HEALTH, tag.contains("LeftTrackHealth")
        ? Math.clamp(tag.getInt("LeftTrackHealth"), 0, 70) : 70);
    this.entityData.set(RIGHT_TRACK_HEALTH, tag.contains("RightTrackHealth")
        ? Math.clamp(tag.getInt("RightTrackHealth"), 0, 70) : 70);
    this.entityData.set(SLAT_HEALTH, tag.contains("SlatHealth")
        ? Math.clamp(tag.getInt("SlatHealth"), 0, 300) : 300);
    this.entityData.set(SKIN, Math.clamp(tag.getInt("Skin"), 0, 4));
    this.entityData.set(HEADLIGHTS, tag.getBoolean("Headlights"));
    this.entityData.set(COAX_LOADED, Math.clamp(tag.getInt("CoaxLoaded"), 0, COAX_CAPACITY));
    this.entityData.set(COAX_RELOAD, Math.max(0, tag.getInt("CoaxReload")));
    this.entityData.set(SMOKE_LOADED, Math.clamp(tag.getInt("SmokeLoaded"), 0, SMOKE_CAPACITY));
    this.entityData.set(SMOKE_RELOAD, Math.max(0, tag.getInt("SmokeReload")));
    this.entityData.set(ENGINE_TEMPERATURE, tag.contains("EngineTemperature")
        ? Math.clamp(tag.getInt("EngineTemperature"), 0, 125) : 20);
    this.entityData.set(BATTERY, tag.contains("Battery")
        ? Math.clamp(tag.getInt("Battery"), 0, 100) : 100);
    this.automaticGear = tag.contains("AutomaticGear")
        ? Math.clamp(tag.getInt("AutomaticGear"), 1, 5) : 1;
    this.engineRpmState = tag.contains("EngineRpm")
        ? Math.clamp(tag.getFloat("EngineRpm"), 0.0F, ENGINE_MAX_RPM) : 0.0F;
    this.entityData.set(ENGINE_RPM, Math.round(this.engineRpmState));
    this.entityData.set(DESTROYED, tag.getBoolean("Destroyed"));
    this.entityData.set(WRECK_AGE, Math.max(0, tag.getInt("WreckAge")));
    this.entityData.set(WRECK_TURRET_YAW, tag.getFloat("WreckTurretYaw"));
    this.entityData.set(WRECK_TURRET_ROLL, tag.contains("WreckTurretRoll")
        ? tag.getFloat("WreckTurretRoll") : 78.0F);
    NonNullList<ItemStack> items = NonNullList.withSize(CARGO_SIZE, ItemStack.EMPTY);
    ContainerHelper.loadAllItems(tag, items, this.registryAccess());
    this.cargo = new SimpleContainer(items.toArray(ItemStack[]::new));
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    tag.putInt("Integrity", this.getIntegrity());
    tag.putBoolean("LeftTrackInstalled", this.isLeftTrackInstalled());
    tag.putBoolean("RightTrackInstalled", this.isRightTrackInstalled());
    tag.putBoolean("EngineInstalled", this.isEngineInstalled());
    tag.putBoolean("TurretInstalled", this.isTurretInstalled());
    tag.putBoolean("SlatArmourInstalled", this.isSlatArmourInstalled());
    tag.putInt("Fuel", this.getFuel());
    tag.putInt("SelectedAmmo", this.entityData.get(SELECTED_AMMO));
    tag.putInt("MainReload", this.entityData.get(MAIN_RELOAD));
    tag.putInt("EngineHealth", this.entityData.get(ENGINE_HEALTH));
    tag.putInt("TurretHealth", this.entityData.get(TURRET_HEALTH));
    tag.putInt("LeftTrackHealth", this.entityData.get(LEFT_TRACK_HEALTH));
    tag.putInt("RightTrackHealth", this.entityData.get(RIGHT_TRACK_HEALTH));
    tag.putInt("SlatHealth", this.entityData.get(SLAT_HEALTH));
    tag.putInt("Skin", this.getSkin());
    tag.putBoolean("Headlights", this.areHeadlightsOn());
    tag.putInt("CoaxLoaded", this.entityData.get(COAX_LOADED));
    tag.putInt("CoaxReload", this.entityData.get(COAX_RELOAD));
    tag.putInt("SmokeLoaded", this.entityData.get(SMOKE_LOADED));
    tag.putInt("SmokeReload", this.entityData.get(SMOKE_RELOAD));
    tag.putInt("EngineTemperature", this.entityData.get(ENGINE_TEMPERATURE));
    tag.putInt("Battery", this.entityData.get(BATTERY));
    tag.putInt("AutomaticGear", this.automaticGear);
    tag.putFloat("EngineRpm", this.engineRpmState);
    tag.putBoolean("Destroyed", this.isDestroyed());
    tag.putInt("WreckAge", this.getWreckAge());
    tag.putFloat("WreckTurretYaw", this.getWreckTurretYaw());
    tag.putFloat("WreckTurretRoll", this.getWreckTurretRoll());
    NonNullList<ItemStack> items = NonNullList.withSize(CARGO_SIZE, ItemStack.EMPTY);
    for (int index = 0; index < CARGO_SIZE; index++) {
      items.set(index, this.cargo.getItem(index));
    }
    ContainerHelper.saveAllItems(tag, items, this.registryAccess());
  }

  private void dropCargo() {
    for (int index = 0; index < this.cargo.getContainerSize(); index++) {
      ItemStack stack = this.cargo.getItem(index);
      if (!stack.isEmpty()) {
        this.spawnAtLocation(stack.copy());
        this.cargo.setItem(index, ItemStack.EMPTY);
      }
    }
  }

  private record HullSection(double centerX, double centerZ, double halfWidth,
      double halfLength) {}

  private record SourceHullBox(double centerX, double centerY, double centerZ,
      double halfWidth, double halfHeight) {}

  private record CollisionHit(BlockPos pos, AABB box) {}
}
