/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.core.world.item.gun;

import com.craftingdead.core.util.ItemStackNbtUtil;
import com.craftingdead.core.util.RegistryNameUtil;
import net.minecraft.network.chat.Component;
import com.craftingdead.core.network.message.play.NPCTriggerPressedMessage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.event.GunEvent;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.SynchedData;
import com.craftingdead.core.network.message.play.HitMessage;
import com.craftingdead.core.network.message.play.SecondaryActionMessage;
import com.craftingdead.core.network.message.play.SyncGunEquipmentSlotMessage;
import com.craftingdead.core.network.message.play.SetFireModeMessage;
import com.craftingdead.core.network.message.play.TriggerPressedMessage;
import com.craftingdead.core.sounds.ModSoundEvents;
import com.craftingdead.core.util.RayTraceUtil;
import com.craftingdead.core.world.damagesource.ModDamageSource;
import com.craftingdead.core.world.entity.extension.EntitySnapshot;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.projectile.RpgRocket;
import com.craftingdead.core.world.entity.explosion.ModExplosionUtil;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.inventory.GunCraftSlotType;
import com.craftingdead.core.world.item.enchantment.EnchantmentLevelUtil;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.equipment.Hat;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProvider;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProviderType;
import com.craftingdead.core.world.item.gun.ammoprovider.MagazineAmmoProvider;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProviderTypes;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.Attachments;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.gun.magazine.MagazineStackDataComponentUtil;
import com.craftingdead.core.world.item.gun.skin.Paint;
import com.craftingdead.core.world.item.gun.skin.Skin;
import com.craftingdead.core.world.item.gun.skin.SkinLookupUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import com.craftingdead.survival.world.entity.SurvivalZombie;
import com.craftingdead.survival.world.SurvivalEnchantments;
import com.craftingdead.survival.world.effect.SurvivalMobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import com.craftingdead.core.util.LegacyCompoundSerializable;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class AbstractGun implements Gun, LegacyCompoundSerializable {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final Logger logger = LogUtils.getLogger();

  public static final byte HIT_VALIDATION_DELAY_TICKS = 3;

  private static final ScheduledExecutorService executorService =
      Executors.newScheduledThreadPool(3,
          new ThreadFactoryBuilder()
              .setNameFormat("gun-pool-%s")
              .setDaemon(true)
              .setPriority(Thread.MAX_PRIORITY)
              .build());

  /**
   * Keeps trigger ownership outside a single item capability instance. During the 1.21.1 port,
   * client/server ItemStack capabilities can be recreated when the gun stack is synced. If a mouse
   * release is handled by the new Gun instance while the old instance owns the shooting future, the
   * old future keeps consuming ammo. This map lets release packets stop that old owner.
   */
  private static final ConcurrentMap<String, AbstractGun> ACTIVE_TRIGGER_GUNS =
      new ConcurrentHashMap<>();

  private static final ConcurrentMap<String, Boolean> ACTIVE_SECONDARY_ACTIONS =
      new ConcurrentHashMap<>();

  private static String activeTriggerKey(LivingExtension<?, ?> living) {
    return (living.level().isClientSide() ? "C:" : "S:") + living.entity().getId();
  }

  private static String activeActionKey(LivingExtension<?, ?> living, ItemStack stack) {
    return activeTriggerKey(living) + ":" + itemId(stack);
  }

  private static ResourceLocation itemId(ItemStack stack) {
    return stack == null || stack.isEmpty()
        ? BuiltInRegistries.ITEM.getKey(Items.AIR)
        : BuiltInRegistries.ITEM.getKey(stack.getItem());
  }

  /**
   * Stop the active client/server firing loop for the supplied entity. This is used when the
   * visible main-hand stack changes, the player drops the gun, or the server rejects a stale shot
   * request. The optional expected gun id prevents an old rejection packet for AK47 from stopping a
   * newly-started minigun loop after hotbar switching.
   */
  public static void stopActiveTrigger(LivingExtension<?, ?> living, ResourceLocation expectedGunId,
      boolean sendRelease) {
    if (living == null) {
      return;
    }

    boolean stopped = false;
    final String triggerKey = activeTriggerKey(living);
    var activeGun = ACTIVE_TRIGGER_GUNS.get(triggerKey);
    if (activeGun != null) {
      if (expectedGunId == null || expectedGunId.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))
          || itemId(activeGun.itemStack).equals(expectedGunId)) {
        activeGun.stopShooting();
        stopped = true;
      }
    }

    // Do not spam server with gun=air release packets every tick when no active firing loop exists.
    // v18 used those packets as state requests, but sending them with AIR created hundreds of
    // useless TriggerMessage lines and could race with hotbar sync. Only forward a release if we
    // actually stopped a local owner or if the caller requested a specific gun id.
    if (sendRelease && living.level().isClientSide()
        && (stopped || (expectedGunId != null
            && !expectedGunId.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))))) {
      PacketDistributor.sendToServer(
          new TriggerPressedMessage(living.entity().getId(), false, expectedGunId));
    }
  }

  public static void stopActiveTrigger(LivingExtension<?, ?> living, boolean sendRelease) {
    stopActiveTrigger(living, null, sendRelease);
  }

  private static final EntityDataAccessor<ItemStack> PAINT_STACK =
      new EntityDataAccessor<>(0x01, EntityDataSerializers.ITEM_STACK);

  private final SynchedData dataManager = new SynchedData();

  protected final ItemStack itemStack;

  /**
   * If the gun's trigger is pressed.
   *
   * @see #triggerPressedUpdater
   */
  private boolean wasTriggerPressed;

  /**
   * The amount of ticks the trigger has been pressed for. Used to determine if a hit validation
   * packet is valid.
   */
  private volatile int triggerPressedTicks;

  /**
   * The current {@link FireMode} being used.
   */
  private volatile FireMode fireMode;

  /**
   * The amount of shots since the last time the trigger was pressed. Used to determine if the gun
   * can continue firing using the current fire mode.
   */
  private final AtomicInteger shotCount = new AtomicInteger();

  /** Prevents pellets or duplicate pending-hit packets from rolling a combat effect repeatedly. */
  private final ConcurrentMap<Integer, Long> combatEffectTicks = new ConcurrentHashMap<>();

  /**
   * Immutable set of attachments.
   */
  private Map<GunCraftSlotType, Attachment> attachments;
  private boolean attachmentsDirty;

  private Iterator<FireMode> fireModeInfiniteIterator;

  private volatile boolean performingSecondaryAction;

  private Lazy<GunClientRuntime> client;

  // Used to ensure the gun thread gets killed if we're not being ticked anymore.
  private volatile long lastTickMs;

  private volatile AmmoProvider ammoProvider;
  private boolean ammoProviderChanged;

  @Nullable
  private volatile Future<?> gunFuture;

  /**
   * Logical trigger state independent of the scheduled future. On the server, player-controlled
   * guns are now driven one shot at a time by client shot requests, so there may be no server
   * future even while the trigger is held. Pending-hit validation still needs a true trigger state.
   */
  private volatile boolean triggerHeld;

  protected volatile long lastShotMs;

  private volatile long lastTriggerPressedUpdateMs;

  private volatile String activeTriggerKey;

  private boolean initialized;

  @Nullable
  private Holder<Skin> skin;
  private boolean skinDirty;

  public AbstractGun(ItemStack itemStack) {
    this.itemStack = itemStack;
    this.dataManager.register(PAINT_STACK, ItemStack.EMPTY);
  }

  protected abstract GunClientRuntime createClient();

  public void checkInitialized() {
    Preconditions.checkState(this.initialized, "Gun not initialized.");
  }

  protected void initialize() {
    if (this.initialized) {
      throw new IllegalStateException("Already initialized");
    }
    this.initialized = true;

    var event = new GunEvent.Initialize(this, this.itemStack,
        this.attachments == null ? Map.of() : this.attachments,
        this.ammoProvider == null ? this.createAmmoProvider() : this.ammoProvider);
    NeoForge.EVENT_BUS.post(event);

    // Construction precedes deserializeNBT: do not flush empty live state over the saved gun.
    this.ammoProvider = event.getAmmoProvider();
    this.ammoProviderChanged = true;
    this.attachments = Map.copyOf(event.getAttachments());

    this.fireModeInfiniteIterator = Iterables.cycle(Iterables.filter(this.getFireModes(),
        (mode) -> mode != FireMode.BURST || ServerConfig.instance.burstfireEnabled.get()))
        .iterator();
    this.fireMode = this.fireModeInfiniteIterator.next();

    this.client = FMLEnvironment.dist.isClient()
        ? Lazy.of(() -> this.createClient())
        : Lazy.of(() -> {
          throw new IllegalStateException("Cannot access gun client on server.");
        });
  }

  protected abstract Set<FireMode> getFireModes();

  protected abstract AmmoProvider createAmmoProvider();

  private CompoundTag readPersistedGunState() {
    return GunStackDataComponentUtil.readGunState(this.itemStack);
  }

  /**
   * In 1.21.1 the ItemStack capability can be recreated after container or equipment sync. Some
   * callers, such as zoom/overlay logic, read the live Gun capability instead of the renderer's
   * fallback data. Restore visual/action state from CUSTOM_DATA before those callers make their
   * decisions so attachments have real effects and not only render-time effects.
   */
  private void restorePersistedAttachmentsIfNeeded() {
    if (this.attachments != null && !this.attachments.isEmpty()) {
      return;
    }

    var gunState = this.readPersistedGunState();
    if (!gunState.contains(GunStackDataComponentUtil.ATTACHMENTS, Tag.TAG_LIST)) {
      return;
    }

    var restoredAttachments = gunState.getList(GunStackDataComponentUtil.ATTACHMENTS,
            Tag.TAG_STRING)
        .stream()
        .map(Tag::getAsString)
        .map(ResourceLocation::tryParse)
        .filter(java.util.Objects::nonNull)
        .map(Attachments.registry::get)
        .filter(java.util.Objects::nonNull)
        .filter(attachment -> attachment.getInventorySlot() != null)
        .collect(Collectors.toMap(Attachment::getInventorySlot, attachment -> attachment,
            (left, right) -> right));

    if (!restoredAttachments.isEmpty()) {
      this.attachments = Map.copyOf(restoredAttachments);
    }
  }

  private ItemStack restorePersistedPaintStackIfNeeded() {
    var paintStack = this.dataManager.get(PAINT_STACK);
    if (!paintStack.isEmpty()) {
      return paintStack;
    }

    var gunState = this.readPersistedGunState();
    if (!gunState.contains(GunStackDataComponentUtil.PAINT_STACK, Tag.TAG_COMPOUND)) {
      return paintStack;
    }

    var restoredPaintStack = ItemStackNbtUtil.parseOptional(
        gunState.getCompound(GunStackDataComponentUtil.PAINT_STACK));
    if (!restoredPaintStack.isEmpty()) {
      this.dataManager.set(PAINT_STACK, restoredPaintStack);
      return restoredPaintStack;
    }
    return paintStack;
  }

  private void restorePersistedSkinIfNeeded() {
    if (this.skin != null) {
      return;
    }

    var gunState = this.readPersistedGunState();
    this.skin = SkinLookupUtil.load(gunState);
    if (this.skin != null) {
      return;
    }

    var paintStack = this.restorePersistedPaintStackIfNeeded();
    if (!paintStack.isEmpty()) {
      this.skin = Paint.get(paintStack)
          .map(Paint::getSkin)
          .orElse(null);
    }
  }

  private void refreshAmmoProviderFromStackIfEmpty() {
    this.refreshAmmoProviderFromStackIfEmpty(this.itemStack);
  }

  private boolean isCurrentMainHandGun(LivingExtension<?, ?> living) {
    if (living == null) {
      return true;
    }
    ItemStack mainHandStack = living.mainHandItem();
    return mainHandStack != null && !mainHandStack.isEmpty()
        && !this.itemStack.isEmpty() && mainHandStack.is(this.itemStack.getItem());
  }

  private boolean isMagazineAccepted(ItemStack magazineStack) {
    return magazineStack != null && !magazineStack.isEmpty()
        && this.getAcceptedMagazines().contains(magazineStack.getItem());
  }

  private void refreshAmmoProviderFromLivingStack(LivingExtension<?, ?> living) {
    if (living != null) {
      ItemStack mainHandStack = living.mainHandItem();
      if (mainHandStack == null || mainHandStack.isEmpty()
          || this.itemStack.isEmpty() || !mainHandStack.is(this.itemStack.getItem())) {
        // A scheduled client firing task can outlive the visible stack after hotbar switching.
        // Do not let that old gun instance recover ammo from the newly-held gun's cache/state.
        if (this.isTriggerPressed()) {
          this.stopShooting();
        }
        return;
      }

      this.refreshAmmoProviderFromStackIfEmpty(mainHandStack);

      // A server Gun runtime object may be recreated after an ItemStack/data-component sync.
      // Restore only from the CURRENT main-hand stack's persisted loaded-magazine component.
      // Never use the old runtime/authoritative cache here, because that cache may contain a stale
      // creative-mode full magazine and would resurrect ammo after switching to survival.
      if (!living.level().isClientSide()
          && this.ammoProvider instanceof MagazineAmmoProvider magazineProvider
          && this.ammoProvider.getMagazine().map(Magazine::getSize).orElse(0) <= 0) {
        ItemStack stackMagazine = GunAmmoStateUtil.getLoadedMagazineStack(mainHandStack);
        if (!stackMagazine.isEmpty()
            && GunAmmoStateUtil.getMagazineSize(stackMagazine) > 0
            && this.isMagazineAccepted(stackMagazine)) {
          magazineProvider.setMagazineStack(stackMagazine.copy());
          this.setAmmoProvider(magazineProvider);
          GunAmmoStateUtil.writeLoadedMagazine(this.itemStack, stackMagazine);
          this.ammoProviderChanged = true;
        }
      }

      // 1.21.1 port safety: item capabilities can be recreated independently from the
      // shooting task that was scheduled by an older client Gun instance. The server sync packet
      // is authoritative for the local player's loaded magazine, but the cache must be keyed by
      // the current gun item. Otherwise switching M4A1 -> AK47 lets the AK restore the M4's
      // stanag magazine and fire one fake client shot.
      if (living.level().isClientSide()
          && this.ammoProvider instanceof MagazineAmmoProvider magazineProvider
          && this.ammoProvider.getMagazine().map(Magazine::getSize).orElse(0) <= 0) {
        ItemStack stackMagazine = GunAmmoStateUtil.getLoadedMagazineStack(mainHandStack);
        boolean stackHasAuthoritativeMagazine = !stackMagazine.isEmpty()
            && GunAmmoStateUtil.getMagazineSize(stackMagazine) > 0
            && this.isMagazineAccepted(stackMagazine);

        // The per-gun cache is only a short bridge after a server SyncGunSlot packet has already
        // written the same loaded magazine into the visible ItemStack. Never resurrect ammo from
        // cache alone after dropping/picking a gun; otherwise a stale client cache can make an
        // empty server stack shoot locally forever.
        if (stackHasAuthoritativeMagazine) {
          ItemStack cachedMagazine = GunAmmoStateUtil.getCachedLoadedMagazineStack(
              living.entity().getId(), EquipmentSlot.MAINHAND, mainHandStack);
          ItemStack restoreMagazine = !cachedMagazine.isEmpty()
              && GunAmmoStateUtil.getMagazineSize(cachedMagazine) > 0
              && this.isMagazineAccepted(cachedMagazine)
                  ? cachedMagazine
                  : stackMagazine;
          magazineProvider.setMagazineStack(restoreMagazine.copy());
          this.setAmmoProvider(magazineProvider);
          GunAmmoStateUtil.writeLoadedMagazine(this.itemStack, restoreMagazine);
          GunAmmoStateUtil.writeLoadedMagazine(mainHandStack, restoreMagazine);
          this.ammoProviderChanged = true;
          
        }
      }
    }
    this.refreshAmmoProviderFromStackIfEmpty(this.itemStack);
  }

  private void refreshAmmoProviderFromStackIfEmpty(ItemStack sourceStack) {
    try {
      if (this.ammoProvider == null || sourceStack == null || sourceStack.isEmpty()) {
        return;
      }
      ItemStack currentProviderStack = this.ammoProvider.getMagazineStack();
      if (!currentProviderStack.isEmpty()
          && this.ammoProvider.getMagazine().map(Magazine::getSize).orElse(0) > 0) {
        if (this.isMagazineAccepted(currentProviderStack)) {
          return;
        }
        if (this.ammoProvider instanceof MagazineAmmoProvider magazineProvider) {
          magazineProvider.setMagazineStack(ItemStack.EMPTY);
          this.setAmmoProvider(magazineProvider);
        }
        GunAmmoStateUtil.clearLoadedMagazine(sourceStack);
        GunAmmoStateUtil.clearLoadedMagazine(this.itemStack);
      }

      var loadedMagazine = GunAmmoStateUtil.getLoadedMagazineStack(sourceStack);
      if (!loadedMagazine.isEmpty()) {
        if (!this.isMagazineAccepted(loadedMagazine)) {
          GunAmmoStateUtil.clearLoadedMagazine(sourceStack);
          GunAmmoStateUtil.clearLoadedMagazine(this.itemStack);
          return;
        }
        if (this.ammoProvider instanceof MagazineAmmoProvider magazineProvider) {
          magazineProvider.setMagazineStack(loadedMagazine.copy());
          GunAmmoStateUtil.writeLoadedMagazine(this.itemStack, loadedMagazine);
          GunAmmoStateUtil.writeLoadedMagazine(sourceStack, loadedMagazine);
          this.ammoProviderChanged = true;
          GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
          GunStackDataComponentUtil.writeGunState(sourceStack, this.createGunStateTag());
          return;
        }
      }

      var state = GunStackDataComponentUtil.readGunState(sourceStack);
      if (state == null || state.isEmpty()
          || !state.contains(GunStackDataComponentUtil.AMMO_PROVIDER, Tag.TAG_COMPOUND)) {
        return;
      }
      var restored = this.createAmmoProvider();
      restored.deserializeNBT(state.getCompound(GunStackDataComponentUtil.AMMO_PROVIDER));
      if (!restored.getMagazineStack().isEmpty()) {
        if (!this.isMagazineAccepted(restored.getMagazineStack())) {
          GunAmmoStateUtil.clearLoadedMagazine(sourceStack);
          GunAmmoStateUtil.clearLoadedMagazine(this.itemStack);
          return;
        }
        this.setAmmoProvider(restored);
        GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
        GunStackDataComponentUtil.writeGunState(sourceStack, this.createGunStateTag());
      }
    } catch (RuntimeException ignored) {
      // Defensive runtime restore only.
    }
  }

  /**
   * Persists this capability's authoritative gun state back into the real held ItemStack. During
   * the 1.21.1 port, the Gun capability instance can hold an ItemStack copy that is not the exact
   * object later dropped into the world. If we only update {@code this.itemStack}, dropping and
   * picking the gun can lose the loaded magazine and the next capability is restored as empty.
   */
  private void persistStateToLivingMainHand(LivingExtension<?, ?> living) {
    if (living == null || this.ammoProvider == null || this.itemStack == null
        || this.itemStack.isEmpty()) {
      return;
    }

    ItemStack mainHandStack = living.mainHandItem();
    if (mainHandStack == null || mainHandStack.isEmpty()
        || !mainHandStack.is(this.itemStack.getItem())) {
      return;
    }

    try {
      CompoundTag gunState = this.createGunStateTag();
      GunStackDataComponentUtil.writeGunState(this.itemStack, gunState);
      GunStackDataComponentUtil.writeGunState(mainHandStack, gunState);

      ItemStack magazineStack = this.ammoProvider.getMagazineStack();
      if (magazineStack != null && !magazineStack.isEmpty()) {
        GunAmmoStateUtil.writeLoadedMagazine(this.itemStack, magazineStack);
        GunAmmoStateUtil.writeLoadedMagazine(mainHandStack, magazineStack);
        GunAmmoStateUtil.cacheAuthoritativeLoadedMagazine(living.entity().getId(),
            EquipmentSlot.MAINHAND, mainHandStack, magazineStack);
      } else {
        GunAmmoStateUtil.clearLoadedMagazine(this.itemStack);
        GunAmmoStateUtil.clearLoadedMagazine(mainHandStack);
      }

      if (living.entity() instanceof Player player) {
        try {
          player.getInventory().setChanged();
        } catch (RuntimeException ignored) {
        }
      }
    } catch (RuntimeException ignored) {
      // Defensive persistence bridge only.
    }
  }

  @Override
  public void tick(LivingExtension<?, ?> living) {
    this.refreshAmmoProviderFromLivingStack(living);
    this.lastTickMs = Util.getMillis();

    if (!living.level().isClientSide() && !this.isTriggerPressed()
        && this.wasTriggerPressed) {
      this.triggerPressedTicks = living.entity().getServer().getTickCount();
    }
    this.wasTriggerPressed = this.isTriggerPressed();


    if (this.isPerformingSecondaryAction() && living.entity().isSprinting()) {
      this.setPerformingSecondaryAction(living, false, true);
    }

    if (living.level().isClientSide()) {
      this.client.get().handleTick(living);
    }
  }

  @Override
  public void reset(LivingExtension<?, ?> living) {
    this.setTriggerPressed(living, false, false);
    if (this.isPerformingSecondaryAction()) {
      this.setPerformingSecondaryAction(living, false, false);
    }
  }

  @Override
  public void setTriggerPressed(LivingExtension<?, ?> living, boolean triggerPressed,
      boolean sendUpdate) {
    final String triggerKey = activeTriggerKey(living);

    if (triggerPressed) {
      this.lastTriggerPressedUpdateMs = Util.getMillis();

      // If an older capability instance still owns the shooting future, stop it before starting
      // this instance. This prevents one click from creating multiple gun-pool tasks and consuming
      // several rounds.
      var activeGun = ACTIVE_TRIGGER_GUNS.get(triggerKey);
      if (activeGun != null && activeGun != this && activeGun.isTriggerPressed()) {
        boolean sameGunItem = !activeGun.itemStack.isEmpty() && !this.itemStack.isEmpty()
            && activeGun.itemStack.is(this.itemStack.getItem());
        if (living.level().isClientSide() && sameGunItem) {
          // If a SyncGunSlot recreated the current capability while an older client gun instance
          // for the same visible gun still owns the scheduled firing loop, do not kill that old
          // loop just because the freshly-created capability is asked to mirror the held mouse
          // state. Killing the old loop here made long-hold fire restart/stutter after every ammo
          // sync. If the item differs, however, this is a hotbar switch: stop the old gun so it
          // cannot keep sending stale shot/sync packets into the newly held gun.
          return;
        }
        activeGun.stopShooting();
      }
    } else {
      this.lastTriggerPressedUpdateMs = 0L;

      // Release must stop whichever instance owns the shooting future, not just the current
      // capability instance. Without this, pressing with one Gun instance and releasing after a
      // SyncGunSlot-created instance leaves the old client/server state alive.
      var activeGun = ACTIVE_TRIGGER_GUNS.get(triggerKey);
      if (activeGun != null && activeGun != this) {
        activeGun.stopShooting();
      }
    }

    boolean alreadyInState = triggerPressed == this.isTriggerPressed();
    if (alreadyInState) {
      // A recreated client Gun instance may already be locally false on release, but the server
      // can still think the trigger is held. Always forward release packets from the client.
      if (!triggerPressed && sendUpdate && living.level().isClientSide()) {
        PacketDistributor.sendToServer(
            new TriggerPressedMessage(living.entity().getId(), false, this.itemStack));
      }
      return;
    }

    if (triggerPressed && !this.canShoot(living)) {
      return;
    }
    if (triggerPressed) {
      var triggerEvent = new GunEvent.TriggerPressed(this, this.itemStack, living);
      NeoForge.EVENT_BUS.post(triggerEvent);
      if (triggerEvent.isCanceled()) {
        return;
      }
    }

    if (triggerPressed) {
      this.triggerHeld = true;
      this.gunFuture = executorService.scheduleAtFixedRate(() -> this.shoot(living), 0L,
          this.getFireDelayMs(), TimeUnit.MILLISECONDS);
      this.activeTriggerKey = triggerKey;
      ACTIVE_TRIGGER_GUNS.put(triggerKey, this);
    } else {
      this.stopShooting();
    }

    // Player-controlled client firing is now packeted per actual shot from shoot(). Sending an
    // immediate pressed=true here would make a single click consume twice: once for the press packet
    // and once for the first client shot. Release still has to be sent immediately.
    if (sendUpdate) {
      if (living.level().isClientSide()) {
        if (!triggerPressed) {
          PacketDistributor.sendToServer(
              new TriggerPressedMessage(living.entity().getId(), false, this.itemStack));
        }
      } else {
        PacketDistributor.sendToPlayersTrackingEntity(living.entity(),
            new TriggerPressedMessage(living.entity().getId(), triggerPressed, this.itemStack));
      }
    }
  }

  @Override
  public void handleClientShotRequest(LivingExtension<?, ?> living) {
    // The client is the source of visual fire timing for player-held guns. The server consumes
    // exactly one round for each client shot request, instead of starting its own fixed-rate loop.
    // That keeps ammo consumption, animation, pending-hit validation and fire delay aligned.
    if (living.level().isClientSide()) {
      this.setTriggerPressed(living, true, true);
      return;
    }

    final String triggerKey = activeTriggerKey(living);
    var activeGun = ACTIVE_TRIGGER_GUNS.get(triggerKey);
    if (activeGun != null && activeGun != this && activeGun.isTriggerPressed()) {
      if (activeGun.itemStack.is(this.itemStack.getItem())) {
        // Slot synchronization must not reset the per-press shot budget or fire delay.
        this.triggerHeld = true;
        this.shotCount.set(activeGun.shotCount.get());
        this.lastShotMs = activeGun.lastShotMs;
      }
      activeGun.stopShooting();
    }

    if (!this.triggerHeld) {
      if (!this.canShoot(living)) {
        return;
      }
      var triggerEvent = new GunEvent.TriggerPressed(this, this.itemStack, living);
      NeoForge.EVENT_BUS.post(triggerEvent);
      if (triggerEvent.isCanceled()) {
        return;
      }
      this.shotCount.set(0);
    }

    this.triggerHeld = true;
    this.activeTriggerKey = triggerKey;
    ACTIVE_TRIGGER_GUNS.put(triggerKey, this);
    this.refreshAmmoProviderFromLivingStack(living);
    this.shoot(living);
  }

  @Override
  public void prepareForMainHandStateSync(LivingExtension<?, ?> living) {
    this.refreshAmmoProviderFromLivingStack(living);
    if (living != null && !living.level().isClientSide()) {
      this.persistStateToLivingMainHand(living);
    }
  }

  @Override
  public void handleClientShotRejected(LivingExtension<?, ?> living) {
    this.stopShooting();
    // A rejected click only proves that the server could not match this *request* to its current
    // capability. It does not prove the visible gun's magazine is empty: immediately after a
    // reload, the capability and the ItemStack can be recreated on different ticks. Clearing the
    // ItemStack here was therefore the remaining path that turned a full magazine into an empty
    // one after the first shot. The next authoritative slot sync remains free to correct a truly
    // stale client state; this handler only stops the rejected firing loop.
  }

  @Override
  public void setNPCTriggerPressed(LivingExtension<?, ?> living, boolean triggerPressed,
      boolean sendUpdate, float accuracy) {
    if (triggerPressed == this.isTriggerPressed()) {
      return;
    }
    if (triggerPressed && !this.canShoot(living)) {
      return;
    }
    if (triggerPressed) {
      var npcEvent = new GunEvent.NPCTriggerPressed(this, this.itemStack, living);
      NeoForge.EVENT_BUS.post(npcEvent);
      if (npcEvent.isCanceled()) {
        return;
      }
    }

    if (triggerPressed) {
      this.gunFuture = executorService.scheduleAtFixedRate(() -> this.npcShoot(living, accuracy), 0L,
          this.getFireDelayMs(), TimeUnit.MILLISECONDS);
    } else {
      this.stopShooting();
    }

    if (sendUpdate) {
      var payload = new NPCTriggerPressedMessage(living.entity().getId(), triggerPressed);
      if (living.level().isClientSide()) {
        PacketDistributor.sendToServer(payload);
      } else {
        PacketDistributor.sendToPlayersTrackingEntity(living.entity(), payload);
      }
    }
  }

  @Override
  public boolean isTriggerPressed() {
    return this.triggerHeld || (this.gunFuture != null && !this.gunFuture.isDone());
  }

  private void stopShooting() {
    this.triggerHeld = false;
    this.shotCount.set(0);
    if (this.gunFuture != null) {
      this.gunFuture.cancel(false);
      this.gunFuture = null;
    }
    String triggerKey = this.activeTriggerKey;
    if (triggerKey != null) {
      ACTIVE_TRIGGER_GUNS.remove(triggerKey, this);
      this.activeTriggerKey = null;
    }
  }

  @Override
  public void validatePendingHit(PlayerExtension<ServerPlayer> player,
      LivingExtension<?, ?> hitLiving, PendingHit pendingHit) {
    final byte tickOffset = pendingHit.tickOffset();

    // Keep the 1.18.2 hit model: the client predicts hits for every LivingEntity,
    // then the server validates that pending hit against stored snapshots and applies
    // real damage here. This is required because the server-side shot ray is often
    // one tick behind the rendered crosshair in singleplayer/multiplayer.

    if (tickOffset > HIT_VALIDATION_DELAY_TICKS) {
      logger.warn("Bad living hit packet received, tick offset is too big!");
      return;
    }

    int latencyTicks = tickOffset;
    int tick = player.entity().getServer().getTickCount();

    if (tick - latencyTicks > this.triggerPressedTicks && !this.isTriggerPressed()) {
      return;
    }

    EntitySnapshot playerSnapshot;
    try {
      playerSnapshot = player.getSnapshot(tick - latencyTicks)
          .combineUntrustedSnapshot(pendingHit.playerSnapshot());
    } catch (IndexOutOfBoundsException e) {
      return;
    }

    EntitySnapshot hitSnapshot;
    try {
      hitSnapshot = hitLiving.getSnapshot(tick - latencyTicks)
          .combineUntrustedSnapshot(pendingHit.hitSnapshot());
    } catch (IndexOutOfBoundsException e) {
      return;
    }

    if (hitLiving.entity().isAlive()) {
      var random = player.random();
      random.setSeed(pendingHit.randomSeed());
      var validatedHit = rayTrace(player.level(), player.entity(), playerSnapshot, hitSnapshot,
          this.getRange(), this.getAccuracy(playerSnapshot.velocity(), playerSnapshot.crouching()),
          pendingHit.shotCount(), random);
      boolean relaxed = false;
      if (validatedHit.isEmpty()) {
        validatedHit = relaxedRayTrace(player.level(), player.entity(), playerSnapshot, hitSnapshot,
            this.getRange(), 0.35D);
        relaxed = validatedHit.isPresent();
      }
      if (validatedHit.isPresent()) {
        
        this.hitEntity(player, hitLiving.entity(), validatedHit.get(), false);
      } else {
        
      }
    }
  }

  @Override
  public final float getAccuracy(LivingExtension<?, ?> living) {
    return this.getAccuracy(living.getVelocity(), living.isCrouching());
  }

  protected abstract float getAccuracy();

  private float getAccuracy(Vec3 velocity, boolean crouching) {
    var accuracy =
        this.getAccuracy() * this.getAttachmentMultiplier(Attachment.MultiplierType.ACCURACY);

    accuracy *= 1.0F - (float) velocity.length();

    if (crouching) {
      // Increase by 10%
      accuracy *= 1.1F;
    }

    return Math.min(1.0F, accuracy);
  }

  protected abstract double getRange();

  protected abstract long getFireDelayMs();

  /*
   * Warning: this is not called on the main thread.
   */
  private void shoot(LivingExtension<?, ?> living) {
    if (!this.isTriggerPressed()) {
      return;
    }
    this.refreshAmmoProviderFromLivingStack(living);
    if (!this.isCurrentMainHandGun(living)) {
      this.stopShooting();
      return;
    }
    long time = Util.getMillis();

    if (!this.isTriggerPressed()
        || (time - this.lastTickMs >= 500L)
        || !this.canShoot(living)) {
      this.stopShooting();
      return;
    }

    // Client shots arrive over the network and effect-enchanted hits add a small amount of server
    // work. A fixed 10 ms allowance caused otherwise valid automatic-fire packets to be discarded,
    // making enchanted guns appear slower. Keep an anti-spam floor while allowing normal jitter
    // proportional to this weapon's configured fire interval.
    long fireDelay = this.getFireDelayMs();
    long timingTolerance = living.entity() instanceof ServerPlayer
        ? Math.max(10L, Math.min(50L, fireDelay / 4L))
        : 10L;
    if (time - this.lastShotMs + timingTolerance < fireDelay) {
      return;
    }
    this.lastShotMs = time;

    boolean sideIsClient = living.level().isClientSide();

    int availableAmmo = this.getLoadedAmmo(living);
    
    if (availableAmmo <= 0) {
      this.executeOnLogicalSide(living, () -> {
        living.entity().playSound(ModSoundEvents.DRY_FIRE.get(), 1.0F, 1.0F);
        if (!sideIsClient) {
          // Authoritative empty-state correction. A dropped/picked-up gun can leave the client
          // with a cached magazine while the server-side main-hand stack/provider is empty. If we
          // only dry-fire/reload here, the client keeps its local gun-pool loop and appears to
          // have infinite ammo. Push the server's empty stack state back and tell the client to
          // stop the matching trigger loop.
          PacketDistributor.sendToPlayersTrackingEntityAndSelf(living.entity(),
              new SyncGunEquipmentSlotMessage(living.entity().getId(),
                  EquipmentSlot.MAINHAND, this, true));
          if (living.entity() instanceof ServerPlayer player) {
            NetworkChannel.PLAY.sendToPlayer(player,
                new TriggerPressedMessage(living.entity().getId(), false, this.itemStack));
          }
        }
        // Only the server may start the automatic reload. A stale client gun instance can
        // temporarily think it is empty even after SyncGunSlot. Letting the client send reload
        // actions in that state caused every first shot after reload to become another reload.
        if (!living.level().isClientSide() && this.ammoProvider != null) {
          this.ammoProvider.reload(living);
        }
      });
      this.stopShooting();
      return;
    }

    int shotCount = this.shotCount.getAndIncrement();
    int maxShots = this.fireMode.getMaxShots().orElse(Integer.MAX_VALUE);
    if (shotCount >= maxShots) {
      // Finishing a single shot/burst is NOT a physical trigger release.
      // Keep ownership across slot sync until release, otherwise held input starts a new burst.
      if (this.gunFuture != null) {
        this.gunFuture.cancel(false);
        this.gunFuture = null;
      }
      return;
    }

    this.executeOnLogicalSide(living, () -> this.processShot(living));

    if (sideIsClient) {
      // Send one server shot request per actual client shot. This replaces the old server-side
      // continuous shooting loop for player guns, preventing one click from consuming multiple
      // invisible server-only rounds while preserving long-hold automatic fire.
      PacketDistributor.sendToServer(
          new TriggerPressedMessage(living.entity().getId(), true, this.itemStack));
      this.client.get().handleShoot(living);
    }

    Thread.yield();
  }

  /** Executes work on the correct logical thread without linking client classes on a server. */
  private void executeOnLogicalSide(LivingExtension<?, ?> living, Runnable task) {
    if (living.level().isClientSide()) {
      this.client.get().execute(task);
      return;
    }
    var server = living.entity().getServer();
    if (server != null) {
      server.execute(task);
    }
  }

  private void npcShoot(LivingExtension<?, ?> living, float accuracy) {
    this.refreshAmmoProviderFromLivingStack(living);
    if (!this.isCurrentMainHandGun(living)) {
      this.stopShooting();
      return;
    }
    long time = Util.getMillis();

    if (!this.isTriggerPressed()
        || (time - this.lastTickMs >= 500L)
        || !this.canShoot(living)) {
      this.stopShooting();
      return;
    }

    long fireDelay = this.getFireDelayMs();
    long timingTolerance = 10L;
    if (time - this.lastShotMs + timingTolerance < fireDelay) {
      return;
    }
    this.lastShotMs = time;

    boolean sideIsClient = living.level().isClientSide();

    int availableAmmo = this.getLoadedAmmo(living);
    if (availableAmmo <= 0) {
      this.executeOnLogicalSide(living, () -> {
        living.entity().playSound(ModSoundEvents.DRY_FIRE.get(), 1.0F, 1.0F);
        // Only the server may start the automatic reload. A stale client gun instance can
        // temporarily think it is empty even after SyncGunSlot. Letting the client send reload
        // actions in that state caused every first shot after reload to become another reload.
        if (!sideIsClient && this.ammoProvider != null) {
          this.ammoProvider.reload(living);
        }
      });
      this.stopShooting();
      return;
    }

    int shotCount = this.shotCount.getAndIncrement();
    int maxShots = this.fireMode.getMaxShots().orElse(Integer.MAX_VALUE);
    if (shotCount >= maxShots) {
      this.stopShooting();
      return;
    }

    this.executeOnLogicalSide(living, () -> this.processNPCShot(living, accuracy));

    if (living.entity() instanceof SurvivalZombie) {
      this.stopShooting();
    }

    if (sideIsClient) {
      this.client.get().handleShoot(living);
    }

    Thread.yield();
  }

  protected boolean canShoot(LivingExtension<?, ?> living) {
    return !living.getActionObserver().isPresent()
        && !living.entity().isSprinting()
        && !living.entity().isSpectator()
        && !(living instanceof PlayerExtension<?> player && player.isHandcuffed());
  }

  protected abstract int getRoundsPerShot();

  private int getLoadedAmmo(LivingExtension<?, ?> living) {
    // On the dedicated/integrated server the real main-hand ItemStack data component is the
    // authority. A runtime Gun/AmmoProvider may be recreated during an equipment sync and can be
    // temporarily empty even though the held stack still contains a loaded magazine. Treating the
    // transient provider as authoritative caused every valid shot to be followed by auto-reload.
    if (living != null && !living.level().isClientSide()) {
      ItemStack heldStack = living.mainHandItem();
      if (heldStack != null && !heldStack.isEmpty() && !this.itemStack.isEmpty()
          && heldStack.is(this.itemStack.getItem())) {
        ItemStack persistedMagazine = GunAmmoStateUtil.getLoadedMagazineStack(heldStack);
        if (!persistedMagazine.isEmpty() && this.isMagazineAccepted(persistedMagazine)) {
          return Math.max(0, GunAmmoStateUtil.getMagazineSize(persistedMagazine));
        }
      }
      return this.ammoProvider == null
          ? 0
          : Math.max(0, this.ammoProvider.getMagazine().map(Magazine::getSize).orElse(0));
    }

    int loadedAmmo = this.getLoadedAmmo();
    if (loadedAmmo > 0 || living == null || !this.isCurrentMainHandGun(living)) {
      return loadedAmmo;
    }

    ItemStack mainHandStack = living.mainHandItem();
    if (mainHandStack == null || mainHandStack.isEmpty() || this.itemStack.isEmpty()
        || !mainHandStack.is(this.itemStack.getItem())) {
      return loadedAmmo;
    }

    // Client-side capability instances can be recreated while an older scheduled gun-pool task is
    // still alive. In that short window the provider on this instance can be AIR even though the
    // most recent server SyncGunSlot cache still contains the correct magazine. Use the cache only
    // as a local bridge for the currently visible gun; the server remains authoritative and will
    // reject stale/wrong-stack shots.
    ItemStack cachedMagazine = GunAmmoStateUtil.getCachedLoadedMagazineStack(
        living.entity().getId(), EquipmentSlot.MAINHAND, mainHandStack);
    int cachedAmmo = GunAmmoStateUtil.getCachedLoadedAmmo(
        living.entity().getId(), EquipmentSlot.MAINHAND, mainHandStack);

    if (!cachedMagazine.isEmpty() && this.isMagazineAccepted(cachedMagazine)) {
      int cachedMagazineAmmo = GunAmmoStateUtil.getMagazineSize(cachedMagazine);
      if (cachedMagazineAmmo > 0) {
        cachedAmmo = Math.max(cachedAmmo, cachedMagazineAmmo);
        if (this.ammoProvider instanceof MagazineAmmoProvider magazineProvider) {
          magazineProvider.setMagazineStack(cachedMagazine.copy());
          this.setAmmoProvider(magazineProvider);
          GunAmmoStateUtil.writeLoadedMagazine(this.itemStack, cachedMagazine);
          GunAmmoStateUtil.writeLoadedMagazine(mainHandStack, cachedMagazine);
          this.ammoProviderChanged = true;
          
        }
        return cachedAmmo;
      }
    }

    // Some sync races leave only the cached ammo count available for one or two client ticks, while
    // the cached magazine stack/component has not been restored yet. Do not let that transient
    // client-only AIR provider stop the held-fire future. The server still validates the current
    // main-hand stack and consumes the authoritative magazine, so this cannot create real ammo; it
    // only keeps the client-side automatic fire loop alive until the next SyncGunSlot rewrites the
    // local provider stack.
    if (cachedAmmo > 0) {
      
      return cachedAmmo;
    }

    return loadedAmmo;
  }

  private int getLoadedAmmo() {
    int providerAmmo = this.ammoProvider == null
        ? 0
        : Math.max(0, this.ammoProvider.getMagazine().map(Magazine::getSize).orElse(0));
    if (providerAmmo > 0) {
      return providerAmmo;
    }
    ItemStack loadedMagazine = GunAmmoStateUtil.getLoadedMagazineStack(this.itemStack);
    if (!loadedMagazine.isEmpty()) {
      return this.isMagazineAccepted(loadedMagazine)
          ? GunAmmoStateUtil.getMagazineSize(loadedMagazine)
          : 0;
    }
    return Math.max(providerAmmo, GunAmmoStateUtil.getLoadedAmmo(this.itemStack));
  }

  private static boolean shouldConsumeMagazineRound(ItemStack stack, int unbreakingLevel, RandomSource random) {
    if (stack == null || stack.isEmpty()) {
      return true;
    }
    if (unbreakingLevel <= 0 || random == null) {
      return true;
    }
    return random.nextInt(unbreakingLevel + 1) == 0;
  }

  protected void processShot(LivingExtension<?, ?> living) {
    this.refreshAmmoProviderFromLivingStack(living);
    if (this.getLoadedAmmo(living) <= 0) {
      return;
    }

    var entity = living.entity();
    var level = living.level();
    var random = entity.getRandom();

    NeoForge.EVENT_BUS.post(new GunEvent.Shoot(this, this.itemStack, living));

    // Magazine size is authoritative on the server. Always consume from the CURRENT held
    // ItemStack's persisted loaded-magazine component, then mirror that exact stack into the
    // runtime provider. This survives Gun instance recreation and prevents a false empty provider
    // from starting automatic reload after one shot.
    if (!level.isClientSide()) {
      ItemStack heldStack = living.mainHandItem();
      ItemStack magazineStack = heldStack == null || heldStack.isEmpty()
          ? ItemStack.EMPTY
          : GunAmmoStateUtil.getLoadedMagazineStack(heldStack);

      if (magazineStack.isEmpty() || !this.isMagazineAccepted(magazineStack)) {
        magazineStack = this.ammoProvider == null
            ? ItemStack.EMPTY
            : this.ammoProvider.getMagazineStack().copy();
      } else {
        magazineStack = magazineStack.copy();
      }

      var persistedMagazine = Magazine.get(magazineStack).orElse(null);
      if (persistedMagazine != null && persistedMagazine.getSize() > 0) {
        persistedMagazine.decrementSize();
        MagazineStackDataComponentUtil.writeMagazineState(
            magazineStack, persistedMagazine.serializeNBT());

        if (this.ammoProvider instanceof MagazineAmmoProvider magazineProvider) {
          magazineProvider.setMagazineStack(magazineStack.copy());
          this.setAmmoProvider(magazineProvider);
        }

        GunAmmoStateUtil.writeLoadedMagazine(this.itemStack, magazineStack);
        if (heldStack != null && !heldStack.isEmpty()
            && heldStack.is(this.itemStack.getItem())) {
          GunAmmoStateUtil.writeLoadedMagazine(heldStack, magazineStack);
          GunStackDataComponentUtil.writeGunState(heldStack, this.createGunStateTag());
        }

        this.ammoProviderChanged = true;
        GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
        this.persistStateToLivingMainHand(living);

        if (living.entity() instanceof Player player) {
          player.getInventory().setChanged();
        }

        // Restore the ninth-round per-shot authoritative sync so the HUD updates immediately.
        // SyncGunEquipmentSlotMessage now decodes into the existing held ItemStack/Gun instance
        // when the item type matches, so it no longer needs to replace the stack or reset ADS.
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(living.entity(),
            new SyncGunEquipmentSlotMessage(living.entity().getId(),
                EquipmentSlot.MAINHAND, this, true));
      }
    }

    if (this.itemStack.is(ModItems.RPG.get())) {
      if (!level.isClientSide()) {
        this.launchRpgRocket(living);
      }
      return;
    }

    // Used to avoid playing the same hit sound more than once.
    boolean hitEntity = false;
    Set<BlockState> blocksHit = new HashSet<>();

    for (int i = 0; i < this.getRoundsPerShot(); i++) {
      final long randomSeed = level.getGameTime() + i;
      random.setSeed(randomSeed);

    var partialTick = level.isClientSide() ? this.client.get().getPartialTick() : 1.0F;
      var hitResult =
          RayTraceUtil.rayTrace(entity, this.getRange(), partialTick, 0, 0).orElse(null);

      if (hitResult != null) {
        switch (hitResult.getType()) {
          case BLOCK:
            var blockHitResult = (BlockHitResult) hitResult;
            var blockState = level.getBlockState(blockHitResult.getBlockPos());
            this.hitBlock(living, blockHitResult, blockState,
                level.isClientSide() && (i == 0 || !blocksHit.contains(blockState)));
            blocksHit.add(blockState);
            break;
          case ENTITY:
            var entityHitResult = (EntityHitResult) hitResult;
            if (!entityHitResult.getEntity().isAlive()) {
              break;
            }

            // 1.18.2 behaviour: when a player fires, every LivingEntity hit is handled by
            // validatePendingHit(). The server-side ray trace may miss client-interpolated mobs,
            // so applying damage here makes hits unreliable and can also double-hit when pending
            // validation succeeds. Non-living entities still use the direct path below.
            if (entityHitResult.getEntity() instanceof LivingEntity
                && entity instanceof ServerPlayer) {
              break;
            }

            if (level.isClientSide()) {
              this.client.get().handleHitEntityPre(living,
                  entityHitResult.getEntity(),
                  entityHitResult.getLocation(),
                  randomSeed);
            }

            
            this.hitEntity(living, entityHitResult.getEntity(),
                entityHitResult.getLocation(), !hitEntity && level.isClientSide());
            hitEntity = true;
            break;
          default:
            break;
        }
      }
    }
  }

  protected void processNPCShot(LivingExtension<?, ?> living, float accuracy) {
    this.refreshAmmoProviderFromLivingStack(living);
    if (this.getLoadedAmmo(living) <= 0) {
      return;
    }

    var entity = living.entity();
    var level = living.level();
    var random = entity.getRandom();

    NeoForge.EVENT_BUS.post(new GunEvent.Shoot(this, this.itemStack, living));

    // Magazine size will be synced to clients so only decrement this on the server.
    if (!level.isClientSide()) {
      final int unbreakingLevel =
          EnchantmentLevelUtil.unbreaking(this.itemStack);
      if (shouldConsumeMagazineRound(this.itemStack, unbreakingLevel, level.getRandom())) {
        var magazine = this.ammoProvider.getMagazine().orElse(null);
        if (magazine != null) {
          magazine.decrementSize();

          var magazineStack = this.ammoProvider.getMagazineStack();
          if (!magazineStack.isEmpty()) {
            MagazineStackDataComponentUtil.writeMagazineState(magazineStack, magazine.serializeNBT());
            GunAmmoStateUtil.writeLoadedMagazine(this.itemStack, magazineStack);
          }
        } else {
          GunAmmoStateUtil.decrementLoadedAmmo(this.itemStack);
        }

        this.ammoProviderChanged = true;
        GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
        this.persistStateToLivingMainHand(living);

        if (living.entity() instanceof Player player) {
          player.getInventory().setChanged();
        }
        

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(living.entity(),
            new SyncGunEquipmentSlotMessage(living.entity().getId(),
                EquipmentSlot.MAINHAND, this, true));
      }
    }

    if (this.itemStack.is(ModItems.RPG.get())) {
      if (!level.isClientSide()) {
        this.launchRpgRocket(living);
      }
      return;
    }

    // Used to avoid playing the same hit sound more than once.
    boolean hitEntity = false;
    Set<BlockState> blocksHit = new HashSet<>();

    for (int i = 0; i < this.getRoundsPerShot(); i++) {

      // Determines if the shot hits based on accuracy chance
      boolean shouldHit = random.nextFloat() <= accuracy;
      if (!shouldHit) {
        continue;
      }

      final long randomSeed = level.getGameTime() + i;
      random.setSeed(randomSeed);

    float partialTick = level.isClientSide() ? this.client.get().getPartialTick() : 1.0F;

      // ONLY ray trace if accuracy test passed
      var hitResult = RayTraceUtil.rayTrace(entity, this.getRange(), partialTick, 0, 0).orElse(null);

      if (hitResult != null) {
        switch (hitResult.getType()) {
          case BLOCK:
            var blockHitResult = (BlockHitResult) hitResult;
            var blockState = level.getBlockState(blockHitResult.getBlockPos());
            this.hitBlock(living, blockHitResult, blockState,
                level.isClientSide() && (i == 0 || !blocksHit.contains(blockState)));
            blocksHit.add(blockState);
            break;

          case ENTITY:
            var entityHitResult = (EntityHitResult) hitResult;
            if (!entityHitResult.getEntity().isAlive()) {
              break;
            }

            if (level.isClientSide()) {
              this.client.get().handleHitEntityPre(living,
                  entityHitResult.getEntity(),
                  entityHitResult.getLocation(),
                  randomSeed);
            }

            this.npcHitEntity(living,
                entityHitResult.getEntity(),
                entityHitResult.getLocation(),
                !hitEntity && level.isClientSide());

            hitEntity = true;
            break;

          default:
            break;
        }
      }
    }
  }

  protected abstract float getDamage();

  private void launchRpgRocket(LivingExtension<?, ?> living) {
    var shooter = living.entity();
    var look = shooter.getLookAngle();
    var rocket = new RpgRocket(shooter, living.level());
    rocket.setPos(shooter.getX() + look.x * 0.8D,
        shooter.getEyeY() - 0.15D + look.y * 0.8D,
        shooter.getZ() + look.z * 0.8D);
    rocket.shoot(look.x, look.y, look.z, 2.2F, 0.15F);
    living.level().addFreshEntity(rocket);
  }

  private void hitEntity(LivingExtension<?, ?> living, Entity hitEntity, Vec3 hitPos,
      boolean playSound) {
    final var entity = living.entity();
    var damage = this.getDamage();
    if (ServerConfig.instance.damageDropOffEnable.get()) {
      var distance = hitEntity.distanceTo(living.entity());
      // Ensure minimum damage
      var minDamage =
          Math.min(damage, ServerConfig.instance.damageDropOffMinimumDamage.get().floatValue());
      damage = Math.max(minDamage, damage
          - (float) (((ServerConfig.instance.damageDropOffLoss.get() / 100) * this.getRange())
              * distance));
    }

    var armorPenetration = Math.min((1.0F
        + (EnchantmentLevelUtil.armorPenetration(this.itemStack) / 255.0F))
        * this.ammoProvider.getMagazine().map(Magazine::getArmorPenetration).orElse(0.0F), 1.0F);
    if (armorPenetration > 0 && hitEntity instanceof LivingEntity livingEntityHit) {
      damage = applyArmorPenetration(damage, livingEntityHit, armorPenetration);
    }

    boolean headshot = false;
    if (hitEntity instanceof LivingEntity livingEntity) {
      var hitLiving = LivingExtension.get(livingEntity);
      double chinHeight = (hitEntity.getY() + hitEntity.getEyeHeight() - 0.2F);
      headshot = ServerConfig.instance.headshotEnabled.get() &&
          (hitEntity instanceof Player || hitEntity instanceof Zombie
              || hitEntity instanceof Skeleton || hitEntity instanceof Creeper
              || hitEntity instanceof EnderMan || hitEntity instanceof Witch
              || hitEntity instanceof Villager || hitEntity instanceof Vindicator
              || hitEntity instanceof WanderingTrader)
          && hitPos.y >= chinHeight;
      if (headshot) {
        var headshotDamagePercent = 1.0F - (hitLiving == null ? 0.0F : hitLiving
            .getEquipmentInSlot(Equipment.Slot.HAT)
            .filter(Hat.class::isInstance)
            .map(Hat.class::cast)
            .map(Hat::headshotReductionPercentage)
            .orElse(0.0F));
        damage *= (float) (headshotDamagePercent * ServerConfig.instance.headshotBonusDamage.get());
        if (entity instanceof ServerPlayer player) {
          player.playNotifySound(SoundEvents.ITEM_BREAK, player.getSoundSource(), 0.65F, 1.5F);
        }
        hitEntity.level().playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
            SoundEvents.ITEM_BREAK, hitEntity.getSoundSource(), 0.65F, 1.5F);
      }
    }

    // Post gun hit entity event
    var event =
        new GunEvent.EntityHit(this, this.itemStack, living, hitEntity, damage, hitPos, headshot);
    NeoForge.EVENT_BUS.post(event);
    if (event.isCanceled()) {
      return;
    }

    damage = event.damage();
    headshot = event.headshot();

    // Simulated client-side effects
    if (living.level().isClientSide()) {
      this.client.get().handleHitEntityPost(living, hitEntity, hitPos, playSound, headshot);
      return;
    }

    // Resets the temporary invincibility before causing the damage, preventing
    // previous damages from blocking the gun damage.
    // Also, allows multiple bullets to hit the same target at the same time.
    hitEntity.invulnerableTime = 0;

    ModDamageSource.hurtWithoutKnockback(hitEntity,
        ModDamageSource.gun(entity, headshot), damage);
    this.tryApplyCombatEnchantment(living, hitEntity);

    checkCreateExplosion(this.itemStack, entity, hitPos);

    if (EnchantmentLevelUtil.flame(this.itemStack) > 0) {
      hitEntity.igniteForSeconds(100.0F);
    }

    NeoForge.EVENT_BUS
        .post(new GunEvent.EntityDamaged(this, this.itemStack, living, hitEntity,
            damage, hitPos, headshot));

    if (hitEntity instanceof LivingEntity hitLivingEntity
        && entity instanceof ServerPlayer player) {
      // Alert client of hit (real hit as opposed to client prediction)
      NetworkChannel.PLAY.sendToPlayer(player,
          new HitMessage(hitPos, hitLivingEntity.isDeadOrDying()));
    }
  }

  private void tryApplyCombatEnchantment(LivingExtension<?, ?> shooter, Entity hitEntity) {
    if (shooter.level().isClientSide() || !(hitEntity instanceof LivingEntity target)) {
      return;
    }

    long gameTime = shooter.level().getGameTime();
    Long previousTick = this.combatEffectTicks.put(target.getId(), gameTime);
    if (previousTick != null && previousTick == gameTime) {
      return;
    }
    if (this.combatEffectTicks.size() > 64) {
      this.combatEffectTicks.entrySet().removeIf(entry -> entry.getValue() < gameTime - 20L);
    }

    var enchantmentRegistry = shooter.level().registryAccess()
        .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
    ItemStack enchantedStack = shooter.mainHandItem();
    // A player's equipped stack is authoritative. The runtime gun stack may be an older copy
    // created before a reload and must never decide whether the current weapon is enchanted.
    if (!(shooter.entity() instanceof Player)
        && (enchantedStack.isEmpty() || !enchantedStack.is(this.itemStack.getItem()))) {
      enchantedStack = this.itemStack;
    }
    if (enchantedStack.isEmpty() || !enchantedStack.is(this.itemStack.getItem())) {
      return;
    }
    var effectRandom = net.minecraft.util.RandomSource.create(
        gameTime * 31L + shooter.entity().getId() * 17L + target.getId());
    if (hasEnchantment(enchantmentRegistry, SurvivalEnchantments.INFECTION, enchantedStack)
        && effectRandom.nextFloat() < 0.60F) {
      target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
          SurvivalMobEffects.holder(SurvivalMobEffects.INFECTION), 20 * 60 * 5));
    } else if (hasEnchantment(enchantmentRegistry, SurvivalEnchantments.BURNING, enchantedStack)
        && effectRandom.nextFloat() < 0.20F) {
      target.igniteForSeconds(2.0F);
    } else if (hasEnchantment(enchantmentRegistry, SurvivalEnchantments.EXPLOSIVE, enchantedStack)
        && effectRandom.nextFloat() < 0.10F) {
      this.applyEnchantedExplosion(shooter, target);
    } else if (hasEnchantment(enchantmentRegistry, SurvivalEnchantments.BLINDING, enchantedStack)
        && effectRandom.nextFloat() < 0.15F) {
      target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
          net.minecraft.world.effect.MobEffects.BLINDNESS, 20 * 3));
    } else if (hasEnchantment(enchantmentRegistry, SurvivalEnchantments.SLOWING, enchantedStack)
        && effectRandom.nextFloat() < 0.50F) {
      target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
          net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20 * 3));
    }
  }

  private boolean hasEnchantment(
      net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> registry,
      net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,
      ItemStack stack) {
    var enchantment = registry.getHolder(key).orElse(null);
    return enchantment != null
        && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            enchantment, stack) > 0;
  }

  private void applyEnchantedExplosion(LivingExtension<?, ?> shooter, LivingEntity center) {
    if (center.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
      serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
          center.getX(), center.getY() + center.getBbHeight() * 0.5D, center.getZ(),
          1, 0.0D, 0.0D, 0.0D, 0.0D);
      serverLevel.playSound(null, center.blockPosition(),
          net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
          net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }
    var source = ModDamageSource.causeUnscaledExplosionDamage(shooter.entity());
    for (LivingEntity affected : center.level().getEntitiesOfClass(LivingEntity.class,
        center.getBoundingBox().inflate(2.5D), entity -> entity != shooter.entity())) {
      affected.invulnerableTime = 0;
      ModDamageSource.hurtWithoutKnockback(affected, source, 8.0F);
    }
  }

  private static float applyArmorPenetration(float damage, LivingEntity target, float armorPenetration) {
    var toughnessAttribute = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
    float armorToughness = toughnessAttribute == null ? 0.0F : (float) toughnessAttribute.getValue();

    // 1.21.x keeps armor math internal to LivingEntity damage handling. For the migration pass we
    // preserve Crafting Dead's armor-piercing intent with a conservative approximation: restore a
    // percentage of the target's effective armor value back into the damage amount. This keeps the
    // gun pipeline compiling while the final damage formula is validated against 1.21.1 gameplay.
    float armorAbsorptionEstimate = Math.min(damage, target.getArmorValue() * 0.04F
        + armorToughness * 0.02F);
    return damage + armorAbsorptionEstimate * armorPenetration;
  }

  private void npcHitEntity(LivingExtension<?, ?> living, Entity hitEntity, Vec3 hitPos,
      boolean playSound) {
    final var entity = living.entity();
    var damage = this.getDamage();
    if (ServerConfig.instance.damageDropOffEnable.get()) {
      var distance = hitEntity.distanceTo(living.entity());
      // Ensure minimum damage
      var minDamage =
          Math.min(damage, ServerConfig.instance.damageDropOffMinimumDamage.get().floatValue());
      damage = Math.max(minDamage, damage
          - (float) (((ServerConfig.instance.damageDropOffLoss.get() / 100) * this.getRange())
          * distance));
    }

    var armorPenetration = Math.min((1.0F
        + (EnchantmentLevelUtil.armorPenetration(this.itemStack) / 255.0F))
        * this.ammoProvider.getMagazine().map(Magazine::getArmorPenetration).orElse(0.0F), 1.0F);
    if (armorPenetration > 0 && hitEntity instanceof LivingEntity livingEntityHit) {
      damage = applyArmorPenetration(damage, livingEntityHit, armorPenetration);
    }

    // 10% Chance that the next Bullet is a Headshot
    boolean headshot = living.random().nextInt(100) < 10;

    if (hitEntity instanceof LivingEntity livingEntity) {
      var hitLiving = LivingExtension.get(livingEntity);
      if (headshot) {
        var headshotDamagePercent = 1.0F - (hitLiving == null ? 0.0F : hitLiving
            .getEquipmentInSlot(Equipment.Slot.HAT)
            .filter(Hat.class::isInstance)
            .map(Hat.class::cast)
            .map(Hat::headshotReductionPercentage)
            .orElse(0.0F));
        damage *= (float) (headshotDamagePercent * ServerConfig.instance.headshotBonusDamage.get());
        hitEntity.level().playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
            SoundEvents.ITEM_BREAK, hitEntity.getSoundSource(), 0.65F, 1.5F);
      }
    }

    if (entity instanceof SurvivalZombie survivalZombie
        && hitEntity instanceof net.minecraft.world.entity.player.Player) {
      damage = survivalZombie.consumeNpcGunDamage();
      if (damage <= 0.0F) {
        return;
      }
      headshot = false;
    }

    // Post gun hit entity event
    var event =
        new GunEvent.EntityHit(this, this.itemStack, living, hitEntity, damage, hitPos, headshot);
    NeoForge.EVENT_BUS.post(event);
    if (event.isCanceled()) {
      return;
    }

    damage = event.damage();
    headshot = event.headshot();

    // Simulated client-side effects
    if (living.level().isClientSide()) {
      this.client.get().handleHitEntityPost(living, hitEntity, hitPos, playSound, headshot);
      return;
    }

    // Resets the temporary invincibility before causing the damage, preventing
    // previous damages from blocking the gun damage.
    // Also, allows multiple bullets to hit the same target at the same time.
    hitEntity.invulnerableTime = 0;

    ModDamageSource.hurtWithoutKnockback(hitEntity,
        ModDamageSource.gun(entity, headshot), damage);
    this.tryApplyCombatEnchantment(living, hitEntity);

    checkCreateExplosion(this.itemStack, entity, hitPos);

    if (EnchantmentLevelUtil.flame(this.itemStack) > 0) {
      hitEntity.igniteForSeconds(100.0F);
    }

    NeoForge.EVENT_BUS
        .post(new GunEvent.EntityDamaged(this, this.itemStack, living, hitEntity,
            damage, hitPos, headshot));

    if (hitEntity instanceof LivingEntity hitLivingEntity
        && entity instanceof ServerPlayer player) {
      // Alert client of hit (real hit as opposed to client prediction)
      NetworkChannel.PLAY.sendToPlayer(player,
          new HitMessage(hitPos, hitLivingEntity.isDeadOrDying()));
    }
  }

  private void hitBlock(LivingExtension<?, ?> living, BlockHitResult result,
      BlockState blockState, boolean playSound) {
    final var entity = living.entity();
    final var block = blockState.getBlock();
    final var level = entity.level();
    final var blockPos = result.getBlockPos();

    // Post gun hit block event
    var event =
        new GunEvent.BlockHit(this, itemStack, result, blockState, living, level);
    NeoForge.EVENT_BUS.post(event);
    if (event.isCanceled()) {
      return;
    }

    // Client-side effects
    if (level.isClientSide()) {
      this.client.get().handleHitBlock(living, result, blockState, playSound);
      return;
    }

    if (block instanceof BellBlock bell) {
      bell.onHit(level, blockState, result, entity instanceof Player player ? player : null,
          playSound);
    }

    if (block instanceof TntBlock tnt) {
      tnt.onCaughtFire(blockState, level, blockPos, null, entity);
      level.removeBlock(blockPos, false);
    }

    checkCreateExplosion(this.itemStack, entity, result.getLocation());

    if (EnchantmentLevelUtil.flame(this.itemStack) > 0) {
      if (CampfireBlock.canLight(blockState)) {
        level.setBlockAndUpdate(blockPos, blockState.setValue(BlockStateProperties.LIT, true));
      } else {
        var directedPos = blockPos.relative(result.getDirection());
        if (BaseFireBlock.canBePlacedAt(level, directedPos, entity.getDirection())) {
          level.setBlockAndUpdate(directedPos, BaseFireBlock.getState(level, directedPos));
        }
      }
    }
  }

  @Override
  public Map<GunCraftSlotType, Attachment> getAttachments() {
    this.restorePersistedAttachmentsIfNeeded();
    return this.attachments == null ? Map.of() : this.attachments;
  }

  @Override
  public void setAttachments(Map<GunCraftSlotType, Attachment> attachments) {
    this.attachments = Map.copyOf(attachments);
    this.attachmentsDirty = true;
    if (this.initialized) {
      GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
    }
  }

  @Override
  public ItemStack getPaintStack() {
    return this.restorePersistedPaintStackIfNeeded();
  }

  @Override
  public void setPaintStack(ItemStack paintStack) {
    this.dataManager.set(PAINT_STACK, paintStack);
    this.setSkin(Paint.get(paintStack)
        .map(Paint::getSkin)
        .orElse(null));
    if (this.initialized) {
      GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
    }
  }

  @Override
  public Skin getSkin() {
    this.restorePersistedSkinIfNeeded();
    return this.skin == null ? null : this.skin.value();
  }

  @Override
  public void setSkin(Holder<Skin> skin) {
    this.skin = skin;
    this.skinDirty = true;
    if (this.initialized) {
      GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
    }
  }

  @Override
  public void toggleFireMode(LivingExtension<?, ?> living, boolean sendUpdate) {
    if (this.fireModeInfiniteIterator.hasNext()) {
      this.setFireMode(living, this.fireModeInfiniteIterator.next(), sendUpdate);
    }
  }

  @Override
  public void setFireMode(LivingExtension<?, ?> living, FireMode fireMode, boolean sendUpdate) {
    this.fireMode = fireMode;

    living.entity().playSound(ModSoundEvents.TOGGLE_FIRE_MODE.get(), 1.0F, 1.0F);
    if (living.entity() instanceof Player player) {
      player.displayClientMessage(Component.translatable("message.switch_fire_mode",
          Component.translatable(this.fireMode.getTranslationKey())), true);
    }

    if (sendUpdate) {
      var payload = new SetFireModeMessage(living.entity().getId(), this.fireMode);
      if (living.level().isClientSide()) {
        PacketDistributor.sendToServer(payload);
      } else {
        PacketDistributor.sendToPlayersTrackingEntity(living.entity(), payload);
      }
    }
  }

  @Override
  public boolean isPerformingSecondaryAction() {
    return this.performingSecondaryAction;
  }

  protected boolean isPerformingSecondaryAction(LivingExtension<?, ?> living) {
    if (this.performingSecondaryAction || living == null) {
      return this.performingSecondaryAction;
    }
    return ACTIVE_SECONDARY_ACTIONS.getOrDefault(activeActionKey(living, this.itemStack), false);
  }

  protected boolean canPerformSecondaryAction(LivingExtension<?, ?> living) {
    return !living.entity().isSprinting()
        && !(living instanceof PlayerExtension<?> player && player.isHandcuffed());
  }

  @Override
  public void setPerformingSecondaryAction(LivingExtension<?, ?> living,
      boolean performingAction, boolean sendUpdate) {
    String actionKey = activeActionKey(living, this.itemStack);
    boolean globallyActive = ACTIVE_SECONDARY_ACTIONS.getOrDefault(actionKey, false);

    // ItemStack component sync may recreate the runtime Gun after every shot. When the previous
    // instance was already aiming, the new instance starts with a false local field even though
    // the player-level action map is still true. Restore that inherited state silently: replaying
    // the client transition and network packet on every recreated instance makes ADS/FOV flash.
    if (performingAction == globallyActive
        && performingAction != this.performingSecondaryAction) {
      this.performingSecondaryAction = performingAction;
      return;
    }

    if (performingAction == this.performingSecondaryAction
        || (performingAction && !this.canPerformSecondaryAction(living))) {
      return;
    }

    this.performingSecondaryAction = performingAction;
    ACTIVE_SECONDARY_ACTIONS.put(actionKey, performingAction);

    if (living.level().isClientSide()) {
      this.client.get().handleToggleSecondaryAction(living);
    }

    if (sendUpdate) {
      var payload = new SecondaryActionMessage(living.entity().getId(),
          this.isPerformingSecondaryAction());
      if (living.level().isClientSide()) {
        PacketDistributor.sendToServer(payload);
      } else {
        PacketDistributor.sendToPlayersTrackingEntity(living.entity(), payload);
      }
    }
  }

  protected int getShotCount() {
    return this.shotCount.get();
  }

  @Override
  public FireMode getFireMode() {
    return this.fireMode;
  }

  @Override
  public GunClient getClient() {
    return (GunClient) this.client.get();
  }

  @Override
  public void startClientReload() {
    this.startClientReload(false);
  }

  @Override
  public void startClientReload(boolean removingMagazine) {
    this.client.get().startReload(removingMagazine);
  }

  @Override
  public void cancelClientReload() {
    this.client.get().cancelReload();
  }

  @Override
  public AmmoProvider getAmmoProvider() {
    return this.ammoProvider;
  }

  @Override
  public void setAmmoProvider(AmmoProvider ammoProvider) {
    this.ammoProvider = ammoProvider;
    this.ammoProviderChanged = true;
    if (this.initialized) {
      GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
    }
  }

  @Override
  public ItemStack getItemStack() {
    return this.itemStack;
  }

  @Override
  public CompoundTag serializeNBT() {
    var tag = this.createGunStateTag();
    if (this.initialized) {
      GunStackDataComponentUtil.writeGunState(this.itemStack, tag);
    }
    return tag;
  }

  public CompoundTag createGunStateTag() {
    var tag = new CompoundTag();
    if (!this.initialized || this.ammoProvider == null) {
      return tag;
    }
    // Serialize the authoritative live state, never getters that restore old persisted data.
    var currentAttachments = this.attachments == null ? Map.<GunCraftSlotType, Attachment>of() : this.attachments;
    tag.putString(GunStackDataComponentUtil.AMMO_PROVIDER_TYPE,
        RegistryNameUtil.ammoProviderType(this.ammoProvider.getType()).toString());
    tag.put(GunStackDataComponentUtil.AMMO_PROVIDER, this.ammoProvider.serializeNBT());
    var attachmentsTag = currentAttachments.values().stream()
        .filter(java.util.Objects::nonNull)
        .map(RegistryNameUtil::attachment)
        .filter(java.util.Objects::nonNull)
        .map(ResourceLocation::toString)
        .map(StringTag::valueOf)
        .collect(ListTag::new, ListTag::add, List::addAll);
    tag.put(GunStackDataComponentUtil.ATTACHMENTS, attachmentsTag);
    tag.put(GunStackDataComponentUtil.PAINT_STACK, ItemStackNbtUtil.save(this.dataManager.get(PAINT_STACK)));
    SkinLookupUtil.save(tag, this.skin);
    GunAmmoStateUtil.mirrorLoadedState(this.itemStack, tag, this.ammoProvider);
    return tag;
  }

  @Override
  public void deserializeNBT(CompoundTag tag) {
    var gunState = (tag == null || tag.isEmpty())
        ? GunStackDataComponentUtil.readGunState(this.itemStack)
        : tag;

    if (gunState.contains(GunStackDataComponentUtil.AMMO_PROVIDER_TYPE, Tag.TAG_STRING)) {
      var ammoProviderTypeId = ResourceLocation.tryParse(
          gunState.getString(GunStackDataComponentUtil.AMMO_PROVIDER_TYPE));
      if (ammoProviderTypeId != null) {
        var ammoProviderType = AmmoProviderTypes.registry.get(ammoProviderTypeId);
        if (ammoProviderType != null) {
          this.setAmmoProvider(ammoProviderType.create());
          this.ammoProvider.deserializeNBT(gunState.getCompound(GunStackDataComponentUtil.AMMO_PROVIDER));
          this.ammoProviderChanged = true;
        }
      }
    }
    if (gunState.contains(GunStackDataComponentUtil.ATTACHMENTS, Tag.TAG_LIST)) {
      this.setAttachments(gunState.getList(GunStackDataComponentUtil.ATTACHMENTS, Tag.TAG_STRING)
          .stream()
          .map(Tag::getAsString)
          .map(ResourceLocation::tryParse)
          .filter(java.util.Objects::nonNull)
          .map(Attachments.registry::get)
          .filter(java.util.Objects::nonNull)
          .collect(Collectors.toMap(Attachment::getInventorySlot, v -> v, (left, right) -> right)));
    }
    if (gunState.contains(GunStackDataComponentUtil.PAINT_STACK, Tag.TAG_COMPOUND)) {
      this.setPaintStack(ItemStackNbtUtil.parseOptional(gunState.getCompound(GunStackDataComponentUtil.PAINT_STACK)));
    }
    this.skin = SkinLookupUtil.load(gunState);
    GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
  }

  @Override
  public void encode(FriendlyByteBuf out, boolean writeAll) {
    SynchedData.pack(writeAll
        ? this.dataManager.getAll()
        : this.dataManager.packDirty(), out);
    if (writeAll || this.attachmentsDirty) {
      var attachmentIds = this.getAttachments().values().stream()
          .filter(java.util.Objects::nonNull)
          .map(RegistryNameUtil::attachment)
          .filter(java.util.Objects::nonNull)
          .toList();
      NetworkBufferUtil.writeVarInt(out, attachmentIds.size(), 0, 64);
      attachmentIds.forEach(id -> NetworkBufferUtil.writeResourceLocation(out, id, null));
    } else {
      NetworkBufferUtil.writeVarInt(out, -1, -1, 64);
    }
    this.attachmentsDirty = false;

    if ((this.ammoProviderChanged || writeAll) && this.ammoProvider != null) {
      NetworkBufferUtil.writeBoolean(out, true);
      NetworkBufferUtil.writeResourceLocation(out, RegistryNameUtil.ammoProviderType(this.ammoProvider.getType()), null);
    } else {
      NetworkBufferUtil.writeBoolean(out, false);
    }
    if (this.ammoProvider != null) {
      this.ammoProvider.encode(out, this.ammoProviderChanged || writeAll);
      GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
    }
    this.ammoProviderChanged = false;

    if (this.skinDirty || writeAll) {
      this.skinDirty = false;
      NetworkBufferUtil.writeBoolean(out, true);
      if (this.skin == null) {
        NetworkBufferUtil.writeBoolean(out, true);
      } else {
        NetworkBufferUtil.writeBoolean(out, false);
        NetworkBufferUtil.writeResourceLocation(out, SkinLookupUtil.id(this.skin), null);
      }
    } else {
      NetworkBufferUtil.writeBoolean(out, false);
    }
  }

  @Override
  public void decode(FriendlyByteBuf in) {
    if (in == null) {
      return;
    }
    this.dataManager.assignValues(SynchedData.unpack(in));

    int size = NetworkBufferUtil.readCappedVarInt(in, -1, 64, -1);
    if (size > -1) {
      var builder = ImmutableMap.<GunCraftSlotType, Attachment>builderWithExpectedSize(size);
      for (int i = 0; i < size && in.readableBytes() > 0; i++) {
        var attachment = Attachments.registry.get(
            NetworkBufferUtil.readResourceLocation(in, null));
        if (attachment != null && attachment.getInventorySlot() != null) {
          builder.put(attachment.getInventorySlot(), attachment);
        }
      }
      this.attachments = builder.build();
    }

    if (NetworkBufferUtil.readBoolean(in, false)) {
      var ammoProviderType = AmmoProviderTypes.registry.get(
          NetworkBufferUtil.readResourceLocation(in, null));
      if (ammoProviderType != null) {
        this.ammoProvider = ammoProviderType.create();
      }
    }
    if (this.ammoProvider != null && in.readableBytes() > 0) {
      this.ammoProvider.decode(in);
      GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
    }

    if (NetworkBufferUtil.readBoolean(in, false)) {
      if (NetworkBufferUtil.readBoolean(in, false)) {
        this.skin = null;
      } else {
        this.skin = SkinLookupUtil.holder(NetworkBufferUtil.readResourceLocation(in, null));
      }
    }

    // 1.21.x can recreate item capabilities after the container slot sync packet has been
    // processed. Persist the decoded state back onto the visible ItemStack so attachments,
    // paint, skin and ammo survive the next capability reconstruction and remain available to
    // item rendering.
    GunStackDataComponentUtil.writeGunState(this.itemStack, this.createGunStateTag());
  }

  @Override
  public boolean requiresSync() {
    return this.attachmentsDirty
        || this.ammoProviderChanged
        || this.dataManager.isDirty()
        || (this.ammoProvider != null && this.ammoProvider.requiresSync())
        || this.skinDirty;
  }

  private static void checkCreateExplosion(ItemStack magazineStack, Entity entity,
      Vec3 position) {
    float explosionSize =
        EnchantmentLevelUtil.power(magazineStack) / 5.0F;
    if (explosionSize > 0) {
      ModExplosionUtil.explode(entity.level(),
          entity, position.x(), position.y(), position.z(), explosionSize,
          Explosion.BlockInteraction.KEEP);
    }
  }

  public static Optional<Vec3> rayTrace(Level level, Entity contextEntity,
      EntitySnapshot fromSnapshot, EntitySnapshot targetSnapshot, double distance, float accuracy,
      int shotCount, RandomSource random) {
    if (!fromSnapshot.complete() || !targetSnapshot.complete()) {
      return Optional.empty();
    }

    return rayTraceSnapshotBox(level, contextEntity, fromSnapshot, targetSnapshot, distance, 0.0D);
  }

  private static Optional<Vec3> relaxedRayTrace(Level level, Entity contextEntity,
      EntitySnapshot fromSnapshot, EntitySnapshot targetSnapshot, double distance, double inflate) {
    if (!fromSnapshot.complete() || !targetSnapshot.complete()) {
      return Optional.empty();
    }

    return rayTraceSnapshotBox(level, contextEntity, fromSnapshot, targetSnapshot, distance, inflate);
  }

  private static Optional<Vec3> rayTraceSnapshotBox(Level level, Entity contextEntity,
      EntitySnapshot fromSnapshot, EntitySnapshot targetSnapshot, double distance, double inflate) {
    var startPos = fromSnapshot.position().add(0.0D, fromSnapshot.eyeHeight(), 0.0D);
    var look =
        RayTraceUtil.calculateViewVector(fromSnapshot.rotation().x, fromSnapshot.rotation().y);

    var blockRayTraceResult = RayTraceUtil.rayTraceBlocks(startPos, distance, look, level, contextEntity);

    var scaledLook = look.scale(distance);

    var endPos = blockRayTraceResult
        .map(HitResult::getLocation)
        .orElse(startPos.add(scaledLook));

    var hitBox = inflate > 0.0D
        ? targetSnapshot.boundingBox().inflate(inflate)
        : targetSnapshot.boundingBox();
    var potentialHit = hitBox.clip(startPos, endPos);
    if (hitBox.contains(startPos)) {
      return Optional.of(potentialHit.orElse(startPos));
    } else {
      return potentialHit;
    }
  }
}
