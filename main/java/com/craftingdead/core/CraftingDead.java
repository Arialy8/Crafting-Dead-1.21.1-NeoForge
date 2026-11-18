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

package com.craftingdead.core;

import com.craftingdead.core.data.tags.ModBlockTagsProvider;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import com.craftingdead.core.client.ClientDist;
import com.craftingdead.core.data.guns.GunDataProvider;
import com.craftingdead.core.data.recipes.ModRecipeProvider;
import com.craftingdead.core.data.tags.ModItemTagsProvider;
import com.craftingdead.core.event.CombatPickupEvent;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.SyncHydrationMessage;
import com.craftingdead.core.network.message.play.SyncSurvivalStatsMessage;
import com.craftingdead.core.network.message.play.SyncLivingMessage;
import com.craftingdead.core.network.message.play.SyncProtectionConfigMessage;
import com.craftingdead.core.particle.ModParticleTypes;
import com.craftingdead.core.server.ServerDist;
import com.craftingdead.core.sounds.ModSoundEvents;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.effect.ModMobEffects;
import com.craftingdead.core.world.entity.ModEntityTypes;
import com.craftingdead.core.world.entity.ModEntityAttributes;
import com.craftingdead.core.world.entity.PlayerCorpse;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.inventory.ModMenuTypes;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.vehicle.VehicleContent;
import com.craftingdead.core.world.item.GunItem;
import com.craftingdead.core.world.item.gun.skin.Skins;
import com.craftingdead.immerse.CraftingDeadImmerse;
import com.craftingdead.immerse.world.airdrop.ImmerseAirdropConfig;
import com.craftingdead.survival.CraftingDeadSurvival;
import com.craftingdead.core.world.item.combatslot.CombatSlot;
import com.craftingdead.core.world.item.crafting.ModRecipeSerializers;
import com.craftingdead.core.world.item.gun.GunConfigurations;
import com.craftingdead.core.world.item.gun.GunTriggerPredicates;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProviderTypes;
import com.craftingdead.core.world.item.gun.attachment.Attachments;
import com.craftingdead.core.trauma.ProtectionConfig;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.data.PackOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(CraftingDead.ID)
public class CraftingDead {

  public static final String ID = "craftingdead";

  public static final String VERSION = getModVersion();

  private static String getModVersion() {
    try {
      return ModList.get().getModContainerById(ID)
          .map(modContainer -> modContainer.getModInfo().getVersion().toString())
          .orElse("[version]");
    } catch (Throwable ignored) {
      return "[version]";
    }
  }

  /**
   * Logger.
   */
  private static final Logger logger = LogUtils.getLogger();

  /**
   * Singleton.
   */
  private static CraftingDead instance;

  /**
   * Mod distribution.
   */
  private final ModDist modDist;

