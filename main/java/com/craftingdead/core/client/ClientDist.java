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

package com.craftingdead.core.client;
import com.craftingdead.core.world.item.ModItems;

import com.craftingdead.core.client.util.ClientFrameTimeUtil;
import com.craftingdead.core.client.gui.screen.inventory.CraftingScreen;
import com.craftingdead.core.network.message.play.DamageHandcuffsMessage;
import com.craftingdead.core.network.message.play.RightClickStateMessage;
import com.craftingdead.core.network.message.play.TraumaPacket;
import com.craftingdead.core.network.message.play.TriggerPressedMessage;
import com.craftingdead.core.trauma.TraumaSeverity;
import com.craftingdead.core.world.action.RemoveMagazineAction;
import com.craftingdead.core.world.action.reload.AbstractReloadAction;
import com.craftingdead.core.world.item.MeleeWeaponItem;
import com.craftingdead.core.world.item.ModAxeItem;
import com.craftingdead.core.world.item.ModPickaxeItem;
import com.craftingdead.core.world.item.ModShovelItem;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.ModDist;
import com.craftingdead.core.ModAttachmentTypes;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.client.crosshair.CrosshairManager;
import com.craftingdead.core.client.gui.IngameGui;
import com.craftingdead.core.client.gui.screen.inventory.EquipmentScreen;
import com.craftingdead.core.client.gui.screen.inventory.ShopScreen;
import com.craftingdead.core.client.gui.screen.inventory.GenericContainerScreen;
import com.craftingdead.core.client.model.C4ExplosiveModel;
import com.craftingdead.core.client.model.CylinderGrenadeModel;
import com.craftingdead.core.client.model.FragGrenadeModel;
import com.craftingdead.core.client.model.SlimGrenadeModel;
import com.craftingdead.core.client.model.geom.ModModelLayers;
import com.craftingdead.core.client.particle.FlashParticle;
import com.craftingdead.core.client.particle.GrenadeSmokeParticle;
import com.craftingdead.core.client.renderer.CameraManager;
import com.craftingdead.core.client.renderer.entity.grenade.C4ExplosiveRenderer;
import com.craftingdead.core.client.renderer.entity.grenade.GrenadeRenderer;
import com.craftingdead.core.client.renderer.entity.PlayerCorpseRenderer;
import com.craftingdead.core.client.renderer.entity.RpgRocketRenderer;
import com.craftingdead.core.client.renderer.entity.T72B3Renderer;
import com.craftingdead.core.client.renderer.entity.layers.ClothingLayer;
import com.craftingdead.core.client.renderer.entity.layers.EquipmentLayer;
import com.craftingdead.core.client.renderer.entity.layers.HandcuffsLayer;
import com.craftingdead.core.client.renderer.entity.layers.ParachuteLayer;
import com.craftingdead.core.client.renderer.item.GunRenderer;
import com.craftingdead.core.client.renderer.item.ItemRenderDispatcher;
import com.craftingdead.core.client.sounds.EffectsManager;
import com.craftingdead.core.client.sounds.T72EngineSoundInstance;
import com.craftingdead.core.client.tutorial.ModTutorialStepInstance;
import com.craftingdead.core.client.tutorial.ModTutorialSteps;
import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.core.event.RenderArmClothingEvent;
import com.craftingdead.core.mixin.CameraInvoker;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.OpenEquipmentMenuMessage;
import com.craftingdead.core.network.message.play.TankInputMessage;
import com.craftingdead.core.particle.ModParticleTypes;
import com.craftingdead.survival.client.BloodMoonClient;
import com.craftingdead.survival.client.particle.LootGeneratorParticle;
import com.craftingdead.survival.particles.SurvivalParticleTypes;
import com.craftingdead.survival.world.entity.SurvivalEntityTypes;
import com.craftingdead.survival.client.model.SupplyDropModel;
import com.craftingdead.survival.client.model.PipeBombModel;
import com.craftingdead.survival.client.renderer.entity.SupplyDropRenderer;
import com.craftingdead.survival.client.renderer.entity.SurvivalZombieRenderer;
import com.craftingdead.survival.client.gui.GeneratorSelectorScreen;
import com.craftingdead.survival.world.inventory.SurvivalMenuTypes;
import com.craftingdead.survival.world.hydration.HydrationData;
import com.craftingdead.core.util.MutableVector2f;
import com.craftingdead.core.world.effect.ModMobEffects;
import com.craftingdead.core.world.entity.ModEntityTypes;
import com.craftingdead.core.world.vehicle.VehicleContent;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.entity.grenade.FlashGrenadeEntity;
import com.craftingdead.core.world.inventory.ModMenuTypes;
import com.craftingdead.core.world.item.ArbitraryTooltips;
import com.craftingdead.core.world.item.GunItem;
import com.craftingdead.core.world.item.RegisterGunColor;
import com.craftingdead.core.world.item.equipment.Clothing;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.gun.AbstractGun;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.GunAmmoStateUtil;
import com.craftingdead.core.world.item.gun.skin.Paint;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.gun.skin.Skins;
import com.craftingdead.core.world.item.scope.Scope;
import com.craftingdead.immerse.client.BlueprintPreviewRenderer;
import com.craftingdead.immerse.client.ImmerseSurvivalHud;
import com.craftingdead.immerse.client.gui.screen.BaseCenterScreen;
import com.craftingdead.immerse.client.renderer.BaseCenterRenderer;
import com.craftingdead.immerse.world.inventory.ImmerseMenuTypes;
import com.craftingdead.immerse.world.level.block.entity.ImmerseBlockEntityTypes;
import com.craftingdead.immerse.world.level.block.ImmerseBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector3f;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.options.OptionsScreen;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

public class ClientDist implements ModDist {
  public void openChalkScreen(net.minecraft.world.InteractionHand hand) {
    net.minecraft.client.Minecraft.getInstance().setScreen(new com.craftingdead.core.client.gui.ChalkScreen(hand));
  }

  // TODO: Maybe make it a configuration option? - juan
  private static final Set<String> gunPoseBlacklist =
      Set.of("de.maxhenkel.corpse.entities.DummyPlayer",
          "de.maxhenkel.corpse.entities.DummySkeleton");

  private static final ResourceLocation WATER =
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/gui/water.png");

  private static final ResourceLocation SHIELD =
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/gui/shield.png");

  private static final ResourceLocation FOOD =
      ResourceLocation.withDefaultNamespace("hud/food_full");

  private static final Set<ResourceLocation> COMBAT_HIDDEN_VANILLA_LAYERS = Set.of(
      VanillaGuiLayers.PLAYER_HEALTH,
      VanillaGuiLayers.ARMOR_LEVEL,
      VanillaGuiLayers.FOOD_LEVEL,
      VanillaGuiLayers.AIR_LEVEL,
      VanillaGuiLayers.EXPERIENCE_BAR,
      VanillaGuiLayers.EXPERIENCE_LEVEL);

  private static final List<ResourceLocation> additionalItemRendererModels = List.of(
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/acr"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/ak47"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/as50"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/awp"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/desert_eagle"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/dmr"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/fn57"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/fnfal"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/g18"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/g36c"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/hk417"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/m107"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/m1911"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/m1garand"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/m240b"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/m4a1"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/m9"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/mac10"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/magnum"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/minigun"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/mk48mod"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/mossberg"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/mp5a5"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/mpt55"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/p250"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/p90"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/qbz97_girls_frontline"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/rpg"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/rpk"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/scarl"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/sporter22"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/taser"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/trench_gun"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/vector"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/acog_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/bipod"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/desert_eagle_iron_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/eotech_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/fn57_iron_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/g18_iron_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/hp_scope"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/iron_sight_design_1"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/iron_sight_design_2"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/iron_sight_design_3"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/lp_scope"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/m1911_iron_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/p250_iron_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/red_dot_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/red_dot_sight_ak"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/sporter22_iron_sight"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/suppressor"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "attachment/tactical_grip"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/acr_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/ak47_30_round_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/ak47_30_round_magazine_old"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/as50_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/awp_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/desert_eagle_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/dmr_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/fn57_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/fnfal_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/g18_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/g36c_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/hk417_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/m107_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/m1911_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/m240b_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/mac10_extended_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/mac10_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/minigun_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/mk48mod_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/mossberg_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/mp5a5_21_round_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/mp5a5_35_round_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/mpt55_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/p250_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/p90_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/rocket"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/rpk_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/scarh_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/stanag_20_round_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/stanag_30_round_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/stanag_box_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/stanag_drum_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/taser_cartridge"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/taser_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/trench_gun_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/vector_magazine"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/ballistic_helmet"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/bunny_hat"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/chief_fireman_hat"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/fireman_hat"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/gas_mask"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/hard_hat"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/knight_hat"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/mask"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/nv_goggles_hat"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/scuba_mask"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/textured_helmet_multiple_of_8"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/textured_helmet_multiple_of_9"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "hats/top_hat"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "vest/tactical_vest"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "backpack/gun_bag"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "backpack/large_backpack"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "backpack/medium_backpack"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "backpack/small_backpack"));

  public static final KeyMapping RELOAD =
      new KeyMapping("key.reload", GLFW.GLFW_KEY_R, "key.categories." + CraftingDead.ID);
  public static final KeyMapping REMOVE_MAGAZINE =
      new KeyMapping("key.remove_magazine", GLFW.GLFW_KEY_J, "key.categories." + CraftingDead.ID);
  public static final KeyMapping TOGGLE_FIRE_MODE =
      new KeyMapping("key.toggle_fire_mode", GLFW.GLFW_KEY_V, "key.categories." + CraftingDead.ID);
  public static final KeyMapping OPEN_EQUIPMENT_MENU =
      new KeyMapping("key.equipment_menu", GLFW.GLFW_KEY_Z, "key.categories." + CraftingDead.ID);
  public static final KeyMapping TOGGLE_COMBAT_MODE =
      new KeyMapping("key.toggle_combat_mode", GLFW.GLFW_KEY_B,
          "key.categories." + CraftingDead.ID);
  public static final KeyMapping TANK_COAX =
      new KeyMapping("key.tank_coax", GLFW.GLFW_KEY_G, "key.categories." + CraftingDead.ID);
  public static final KeyMapping TANK_SMOKE =
      new KeyMapping("key.tank_smoke", GLFW.GLFW_KEY_X, "key.categories." + CraftingDead.ID);
  public static final KeyMapping TANK_HEADLIGHTS =
      new KeyMapping("key.tank_headlights", GLFW.GLFW_KEY_L, "key.categories." + CraftingDead.ID);
  public static final KeyMapping TANK_GUN_CONTROL =
      new KeyMapping("key.tank_gun_control", GLFW.GLFW_KEY_H, "key.categories." + CraftingDead.ID);
  public static final KeyMapping TANK_THERMAL =
      new KeyMapping("key.tank_thermal", GLFW.GLFW_KEY_N, "key.categories." + CraftingDead.ID);
  public static final KeyMapping TANK_EXPOSURE =
      new KeyMapping("key.tank_exposure", GLFW.GLFW_KEY_U, "key.categories." + CraftingDead.ID);

  public static final ClientConfig clientConfig;
  public static final ModConfigSpec clientConfigSpec;

  static {
    var clientConfigPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
    clientConfigSpec = clientConfigPair.getRight();
    clientConfig = clientConfigPair.getLeft();
  }

  private static final ResourceLocation ADRENALINE_SHADER =
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "shaders/post/adrenaline.json");