  public CraftingDead(IEventBus modEventBus) {
    instance = this;

    if (FMLEnvironment.dist == Dist.CLIENT) {
      this.modDist = new ClientDist(modEventBus);
    } else {
      this.modDist = new ServerDist();
    }
    modEventBus.addListener(this::handleCommonSetup);
    modEventBus.addListener(this::handleGatherData);
    modEventBus.addListener(this::handleConfigLoading);
    modEventBus.addListener(this::handleConfigReloading);
    modEventBus.addListener(this::handleRegisterPayloadHandlers);
    modEventBus.addListener(ModEntityAttributes::registerAttributes);

    var modContainer = ModList.get().getModContainerById(ID).orElseThrow();
    modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.configSpec);
    modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.configSpec);
    modContainer.registerConfig(ModConfig.Type.SERVER, ImmerseAirdropConfig.configSpec,
        "craftingdeadimmerse-server.toml");

    ModAttachmentTypes.ATTACHMENT_TYPES.register(modEventBus);
    ModDataComponents.DATA_COMPONENTS.register(modEventBus);
    ModEntityTypes.deferredRegister.register(modEventBus);
    VehicleContent.register(modEventBus);
    ModItems.deferredRegister.register(modEventBus);
    ModItems.tabDeferredRegister.register(modEventBus);
    ModSoundEvents.deferredRegister.register(modEventBus);
    ModMenuTypes.deferredRegister.register(modEventBus);
    ModMobEffects.deferredRegister.register(modEventBus);
    ModParticleTypes.deferredRegister.register(modEventBus);
    ModRecipeSerializers.deferredRegister.register(modEventBus);

    // Custom registries
    ActionTypes.deferredRegister.register(modEventBus);
    AmmoProviderTypes.deferredRegister.register(modEventBus);
    Attachments.deferredRegister.register(modEventBus);
    GunConfigurations.deferredRegister.register(modEventBus);
    GunTriggerPredicates.deferredRegister.register(modEventBus);

    CraftingDeadImmerse.register(modEventBus);
    CraftingDeadSurvival.register(modEventBus);

    NeoForge.EVENT_BUS.register(this);
  }

  public ModDist getModDist() {
    return this.modDist;
  }

  public ClientDist getClientDist() {
    if (this.modDist instanceof ClientDist clientDist) {
      return clientDist;
    }
    throw new IllegalStateException("Accessing client dist on wrong side");
  }

  public static CraftingDead getInstance() {
    return instance;
  }

  // ================================================================================
  // Mod Events
  // ================================================================================

  private void handleCommonSetup(FMLCommonSetupEvent event) {
    logger.info("Starting Crafting Dead, version {}", VERSION);
    // TelemetryManager.initialize(ID, VERSION, Optional::empty, null,
    //     scope -> scope.setTag("craftingdead.version", VERSION));
    // Sentry telemetry disabled - dependency not bundled
    ProtectionConfig.load();
    ModDataComponents.resolveAll();
    Skins.init();
    // Brewing recipes: syringe is not a PotionItem in 1.21, so addContainerRecipe is unavailable.
  }

  private void handleRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
    NetworkChannel.loadChannels(event);
  }

  private void handleGatherData(GatherDataEvent event) {
    var generator = event.getGenerator();
    PackOutput output = generator.getPackOutput();
    var lookupProvider = event.getLookupProvider();
    if (event.includeServer()) {
      var blockTags = new ModBlockTagsProvider(output, lookupProvider, event.getExistingFileHelper());
      generator.addProvider(true, blockTags);
      generator.addProvider(true, new ModItemTagsProvider(output, lookupProvider,
          blockTags.contentsGetter(), event.getExistingFileHelper()));
      generator.addProvider(true, new ModRecipeProvider(output, lookupProvider));
      generator.addProvider(true, new GunDataProvider(output, lookupProvider));
    }
  }

  private void handleConfigLoading(ModConfigEvent.Loading event) {
    if (event.getConfig().getModId().equals(ID)) {
      ProtectionConfig.load();
      if (event.getConfig().getType() == ModConfig.Type.SERVER) {
        this.syncProtectionConfigToAllPlayers();
      }
    }
  }

  private void handleConfigReloading(ModConfigEvent.Reloading event) {
    if (event.getConfig().getModId().equals(ID)) {
      ProtectionConfig.load();
      if (event.getConfig().getType() == ModConfig.Type.SERVER) {
        this.syncProtectionConfigToAllPlayers();
      }
    }
  }

  // ================================================================================
  // Common Forge Events
  // ================================================================================

  @SubscribeEvent
  public void handleAttack(AttackEntityEvent event) {
    var player = PlayerExtension.get(event.getEntity());
    if (player != null) {
      event.setCanceled(player.handleAttack(event.getTarget()));
    }
  }

  @SubscribeEvent
  public void handleInteract(PlayerInteractEvent.EntityInteract event) {
    var player = PlayerExtension.get(event.getEntity());
    if (player != null) {
      event.setCanceled(player.handleInteract(event.getHand(), event.getTarget()));
    }
  }

  @SubscribeEvent
  public void handlePlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
    var player = PlayerExtension.get(event.getEntity());
    if (player != null) {
      event.setCanceled(player.handleLeftClickBlock(event.getPos(), event.getFace(), event::setUseBlock,
          event::setUseItem));
    }
  }

  @SubscribeEvent
  public void handlePlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    var player = PlayerExtension.get(event.getEntity());
    if (player != null) {
      event.setCanceled(player.handleRightClickBlock(event.getHand(), event.getPos(), event.getFace()));
    }
  }

  @SubscribeEvent
  public void handlePlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
    var player = PlayerExtension.get(event.getEntity());
    if (player != null) {
      event.setCanceled(player.handleRightClickItem(event.getHand()));
    }
  }

  @SubscribeEvent
  public void handleEntityItemPickup(ItemEntityPickupEvent.Pre event) {
    var living = LivingExtension.get(event.getPlayer());
    if (living instanceof PlayerExtension<?> playerExt && playerExt.isCombatModeEnabled()) {
      final ItemStack itemStack = event.getItemEntity().getItem();
      CombatSlot combatSlot = CombatSlot.getSlotType(itemStack).orElse(null);
      CombatPickupEvent combatPickupEvent = new CombatPickupEvent(itemStack, combatSlot);
      NeoForge.EVENT_BUS.post(combatPickupEvent);
      if (combatPickupEvent.isCanceled()) {
        event.getItemEntity().discard();
      } else if (combatSlot != null) {
        if (combatSlot.addToInventory(itemStack, event.getPlayer().getInventory(), false)) {
          event.getItemEntity().discard();
        }
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingSetTarget(LivingChangeTargetEvent event) {
    if (event.getEntity() instanceof Mob mob) {
      if (mob.hasEffect(ModMobEffects.holder(ModMobEffects.FLASH_BLINDNESS))) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingDeath(LivingDeathEvent event) {
    if (!event.getEntity().level().isClientSide()) {
      ServerPlayer killer = resolvePlayerKiller(event);
      if (killer != null && killer != event.getEntity()) {
        var stats = killer.getData(ModAttachmentTypes.getSurvivalStats());
        if (event.getEntity() instanceof Zombie) {
          stats.incrementZombieKills();
          syncSurvivalStats(killer);
        } else if (event.getEntity() instanceof ServerPlayer) {
          stats.incrementPlayerKills();
          syncSurvivalStats(killer);
        }
      }
    }

    if (!event.getEntity().level().isClientSide()
        && event.getEntity() instanceof ServerPlayer victim
        && event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
      Entity sourceEntity = event.getSource().getEntity();
      String killerName = null;
      ItemStack weapon = ItemStack.EMPTY;

      if (sourceEntity instanceof ServerPlayer killer && killer != victim) {
        killerName = killer.getDisplayName().getString();
        weapon = killer.getMainHandItem();
      } else if (sourceEntity == null || sourceEntity == victim) {
        killerName = "自然死亡";
      }

      // Mobs killing players are deliberately excluded. The feed is player-focused and also
      // reports natural/player self-inflicted deaths, but never reports deaths of non-players.
      if (killerName != null) {
        var weaponId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(weapon.getItem());
        var message = new com.craftingdead.core.network.message.play.KillFeedMessage(
            killerName, victim.getDisplayName().getString(), weaponId);
        for (var player : serverLevel.players()) {
          NetworkChannel.PLAY.sendToPlayer(player, message);
        }
      }
    }
    var living = LivingExtension.get(event.getEntity());
    var killer = event.getSource().getEntity() instanceof LivingEntity livingSource ? LivingExtension.get(livingSource) : null;
    if ((living != null && living.handleDeath(event.getSource()))
        || (killer != null && killer.handleKill(event.getEntity()))) {
      event.setCanceled(true);
    }
    if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer victim) {
      Entity attacker = event.getSource().getEntity();
      int penalty = 8;
      if (attacker instanceof ServerPlayer && attacker != victim) {
        penalty = 15;
      } else if (attacker instanceof Zombie zombie) {
        penalty = zombie.getMainHandItem().getItem() instanceof GunItem
            || zombie.getOffhandItem().getItem() instanceof GunItem ? 5 : 10;
      }
      long lost = victim.getData(ModAttachmentTypes.getSurvivalStats()).deductRankScore(penalty);
      victim.sendSystemMessage(net.minecraft.network.chat.Component.literal(
          "死亡扣除军衔积分：" + lost + "，剩余："
              + victim.getData(ModAttachmentTypes.getSurvivalStats()).getRankScore()));
      syncSurvivalStats(victim);
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingDrops(LivingDropsEvent event) {
    var living = LivingExtension.get(event.getEntity());
    if (event.getEntity() instanceof ServerPlayer player
        && !player.level().getGameRules().getBoolean(
            net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
      var playerExtension = PlayerExtension.get(player);
      PlayerCorpse.create(player, event.getDrops(), playerExtension);
      event.setCanceled(true);
      return;
    }
    if (living != null) {
      event.setCanceled(living.handleDeathLoot(event.getSource(), event.getDrops(), 0));
    }
    if (!event.isCanceled() && event.getEntity() instanceof com.craftingdead.survival.world.entity.SurvivalZombie zombie)
      com.craftingdead.survival.world.entity.ZombieLoadouts.doubleDrops(zombie, event.getDrops());
    if (!event.isCanceled() && event.getEntity() instanceof net.minecraft.world.entity.monster.Zombie zombie
        && zombie.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
      int count = com.craftingdead.survival.world.ZombieBalance.scrapCount(
          zombie.getRandom().nextInt(100), zombie.getRandom().nextInt(3),
          zombie.level() instanceof net.minecraft.server.level.ServerLevel level
              && com.craftingdead.survival.world.BloodMoonManager.isBloodMoonDay(level));
      if (count > 0) {
        var scrap = new net.minecraft.world.entity.item.ItemEntity(zombie.level(), zombie.getX(), zombie.getY(), zombie.getZ(),
            new net.minecraft.world.item.ItemStack(com.craftingdead.core.world.item.ModItems.SCRAP_METAL.get(), count));
        scrap.setDefaultPickUpDelay();
        event.getDrops().add(scrap);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingAttack(LivingIncomingDamageEvent event) {
    var living = LivingExtension.get(event.getEntity());
    if (living != null) {
      event.setCanceled(living.handleHurt(event.getSource(), event.getAmount()));
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingDamage(LivingDamageEvent.Pre event) {
    var living = LivingExtension.get(event.getEntity());
    if (living != null) {
      event.getContainer().setNewDamage(living.handleDamaged(event.getSource(), event.getNewDamage()));
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleEntityBlockPlace(BlockEvent.EntityPlaceEvent event) {
    if (event.getEntity() instanceof LivingEntity livingEntity) {
      var living = LivingExtension.get(livingEntity);
      if (living != null) {
        event.setCanceled(living.handleBlockPlace(event.getBlockSnapshot(), event.getPlacedBlock(),
            event.getPlacedAgainst()));
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleEntityBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
    if (event.getEntity() instanceof LivingEntity livingEntity) {
      var living = LivingExtension.get(livingEntity);
      if (living != null) {
        event.setCanceled(living.handleMultiBlockPlace(event.getReplacedBlockSnapshots(),
            event.getPlacedBlock(), event.getPlacedAgainst()));
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleEntityBlockBreakEvent(BlockEvent.BreakEvent event) {
    var player = PlayerExtension.get(event.getPlayer());
    if (player != null) {
      event.setCanceled(player.handleBlockBreak(event.getPos(), event.getState(), new MutableInt(0)));
    }
  }

  @SubscribeEvent
  public void handlePlayerClone(PlayerEvent.Clone event) {
    var player = PlayerExtension.get(event.getEntity());
    var original = event.getOriginal() instanceof ServerPlayer serverPlayer
        ? PlayerExtension.get(serverPlayer)
        : PlayerExtension.get(event.getOriginal());
    if (player != null && original != null) {
      player.copyFrom((PlayerExtension<ServerPlayer>) original, event.isWasDeath());
    }
  }

  @SubscribeEvent
  public void handleLivingUpdate(EntityTickEvent.Post event) {
    if (event.getEntity() instanceof LivingEntity livingEntity) {
      var living = LivingExtension.get(livingEntity);
      if (living != null) {
        living.tick();
        if (!living.level().isClientSide() && living.requiresSync()) {
          FriendlyByteBuf data = new RegistryFriendlyByteBuf(
              Unpooled.buffer(), living.entity().registryAccess());
          living.encode(data, false);
          PacketDistributor.sendToPlayersTrackingEntityAndSelf(
              living.entity(),
              new SyncLivingMessage(living.entity().getId(), data));
        }
      }
    }
  }

  @SubscribeEvent
  public void handlePlayerTick(EntityTickEvent.Post event) {
    if (event.getEntity() instanceof Player player) {
      this.enforceMainHandGuns(player);
      var living = LivingExtension.get(player);
      if (living instanceof PlayerExtension<?> playerExt) {
        playerExt.playerTick();
      }
      this.tickHydration(player);
      this.tickSurvivalStats(player);
    }
  }

  @SubscribeEvent
  public void handleHandSwap(LivingSwapItemsEvent.Hands event) {
    if (event.getItemSwappedToOffHand().getItem() instanceof GunItem) {
      event.setCanceled(true);
    }
  }

  private void enforceMainHandGuns(Player player) {
    if (player.level().isClientSide()
        || !(player.getOffhandItem().getItem() instanceof GunItem)) {
      return;
    }

    ItemStack gunStack = player.getOffhandItem();
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    if (player.getMainHandItem().isEmpty()) {
      player.setItemInHand(InteractionHand.MAIN_HAND, gunStack);
    } else if (!player.getInventory().add(gunStack)) {
      player.drop(gunStack, false);
    }
  }

  @SubscribeEvent
  public void handlePlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    var player = PlayerExtension.get(event.getEntity());
    if (player != null) {
      FriendlyByteBuf data = new RegistryFriendlyByteBuf(
          Unpooled.buffer(), event.getEntity().registryAccess());
      player.encode(data, true);
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(
          event.getEntity(),
          new SyncLivingMessage(event.getEntity().getId(), data));
    }
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      syncHydration(serverPlayer);
      syncSurvivalStats(serverPlayer);
    }
  }

  @SubscribeEvent
  public void handlePlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer joiningPlayer) {
      com.craftingdead.core.world.shop.MaBalanceData.get(joiningPlayer.server).getBalance(joiningPlayer.getUUID());
    }
    startTracking(event.getEntity(), (ServerPlayer) event.getEntity());
    this.syncProtectionConfig((ServerPlayer) event.getEntity());
    syncHydration((ServerPlayer) event.getEntity());
    syncSurvivalStats((ServerPlayer) event.getEntity());
  }

  @SubscribeEvent
  public void handlePlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      syncSurvivalStats(serverPlayer);
    }
  }

  @SubscribeEvent
  public void handlePlayerStartTracking(PlayerEvent.StartTracking event) {
    startTracking(event.getTarget(), (ServerPlayer) event.getEntity());
  }

  private static void startTracking(Entity targetEntity, ServerPlayer playerEntity) {
    var trackedLiving = targetEntity instanceof LivingEntity living ? LivingExtension.get(living) : null;
    if (trackedLiving != null) {
      trackedLiving.handleStartTracking(playerEntity);
      FriendlyByteBuf data = new RegistryFriendlyByteBuf(
          Unpooled.buffer(), targetEntity.registryAccess());
      trackedLiving.encode(data, true);
      NetworkChannel.PLAY.sendToPlayer(playerEntity,
          new SyncLivingMessage(trackedLiving.entity().getId(), data));
    }
  }

  private void syncProtectionConfig(ServerPlayer player) {
    NetworkChannel.PLAY.sendToPlayer(player,
        new SyncProtectionConfigMessage(ProtectionConfig.getSerializedConfig()));
  }

  private void tickHydration(Player player) {
    if (!(player instanceof ServerPlayer serverPlayer)
        || player.level().isClientSide()
        || player.isCreative()
        || player.isSpectator()
        || !ServerConfig.instance.hydrationEnabled.get()) {
      return;
    }

    var hydration = serverPlayer.getData(ModAttachmentTypes.getHydration());
    int drainInterval = Math.max(1, ServerConfig.instance.hydrationDrainIntervalTicks.get());
    int tickCounter = hydration.getTickCounter() + 1;
    boolean changed = false;
    if (tickCounter >= drainInterval) {
      tickCounter = 0;
      changed = hydration.consumeWater(1);
    }
    hydration.setTickCounter(tickCounter);

    if (hydration.getWater() <= 0) {
      int damageInterval =
          Math.max(1, ServerConfig.instance.hydrationStarvationDamageIntervalTicks.get());
      if (serverPlayer.tickCount % damageInterval == 0) {
        serverPlayer.hurt(serverPlayer.damageSources().starve(),
            ServerConfig.instance.hydrationStarvationDamage.get().floatValue());
      }
    }

    if (changed || serverPlayer.tickCount % 100 == 0) {
      syncHydration(serverPlayer);
    }
  }

  private void tickSurvivalStats(Player player) {
    if (!(player instanceof ServerPlayer serverPlayer)
        || player.level().isClientSide()
        || player.isCreative()
        || player.isSpectator()
        || !player.isAlive()) {
      return;
    }

    var stats = serverPlayer.getData(ModAttachmentTypes.getSurvivalStats());
    stats.tick();
    if (stats.getSurvivalTicks() % 20 == 0) {
      syncSurvivalStats(serverPlayer);
    }
  }

  private static ServerPlayer resolvePlayerKiller(LivingDeathEvent event) {
    if (event.getSource().getEntity() instanceof ServerPlayer player) {
      return player;
    }
    return event.getEntity().getKillCredit() instanceof ServerPlayer player ? player : null;
  }

  public static boolean restoreHydration(ServerPlayer player, int amount) {
    if (amount <= 0 || !ServerConfig.instance.hydrationEnabled.get()) {
      return false;
    }
    var hydration = player.getData(ModAttachmentTypes.getHydration());
    boolean changed = hydration.addWater(amount);
    if (changed) {
      syncHydration(player);
    }
    return changed;
  }

  public static void syncHydration(ServerPlayer player) {
    NetworkChannel.PLAY.sendToPlayer(player,
        new SyncHydrationMessage(player.getData(ModAttachmentTypes.getHydration()).getWater()));
  }

  public static void syncSurvivalStats(ServerPlayer player) {
    var stats = player.getData(ModAttachmentTypes.getSurvivalStats());
    // The rank is part of the server display name used by chat. Refresh it when stats change.
    player.refreshDisplayName();
    NetworkChannel.PLAY.sendToPlayer(player, new SyncSurvivalStatsMessage(
        stats.getSurvivalTicks(), stats.getZombieKills(), stats.getPlayerKills(),
        stats.getRankScore()));
  }

  private void syncProtectionConfigToAllPlayers() {
    var server = ServerLifecycleHooks.getCurrentServer();
    if (server == null) {
      return;
    }
    String serializedConfig = ProtectionConfig.getSerializedConfig();
    server.getPlayerList().getPlayers().forEach(player ->
        NetworkChannel.PLAY.sendToPlayer(player,
            new SyncProtectionConfigMessage(serializedConfig)));
  }
}