  private static final ResourceLocation HANDCUFFS_TEXTURE =
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/entity/handcuffs.png");

  private static final Vector3f mutableCameraRotations = new Vector3f();
  private static final MutableVector2f FOV = new MutableVector2f();

  private static final int DOUBLE_CLICK_DURATION = 500;

  private final Minecraft minecraft;

  private final CrosshairManager crosshairManager;

  private final IngameGui ingameGui;

  private final ItemRenderDispatcher itemRenderDispatcher;

  private final CameraManager cameraManager;

  private EffectsManager effectsManager;

  private TutorialSteps lastTutorialStep;

  private ModTutorialStepInstance currentModTutorialStep;

  private long adrenalineShaderStartTime = 0L;

  /**
   * MC 1.21 no longer provides the vanilla blur shader program used by the legacy adrenaline
   * post effect. If loading fails and we retry every render tick, the client log grows extremely
   * fast and can exhaust native memory. Disable only the visual shader after the first failure;
   * the adrenaline effect itself still works.
   */
  private boolean adrenalineShaderUnavailable;

  private boolean wasAdrenalineActive;

  private boolean wasSneaking;
  private long lastSneakPressTime;

  public int rightClickTicks = 0;
  public boolean wasRightClickDown = false;
  private boolean wasAttackDown;
  /** Tracks the previous physical right-click state for edge-triggered aiming. */
  private boolean wasUseDown;
  /** Persistent client intent for TOGGLE-style aiming across ItemStack/Gun recreation. */
  private boolean toggleSecondaryActionActive;

  /**
   * If the player switches hotbar slots, drops the gun, or changes game mode while holding the
   * attack button, do not automatically transfer that old held-trigger state to the next visible
   * gun. This prevents stale client firing loops from becoming local-only infinite fire.
   */
  private boolean suppressAttackUntilReleased;
  private Item lastMainHandItem = Items.AIR;
  /**
   * After a hotbar switch to a gun, ask the server for the authoritative main-hand gun state for a
   * few ticks. Minecraft may update the selected slot and mod packets in different orders, so one
   * request can arrive before the server has observed the new slot.
   */
  private int pendingMainHandGunSyncTicks;
  private boolean lastInstabuild;
  private boolean lastSpectator;
  private long lastAttackHeartbeatMs;

  private float lastPitch;
  private float lastYaw;
  private float lastRoll;

  private int traumaAimTicks;
  private float traumaAimStrength;
  private TraumaSeverity lastTraumaSeverity = TraumaSeverity.NONE;
  private float pendingTankThrottle;
  private float pendingTankTurn;
  private boolean pendingTankPrediction;
  private boolean tankThermalEnabled;
  @Nullable
  private CameraType cameraTypeBeforeTankSight;
  private final Map<Integer, T72EngineSoundInstance> tankEngineSounds = new HashMap<>();

  private final BloodMoonClient bloodMoonClient = new BloodMoonClient();

  public ClientDist(IEventBus modBus) {
    modBus.addListener(this::handleClientSetup);
    modBus.addListener(this::handleRegisterMenuScreens);
    modBus.addListener(this::handleRegisterKeyMappings);
    modBus.addListener(this::handleParticleFactoryRegisterEvent);
    modBus.addListener(this::handleItemColor);
    modBus.addListener(this::handleSoundLoad);
    modBus.addListener(ModConfigEvent.Loading.class, this::handleConfigChanged);
    modBus.addListener(ModConfigEvent.Reloading.class, this::handleConfigChanged);
    modBus.addListener(this::handleEntityRenderers);
    modBus.addListener(this::handleEntityRenderersAddLayers);
    modBus.addListener(this::handleEntityRenderersLayerDefinitions);
    modBus.addListener(this::handleRegisterClientReloadListeners);
    modBus.addListener(this::handleRegisterAdditionalModels);

    NeoForge.EVENT_BUS.register(this);
    NeoForge.EVENT_BUS.register(this.bloodMoonClient);
    NeoForge.EVENT_BUS.register(new BlueprintPreviewRenderer());
    NeoForge.EVENT_BUS.register(new ImmerseSurvivalHud());
    ModList.get().getModContainerById(CraftingDead.ID).orElseThrow().registerConfig(ModConfig.Type.CLIENT, clientConfigSpec);

    this.minecraft = Minecraft.getInstance();
    this.crosshairManager = new CrosshairManager();
    this.itemRenderDispatcher = new ItemRenderDispatcher();

    this.ingameGui =
        new IngameGui(this.minecraft, this, CrosshairManager.DEFAULT_CROSSHAIR);
    this.cameraManager = new CameraManager();
  }

  @SuppressWarnings("deprecation")
  @Override
  public RegistryAccess registryAccess() {
    var minecraft = Minecraft.getInstance();
    if (FMLEnvironment.dist.isDedicatedServer() && minecraft.getSingleplayerServer() != null) {
      return minecraft.getSingleplayerServer().registryAccess();
    } else if (FMLEnvironment.dist.isClient() && minecraft.player != null) {
      return minecraft.player.connection.registryAccess();
    }

    return ModDist.super.registryAccess();
  }

  public void setTutorialStep(ModTutorialSteps step) {
    clientConfig.tutorialStep.set(step);
    Tutorial tutorial = this.minecraft.getTutorial();
    tutorial.setStep(TutorialSteps.NONE);
    var tutorialStepInstance = step.create(this);
    this.currentModTutorialStep = tutorialStepInstance instanceof ModTutorialStepInstance modTutorialStepInstance
        ? modTutorialStepInstance
        : null;
  }


  private static ResourceLocation parseCrosshairLocation(String value) {
    ResourceLocation location = ResourceLocation.tryParse(value);
    return location != null ? location : CrosshairManager.DEFAULT_CROSSHAIR;
  }

  private static ResourceLocation parseSoundLocation(String value) {
    ResourceLocation location = ResourceLocation.tryParse(value);
    return location != null ? location : SoundEvents.TRIDENT_RETURN.getLocation();
  }

  public CrosshairManager getCrosshairManager() {
    return this.crosshairManager;
  }

  public IngameGui getIngameGui() {
    return this.ingameGui;
  }

  public CameraManager getCameraManager() {
    return this.cameraManager;
  }

  public ItemRenderDispatcher getItemRendererManager() {
    return this.itemRenderDispatcher;
  }

  /**
   * Get the {@link Minecraft} instance. If accessing {@link Minecraft} from a common class
   * (contains both client and server code) don't access fields directly from {@link Minecraft} as
   * it will cause class loading problems. To safely access {@link LocalPlayer} in a
   * multi-sided environment, use {@link #getPlayerExtension()}.
   *
   * @return {@link Minecraft}
   */
  public Minecraft getMinecraft() {
    return this.minecraft;
  }

  public Optional<PlayerExtension<LocalPlayer>> getPlayerExtension() {
    return this.minecraft.player == null
        ? Optional.empty()
        : Optional.ofNullable(PlayerExtension.get(this.minecraft.player));
  }

  public boolean isRightMouseDown() {
    return this.minecraft.options.keyUse.isDown();
  }

  public boolean isLocalPlayer(Entity entity) {
    return entity == this.minecraft.player;
  }

  public void handleHit(Vec3 hitPos, boolean dead) {
    ServerConfig.instance.hitMarkerMode.get().createHitMarker(hitPos, dead)
        .ifPresent(this.ingameGui::setHitMarker);
    if (dead && ServerConfig.instance.killSoundEnabled.get()) {
      // Plays a sound that follows the player
      SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(
          parseSoundLocation(ClientDist.clientConfig.killSound.get()));
      if (soundEvent != null) {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 5.0F, 0.3F));
      }
    }
  }

  public void handleTrauma(TraumaPacket packet) {
    this.lastTraumaSeverity = packet.severity();
    this.traumaAimTicks = Math.max(this.traumaAimTicks, packet.aimSwayTicks());
    this.traumaAimStrength = Math.max(this.traumaAimStrength, packet.aimSwayStrength());

    if (this.minecraft.gui != null && packet.severity() != TraumaSeverity.NONE) {
      var translationKey = "message." + CraftingDead.ID + ".trauma." + packet.severity().name()
          .toLowerCase(Locale.ROOT);
      this.minecraft.gui.setOverlayMessage(Component.translatable(translationKey), false);
    }
  }

  public void handleHydrationSync(int water) {
    if (this.minecraft.player != null) {
      this.minecraft.player.getData(ModAttachmentTypes.getHydration()).setWater(water);
    }
  }

  public void handleBloodMoonSync(boolean bloodMoonDay, boolean omenDay, int daysUntilBloodMoon) {
    this.bloodMoonClient.sync(bloodMoonDay, omenDay, daysUntilBloodMoon);
  }

  public static boolean isDamageFlashEnabled() {
    return clientConfig.displayBlood.get();
  }

  @Nullable
  public PlayerExtension<AbstractClientPlayer> getCameraPlayer() {
    return this.minecraft.getCameraEntity() instanceof AbstractClientPlayer player
        ? PlayerExtension.get(player)
        : null;
  }

  // ================================================================================
  // Mod Events
  // ================================================================================

  private void handleRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(this.crosshairManager);
    event.registerReloadListener(this.itemRenderDispatcher);
  }


  private void handleRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
    event.register(new ModelResourceLocation(
        ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/m4a1_painted"), "standalone"));
    for (var modelLocation : additionalItemRendererModels) {
      event.register(new ModelResourceLocation(modelLocation, "standalone"));
    }
  }

  /**
   * This has to be handled on the mod bus and forge bus.
   */
  public void handleSoundLoad(SoundEngineLoadEvent event) {
    this.effectsManager = new EffectsManager(event.getEngine());
  }

  private void handleConfigChanged(ModConfigEvent event) {
    if (event.getConfig().getSpec() == clientConfigSpec) {
      this.ingameGui.setCrosshairLocation(parseCrosshairLocation(clientConfig.crosshair.get()));
    }
  }

  private void handleClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
      net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
          ImmerseBlocks.BLOCKS.get("reinforced_barbed_wire").get(), RenderType.cutout());
      net.minecraft.client.renderer.item.ItemProperties.register(ModItems.CROSSBOW.get(),
          ResourceLocation.withDefaultNamespace("charged"),
          (stack, level, entity, seed) -> net.minecraft.world.item.CrossbowItem.isCharged(stack) ? 1F : 0F);
      net.minecraft.client.renderer.item.ItemProperties.register(ModItems.CROSSBOW.get(),
          ResourceLocation.withDefaultNamespace("pulling"),
          (stack, level, entity, seed) -> entity != null && entity.isUsingItem()
              && entity.getUseItem() == stack && !net.minecraft.world.item.CrossbowItem.isCharged(stack) ? 1F : 0F);
    });
    ArbitraryTooltips.registerAll();
    event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
        ImmerseBlocks.BLOCKS.get("barbed_wire").get(), RenderType.cutout()));
  }

  public void handleRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
    event.register(ModMenuTypes.EQUIPMENT.get(), EquipmentScreen::new);
    event.register(ModMenuTypes.CRAFTING.get(), CraftingScreen::new);
    event.register(ModMenuTypes.VEST.get(), GenericContainerScreen::new);
    event.register(ModMenuTypes.SMALL_BACKPACK.get(), GenericContainerScreen::new);
    event.register(ModMenuTypes.MEDIUM_BACKPACK.get(), GenericContainerScreen::new);
    event.register(ModMenuTypes.LARGE_BACKPACK.get(), GenericContainerScreen::new);
    event.register(ModMenuTypes.GUN_BAG.get(), GenericContainerScreen::new);
    event.register(ModMenuTypes.SHOP.get(), ShopScreen::new);
    event.register(ImmerseMenuTypes.BASE_CENTER.get(), BaseCenterScreen::new);
    event.register(ImmerseMenuTypes.RANK_REWARDS.get(),
        com.craftingdead.immerse.client.gui.screen.RankRewardScreen::new);
    event.register(SurvivalMenuTypes.GENERATOR_SELECTOR.get(), GeneratorSelectorScreen::new);
  }



  private void handleRegisterKeyMappings(RegisterKeyMappingsEvent event) {
    event.register(TOGGLE_FIRE_MODE);
    event.register(RELOAD);
    event.register(REMOVE_MAGAZINE);
    event.register(OPEN_EQUIPMENT_MENU);
    event.register(TOGGLE_COMBAT_MODE);
    event.register(TANK_COAX);
    event.register(TANK_SMOKE);
    event.register(TANK_HEADLIGHTS);
    event.register(TANK_GUN_CONTROL);
    event.register(TANK_THERMAL);
    event.register(TANK_EXPOSURE);
    event.register(ImmerseSurvivalHud.TOGGLE_STATS);
  }

  @SubscribeEvent
  public void handleSuppressVanillaHotbar(RenderGuiLayerEvent.Pre event) {
    var player = this.getCameraPlayer();
    if (player == null || !player.isCombatModeEnabled()) {
      return;
    }

    if (VanillaGuiLayers.HOTBAR.equals(event.getName())) {
      this.renderCraftingDeadHud(event.getGuiGraphics());
      event.setCanceled(true);
    } else if (COMBAT_HIDDEN_VANILLA_LAYERS.contains(event.getName())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void handleCustomizeGuiOverlay(RenderGuiLayerEvent.Post event) {
    if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
      return;
    }

    var player = this.getCameraPlayer();
    if (player == null || player.isCombatModeEnabled()) {
      return;
    }
    this.renderCraftingDeadHud(event.getGuiGraphics());
  }

  private void renderCraftingDeadHud(GuiGraphics guiGraphics) {
    var player = this.getCameraPlayer();
    if (player == null || guiGraphics == null || this.minecraft.options.hideGui) {
      return;
    }
    var window = this.minecraft.getWindow();
    var heldStack = player.mainHandItem();
    var gun = Gun.get(heldStack).orElse(null);
    this.ingameGui.renderOverlay(player, heldStack, gun, guiGraphics,
        window.getGuiScaledWidth(), window.getGuiScaledHeight(),
        minecraft.getTimer().getGameTimeDeltaPartialTick(false));

    this.renderHandcuffsHud(player, guiGraphics,
        window.getGuiScaledWidth(), window.getGuiScaledHeight());
    if (player.entity().getVehicle()
        instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank) {
      this.renderTankHud(guiGraphics, tank, window.getGuiScaledWidth(),
          window.getGuiScaledHeight());
    }
    if (player.isCombatModeEnabled()) {
      this.renderCombatStatusHud(guiGraphics, window.getGuiScaledHeight());
    } else {
      this.renderHydrationHud(guiGraphics,
          window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }
  }

  private void renderTankHud(GuiGraphics guiGraphics,
      com.craftingdead.core.world.vehicle.T72B3Entity tank, int width, int height) {
    if (this.tankThermalEnabled) {
      RenderUtil.fillRaw(guiGraphics, 0, 0, width, height, 0x2438D67A);
    }
    if (this.minecraft.player != null && tank.isWeaponController(this.minecraft.player)
        && this.isPhysicalTankSightDown()) {
      this.renderTankGunnerSight(guiGraphics, tank, width, height);
      return;
    }
    float hudScale = 0.72F;
    int panelWidth = 218;
    var hudPose = guiGraphics.pose();
    hudPose.pushPose();
    hudPose.translate(width - panelWidth * hudScale - 5.0F, 5.0F, 0.0F);
    hudPose.scale(hudScale, hudScale, 1.0F);
    int x = 0;
    int y = 0;
    int panelHeight = 110;
    RenderUtil.fillRaw(guiGraphics, x, y, x + panelWidth, y + panelHeight, 0x990B0E12);
    RenderUtil.fillRaw(guiGraphics, x, y, x + 3, y + panelHeight, 0xCC5D7565);
    int contentX = x + 8;
    Component ammo = Component.translatable(tank.isHighExplosiveSelected()
        ? "message.craftingdead.t72b3.ammo_he" : "message.craftingdead.t72b3.ammo_ap");
    Component reload = tank.getReloadTicks() > 0
        ? Component.translatable("hud.craftingdead.t72b3.reload", tank.getReloadTicks() / 20.0F)
        : Component.translatable("hud.craftingdead.t72b3.ready");
    guiGraphics.drawString(this.minecraft.font, "T-72B3", contentX, y + 5,
        0xFFF1E7C8, true);
    this.renderTankStatusBar(guiGraphics, Component.translatable("hud.craftingdead.t72b3.fuel_short"),
        tank.getFuel(), tank.getMaxFuel(), contentX, y + 17, 0xFFE9C46A);
    this.renderTankStatusBar(guiGraphics, Component.translatable("hud.craftingdead.t72b3.hull_short"),
        tank.getIntegrity(), tank.getMaxIntegrity(), contentX, y + 29, 0xFF72B9E6);
    this.renderTankStatusBar(guiGraphics, Component.translatable("hud.craftingdead.t72b3.engine_short"),
        tank.getEngineHealth(), 50, contentX, y + 41, 0xFF83D17D);
    this.renderTankStatusBar(guiGraphics, Component.translatable("hud.craftingdead.t72b3.turret_short"),
        tank.getTurretHealth(), 400, contentX, y + 53, 0xFFD3A85C);
    this.renderTankStatusBar(guiGraphics, Component.translatable("hud.craftingdead.t72b3.tracks_short"),
        (tank.getLeftTrackHealth() + tank.getRightTrackHealth()) / 2, 70,
        contentX, y + 65, 0xFFB5A58C);
    guiGraphics.drawString(this.minecraft.font,
        Component.translatable("hud.craftingdead.t72b3.ammo_compact", ammo,
            tank.getSelectedMainAmmoCount(), tank.getCoaxLoaded(), tank.getCoaxAmmoCount()),
        contentX, y + 78, 0xFFFFFFFF, true);
    guiGraphics.drawString(this.minecraft.font,
        Component.translatable("hud.craftingdead.t72b3.smoke_compact", tank.getSmokeLoaded(),
            tank.getSmokeAmmoCount(), reload), contentX, y + 89,
        tank.getReloadTicks() > 0 ? 0xFFFFB45B : 0xFF9BE49B, true);
    guiGraphics.drawString(this.minecraft.font,
        Component.translatable("hud.craftingdead.t72b3.telemetry", tank.getEngineRpm(),
            tank.getEngineGear(), tank.getEngineTemperature(), tank.getBatteryLevel()),
        contentX, y + 100, 0xFFBFC7D5, true);
    hudPose.popPose();
  }

  private void renderTankGunnerSight(GuiGraphics guiGraphics,
      com.craftingdead.core.world.vehicle.T72B3Entity tank, int width, int height) {
    int centerX = width / 2;
    int centerY = height / 2;
    int opticRadius = Math.max(90, Math.min((int) (height * 0.43F), width / 2 - 12));
    int opticLeft = centerX - opticRadius;
    int opticRight = centerX + opticRadius;
    int opticTop = centerY - opticRadius;
    int opticBottom = centerY + opticRadius;
    int frameColor = 0xFF7B5A20;
    // Circular T-72 optic housing. Four-pixel scan bands keep the mask inexpensive while the
    // one-pixel inner ring removes the rectangular periscope look.
    RenderUtil.fillRaw(guiGraphics, 0, 0, width, Math.max(0, opticTop - 4), 0xFF030303);
    RenderUtil.fillRaw(guiGraphics, 0, Math.min(height, opticBottom + 4), width, height, 0xFF030303);
    int outerRadius = opticRadius + 4;
    int innerRadius = opticRadius - 2;
    for (int row = -outerRadius; row <= outerRadius; row += 4) {
      int y0 = centerY + row;
      int y1 = Math.min(centerY + outerRadius + 1, y0 + 4);
      int outerX = (int) Math.sqrt(Math.max(0,
          outerRadius * outerRadius - row * row));
      int innerX = Math.abs(row) <= innerRadius
          ? (int) Math.sqrt(Math.max(0, innerRadius * innerRadius - row * row)) : 0;
      RenderUtil.fillRaw(guiGraphics, 0, y0, centerX - outerX, y1, 0xFF030303);
      RenderUtil.fillRaw(guiGraphics, centerX + outerX, y0, width, y1, 0xFF030303);
      if (innerX > 0) {
        RenderUtil.fillRaw(guiGraphics, centerX - outerX, y0,
            centerX - innerX, y1, frameColor);
        RenderUtil.fillRaw(guiGraphics, centerX + innerX, y0,
            centerX + outerX, y1, frameColor);
      } else {
        RenderUtil.fillRaw(guiGraphics, centerX - outerX, y0,
            centerX + outerX, y1, frameColor);
      }
    }
    int color = tank.getReloadTicks() > 0 ? 0xFFD89A62 : 0xFF8FE28A;
    int stadia = Math.min(96, opticRadius / 2);
    RenderUtil.fillRaw(guiGraphics, centerX - stadia, centerY, centerX - 9, centerY + 1, color);
    RenderUtil.fillRaw(guiGraphics, centerX + 9, centerY, centerX + stadia, centerY + 1, color);
    RenderUtil.fillRaw(guiGraphics, centerX, centerY - 62, centerX + 1, centerY - 8, color);
    RenderUtil.fillRaw(guiGraphics, centerX, centerY + 8, centerX + 1, centerY + 70, color);
    RenderUtil.fillRaw(guiGraphics, centerX - 3, centerY - 3, centerX + 4, centerY + 4,
        0x66000000);
    // T-72 style central chevron and range ladder.
    for (int diagonal = 0; diagonal < 10; diagonal++) {
      RenderUtil.fillRaw(guiGraphics, centerX - 10 + diagonal, centerY + 14 + diagonal,
          centerX - 8 + diagonal, centerY + 15 + diagonal, color);
      RenderUtil.fillRaw(guiGraphics, centerX + 9 - diagonal, centerY + 14 + diagonal,
          centerX + 11 - diagonal, centerY + 15 + diagonal, color);
    }
    for (int step = 1; step <= 5; step++) {
      int ladderY = centerY + 24 + step * 13;
      int halfWidth = 4 + step * 3;
      RenderUtil.fillRaw(guiGraphics, centerX - halfWidth, ladderY,
          centerX + halfWidth + 1, ladderY + 1, color);
      String rangeMark = Integer.toString(step * 400);
      guiGraphics.drawString(this.minecraft.font, rangeMark,
          centerX + halfWidth + 4, ladderY - 4, color, true);
    }
    double range = 0.0D;
    Vec3 optic = tank.getGunnerOpticPosition(
        ClientFrameTimeUtil.getFrameTime(this.minecraft));
    Vec3 sightDirection = Vec3.directionFromRotation(-tank.getTurretPitch(),
        tank.getYRot() + tank.getTurretYaw());
    HitResult hit = tank.level().clip(new ClipContext(optic,
        optic.add(sightDirection.scale(4096.0D)), ClipContext.Block.COLLIDER,
        ClipContext.Fluid.ANY, tank));
    if (hit.getType() != HitResult.Type.MISS) {
      range = optic.distanceTo(hit.getLocation());
    }
    String rangeText = range > 0.0D
        ? String.format(java.util.Locale.ROOT, "RNG %04d m", Math.min(9999, Math.round(range)))
        : "RNG ---- m";
    guiGraphics.drawString(this.minecraft.font, rangeText, opticLeft + 14, opticTop + 14,
        0xFFFFFFFF, true);
    String elevation = String.format(java.util.Locale.ROOT, "EL %+04.1f", tank.getTurretPitch());
    guiGraphics.drawString(this.minecraft.font, elevation,
        opticRight - 14 - this.minecraft.font.width(elevation), opticTop + 14,
        0xFFFFFFFF, true);
    guiGraphics.drawString(this.minecraft.font,
        tank.getReloadTicks() > 0
            ? Component.translatable("hud.craftingdead.t72b3.reload", tank.getReloadTicks() / 20.0F)
            : Component.translatable("hud.craftingdead.t72b3.ready"),
        centerX - 30, Math.min(opticBottom - 22, centerY + 108), color, true);
  }

  private void renderTankStatusBar(GuiGraphics guiGraphics, Component label, int value,
      int maximum, int x, int y, int color) {
    guiGraphics.drawString(this.minecraft.font, label, x, y, 0xFFE4E7EB, true);
    int barX = x + 38;
    int barY = y + 2;
    int barWidth = 112;
    RenderUtil.fillRaw(guiGraphics, barX, barY, barX + barWidth, barY + 5, 0x77000000);
    int filled = Math.round(barWidth * Mth.clamp(value / (float) maximum, 0.0F, 1.0F));
    RenderUtil.fillRaw(guiGraphics, barX, barY, barX + filled, barY + 5, color);
    String amount = value + "/" + maximum;
    guiGraphics.drawString(this.minecraft.font, amount,
        x + 204 - this.minecraft.font.width(amount), y, 0xFFFFFFFF, true);
  }

  private void renderCombatStatusHud(GuiGraphics guiGraphics, int height) {
    var player = this.minecraft.player;
    if (player == null || player.isCreative() || player.isSpectator()) {
      return;
    }

    int armor = player.getArmorValue();
    int food = player.getFoodData().getFoodLevel();
    int water = player.getData(ModAttachmentTypes.getHydration()).getWater();
    int x = 6;
    int y = height - 59;

    RenderUtil.fillRaw(guiGraphics, 0, y - 3, 94, height - 27, 0x44000000);
    this.renderCompactStatusRow(guiGraphics, SHIELD, false, x, y, armor, 20, 0xFFBFC7D5);
    this.renderCompactStatusRow(guiGraphics, FOOD, true, x, y + 10, food, 20, 0xFFD7A45A);
    this.renderCompactStatusRow(guiGraphics, WATER, false, x, y + 20, water,
        HydrationData.MAX_WATER, 0xFF6EC5E9);
  }

  private void renderCompactStatusRow(GuiGraphics guiGraphics, ResourceLocation icon,
      boolean sprite, int x, int y, int value, int maximum, int color) {
    if (sprite) {
      guiGraphics.blitSprite(icon, x, y, 9, 9);
    } else {
      var poseStack = guiGraphics.pose();
      poseStack.pushPose();
      poseStack.translate(x, y, 0.0D);
      poseStack.scale(0.5F, 0.5F, 1.0F);
      RenderUtil.blit(guiGraphics, icon, 0, 0, 16, 16);
      poseStack.popPose();
    }

    guiGraphics.drawString(this.minecraft.font, String.valueOf(value), x + 12, y,
        0xFFFFFFFF, true);
    int barX = x + 29;
    int barY = y + 2;
    int barWidth = 55;
    RenderUtil.fillRaw(guiGraphics, barX, barY, barX + barWidth, barY + 5, 0x66000000);
    int filled = Math.round(barWidth * Mth.clamp(value / (float) maximum, 0.0F, 1.0F));
    RenderUtil.fillRaw(guiGraphics, barX, barY, barX + filled, barY + 5, color);
  }

  private void renderHydrationHud(GuiGraphics guiGraphics, int width, int height) {
    var player = this.minecraft.player;
    if (player == null || player.isCreative() || player.isSpectator()) {
      return;
    }

    int water = player.getData(ModAttachmentTypes.getHydration()).getWater();
    int iconCount = Mth.ceil(water / (HydrationData.MAX_WATER / 10.0F));
    int x = width / 2 + 10;
    boolean oxygenHudVisible =
        player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply();
    int y = height - (oxygenHudVisible ? 59 : 49);
    int spacing = 8;

    var poseStack = guiGraphics.pose();
    for (int i = 0; i < iconCount; i++) {
      poseStack.pushPose();
      poseStack.translate(x + i * spacing, y, 0.0D);
      poseStack.scale(0.5625F, 0.5625F, 1.0F);
      RenderUtil.blit(guiGraphics, WATER, 0, 0, 16, 16);
      poseStack.popPose();
    }
  }

  private void renderHandcuffsHud(PlayerExtension<?> player, GuiGraphics guiGraphics, int width,
      int height) {
    final ItemStack handcuffs = player.getHandcuffs();
    if (handcuffs.isEmpty()) {
      return;
    }

    final int centerX = width / 2;
    final int y = height / 2 + 42;
    guiGraphics.renderItem(handcuffs, centerX - 8, y - 20);

    final Component statusText;
    if (handcuffs.isDamageableItem()) {
      final int maxDamage = handcuffs.getMaxDamage();
      final int remainingDamage = Math.max(0, maxDamage - handcuffs.getDamageValue());
      int expectedClicks = remainingDamage;
      statusText = Component.translatable("handcuffs.hud.escape_clicks",
          expectedClicks, remainingDamage, maxDamage);
    } else {
      statusText = Component.translatable("handcuffs.hud.locked");
    }
    guiGraphics.drawCenteredString(this.minecraft.font, statusText, centerX, y, 0xFFFFFF);
  }

  private void handleEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(ImmerseBlockEntityTypes.BASE_CENTER.get(),
        BaseCenterRenderer::new);
    event.registerEntityRenderer(ModEntityTypes.C4_EXPLOSIVE.get(),
        C4ExplosiveRenderer::new);
    event.registerEntityRenderer(ModEntityTypes.FIRE_GRENADE.get(),
        GrenadeRenderer.cylinder());
    event.registerEntityRenderer(ModEntityTypes.FRAG_GRENADE.get(),
        GrenadeRenderer.frag());
    event.registerEntityRenderer(ModEntityTypes.DECOY_GRENADE.get(),
        GrenadeRenderer.slim());
    event.registerEntityRenderer(ModEntityTypes.SMOKE_GRENADE.get(),
        GrenadeRenderer.cylinder());
    event.registerEntityRenderer(ModEntityTypes.FLASH_GRENADE.get(),
        GrenadeRenderer.slim());
    event.registerEntityRenderer(ModEntityTypes.RPG_ROCKET.get(),
        RpgRocketRenderer::new);
    event.registerEntityRenderer(ModEntityTypes.PLAYER_CORPSE.get(),
        PlayerCorpseRenderer::new);
    event.registerEntityRenderer(VehicleContent.T72B3.get(), T72B3Renderer::new);
    event.registerEntityRenderer(VehicleContent.T72_SHELL.get(),
        net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    for (var entityType : SurvivalEntityTypes.ZOMBIES.values()) {
      event.registerEntityRenderer(entityType.get(), SurvivalZombieRenderer::new);
    }
    event.registerEntityRenderer(SurvivalEntityTypes.SUPPLY_DROP.get(), SupplyDropRenderer::new);
    event.registerEntityRenderer(SurvivalEntityTypes.PIPE_BOMB.get(),
        GrenadeRenderer.forModel(ModModelLayers.PIPE_BOMB));
  }

  private void handleEntityRenderersAddLayers(EntityRenderersEvent.AddLayers event) {
    for (var skin : event.getSkins()) {
      PlayerRenderer renderer = event.getSkin(skin);
      if (renderer == null) {
        continue;
      }
      renderer.addLayer(new ParachuteLayer<>(renderer, event.getEntityModels()));
      renderer.addLayer(new HandcuffsLayer<>(renderer, event.getEntityModels()));
      renderer.addLayer(new ClothingLayer<>(renderer));
      renderer.addLayer(EquipmentLayer.builder(renderer)
          .slot(Equipment.Slot.MELEE)
          .useCrouchOrientation(true)
          .build());
      renderer.addLayer(EquipmentLayer.builder(renderer)
          .slot(Equipment.Slot.VEST)
          .attachment(EquipmentLayer.Attachment.BODY)
          .build());
      renderer.addLayer(EquipmentLayer.builder(renderer)
          .slot(Equipment.Slot.HAT)
          .attachment(EquipmentLayer.Attachment.HEAD)
          .build());
      renderer.addLayer(EquipmentLayer.builder(renderer)
          .slot(Equipment.Slot.GUN)
          .useCrouchOrientation(true)
          .transformation(poseStack -> poseStack.translate(-0.09375D, -0.15625D, 0.125D))
          .build());
      renderer.addLayer(EquipmentLayer.builder(renderer)
          .slot(Equipment.Slot.BACKPACK)
          .useCrouchOrientation(true)
          .build());
    }
  }

  private void handleEntityRenderersLayerDefinitions(
      EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(ModModelLayers.MUZZLE_FLASH,
        GunRenderer::createMuzzleFlashBodyLayer);
    event.registerLayerDefinition(ModModelLayers.PARACHUTE,
        ParachuteLayer::createParachuteBodyLayer);
    event.registerLayerDefinition(ModModelLayers.SUPPLY_DROP,
        SupplyDropModel::createBodyLayer);
    event.registerLayerDefinition(ModModelLayers.PIPE_BOMB,
        PipeBombModel::createBodyLayer);
    event.registerLayerDefinition(ModModelLayers.HANDCUFFS,
        HandcuffsLayer::createHandcuffsBodyLayer);
    event.registerLayerDefinition(ModModelLayers.C4_EXPLOSIVE,
        C4ExplosiveModel::createBodyLayer);
    event.registerLayerDefinition(ModModelLayers.CYLINDER_GRENADE,
        CylinderGrenadeModel::createBodyLayer);
    event.registerLayerDefinition(ModModelLayers.FRAG_GRENADE,
        FragGrenadeModel::createBodyLayer);
    event.registerLayerDefinition(ModModelLayers.SLIM_GRENADE,
        SlimGrenadeModel::createBodyLayer);
  }

  private void handleParticleFactoryRegisterEvent(RegisterParticleProvidersEvent event) {
    event.registerSpriteSet(ModParticleTypes.GRENADE_SMOKE.get(), GrenadeSmokeParticle.Factory::new);
    event.registerSpriteSet(ModParticleTypes.RGB_FLASH.get(), FlashParticle.Factory::new);
    event.registerSpriteSet(SurvivalParticleTypes.MILITARY_LOOT_GEN.get(),
        LootGeneratorParticle.Factory::new);
    event.registerSpriteSet(SurvivalParticleTypes.MEDIC_LOOT_GEN.get(),
        LootGeneratorParticle.Factory::new);
    event.registerSpriteSet(SurvivalParticleTypes.CIVILIAN_LOOT_GEN.get(),
        LootGeneratorParticle.Factory::new);
    event.registerSpriteSet(SurvivalParticleTypes.CIVILIAN_RARE_LOOT_GEN.get(),
        LootGeneratorParticle.Factory::new);
    event.registerSpriteSet(SurvivalParticleTypes.POLICE_LOOT_GEN.get(),
        LootGeneratorParticle.Factory::new);
  }

  private void handleItemColor(RegisterColorHandlersEvent.Item event) {
    event.register(new DynamicFluidContainerModel.Colors(),
        VehicleContent.PETROLEUM_BUCKET.get());
    ItemColor gunColour =
        (itemStack, tintIndex) -> {
          var gun = Gun.get(itemStack).orElse(null);
          if (gun != null) {
            var paint = Paint.get(gun.getPaintStack()).orElse(null);
            if (paint != null) {
              return paint.getColor().stream().findAny().orElse(0xFFFFFFFF);
            }
          }
          return 0xFFFFFFFF;
        };
    BuiltInRegistries.ITEM.stream()
        .filter(item -> item.getClass().isAnnotationPresent(RegisterGunColor.class))
        .forEach(item -> event.register(gunColour, item));
  }

  private void handleTextureStitch() {
    // TextureStitchEvent was removed in NeoForge 1.21.x. Additional sprite registration needs to
    // be moved to the newer client model/atlas events in a later runtime pass.
  }

  // ================================================================================
  // Client Forge Events
  // ================================================================================

  @SubscribeEvent
  public void handleTooltipEvent(ItemTooltipEvent event) {
    var item = event.getItemStack().getItem();
    var functions = ArbitraryTooltips.getFunctions(event.getItemStack().getItem());
    int lineIndex = 1;

    // Removes Attribute Text
    if (item instanceof MeleeWeaponItem || item instanceof ModAxeItem
        || item instanceof ModPickaxeItem || item instanceof ModShovelItem) {
      cleanUpToolTip(event.getToolTip());
    }

    // Applies the arbitrary tooltips
    for (var function : functions) {
      var level = event.getEntity() != null ? event.getEntity().level() : null;
      var tooltip = function.createTooltip(event.getItemStack(), level, event.getFlags());
      if (tooltip != null) {
        event.getToolTip().add(lineIndex++, tooltip);
      }
    }
  }

  private static boolean hasClientVisibleAmmo(Gun gun) {
    if (gun == null) {
      return false;
    }
    try {
      int providerAmmo = gun.getAmmoProvider().getMagazine().map(Magazine::getSize).orElse(0);
      int componentAmmo = GunAmmoStateUtil.getLoadedAmmo(gun.getItemStack());
      int cachedAmmo = 0;
      var minecraft = Minecraft.getInstance();
      if (minecraft != null && minecraft.player != null) {
        cachedAmmo = GunAmmoStateUtil.getCachedLoadedAmmo(minecraft.player.getId(),
            EquipmentSlot.MAINHAND, gun.getItemStack());
      }
      return Math.max(Math.max(providerAmmo, componentAmmo), cachedAmmo) > 0;
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  @SubscribeEvent
  public void handleClientTick(ClientTickEvent.Pre event) {

    this.pendingTankPrediction = false;
    this.tickTankEngineSounds();

    if (this.minecraft.player == null) {
      this.updateTankSightCamera(false);
      return;
    }

    // Keep every client renderer and shader from observing a hurt flash while the option is off.
    if (!isDamageFlashEnabled()) {
      this.minecraft.player.hurtTime = 0;
    }

    if (this.minecraft.getConnection() == null) {
      return;
    }

    if (this.minecraft.player.getVehicle()
        instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank) {
      this.updateTankSightCamera(this.minecraft.screen == null
          && tank.isWeaponController(this.minecraft.player)
          && this.isPhysicalTankSightDown());
      while (TANK_THERMAL.consumeClick()) {
        this.tankThermalEnabled = !this.tankThermalEnabled;
      }
      long window = this.minecraft.getWindow().getWindow();
      // Read the requested physical WASD controls directly. This avoids other movement/keybinding
      // handlers consuming the normal LocalPlayer input while it is riding a non-living entity.
      float throttle = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS ? 1.0F : 0.0F)
          - (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS ? 1.0F : 0.0F);
      float turn = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS ? 1.0F : 0.0F)
          - (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS ? 1.0F : 0.0F);
      boolean isDriver = tank.getControllingPassenger() == this.minecraft.player;
      if (!isDriver) {
        throttle = 0.0F;
        turn = 0.0F;
      }
      float lookYaw = this.minecraft.player.getYRot();
      float lookPitch = this.minecraft.player.getXRot();
      // The front-facing third-person camera looks in the opposite direction to the player
      // entity. Use the actual camera-facing direction so the turret still follows the view.
      if (this.minecraft.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
        lookYaw = Mth.wrapDegrees(lookYaw + 180.0F);
      }
      int tankActions = 0;
      if (tank.isWeaponController(this.minecraft.player)) {
        boolean tankTriggerDown;
        try {
          tankTriggerDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT)
              == GLFW.GLFW_PRESS;
        } catch (RuntimeException ignored) {
          tankTriggerDown = this.minecraft.options.keyAttack.isDown();
        }
        if (tankTriggerDown) tankActions |= 1;
        if (TANK_COAX.isDown() && tank.canUseCoax(this.minecraft.player)) tankActions |= 2;
        if (TANK_SMOKE.isDown()) tankActions |= 4;
        if (TOGGLE_FIRE_MODE.isDown()) tankActions |= 8;
        if (RELOAD.isDown()) tankActions |= 16;
        if (TANK_HEADLIGHTS.isDown()) tankActions |= 32;
      }
      if (TANK_GUN_CONTROL.isDown()) tankActions |= 64;
      if (TANK_EXPOSURE.isDown()) tankActions |= 128;
      NetworkChannel.PLAY.sendToServer(
          new TankInputMessage(throttle, turn, lookYaw, lookPitch, tankActions));
      if (isDriver) {
        // Apply prediction after the world tick. Entity.tick() records the old transform first;
        // moving here used to overwrite that baseline and made the tank visibly jump at 20 Hz.
        this.pendingTankThrottle = throttle;
        this.pendingTankTurn = turn;
        this.pendingTankPrediction = true;
      }
    } else {
      this.tankThermalEnabled = false;
      this.updateTankSightCamera(false);
    }

    boolean isRightClickDown = this.minecraft.options.keyUse.isDown();
    int ticks = this.minecraft.player.getUseItemRemainingTicks();

    if (isRightClickDown || ticks > 0) {
      if (!this.wasRightClickDown || ticks == 0) {
        this.rightClickTicks = 0;
      }

      this.rightClickTicks++;
    } else {
      this.rightClickTicks = 0;
    }
    this.wasRightClickDown = isRightClickDown;
    NetworkChannel.PLAY.sendToServer(new RightClickStateMessage(
        isRightClickDown, this.rightClickTicks));

    var player = this.getPlayerExtension().orElse(null);
    if (player != null) {
      while (TOGGLE_COMBAT_MODE.consumeClick()) {
        player.setCombatModeEnabled(!player.isCombatModeEnabled());
      }

      if (this.traumaAimTicks > 0) {
        this.traumaAimTicks--;
        var severityMultiplier = 1.0F + this.lastTraumaSeverity.ordinal() * 0.25F;
        this.cameraManager.randomRecoil(this.traumaAimStrength * severityMultiplier, false);
      } else if (this.lastTraumaSeverity != TraumaSeverity.NONE) {
        this.lastTraumaSeverity = TraumaSeverity.NONE;
        this.traumaAimStrength = 0.0F;
      }

      var gun = player.mainHandGun().orElse(null);
      boolean drivingTank = this.minecraft.player.getVehicle()
          instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank
          && (tank.isWeaponController(this.minecraft.player)
              || (tank.isRearPassenger(this.minecraft.player)
                  && !tank.isPassengerExposed(this.minecraft.player)));

      var levelFocused = !this.minecraft.isPaused() && this.minecraft.getOverlay() == null
          && (this.minecraft.screen == null);

      // Forge 1.21.1 + OptiFine can recreate the gun capability after each SyncGunSlot and the
      // MouseButton.Pre release/press stream is not stable enough to be the only long-hold source.
      // Poll the physical left mouse button every client tick to keep the client-side shooting
      // future alive while the button is actually held. Do not send a server shot request here:
      // server consumption is driven by AbstractGun.shoot(), once per rendered client shot.
      boolean physicalAttackDown = false;
      try {
        physicalAttackDown = GLFW.glfwGetMouseButton(this.minecraft.getWindow().getWindow(),
            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
      } catch (RuntimeException ignored) {
        physicalAttackDown = this.minecraft.options.keyAttack.isDown();
      }
      if (drivingTank) {
        physicalAttackDown = false;
      }

      boolean physicalUseDown = isRightClickDown;
      try {
        physicalUseDown = GLFW.glfwGetMouseButton(this.minecraft.getWindow().getWindow(),
            GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
      } catch (RuntimeException ignored) {
        physicalUseDown = this.minecraft.options.keyUse.isDown();
      }

      var currentMainHandStack = this.minecraft.player.getMainHandItem();
      Item currentMainHandItem = currentMainHandStack.isEmpty() ? Items.AIR : currentMainHandStack.getItem();
      boolean holdingGunItem = currentMainHandItem instanceof GunItem;
      boolean gameModeChanged = this.lastInstabuild != this.minecraft.player.getAbilities().instabuild
          || this.lastSpectator != this.minecraft.player.isSpectator();
      boolean mainHandItemChanged = currentMainHandItem != this.lastMainHandItem;

      if (gameModeChanged || mainHandItemChanged || !holdingGunItem) {
        // A real slot/item ownership change (hotbar switch, drop, pickup, or game-mode swap) must
        // invalidate the per-slot client ammo cache. The cache is only a capability-recreation
        // bridge for the same visible stack; keeping it after a drop/pickup lets a newly picked
        // empty server stack restore the old local magazine and shoot forever client-side.
        GunAmmoStateUtil.clearSyncedLoadedMagazine(this.minecraft.player.getId(), EquipmentSlot.MAINHAND);
        if (holdingGunItem && mainHandItemChanged) {
          this.pendingMainHandGunSyncTicks = 5;
        } else if (!holdingGunItem) {
          this.pendingMainHandGunSyncTicks = 0;
        }
        if (this.wasAttackDown || physicalAttackDown || (gun != null && gun.isTriggerPressed())) {
          if (gun != null) {
            gun.setTriggerPressed(player, false, true);
          }
          AbstractGun.stopActiveTrigger(player, true);
        }
        this.wasAttackDown = false;
        this.wasUseDown = false;
        this.toggleSecondaryActionActive = false;
        if (physicalAttackDown) {
          this.suppressAttackUntilReleased = true;
        }
      }

      if (!physicalAttackDown) {
        this.suppressAttackUntilReleased = false;
      }

      if (holdingGunItem && this.pendingMainHandGunSyncTicks > 0) {
        PacketDistributor.sendToServer(
            new TriggerPressedMessage(this.minecraft.player.getId(), false, currentMainHandStack));
        this.pendingMainHandGunSyncTicks--;
      }

      boolean shouldHoldTrigger = holdingGunItem && levelFocused && !player.entity().isSpectator()
          && !this.suppressAttackUntilReleased && physicalAttackDown && hasClientVisibleAmmo(gun);

      this.lastMainHandItem = currentMainHandItem;
      this.lastInstabuild = this.minecraft.player.getAbilities().instabuild;
      this.lastSpectator = this.minecraft.player.isSpectator();

      if (gun != null && !drivingTank) {
        boolean isReloadingOrUnloading = player.getAction()
            .filter(action -> action instanceof AbstractReloadAction
                || action instanceof RemoveMagazineAction).isPresent();

        // NeoForge 1.21.1 does not reliably deliver MouseButton.Pre to this mod in every
        // client setup. Drive secondary actions from the physical RMB state instead. HOLD mirrors
        // the button continuously; TOGGLE changes exactly once on the up -> down edge.
        if (holdingGunItem && levelFocused) {
          switch (gun.getSecondaryActionTrigger()) {
            case HOLD -> {
              if (gun.isPerformingSecondaryAction() != physicalUseDown) {
                gun.setPerformingSecondaryAction(player, physicalUseDown, true);
              }
            }
            case TOGGLE -> {
              // Keep the player's desired ADS state outside the Gun instance. NeoForge may replace
              // the held ItemStack after each shot/ammo component sync, which creates a fresh Gun
              // whose secondary-action flag defaults to false. Without this persistent intent, ADS
              // is lost on firing and cannot toggle back until RMB is released and pressed again.
              boolean toggledByPlayer = false;
              if (physicalUseDown && !this.wasUseDown && !isReloadingOrUnloading) {
                this.toggleSecondaryActionActive = !this.toggleSecondaryActionActive;
                toggledByPlayer = true;
              }
              if (isReloadingOrUnloading) {
                this.toggleSecondaryActionActive = false;
              }
              if (gun.isPerformingSecondaryAction() != this.toggleSecondaryActionActive) {
                // Only a genuine RMB edge is authoritative input and needs a network update.
                // Re-applying ADS to a recreated client Gun instance must be local-only; sending
                // another packet here makes the server echo state back and produces per-shot ADS
                // oscillation/flicker.
                gun.setPerformingSecondaryAction(player,
                    this.toggleSecondaryActionActive, toggledByPlayer);
              }
            }
          }
        }

        if (shouldHoldTrigger) {
          // Semi/burst weapons need a new physical press, even if recoil/reload or
          // a capability sync stopped the previous runtime firing task.
          if (!gun.isTriggerPressed()
              && (gun.getFireMode().getMaxShots().isEmpty() || !this.wasAttackDown)) {
            gun.setTriggerPressed(player, true, false);
          }
        } else if (this.wasAttackDown || gun.isTriggerPressed()) {
          gun.setTriggerPressed(player, false, true);
        }
      } else if (this.wasAttackDown || physicalAttackDown) {
        AbstractGun.stopActiveTrigger(player, true);
        if (physicalAttackDown) {
          this.suppressAttackUntilReleased = true;
        }
      }
      this.wasAttackDown = shouldHoldTrigger;
      this.wasUseDown = holdingGunItem && levelFocused && physicalUseDown;

      this.cameraManager.tick();

      if (!levelFocused || player.entity().isSpectator()) {
        // Stop gun actions if level not focused. Also stop any older active trigger owner even if
        // the current visible main-hand stack is no longer a gun.
        if (gun != null) {
          if (gun.isTriggerPressed()) {
            gun.setTriggerPressed(player, false, true);
          }
          if (gun.isPerformingSecondaryAction()) {
            gun.setPerformingSecondaryAction(player, false, true);
          }
        }
        AbstractGun.stopActiveTrigger(player, true);
        this.wasAttackDown = false;
        this.wasUseDown = false;
        this.toggleSecondaryActionActive = false;
        if (physicalAttackDown) {
          this.suppressAttackUntilReleased = true;
        }
        return;
      }

      // Update gun input
      if (gun != null && !drivingTank) {
        while (TOGGLE_FIRE_MODE.consumeClick()) {
          gun.toggleFireMode(player, true);
        }
        while (RELOAD.consumeClick()) {
          gun.getAmmoProvider().reload(player);
        }
        while (REMOVE_MAGAZINE.consumeClick()) {
          gun.getAmmoProvider().unload(player);
        }
      }

      // Update crouching
      if (this.minecraft.player.isShiftKeyDown() != this.wasSneaking) {
        if (this.minecraft.player.isShiftKeyDown()) {
          final long currentTime = Util.getMillis();
          if (currentTime - this.lastSneakPressTime <= DOUBLE_CLICK_DURATION) {
            player.setCrouching(true, true);
          }
          this.lastSneakPressTime = Util.getMillis();
        } else {
          player.setCrouching(false, true);
        }
        this.wasSneaking = this.minecraft.player.isShiftKeyDown();
      }

      // Update tutorial
      // While seated in a tank, the normal inventory key opens the vehicle's internal storage.
      // Consume it only in that state so vanilla inventory behavior remains unchanged elsewhere.
      if (this.minecraft.player.getVehicle()
          instanceof com.craftingdead.core.world.vehicle.T72B3Entity) {
        while (this.minecraft.options.keyInventory.consumeClick()) {
          NetworkChannel.PLAY.sendToServer(new OpenEquipmentMenuMessage());
        }
      }
      while (OPEN_EQUIPMENT_MENU.consumeClick()) {
        if (!player.isHandcuffed()) {
          NetworkChannel.PLAY.sendToServer(new OpenEquipmentMenuMessage());
          if (this.currentModTutorialStep != null) {
            this.currentModTutorialStep.openEquipmentMenu();
          }
        }
      }
      TutorialSteps currentTutorialStep = this.minecraft.options.tutorialStep;
      if (this.lastTutorialStep != currentTutorialStep) {
        if (currentTutorialStep == TutorialSteps.NONE) {
          this.setTutorialStep(clientConfig.tutorialStep.get());
        }
        this.lastTutorialStep = currentTutorialStep;
      }

      // Update adrenaline effects
      if (this.minecraft.player.hasEffect(ModMobEffects.holder(ModMobEffects.ADRENALINE))) {
        this.wasAdrenalineActive = true;
        this.effectsManager.setHighpassLevels(1.0F, 0.015F);
        this.effectsManager.setDirectHighpassForAll();
      } else if (this.wasAdrenalineActive) {
        this.wasAdrenalineActive = false;
        this.effectsManager.removeFilterForAll();
      }

    }
  }

  private void updateTankSightCamera(boolean active) {
    if (active) {
      if (this.cameraTypeBeforeTankSight == null) {
        this.cameraTypeBeforeTankSight = this.minecraft.options.getCameraType();
      }
      if (this.minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
        this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);
      }
    } else if (this.cameraTypeBeforeTankSight != null) {
      this.minecraft.options.setCameraType(this.cameraTypeBeforeTankSight);
      this.cameraTypeBeforeTankSight = null;
    }
  }

  private void tickTankEngineSounds() {
    this.tankEngineSounds.entrySet().removeIf(entry -> entry.getValue().isStopped());
    if (this.minecraft.level == null || this.minecraft.level.getGameTime() % 10L != 0L) {
      return;
    }
    for (Entity entity : this.minecraft.level.entitiesForRendering()) {
      if (!(entity instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank)
          || tank.isRemoved()) {
        continue;
      }
      this.tankEngineSounds.computeIfAbsent(tank.getId(), id -> {
        T72EngineSoundInstance sound = new T72EngineSoundInstance(tank);
        this.minecraft.getSoundManager().play(sound);
        return sound;
      });
    }
  }

  @SubscribeEvent
  public void handleTankPrediction(ClientTickEvent.Post event) {
    if (!this.pendingTankPrediction || this.minecraft.player == null) return;
    if (this.minecraft.player.getVehicle()
        instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank
        && tank.getControllingPassenger() == this.minecraft.player) {
      tank.applyClientPrediction(this.pendingTankThrottle, this.pendingTankTurn);
    }
    this.pendingTankPrediction = false;
  }

  @SubscribeEvent
  public void handleRawMouse(InputEvent.MouseButton.Pre event) {
    if (this.minecraft.player != null
        && this.minecraft.player.getVehicle()
            instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank
        && (tank.isWeaponController(this.minecraft.player)
            || (tank.isRearPassenger(this.minecraft.player)
                && !tank.isPassengerExposed(this.minecraft.player)))
        && this.minecraft.options.keyAttack.matchesMouse(event.getButton())) {
      // The selected weapon controller owns the tank trigger. Other hatch passengers retain
      // normal held-gun input until they press H to take over the turret.
      event.setCanceled(true);
      return;
    }
    var player = this.getPlayerExtension().orElse(null);
    if (player == null
        || this.minecraft.getOverlay() != null
        || this.minecraft.screen != null
        || player.entity().isSpectator()) {
      return;
    }

    var gun = player.mainHandGun().orElse(null);
    boolean isReloadingOrUnloading  = player.getAction()
        .filter(action -> action instanceof AbstractReloadAction
            || action instanceof RemoveMagazineAction).isPresent();
    if (this.minecraft.options.keyAttack.matchesMouse(event.getButton())) {
      if (gun != null) {
        boolean triggerPressed = event.getAction() == GLFW.GLFW_PRESS;
        if (triggerPressed) {
          event.setCanceled(true);
        }
        gun.setTriggerPressed(player, triggerPressed, true);
      }
    } else if (this.minecraft.options.keyUse.matchesMouse(event.getButton())) {
      if (gun != null) {
        // Cancel vanilla item use, but do not mutate ADS here. ClientTick reads the physical
        // button and handles HOLD/TOGGLE exactly once, avoiding missing or duplicate aim events.
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public void handleRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
    final var heldStack = event.getEntity().getMainHandItem();
    if (event.getEntity().getVehicle()
        instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank) {
      float scale = tank.getControllingPassenger() == event.getEntity() ? 0.70F : 0.80F;
      event.getPoseStack().scale(scale, scale, scale);
      if (event.getRenderer().getModel() instanceof HumanoidModel<?> tankModel) {
        tankModel.riding = true;
      }
    }
    // TODO Unpleasant way of setting pose for gun. Introduce nicer system (with better poses).
    if (event.getRenderer().getModel() instanceof HumanoidModel<?> model
        && heldStack.getItem() instanceof GunItem
        && !gunPoseBlacklist.contains(event.getEntity().getClass().getName())) {
      switch (event.getEntity().getMainArm()) {
        case LEFT -> {
          model.leftArmPose = ArmPose.BOW_AND_ARROW;
        }
        case RIGHT -> {
          model.rightArmPose = ArmPose.BOW_AND_ARROW;
        }
      }
    }
  }

  /**
   * Forge 1.21.x removed the old RenderGuiOverlayEvent cancellation path.
   * Vanilla overlay suppression now needs either layered GUI registration or a targeted Mixin.
   *
   * <p>The custom Crafting Dead HUD itself is registered in handleRegisterGuiOverlays().
   * Crosshair replacement remains implemented there / in IngameGui until the final overlay
   * cancellation strategy is chosen.
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleCameraSetup(ViewportEvent.ComputeCameraAngles event) {
    if (this.minecraft.player != null
        && this.minecraft.player.getVehicle()
            instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank
        && tank.getControllingPassenger() == this.minecraft.player
        && this.minecraft.options.getCameraType() != CameraType.FIRST_PERSON
        && !this.isPhysicalTankSightDown()) {
      // Position from the tank itself instead of extending vanilla's four-block player camera.
      // That old anchor sits inside this large model and produced the close-up shown in testing.
      float cameraPitch = Math.max(16.0F, event.getPitch());
      Vec3 view = Vec3.directionFromRotation(cameraPitch, event.getYaw());
      float partialTick = ClientFrameTimeUtil.getFrameTime(this.minecraft);
      Vec3 anchor = tank.getPosition(partialTick).add(0.0D, 2.35D, 0.0D);
      Vec3 desiredPosition = anchor.subtract(view.scale(19.0D));
      CameraInvoker camera = (CameraInvoker) event.getCamera();
      camera.craftingdead$setPosition(desiredPosition);
      event.setPitch(cameraPitch);
    } else if (this.minecraft.player != null
        && this.minecraft.player.getVehicle()
            instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank
        && tank.isWeaponController(this.minecraft.player)
        && this.isPhysicalTankSightDown()) {
      // Right mouse moves an active rear gunner from the normal open-hatch view to the original
      // turret optic. Releasing it immediately restores the ordinary passenger camera and gun use.
      float partialTick = ClientFrameTimeUtil.getFrameTime(this.minecraft);
      ((CameraInvoker) event.getCamera()).craftingdead$setPosition(
          tank.getGunnerOpticPosition(partialTick));
      event.setYaw(tank.getYRot() + tank.getTurretYaw());
      event.setPitch(-tank.getTurretPitch());
    }
    boolean aiming = this.isCameraPlayerAiming();
    if (aiming) {
      mutableCameraRotations.set(0.0F, 0.0F, 0.0F);
      this.lastPitch = 0.0F;
      this.lastYaw = 0.0F;
      this.lastRoll = 0.0F;
    } else {
      this.cameraManager.getCameraRotations(ClientFrameTimeUtil.getFrameTime(this.minecraft),
          mutableCameraRotations);
    }
    if (this.minecraft.cameraEntity instanceof LivingEntity livingEntity) {
      var itemStack = livingEntity.getMainHandItem();
      var itemRenderer = this.itemRenderDispatcher.getItemRenderer(itemStack.getItem());
      if (itemRenderer != null && !aiming) {
        itemRenderer.rotateCamera(itemStack, livingEntity, ClientFrameTimeUtil.getFrameTime(this.minecraft),
            mutableCameraRotations);
      }
    }

    this.lastPitch = Mth.lerp(0.1F, this.lastPitch, mutableCameraRotations.x());
    this.lastYaw = Mth.lerp(0.1F, this.lastYaw, mutableCameraRotations.y());
    this.lastRoll = Mth.lerp(0.1F, this.lastRoll, mutableCameraRotations.z());
    mutableCameraRotations.set(0.0F, 0.0F, 0.0F);
    event.setPitch(event.getPitch() + this.lastPitch);
    event.setYaw(event.getYaw() + this.lastYaw);
    event.setRoll(event.getRoll() + this.lastRoll);
  }

  @SubscribeEvent
  public void handeFOVModifier(ViewportEvent.ComputeFov event) {
    if (this.minecraft.player != null
        && this.minecraft.player.getVehicle()
            instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank
        && tank.getControllingPassenger() == this.minecraft.player
        && this.minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
      event.setFOV(Math.max(event.getFOV(), 85.0D));
    }
    if (this.minecraft.player != null
        && this.minecraft.player.getVehicle()
            instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank
        && tank.isWeaponController(this.minecraft.player)
        && this.isPhysicalTankSightDown()) {
      event.setFOV(50.0D);
    }
    var player = this.getCameraPlayer();
    if (player != null) {
      var scope = Scope.get(player.entity().getMainHandItem()).orElse(null);
      if (scope != null && (this.toggleSecondaryActionActive || scope.isScoping(player))) {
        // ClientDist owns the player's ADS intent. Use it as the render authority so a freshly
        // recreated Gun instance cannot expose one unscoped frame before its local flag is restored.
        float zoomMultiplier = Math.max(1.0F, scope.getZoomMultiplier(player));
        event.setFOV(event.getFOV() / zoomMultiplier);
      }
    }
    if (!this.isCameraPlayerAiming()) {
      event.setFOV(event.getFOV()
          + this.cameraManager.getFov(ClientFrameTimeUtil.getFrameTime(this.minecraft)));
    }
  }

  private boolean isPhysicalTankSightDown() {
    try {
      return GLFW.glfwGetMouseButton(this.minecraft.getWindow().getWindow(),
          GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    } catch (RuntimeException ignored) {
      return this.minecraft.options.keyUse.isDown();
    }
  }

  private boolean isCameraPlayerAiming() {
    var player = this.getCameraPlayer();
    if (player == null) {
      return false;
    }
    var scope = Scope.get(player.entity().getMainHandItem()).orElse(null);
    return scope != null && (this.toggleSecondaryActionActive || scope.isScoping(player));
  }

  @SubscribeEvent
  public void handleRenderTick(RenderFrameEvent.Pre event) {
    if (this.minecraft.player != null) {
      this.cameraManager.getLookRotationDelta(FOV);
      this.minecraft.player.turn(FOV.getY(), FOV.getX());
    }
  }

  @SubscribeEvent
  public void handleRenderTickPost(RenderFrameEvent.Post event) {
    if (this.minecraft.player != null) {
      this.updateAdrenalineShader(ClientFrameTimeUtil.getFrameTime(this.minecraft));
      if (this.minecraft.screen == null) {
        var guiGraphics = new GuiGraphics(this.minecraft, this.minecraft.renderBuffers().bufferSource());
        this.ingameGui.renderFlashBangOverlay(this.minecraft.player, guiGraphics,
            this.minecraft.getWindow().getGuiScaledWidth(),
            this.minecraft.getWindow().getGuiScaledHeight(), ClientFrameTimeUtil.getFrameTime(this.minecraft));
        guiGraphics.flush();
      }
    }
  }

  @SubscribeEvent
  public void handleRenderScreen(ScreenEvent.Render.Post event) {
    if (this.minecraft.player != null) {
      this.ingameGui.renderFlashBangOverlay(this.minecraft.player, event.getGuiGraphics(),
          this.minecraft.getWindow().getGuiScaledWidth(),
          this.minecraft.getWindow().getGuiScaledHeight(), ClientFrameTimeUtil.getFrameTime(this.minecraft));
    }
  }

  private void updateAdrenalineShader(float partialTicks) {
    final var gameRenderer = this.minecraft.gameRenderer;
    final var currentEffect = gameRenderer.currentEffect();
    final var shaderLoaded = currentEffect != null
        && currentEffect.getName().equals(ADRENALINE_SHADER.toString());

    // The legacy adrenaline post shader depends on vanilla blur.json, which is not present in
    // Minecraft 1.21.x. Even a single attempt logs a full stack trace; repeated attempts can fill
    // memory and freeze HMCL/IDEA. Keep the adrenaline gameplay effect, but disable this visual
    // post-process entirely until a 1.21-compatible shader is provided.
    this.adrenalineShaderUnavailable = true;
    this.adrenalineShaderStartTime = 0L;
    if (shaderLoaded) {
      gameRenderer.shutdownEffect();
    }
  }

  @SubscribeEvent
  public void handleScreenOpen(ScreenEvent.Opening event) {
    if (this.minecraft == null || this.minecraft.player == null) {
      return;
    }

    final var player = this.minecraft.player;
    var playerExtension = PlayerExtension.get(player);

    // Prevents current screen from being closed before new one opens.
    if (this.minecraft.screen instanceof EquipmentScreen screen
        && event.getNewScreen() == null
        && screen.isTransitioning()) {
      event.setCanceled(true);
    }

    // Prevents the player from opening the inventory if handcuffed
    if (playerExtension != null && playerExtension.isHandcuffed()) {
      if (event.getNewScreen() instanceof InventoryScreen && isSurvivalMode()) {
        event.setCanceled(true);
        return;
      }
    }
  }

  @SubscribeEvent
  public void handleOptionsScreenInit(ScreenEvent.Init.Post event) {
    if (!(event.getScreen() instanceof OptionsScreen screen)) {
      return;
    }

    var toggle = CycleButton.onOffBuilder(clientConfig.displayBlood.get())
        .create(screen.width - 155, 6, 150, 20,
            Component.translatable("options.craftingdead.client.damage_flash"),
            (button, enabled) -> {
              clientConfig.displayBlood.set(enabled);
              clientConfigSpec.save();
              if (!enabled && this.minecraft.player != null) {
                this.minecraft.player.hurtTime = 0;
              }
            });
    event.addListener(toggle);
  }

  @SubscribeEvent
  public void handleRenderHand(RenderHandEvent event) {
    final var player = this.minecraft.player;
    final var cameraPlayer = this.getCameraPlayer();

    if (player != null
        && player.getVehicle() instanceof com.craftingdead.core.world.vehicle.T72B3Entity tank
        && tank.isWeaponController(player) && this.isPhysicalTankSightDown()) {
      event.setCanceled(true);
      return;
    }

    if (cameraPlayer != null && cameraPlayer.isHandcuffed()) {
      event.setCanceled(true);
      if (event.getHand() == InteractionHand.MAIN_HAND) {
        this.renderFirstPersonHandcuffs(event.getPoseStack(), event.getMultiBufferSource(),
            event.getPackedLight());
      }
    }

    if (player != null && player.hasEffect(ModMobEffects.holder(ModMobEffects.PARACHUTE))) {
      renderParachute(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }
  }

  private void renderFirstPersonHandcuffs(PoseStack poseStack, MultiBufferSource bufferSource,
      int packedLight) {
    if (this.minecraft.player == null
        || !(this.minecraft.getEntityRenderDispatcher().getRenderer(this.minecraft.player)
            instanceof PlayerRenderer renderer)) {
      return;
    }

    poseStack.pushPose();
    poseStack.translate(-0.27D, -0.58D, -0.82D);
    poseStack.scale(0.68F, 0.68F, 0.68F);
    poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F));
    poseStack.mulPose(Axis.YP.rotationDegrees(12.0F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F));
    renderer.renderRightHand(poseStack, bufferSource, packedLight, this.minecraft.player);
    poseStack.popPose();

    poseStack.pushPose();
    poseStack.translate(0.27D, -0.58D, -0.82D);
    poseStack.scale(0.68F, 0.68F, 0.68F);
    poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F));
    poseStack.mulPose(Axis.YP.rotationDegrees(-12.0F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
    renderer.renderLeftHand(poseStack, bufferSource, packedLight, this.minecraft.player);
    poseStack.popPose();

    poseStack.pushPose();
    poseStack.translate(0.0D, -0.54D, -0.92D);
    poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    poseStack.scale(0.42F, 0.42F, 0.42F);

    ModelPart handcuffsModel = this.minecraft.getEntityModels().bakeLayer(ModModelLayers.HANDCUFFS);
    handcuffsModel.render(poseStack,
        bufferSource.getBuffer(RenderType.entityCutoutNoCull(HANDCUFFS_TEXTURE)), packedLight,
        OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
  }

  @SubscribeEvent
  public void handleClickInput(InputEvent.InteractionKeyMappingTriggered event) {
    final var player = this.getCameraPlayer();
    if (player != null && player.isHandcuffed()) {
      event.setSwingHand(false);
      if (event.isUseItem() || event.isAttack()) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public void onMouseInput(InputEvent.MouseButton.Pre event) {
    if (this.minecraft.options.keyUse.matchesMouse(event.getButton())
        && event.getAction() == GLFW.GLFW_PRESS) {
      if (this.minecraft.player != null && this.minecraft.screen == null) {
        NetworkChannel.PLAY.sendToServer(new DamageHandcuffsMessage());
      }
    }
  }

  @SubscribeEvent
  public void onKeyInput(InputEvent.Key event) {
    if (this.minecraft.options.keyUse.matches(event.getKey(), event.getScanCode())
        && event.getAction() == GLFW.GLFW_PRESS) {
      if (this.minecraft.player != null && this.minecraft.screen == null) {
        NetworkChannel.PLAY.sendToServer(new DamageHandcuffsMessage());
      }
    }
  }

  // ================================================================================
  // Client-only helper methods
  // ================================================================================

  public void checkApplyFlashEffects(FlashGrenadeEntity flashGrenadeEntity) {
    // Applies the flash effect at client side for a better delay compensation.
    // In 1.21.x the old frustum-based check is not reliable enough for first person: a grenade
    // near the crosshair can be reported as outside the frustum, which only gave the old tiny
    // 5-tick "behind the player" flash. Use distance + line of sight as a robust fallback.
    if (this.minecraft.player == null || flashGrenadeEntity == null) {
      return;
    }

    double flashRange = ServerConfig.instance.explosivesFlashRadius.get();
    double distance = this.minecraft.player.distanceTo(flashGrenadeEntity);
    boolean closeEnough = distance <= flashRange;
    boolean hasLineOfSight = this.minecraft.player.hasLineOfSight(flashGrenadeEntity);
    boolean visible = RenderUtil.isInsideFrustum(flashGrenadeEntity, false);

    if (!visible && closeEnough && hasLineOfSight) {
      Vec3 toGrenade = flashGrenadeEntity.position()
          .subtract(this.minecraft.player.getEyePosition())
          .normalize();
      double facingDot = this.minecraft.player.getLookAngle().normalize().dot(toGrenade);
      // Count anything in front or to the side as visible enough for a real flash.
      visible = facingDot > -0.25D;
    }

    int duration = flashGrenadeEntity.calculateDuration(this.minecraft.player, visible);
    if (closeEnough && hasLineOfSight) {
      double distanceProportion = Mth.clamp(distance / flashRange, 0.0D, 1.0D);
      int proximityDuration = (int) Mth.lerp(1.0D - distanceProportion, 35.0D,
          FlashGrenadeEntity.EFFECT_MAX_DURATION);
      duration = Math.max(duration, proximityDuration);
    }

    if (duration > 0) {
      var flashEffect = new MobEffectInstance(
          ModMobEffects.holder(ModMobEffects.FLASH_BLINDNESS),
          duration, 0, false, false, false);
      ModMobEffects.applyOrOverrideIfLonger(this.minecraft.player, flashEffect);
    }
  }

  // ================================================================================
  // Hooks
  // ================================================================================

  /**
   * @see com.craftingdead.core.mixin.PlayerRendererMixin
   */
  public static void renderArmWithClothing(PlayerRenderer renderer, PoseStack poseStack,
      MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer playerEntity,
      ModelPart arm, ModelPart sleeve) {

    ResourceLocation clothingTexture = java.util.Optional.ofNullable(LivingExtension.get(playerEntity))
        .flatMap(living -> living.getEquipmentInSlot(Equipment.Slot.CLOTHING, Clothing.class))
        .map(clothing -> clothing.getTexture(playerEntity.getSkin().model().id()))
        .orElse(null);

    RenderArmClothingEvent event = new RenderArmClothingEvent(playerEntity, clothingTexture);
    NeoForge.EVENT_BUS.post(event);
    clothingTexture = event.getClothingTexture();

    if (clothingTexture != null) {
      final var model = renderer.getModel();
      model.attackTime = 0.0F;
      model.crouching = false;
      model.swimAmount = 0.0F;
      model.setupAnim(playerEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

      arm.visible = true;
      sleeve.visible = true;

      arm.xRot = 0.0F;
      arm.render(poseStack,
          bufferSource.getBuffer(RenderType.entityTranslucent(clothingTexture)), packedLight,
          OverlayTexture.NO_OVERLAY);
      sleeve.xRot = 0.0F;
      sleeve.render(poseStack,
          bufferSource.getBuffer(RenderType.entityTranslucent(clothingTexture)), packedLight,
          OverlayTexture.NO_OVERLAY);
    }
  }

  private void renderParachute(PoseStack poseStack, MultiBufferSource bufferSource,
      int packedLight) {
    poseStack.pushPose();
    poseStack.translate(0.0D, -1.08D, -1.0D);
    poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

    var vertexConsumer = ItemRenderer.getArmorFoilBuffer(
        bufferSource,
        RenderType.armorCutoutNoCull(
            ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/entity/parachute.png")
        ),
        false
    );

    EntityModelSet entityModelSet = Minecraft.getInstance().getEntityModels();
    ModelPart parachuteModel = entityModelSet.bakeLayer(ModModelLayers.PARACHUTE);
    parachuteModel.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
  }

  private boolean isSurvivalMode() {
    return this.minecraft.player != null && !this.minecraft.player.getAbilities().instabuild
        && !this.minecraft.player.isSpectator();
  }

  private void cleanUpToolTip(List<Component> toolTip) {
    toolTip.removeIf(component -> {
      var text = component.getString();
      return text.contains("When in Main Hand:") ||
          text.contains("Attack Damage") ||
          text.contains("Attack Knockback") ||
          text.contains("Attack Speed") ||
          text.isBlank();
    });
  }
}
