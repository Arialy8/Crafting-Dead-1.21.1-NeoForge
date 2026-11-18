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

package com.craftingdead.core.world.item;


import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.trauma.ProtectionConfig;
import com.craftingdead.core.client.animation.gun.InspectAnimation;
import com.craftingdead.core.client.animation.gun.ReloadAnimation;
import com.craftingdead.core.client.animation.gun.RocketReloadAnimation;
import com.craftingdead.core.client.animation.gun.ShootAnimation;
import com.craftingdead.core.util.FunctionalUtil;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.entity.grenade.C4Explosive;
import com.craftingdead.core.world.entity.grenade.DecoyGrenadeEntity;
import com.craftingdead.core.world.entity.grenade.FireGrenadeEntity;
import com.craftingdead.core.world.entity.grenade.FlashGrenadeEntity;
import com.craftingdead.core.world.entity.grenade.FragGrenade;
import com.craftingdead.core.world.entity.grenade.SmokeGrenadeEntity;
import com.craftingdead.core.world.inventory.GenericMenu;
import com.craftingdead.core.world.item.ClothingItem.ClothingType;
import com.craftingdead.core.world.item.combatslot.CombatSlot;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.GunAnimationEvent;
import com.craftingdead.core.world.item.gun.GunConfigurations;
import com.craftingdead.core.world.item.gun.aimable.AimableGunItem;
import com.craftingdead.core.world.item.gun.attachment.Attachments;
import com.craftingdead.core.world.item.gun.minigun.MinigunItem;
import com.craftingdead.core.world.item.gun.skin.Skins;
import com.craftingdead.survival.world.item.SurvivalItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.craftingdead.core.world.item.attribute.ModAttributeModifierIds;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import java.util.function.Supplier;

public class ModItems {

  public static final DeferredRegister<Item> deferredRegister =
      DeferredRegister.create(Registries.ITEM, CraftingDead.ID);

  public static final DeferredRegister<CreativeModeTab> tabDeferredRegister =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftingDead.ID);

  public static final Supplier<CreativeModeTab> COSMETICS_TAB = tabDeferredRegister.register(
      "cosmetics", () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.craftingdead.cosmetics"))
          .icon(ModItems::createCosmeticsTabIcon)
          .displayItems((parameters, output) -> contributeCosmeticsTab(output))
          .build());

  public static final Supplier<CreativeModeTab> COMBAT_TAB = tabDeferredRegister.register(
      "combat", () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.craftingdead.combat"))
          .icon(ModItems::createCombatTabIcon)
          .displayItems((parameters, output) -> contributeCombatTab(output))
          .build());

  public static final Supplier<CreativeModeTab> MEDICAL_TAB = tabDeferredRegister.register(
      "medical", () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.craftingdead.medical"))
          .icon(ModItems::createMedicalTabIcon)
          .displayItems((parameters, output) -> contributeMedicalTab(output))
          .build());

  private static ItemStack createCosmeticsTabIcon() {
    return new ItemStack(ARMY_MEDIC_CLOTHING.get());
  }

  private static ItemStack createCombatTabIcon() {
    return new ItemStack(M4A1.get());
  }

  private static ItemStack createMedicalTabIcon() {
    return new ItemStack(FIRST_AID_KIT.get());
  }

  private static void contributeCombatTab(CreativeModeTab.Output output) {
    // Paints
    output.accept(VULCAN_PAINT.get());
    output.accept(ASMO_PAINT.get());
    output.accept(CANDY_APPLE_PAINT.get());
    output.accept(CYREX_PAINT.get());
    output.accept(DIAMOND_PAINT.get());
    output.accept(DRAGON_PAINT.get());
    output.accept(FADE_PAINT.get());
    output.accept(FURY_PAINT.get());
    output.accept(GEM_PAINT.get());
    output.accept(INFERNO_PAINT.get());
    output.accept(RUBY_PAINT.get());
    output.accept(SCORCHED_PAINT.get());
    output.accept(SLAUGHTER_PAINT.get());
    output.accept(UV_PAINT.get());
    output.accept(HYPER_BEAST_PAINT.get());
    output.accept(EMPEROR_DRAGON_PAINT.get());
    output.accept(NUCLEAR_WINTER_PAINT.get());
    output.accept(MONARCH_PAINT.get());
    output.accept(LOVELACE_PAINT.get());
    // Magazines
    output.accept(FLAMETHROWER_FUEL.get());
    output.accept(CROSSBOW_BOLT.get());
    output.accept(STANAG_BOX_MAGAZINE.get());
    output.accept(STANAG_DRUM_MAGAZINE.get());
    output.accept(STANAG_30_ROUND_MAGAZINE.get());
    output.accept(STANAG_20_ROUND_MAGAZINE.get());
    output.accept(MPT55_MAGAZINE.get());
    output.accept(AK47_30_ROUND_MAGAZINE.get());
    output.accept(FNFAL_MAGAZINE.get());
    output.accept(ACR_MAGAZINE.get());
    output.accept(G36C_MAGAZINE.get());
    output.accept(HK417_MAGAZINE.get());
    output.accept(M1911_MAGAZINE.get());
    output.accept(G18_MAGAZINE.get());
    output.accept(M9_MAGAZINE.get());
    output.accept(DESERT_EAGLE_MAGAZINE.get());
    output.accept(P250_MAGAZINE.get());
    output.accept(MAGNUM_AMMUNITION.get());
    output.accept(FN57_MAGAZINE.get());
    output.accept(P90_MAGAZINE.get());
    output.accept(VECTOR_MAGAZINE.get());
    output.accept(MP5A5_35_ROUND_MAGAZINE.get());
    output.accept(MP5A5_21_ROUND_MAGAZINE.get());
    output.accept(MAC10_EXTENDED_MAGAZINE.get());
    output.accept(MAC10_MAGAZINE.get());
    output.accept(SPORTER22_MAGAZINE.get());
    output.accept(M107_MAGAZINE.get());
    output.accept(AS50_MAGAZINE.get());
    output.accept(M1GARAND_AMMUNITION.get());
    output.accept(AWP_MAGAZINE.get());
    output.accept(TRENCH_GUN_SHELLS.get());
    output.accept(MOSSBERG_SHELLS.get());
    output.accept(DMR_MAGAZINE.get());
    output.accept(TASER_CARTRIDGE.get());
    output.accept(ROCKET.get());
    output.accept(M240B_MAGAZINE.get());
    output.accept(RPK_DRUM_MAGAZINE.get());
    output.accept(RPK_MAGAZINE.get());
    output.accept(MINIGUN_MAGAZINE.get());
    output.accept(MK48MOD_MAGAZINE.get());
    // Attachments
    output.accept(RED_DOT_SIGHT.get());
    output.accept(ACOG_SIGHT.get());
    output.accept(LP_SCOPE.get());
    output.accept(HP_SCOPE.get());
    output.accept(SUPPRESSOR.get());
    output.accept(TACTICAL_GRIP.get());
    output.accept(BIPOD.get());
    output.accept(EOTECH_SIGHT.get());
    // Assault Rifles
    output.accept(M4A1.get());
    output.accept(SCARL.get());
    output.accept(AK47.get());
    output.accept(QBZ97.get());
    output.accept(FNFAL.get());
    output.accept(ACR.get());
    output.accept(HK417.get());
    output.accept(MPT55.get());
    output.accept(M1GARAND.get());
    output.accept(SPORTER22.get());
    output.accept(G36C.get());
    // Machine Guns
    output.accept(M240B.get());
    output.accept(RPK.get());
    output.accept(MINIGUN.get());
    output.accept(MK48MOD.get());
    output.accept(RPG.get());
    output.accept(FLAMETHROWER.get());
    output.accept(CROSSBOW.get());
    // Pistols
    output.accept(TASER.get());
    output.accept(M1911.get());
    output.accept(G18.get());
    output.accept(M9.get());
    output.accept(DESERT_EAGLE.get());
    output.accept(P250.get());
    output.accept(MAGNUM.get());
    output.accept(FN57.get());
    // Submachine Guns
    output.accept(MAC10.get());
    output.accept(P90.get());
    output.accept(VECTOR.get());
    output.accept(MP5A5.get());
    // Sniper Rifles
    output.accept(M107.get());
    output.accept(AS50.get());
    output.accept(AWP.get());
    output.accept(DMR.get());
    // Shotguns
    output.accept(TRENCH_GUN.get());
    output.accept(MOSSBERG.get());
    // Grenades
    output.accept(FIRE_GRENADE.get());
    output.accept(SMOKE_GRENADE.get());
    output.accept(FLASH_GRENADE.get());
    output.accept(DECOY_GRENADE.get());
    output.accept(FRAG_GRENADE.get());
    output.accept(C4_EXPLOSIVE.get());
    output.accept(STICKY_C4_EXPLOSIVE.get());
    output.accept(REMOTE_DETONATOR.get());
    // Weapon
    output.accept(CROWBAR.get());
    output.accept(BAT.get());
    output.accept(KATANA.get());
    output.accept(PIPE.get());
    output.accept(RUSTY_PIPE.get());
    output.accept(FIRE_AXE.get());
    output.accept(CHAINSAW.get());
    output.accept(BOWIE_KNIFE.get());
    output.accept(GOLF_CLUB.get());
    output.accept(NIGHT_STICK.get());
    output.accept(SLEDGEHAMMER.get());
    output.accept(NAIL_BAT.get());
    output.accept(SHOVEL.get());
    output.accept(HATCHET.get());
    output.accept(BROADSWORD.get());
    output.accept(MACHETE.get());
    output.accept(WEAPONIZED_SCYTHE.get());
    output.accept(SCYTHE.get());
    output.accept(PICKAXE.get());
    output.accept(BO_STAFF.get());
    output.accept(WRENCH.get());
    output.accept(FRYING_PAN.get());
    output.accept(BOLT_CUTTERS.get());
    output.accept(COMBAT_KNIFE.get());
    output.accept(STEEL_BAT.get());
    output.accept(CLEAVER.get());
    output.accept(BROKEN_BOTTLE.get());
    // Gun Parts
    output.accept(SMALL_BARREL.get());
    output.accept(MEDIUM_BARREL.get());
    output.accept(HEAVY_BARREL.get());
    output.accept(SMALL_BODY.get());
    output.accept(MEDIUM_BODY.get());
    output.accept(HEAVY_BODY.get());
    output.accept(SMALL_HANDLE.get());
    output.accept(MEDIUM_HANDLE.get());
    output.accept(HEAVY_HANDLE.get());
    output.accept(SMALL_STOCK.get());
    output.accept(MEDIUM_STOCK.get());
    output.accept(MEDIUM_BOLT.get());
    output.accept(HEAVY_BOLT.get());
    // Miscellaneous
    output.accept(BINOCULARS.get());
    output.accept(PARACHUTE.get());
    output.accept(HANDCUFFS.get());
    output.accept(HANDCUFFS_KEY.get());
    output.accept(ELECTRONIC_SIGNAL.get());
    output.accept(CHALK.get());
  }

  private static void contributeCosmeticsTab(CreativeModeTab.Output output) {
    // Vests
    output.accept(BLACK_TACTICAL_VEST.get());
    output.accept(GHILLIE_TACTICAL_VEST.get());
    output.accept(GREEN_TACTICAL_VEST.get());
    output.accept(GREY_TACTICAL_VEST.get());
    output.accept(RIOT_VEST.get());
    output.accept(TAN_TACTICAL_VEST.get());
    // Hats, Helmets and Masks
    output.accept(ARMY_HELMET.get());
    output.accept(BEANIE_HAT.get());
    output.accept(BLACK_BALLISTIC_HAT.get());
    output.accept(FIREMAN_CHIEF_HAT.get());
    output.accept(BLUE_HARD_HAT.get());
    output.accept(BUNNY_HAT.get());
    output.accept(CAMO_HELMET.get());
    output.accept(CLONE_HAT.get());
    output.accept(COMBAT_BDU_HELMET.get());
    output.accept(COOKIE_MASK.get());
    output.accept(COW_MASK.get());
    output.accept(CREEPER_MASK.get());
    output.accept(DEADPOOL_MASK.get());
    output.accept(DOCTOR_MASK.get());
    output.accept(FIREMAN_HAT.get());
    output.accept(GAS_MASK.get());
    output.accept(GHILLIE_HAT.get());
    output.accept(GREEN_ARMY_HELMET.get());
    output.accept(GREEN_BALLISTIC_HELMET.get());
    output.accept(GREEN_HARD_HAT.get());
    output.accept(GREY_ARMY_HELMET.get());
    output.accept(HACKER_MASK.get());
    output.accept(HAZMAT_HAT.get());
    output.accept(JUGGERNAUT_HELMET.get());
    output.accept(KNIGHT_HAT.get());
    output.accept(MILITARY_HAZMAT_HAT.get());
    output.accept(NINJA_HAT.get());
    output.accept(NV_GOGGLES_HAT.get());
    output.accept(ORANGE_HARD_HAT.get());
    output.accept(PAYDAY_MASK.get());
    output.accept(PAYDAY2_MASK.get());
    output.accept(PILOT_HELMET.get());
    output.accept(PUMPKIN_MASK.get());
    output.accept(RADAR_CAP.get());
    output.accept(RIOT_HAT.get());
    output.accept(SANTA_HAT.get());
    output.accept(SCUBA_MASK.get());
    output.accept(SHEEP_MASK.get());
    output.accept(SKI_MASK.get());
    output.accept(SPETSNAZ_HELMET.get());
    output.accept(TOP_HAT.get());
    output.accept(TRAPPER_HAT.get());
    output.accept(USHANKA_HAT.get());
    output.accept(WINTER_MILITARY_HELMET.get());
    output.accept(YELLOW_HARD_HAT.get());
    output.accept(ZOMBIE_MASK.get());
    // Clothing
    output.accept(ARMY_CLOTHING.get());
    output.accept(SAS_CLOTHING.get());
    output.accept(SPETSNAZ_CLOTHING.get());
    output.accept(POLICE_CLOTHING.get());
    output.accept(CAMO_CLOTHING.get());
    output.accept(COMBAT_BDU_CLOTHING.get());
    output.accept(WINTER_ARMY_CLOTHING.get());
    output.accept(ARMY_DESERT_CLOTHING.get());
    output.accept(PILOT_CLOTHING.get());
    output.accept(HAZMAT_CLOTHING.get());
    output.accept(TAC_GHILLIE_CLOTHING.get());
    output.accept(SWAT_CLOTHING.get());
    output.accept(SPACE_SUIT_CLOTHING.get());
    output.accept(SHERIFF_CLOTHING.get());
    output.accept(JUGGERNAUT_CLOTHING.get());
    output.accept(FIREMAN_CLOTHING.get());
    output.accept(DOCTOR_CLOTHING.get());
    output.accept(SMART_CLOTHING.get());
    output.accept(CASUAL_GREEN_CLOTHING.get());
    output.accept(BUILDER_CLOTHING.get());
    output.accept(BUSINESS_CLOTHING.get());
    output.accept(SEC_GUARD_CLOTHING.get());
    output.accept(MIL_HAZMAT_CLOTHING.get());
    output.accept(FULL_GHILLIE_CLOTHING.get());
    output.accept(RED_DUSK_CLOTHING.get());
    output.accept(CLONE_CLOTHING.get());
    output.accept(COOKIE_CLOTHING.get());
    output.accept(DEADPOOL_CLOTHING.get());
    output.accept(NINJA_CLOTHING.get());
    output.accept(ARMY_MEDIC_CLOTHING.get());
    output.accept(BLUE_DUSK_CLOTHING.get());
    output.accept(PRESIDENT_CLOTHING.get());
    output.accept(YELLOW_DUSK_CLOTHING.get());
    output.accept(ORANGE_DUSK_CLOTHING.get());
    output.accept(GREEN_DUSK_CLOTHING.get());
    output.accept(WHITE_DUSK_CLOTHING.get());
    output.accept(PURPLE_DUSK_CLOTHING.get());
    output.accept(SCUBA_CLOTHING.get());
    output.accept(DDPAT_CLOTHING.get());
    output.accept(CONTRACTOR_CLOTHING.get());
    // Backpacks
    output.accept(SMALL_RED_BACKPACK.get());
    output.accept(SMALL_ORANGE_BACKPACK.get());
    output.accept(SMALL_YELLOW_BACKPACK.get());
    output.accept(SMALL_GREEN_BACKPACK.get());
    output.accept(SMALL_BLUE_BACKPACK.get());
    output.accept(SMALL_PURPLE_BACKPACK.get());
    output.accept(MEDIUM_RED_BACKPACK.get());
    output.accept(MEDIUM_ORANGE_BACKPACK.get());
    output.accept(MEDIUM_YELLOW_BACKPACK.get());
    output.accept(MEDIUM_GREEN_BACKPACK.get());
    output.accept(MEDIUM_BLUE_BACKPACK.get());
    output.accept(MEDIUM_PURPLE_BACKPACK.get());
    output.accept(MEDIUM_GREY_BACKPACK.get());
    output.accept(MEDIUM_BLACK_BACKPACK.get());
    output.accept(MEDIUM_GHILLIE_BACKPACK.get());
    output.accept(MEDIUM_WHITE_BACKPACK.get());
    output.accept(LARGE_GREY_BACKPACK.get());
    output.accept(LARGE_GREEN_BACKPACK.get());
    output.accept(LARGE_TAN_BACKPACK.get());
    output.accept(LARGE_BLACK_BACKPACK.get());
    output.accept(LARGE_GHILLIE_BACKPACK.get());
    output.accept(TAN_GUN_BAG.get());
    output.accept(GREY_GUN_BAG.get());
  }

  private static void contributeMedicalTab(CreativeModeTab.Output output) {
    // Medical
    output.accept(FIRST_AID_KIT.get());
    output.accept(DIRTY_RAG.get());
    output.accept(BLOODY_RAG.get());
    output.accept(CLEAN_RAG.get());
    output.accept(ADRENALINE_SYRINGE.get());
    output.accept(SYRINGE.get());
    output.accept(BLOOD_SYRINGE.get());
    output.accept(BANDAGE.get());
    output.accept(SurvivalItems.ITEMS.get("splint").get());
    output.accept(SurvivalItems.ITEMS.get("morphine_syringe").get());
    output.accept(SurvivalItems.ITEMS.get("cure_syringe").get());
    output.accept(SurvivalItems.ITEMS.get("rbi_syringe").get());
  }

  // ================================================================================
  // Paints
  // ================================================================================

  public static final Supplier<Item> VULCAN_PAINT =
      deferredRegister.register("vulcan_paint",
          () -> new PaintItem(Skins.VULCAN, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> ASMO_PAINT =
      deferredRegister.register("asmo_paint",
          () -> new PaintItem(Skins.ASMO, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> CANDY_APPLE_PAINT =
      deferredRegister.register("candy_apple_paint",
          () -> new PaintItem(Skins.CANDY_APPLE, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> CYREX_PAINT =
      deferredRegister.register("cyrex_paint",
          () -> new PaintItem(Skins.CYREX, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> DIAMOND_PAINT =
      deferredRegister.register("diamond_paint",
          () -> new PaintItem(Skins.DIAMOND, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> DRAGON_PAINT =
      deferredRegister.register("dragon_paint",
          () -> new PaintItem(Skins.DRAGON, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> FADE_PAINT =
      deferredRegister.register("fade_paint",
          () -> new PaintItem(Skins.FADE, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> FURY_PAINT =
      deferredRegister.register("fury_paint",
          () -> new PaintItem(Skins.FURY, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> GEM_PAINT =
      deferredRegister.register("gem_paint",
          () -> new PaintItem(Skins.GEM, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> INFERNO_PAINT =
      deferredRegister.register("inferno_paint",
          () -> new PaintItem(Skins.INFERNO, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> RUBY_PAINT =
      deferredRegister.register("ruby_paint",
          () -> new PaintItem(Skins.RUBY, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> SCORCHED_PAINT =
      deferredRegister.register("scorched_paint",
          () -> new PaintItem(Skins.SCORCHED, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> SLAUGHTER_PAINT =
      deferredRegister.register("slaughter_paint",
          () -> new PaintItem(Skins.SLAUGHTER, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> UV_PAINT =
      deferredRegister.register("uv_paint",
          () -> new PaintItem(Skins.UV, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> HYPER_BEAST_PAINT =
      deferredRegister.register("hyper_beast_paint",
          () -> new PaintItem(Skins.HYPER_BEAST, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> EMPEROR_DRAGON_PAINT =
      deferredRegister.register("emperor_dragon_paint",
          () -> new PaintItem(Skins.EMPEROR_DRAGON, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> NUCLEAR_WINTER_PAINT =
      deferredRegister.register("nuclear_winter_paint",
          () -> new PaintItem(Skins.NUCLEAR_WINTER, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> MONARCH_PAINT =
      deferredRegister.register("monarch_paint",
          () -> new PaintItem(Skins.MONARCH, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> LOVELACE_PAINT =
      deferredRegister.register("lovelace_paint",
          () -> new PaintItem(Skins.LOVELACE, new Item.Properties()
              .stacksTo(1)));
  // ================================================================================
  // Magazines
  // ================================================================================

  public static final Supplier<MagazineItem> STANAG_BOX_MAGAZINE =
      deferredRegister.register("stanag_box_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(85)
              .setArmorPenetration(0.4F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> STANAG_DRUM_MAGAZINE =
      deferredRegister.register("stanag_drum_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(45)
              .setArmorPenetration(0.4F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> STANAG_30_ROUND_MAGAZINE =
      deferredRegister.register("stanag_30_round_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(30)
              .setArmorPenetration(0.4F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> STANAG_20_ROUND_MAGAZINE =
      deferredRegister.register("stanag_20_round_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(20)
              .setArmorPenetration(0.4F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MPT55_MAGAZINE =
      deferredRegister.register("mpt55_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(30)
              .setArmorPenetration(0.4F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> AK47_30_ROUND_MAGAZINE =
      deferredRegister.register("ak47_30_round_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(30)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> FNFAL_MAGAZINE =
      deferredRegister.register("fnfal_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(20)
              .setArmorPenetration(0.55F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> ACR_MAGAZINE =
      deferredRegister.register("acr_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(20)
              .setArmorPenetration(0.5F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> G36C_MAGAZINE =
      deferredRegister.register("g36c_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(30)
              .setArmorPenetration(0.45F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> HK417_MAGAZINE =
      deferredRegister.register("hk417_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(30)
              .setArmorPenetration(0.47F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> M1911_MAGAZINE =
      deferredRegister.register("m1911_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(7)
              .setArmorPenetration(0.08F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> G18_MAGAZINE =
      deferredRegister.register("g18_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(20)
              .setArmorPenetration(0.08F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> M9_MAGAZINE =
      deferredRegister.register("m9_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(15)
              .setArmorPenetration(0.08F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> DESERT_EAGLE_MAGAZINE =
      deferredRegister.register("desert_eagle_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(13)
              .setArmorPenetration(0.35F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> P250_MAGAZINE =
      deferredRegister.register("p250_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(12)
              .setArmorPenetration(0.08F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MAGNUM_AMMUNITION =
      deferredRegister.register("magnum_ammunition",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(6)
              .setArmorPenetration(0.65F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> FN57_MAGAZINE =
      deferredRegister.register("fn57_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(20)
              .setArmorPenetration(0.09F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> P90_MAGAZINE =
      deferredRegister.register("p90_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(50)
              .setArmorPenetration(0.15F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> VECTOR_MAGAZINE =
      deferredRegister.register("vector_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(30)
              .setArmorPenetration(0.15F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MP5A5_35_ROUND_MAGAZINE =
      deferredRegister.register("mp5a5_35_round_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(35)
              .setArmorPenetration(0.15F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MP5A5_21_ROUND_MAGAZINE =
      deferredRegister.register("mp5a5_21_round_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(21)
              .setArmorPenetration(0.15F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MAC10_EXTENDED_MAGAZINE =
      deferredRegister.register("mac10_extended_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(45)
              .setArmorPenetration(0.15F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MAC10_MAGAZINE =
      deferredRegister.register("mac10_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(30)
              .setArmorPenetration(0.15F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> SPORTER22_MAGAZINE =
      deferredRegister.register("sporter22_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(30)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> M107_MAGAZINE =
      deferredRegister.register("m107_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(10)
              .setArmorPenetration(0.65F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> AS50_MAGAZINE =
      deferredRegister.register("as50_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(10)
              .setArmorPenetration(0.65F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> M1GARAND_AMMUNITION =
      deferredRegister.register("m1garand_ammunition",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(8)
              .setArmorPenetration(0.95F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> AWP_MAGAZINE =
      deferredRegister.register("awp_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(10)
              .setArmorPenetration(0.95F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> TRENCH_GUN_SHELLS =
      deferredRegister.register("trench_gun_shells",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(6)
              .setArmorPenetration(0.35F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MOSSBERG_SHELLS =
      deferredRegister.register("mossberg_shells",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(8)
              .setArmorPenetration(0.3F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> DMR_MAGAZINE =
      deferredRegister.register("dmr_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(5)
              .setArmorPenetration(0.65F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> TASER_CARTRIDGE =
      deferredRegister.register("taser_cartridge",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(3)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> ROCKET =
      deferredRegister.register("rocket",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(1)
              .setArmorPenetration(1.0F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> M240B_MAGAZINE =
      deferredRegister.register("m240b_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(150)
              .setArmorPenetration(0.5F)
              .stacksTo(1)));


  public static final Supplier<MagazineItem> RPK_DRUM_MAGAZINE =
      deferredRegister.register("rpk_drum_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(100)
              .setArmorPenetration(0.5F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> RPK_MAGAZINE =
      deferredRegister.register("rpk_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(60)
              .setArmorPenetration(0.5F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MINIGUN_MAGAZINE =
      deferredRegister.register("minigun_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(350)
              .setArmorPenetration(0.3F)
              .stacksTo(1)));

  public static final Supplier<MagazineItem> MK48MOD_MAGAZINE =
      deferredRegister.register("mk48mod_magazine",
          () -> new MagazineItem((MagazineItem.Properties) new MagazineItem.Properties()
              .setSize(150)
              .setArmorPenetration(0.52F)
              .stacksTo(1)));

  // ================================================================================
  // Attachments
  // ================================================================================

  public static final Supplier<AttachmentItem> RED_DOT_SIGHT =
      deferredRegister.register("red_dot_sight",
          () -> new AttachmentItem(Attachments.RED_DOT_SIGHT, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<AttachmentItem> ACOG_SIGHT =
      deferredRegister.register("acog_sight",
          () -> new AttachmentItem(Attachments.ACOG_SIGHT, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<AttachmentItem> LP_SCOPE =
      deferredRegister.register("lp_scope",
          () -> new AttachmentItem(Attachments.LP_SCOPE, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<AttachmentItem> HP_SCOPE =
      deferredRegister.register("hp_scope",
          () -> new AttachmentItem(Attachments.HP_SCOPE, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<AttachmentItem> SUPPRESSOR =
      deferredRegister.register("suppressor",
          () -> new AttachmentItem(Attachments.SUPPRESSOR, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<AttachmentItem> TACTICAL_GRIP =
      deferredRegister.register("tactical_grip",
          () -> new AttachmentItem(Attachments.TACTICAL_GRIP, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<AttachmentItem> BIPOD =
      deferredRegister.register("bipod",
          () -> new AttachmentItem(Attachments.BIPOD, new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<AttachmentItem> EOTECH_SIGHT =
      deferredRegister.register("eotech_sight",
          () -> new AttachmentItem(Attachments.EOTECH_SIGHT, new Item.Properties()
              .stacksTo(1)));

  // ================================================================================
  // Assault Rifles
  // ================================================================================

  public static final Supplier<GunItem> M4A1 =
      deferredRegister.register("m4a1",
          () -> AimableGunItem.builder(GunConfigurations.M4A1.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.STANAG_20_ROUND_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_30_ROUND_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_DRUM_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_BOX_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .build());

  public static final Supplier<GunItem> SCARL =
      deferredRegister.register("scarl",
          () -> AimableGunItem.builder(GunConfigurations.SCARL.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.STANAG_20_ROUND_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_30_ROUND_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_DRUM_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_BOX_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .build());

  public static final Supplier<GunItem> AK47 =
      deferredRegister.register("ak47",
          () -> AimableGunItem.builder(GunConfigurations.AK47.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.AK47_30_ROUND_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> QBZ97 =
      deferredRegister.register("qbz97_girls_frontline",
          () -> AimableGunItem.builder(GunConfigurations.QBZ97.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.STANAG_30_ROUND_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_20_ROUND_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_DRUM_MAGAZINE)
              .addAcceptedMagazine(ModItems.STANAG_BOX_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> FNFAL =
      deferredRegister.register("fnfal",
          () -> AimableGunItem.builder(GunConfigurations.FNFAL.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.FNFAL_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> ACR =
      deferredRegister.register("acr",
          () -> AimableGunItem.builder(GunConfigurations.ACR.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.ACR_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .build());

  public static final Supplier<GunItem> HK417 =
      deferredRegister.register("hk417",
          () -> AimableGunItem.builder(GunConfigurations.HK417.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.HK417_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .build());

  public static final Supplier<GunItem> MPT55 =
      deferredRegister.register("mpt55",
          () -> AimableGunItem.builder(GunConfigurations.MPT55.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.MPT55_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> M1GARAND =
      deferredRegister.register("m1garand",
          () -> AimableGunItem.builder(GunConfigurations.M1GARAND.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::autoSniper)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.M1GARAND_AMMUNITION)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.BIPOD)
              .build());

  public static final Supplier<GunItem> SPORTER22 =
      deferredRegister.register("sporter22",
          () -> AimableGunItem.builder(GunConfigurations.SPORTER22.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.SPORTER22_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> G36C =
      deferredRegister.register("g36c",
          () -> AimableGunItem.builder(GunConfigurations.G36C.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.G36C_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .build());

  // ================================================================================
  // Machine Guns
  // ================================================================================

  public static final Supplier<GunItem> M240B =
      deferredRegister.register("m240b",
          () -> AimableGunItem.builder(GunConfigurations.M240B.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::submachineGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.M240B_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .build());

  public static final Supplier<GunItem> RPK =
      deferredRegister.register("rpk",
          () -> AimableGunItem.builder(GunConfigurations.RPK.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::rifle)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.RPK_MAGAZINE)
              .addAcceptedMagazine(ModItems.RPK_DRUM_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .build());

  public static final Supplier<GunItem> MINIGUN =
      deferredRegister.register("minigun",
          () -> MinigunItem.builder(GunConfigurations.MINIGUN.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::submachineGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.MINIGUN_MAGAZINE)
              .setTriggerPredicate(Gun::isPerformingSecondaryAction)
              .build());

  public static final Supplier<GunItem> MK48MOD =
      deferredRegister.register("mk48mod",
          () -> AimableGunItem.builder(GunConfigurations.MK48MOD.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::submachineGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.MK48MOD_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .addAcceptedAttachment(Attachments.EOTECH_SIGHT)
              .build());

  public static final Supplier<GunItem> RPG =
      deferredRegister.register("rpg",
          () -> AimableGunItem.builder(GunConfigurations.RPG.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::autoSniper)
              .putReloadAnimation(RocketReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.ROCKET)
              .build());

  // ================================================================================
  // Pistols
  // ================================================================================

  public static final Supplier<GunItem> TASER =
      deferredRegister.register("taser",
          () -> AimableGunItem.builder(GunConfigurations.TASER.getKey())
              .setCombatSlot(CombatSlot.SECONDARY)
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::pistol)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .putReloadAnimation(ReloadAnimation::new)
              .setDefaultMagazine(ModItems.TASER_CARTRIDGE)
              .build());

  public static final Supplier<GunItem> M1911 =
      deferredRegister.register("m1911",
          () -> AimableGunItem.builder(GunConfigurations.M1911.getKey())
              .setCombatSlot(CombatSlot.SECONDARY)
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::pistol)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.M1911_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> G18 =
      deferredRegister.register("g18",
          () -> AimableGunItem.builder(GunConfigurations.G18.getKey())
              .setCombatSlot(CombatSlot.SECONDARY)
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::pistol)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.G18_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> M9 =
      deferredRegister.register("m9",
          () -> AimableGunItem.builder(GunConfigurations.M9.getKey())
              .setCombatSlot(CombatSlot.SECONDARY)
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::pistol)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.M9_MAGAZINE)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> DESERT_EAGLE =
      deferredRegister.register("desert_eagle",
          () -> AimableGunItem.builder(GunConfigurations.DESERT_EAGLE.getKey())
              .setCombatSlot(CombatSlot.SECONDARY)
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::pistol)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.DESERT_EAGLE_MAGAZINE)
              .build());

  public static final Supplier<GunItem> P250 =
      deferredRegister.register("p250",
          () -> AimableGunItem.builder(GunConfigurations.P250.getKey())
              .setCombatSlot(CombatSlot.SECONDARY)
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::pistol)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.P250_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> MAGNUM =
      deferredRegister.register("magnum",
          () -> AimableGunItem.builder(GunConfigurations.MAGNUM.getKey())
              .setCombatSlot(CombatSlot.SECONDARY)
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::pistol)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.MAGNUM_AMMUNITION)
              .build());

  public static final Supplier<GunItem> FN57 =
      deferredRegister.register("fn57",
          () -> AimableGunItem.builder(GunConfigurations.FN57.getKey())
              .setCombatSlot(CombatSlot.SECONDARY)
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::pistol)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.FN57_MAGAZINE)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  // ================================================================================
  // Submachine Guns
  // ================================================================================

  public static final Supplier<GunItem> MAC10 =
      deferredRegister.register("mac10",
          () -> AimableGunItem.builder(GunConfigurations.MAC10.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::submachineGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.MAC10_MAGAZINE)
              .addAcceptedMagazine(ModItems.MAC10_EXTENDED_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> P90 =
      deferredRegister.register("p90",
          () -> AimableGunItem.builder(GunConfigurations.P90.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::submachineGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.P90_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> VECTOR =
      deferredRegister.register("vector",
          () -> AimableGunItem.builder(GunConfigurations.VECTOR.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::submachineGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.VECTOR_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> MP5A5 =
      deferredRegister.register("mp5a5",
          () -> AimableGunItem.builder(GunConfigurations.MP5A5.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::submachineGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.MP5A5_21_ROUND_MAGAZINE)
              .addAcceptedMagazine(ModItems.MP5A5_35_ROUND_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  // ================================================================================
  // Sniper Rifles
  // ================================================================================

  public static final Supplier<GunItem> M107 =
      deferredRegister.register("m107",
          () -> AimableGunItem.builder(GunConfigurations.M107.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::autoSniper)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .putReloadAnimation(ReloadAnimation::new)
              .setDefaultMagazine(ModItems.M107_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .build());

  public static final Supplier<GunItem> AS50 =
      deferredRegister.register("as50",
          () -> AimableGunItem.builder(GunConfigurations.AS50.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::autoSniper)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.AS50_MAGAZINE)
              .addAcceptedAttachment(Attachments.RED_DOT_SIGHT)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.TACTICAL_GRIP)
              .addAcceptedAttachment(Attachments.BIPOD)
              .build());

  public static final Supplier<GunItem> AWP =
      deferredRegister.register("awp",
          () -> AimableGunItem.builder(GunConfigurations.AWP.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::boltActionSniper)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.AWP_MAGAZINE)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  public static final Supplier<GunItem> DMR =
      deferredRegister.register("dmr",
          () -> AimableGunItem.builder(GunConfigurations.DMR.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::autoSniper)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.DMR_MAGAZINE)
              .addAcceptedAttachment(Attachments.LP_SCOPE)
              .addAcceptedAttachment(Attachments.HP_SCOPE)
              .addAcceptedAttachment(Attachments.BIPOD)
              .addAcceptedAttachment(Attachments.ACOG_SIGHT)
              .addAcceptedAttachment(Attachments.SUPPRESSOR)
              .build());

  // ================================================================================
  // Shotguns
  // ================================================================================

  public static final Supplier<GunItem> TRENCH_GUN =
      deferredRegister.register("trench_gun",
          () -> AimableGunItem.builder(GunConfigurations.TRENCH_GUN.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::shotGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.TRENCH_GUN_SHELLS)
              .build());

  public static final Supplier<GunItem> MOSSBERG =
      deferredRegister.register("mossberg",
          () -> AimableGunItem.builder(GunConfigurations.MOSSBERG.getKey())
              .putAnimation(GunAnimationEvent.SHOOT, ShootAnimation::shotGun)
              .putReloadAnimation(ReloadAnimation::new)
              .putAnimation(GunAnimationEvent.INSPECT, InspectAnimation::new)
              .setDefaultMagazine(ModItems.MOSSBERG_SHELLS)
              .build());

  // ================================================================================
  // Grenades
  // ================================================================================

  public static final Supplier<GrenadeItem> FIRE_GRENADE =
      deferredRegister.register("fire_grenade",
          () -> new GrenadeItem((GrenadeItem.Properties) new GrenadeItem.Properties()
              .setGrenadeEntitySupplier(
                  FunctionalUtil.nullsafeFunction(FireGrenadeEntity::new, FireGrenadeEntity::new))
              .setEnabledSupplier(ServerConfig.instance.explosivesFireGrenadeEnabled::get)
              .stacksTo(1)));

  public static final Supplier<GrenadeItem> SMOKE_GRENADE =
      deferredRegister.register("smoke_grenade",
          () -> new GrenadeItem((GrenadeItem.Properties) new GrenadeItem.Properties()
              .setGrenadeEntitySupplier(
                  FunctionalUtil.nullsafeFunction(SmokeGrenadeEntity::new, SmokeGrenadeEntity::new))
              .setEnabledSupplier(ServerConfig.instance.explosivesSmokeGrenadeEnabled::get)
              .stacksTo(1)));

  public static final Supplier<GrenadeItem> FLASH_GRENADE =
      deferredRegister.register("flash_grenade",
          () -> new GrenadeItem((GrenadeItem.Properties) new GrenadeItem.Properties()
              .setGrenadeEntitySupplier(
                  FunctionalUtil.nullsafeFunction(FlashGrenadeEntity::new, FlashGrenadeEntity::new))
              .setEnabledSupplier(ServerConfig.instance.explosivesFlashGrenadeEnabled::get)
              .stacksTo(1)));

  public static final Supplier<GrenadeItem> DECOY_GRENADE =
      deferredRegister.register("decoy_grenade",
          () -> new GrenadeItem((GrenadeItem.Properties) new GrenadeItem.Properties()
              .setGrenadeEntitySupplier(
                  FunctionalUtil.nullsafeFunction(DecoyGrenadeEntity::new, DecoyGrenadeEntity::new))
              .setEnabledSupplier(ServerConfig.instance.explosivesDecoyGrenadeEnabled::get)
              .stacksTo(1)));

  public static final Supplier<GrenadeItem> FRAG_GRENADE =
      deferredRegister.register("frag_grenade",
          () -> new GrenadeItem((GrenadeItem.Properties) new GrenadeItem.Properties()
              .setGrenadeEntitySupplier(
                  FunctionalUtil.nullsafeFunction(FragGrenade::new, FragGrenade::new))
              .setEnabledSupplier(ServerConfig.instance.explosivesFragGrenadeEnabled::get)
              .stacksTo(1)));

  public static final Supplier<GrenadeItem> C4_EXPLOSIVE =
      deferredRegister.register("c4_explosive",
          () -> new GrenadeItem((GrenadeItem.Properties) new GrenadeItem.Properties()
              .setGrenadeEntitySupplier(
                  FunctionalUtil.nullsafeFunction(C4Explosive::new, C4Explosive::new))
              .setEnabledSupplier(ServerConfig.instance.explosivesC4Enabled::get)
              .setThrowSpeed(0.75F)
              .stacksTo(1)));

  public static final Supplier<GrenadeItem> STICKY_C4_EXPLOSIVE =
      deferredRegister.register("sticky_c4_explosive",
          () -> new GrenadeItem((GrenadeItem.Properties) new GrenadeItem.Properties()
              .setGrenadeEntitySupplier(
                  FunctionalUtil.nullsafeFunction(C4Explosive::new, C4Explosive::new))
              .setEnabledSupplier(ServerConfig.instance.explosivesC4Enabled::get)
              .setSticky(true)
              .setThrowSpeed(0.75F)
              .stacksTo(1)));

  public static final Supplier<Item> REMOTE_DETONATOR =
      deferredRegister.register("remote_detonator",
          () -> new RemoteDetonatorItem(new Item.Properties()
              .stacksTo(1)));

  // ================================================================================
  // Weapon
  // ================================================================================

  public static final Supplier<Item> CHALK = deferredRegister.register("chalk",
      () -> new ChalkItem(new Item.Properties()));
  public static final Supplier<Item> CROSSBOW = deferredRegister.register("crossbow",
      () -> new SurvivalCrossbowItem(new Item.Properties()));
  public static final Supplier<Item> CROSSBOW_BOLT = deferredRegister.register("crossbow_bolt",
      () -> new CrossbowBoltItem(new Item.Properties()));
  public static final Supplier<Item> FLAMETHROWER = deferredRegister.register("flamethrower",
      () -> new FlamethrowerItem(new Item.Properties()));
  public static final Supplier<Item> FLAMETHROWER_FUEL = deferredRegister.register("flamethrower_fuel",
      () -> new Item(new Item.Properties().durability(200)));

  public static final Supplier<Item> CROWBAR = deferredRegister.register("crowbar",
      () -> new MeleeWeaponItem(6, -2.4F, new Item.Properties()
          .durability(100)));

  public static final Supplier<Item> BAT = deferredRegister.register("bat",
      () -> new MeleeWeaponItem(5, -2.4F, new Item.Properties()
          .durability(55)));

  public static final Supplier<Item> KATANA = deferredRegister.register("katana",
      () -> new MeleeWeaponItem(10, -2.4F, new Item.Properties()
          .durability(40)));

  public static final Supplier<Item> PIPE = deferredRegister.register("pipe",
      () -> new MeleeWeaponItem(7, -2.4F, new Item.Properties()
          .durability(60)));

  public static final Supplier<Item> RUSTY_PIPE = deferredRegister.register("rusty_pipe",
      () -> new MeleeWeaponItem(6, -2.4F, new Item.Properties()
          .durability(20)));

  public static final Supplier<Item> FIRE_AXE = deferredRegister.register("fire_axe",
      () -> new ModAxeItem(Tiers.IRON, 1.0F, 9, -2.4F, new Item.Properties()
          .durability(100)));

  public static final Supplier<Item> CHAINSAW = deferredRegister.register("chainsaw",
      () -> new ModAxeItem(Tiers.IRON, 2.0F,8, -2.4F, new Item.Properties()
          .durability(75)));

  public static final Supplier<Item> BOWIE_KNIFE = deferredRegister.register("bowie_knife",
      () -> new MeleeWeaponItem(7, -2.4F, new Item.Properties()
          .durability(20)));

  public static final Supplier<Item> GOLF_CLUB = deferredRegister.register("golf_club",
      () -> new MeleeWeaponItem(6, -2.4F, new Item.Properties()
          .durability(40)));

  public static final Supplier<Item> NIGHT_STICK = deferredRegister.register("night_stick",
      () -> new MeleeWeaponItem(4, -2.4F, new Item.Properties()
          .durability(70)));

  public static final Supplier<Item> SLEDGEHAMMER = deferredRegister.register("sledgehammer",
      () -> new ModPickaxeItem(Tiers.IRON, 0.55F, 9, -2.4F,
          new Item.Properties()
              .durability(110)));

  public static final Supplier<Item> NAIL_BAT = deferredRegister.register("nail_bat",
      () -> new MeleeWeaponItem(8, -2.4F, new Item.Properties()
          .durability(55)));

  public static final Supplier<Item> SHOVEL = deferredRegister.register("shovel",
      () -> new ModShovelItem(Tiers.IRON, 1.0F, 8, -2.4F, new Item.Properties()
          .durability(70)));

  public static final Supplier<Item> HATCHET = deferredRegister.register("hatchet",
      () -> new ModAxeItem(Tiers.IRON, 1.4F, 8, -2.4F, new Item.Properties()
          .durability(40)));

  public static final Supplier<Item> BROADSWORD = deferredRegister.register("broadsword",
      () -> new MeleeWeaponItem(9, -2.4F, new Item.Properties()
          .durability(55)));

  public static final Supplier<Item> MACHETE = deferredRegister.register("machete",
      () -> new MeleeWeaponItem(8, -2.4F, new Item.Properties()
          .durability(70)));

  public static final Supplier<Item> WEAPONIZED_SCYTHE =
      deferredRegister.register("weaponized_scythe",
          () -> new MeleeWeaponItem(19, -2.4F, new Item.Properties()
              .durability(20)));

  public static final Supplier<Item> SCYTHE = deferredRegister.register("scythe",
      () -> new MeleeWeaponItem(15, -2.4F, new Item.Properties()
          .durability(15)));

  public static final Supplier<Item> PICKAXE = deferredRegister.register("pickaxe",
      () -> new ModPickaxeItem(Tiers.IRON, 0.8F, 7, -2.4F, new Item.Properties()
          .durability(210)));

  public static final Supplier<Item> BO_STAFF = deferredRegister.register("bo_staff",
      () -> new MeleeWeaponItem(4, -2.4F, new Item.Properties()
          .durability(70)));

  public static final Supplier<Item> WRENCH = deferredRegister.register("wrench",
      () -> new MeleeWeaponItem(5, -2.4F, new Item.Properties()
          .durability(120)));

  public static final Supplier<Item> SCRAP_METAL = deferredRegister.register("scrap_metal",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> FRYING_PAN = deferredRegister.register("frying_pan",
      () -> new MeleeWeaponItem(6, -2.4F, new Item.Properties()
          .durability(80)));

  public static final Supplier<Item> BOLT_CUTTERS = deferredRegister.register("bolt_cutters",
      () -> new BoltCuttersItem(40, 6, -2.4F, new Item.Properties()
          .durability(50)));

  public static final Supplier<Item> COMBAT_KNIFE = deferredRegister.register("combat_knife",
      () -> new MeleeWeaponItem(8, -2.4F, new Item.Properties()
          .durability(100)));

  public static final Supplier<Item> STEEL_BAT = deferredRegister.register("steel_bat",
      () -> new MeleeWeaponItem(7, -2.4F, new Item.Properties()
          .durability(180)));

  public static final Supplier<Item> CLEAVER = deferredRegister.register("cleaver",
      () -> new MeleeWeaponItem(7, -2.4F, new Item.Properties()
          .durability(80)));

  public static final Supplier<Item> BROKEN_BOTTLE =
      deferredRegister.register("broken_bottle",
          () -> new MeleeWeaponItem(5, -2.4F, new Item.Properties()
              .durability(10)));

  // ================================================================================
  // Vests
  // ================================================================================

  public static final Supplier<Item> BLACK_TACTICAL_VEST =
      deferredRegister.register("black_tactical_vest", ModItems::weakVest);

  public static final Supplier<Item> GHILLIE_TACTICAL_VEST =
      deferredRegister.register("ghillie_tactical_vest", ModItems::weakVest);

  public static final Supplier<Item> GREEN_TACTICAL_VEST =
      deferredRegister.register("green_tactical_vest", ModItems::weakVest);

  public static final Supplier<Item> GREY_TACTICAL_VEST =
      deferredRegister.register("grey_tactical_vest", ModItems::weakVest);

  public static final Supplier<Item> RIOT_VEST =
      deferredRegister.register("riot_vest", ModItems::strongVest);

  public static final Supplier<Item> TAN_TACTICAL_VEST =
      deferredRegister.register("tan_tactical_vest", ModItems::weakVest);

  // ================================================================================
  // Hats, Helmets and Masks
  // ================================================================================

  public static final Supplier<Item> ARMY_HELMET = deferredRegister.register("army_helmet",
      () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
          .setHeadshotReductionPercentage(0.3F)
          .stacksTo(1)));

  public static final Supplier<Item> BEANIE_HAT = deferredRegister.register("beanie_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> BLACK_BALLISTIC_HAT =
      deferredRegister.register("black_ballistic_hat",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
              .setHeadshotReductionPercentage(0.4F)
              .stacksTo(1)));

  public static final Supplier<Item> FIREMAN_CHIEF_HAT =
      deferredRegister.register("chief_fireman_hat",
          () -> new HatItem((HatItem.Properties) new HatItem.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> BLUE_HARD_HAT =
      deferredRegister.register("blue_hard_hat",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 0.0F)
              .setHeadshotReductionPercentage(0.2F)
              .stacksTo(1)));

  public static final Supplier<Item> BUNNY_HAT = deferredRegister.register("bunny_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .setNightVision(true)
          .stacksTo(1)));

  public static final Supplier<Item> CAMO_HELMET = deferredRegister.register("camo_helmet",
      () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
          .setHeadshotReductionPercentage(0.3F)
          .stacksTo(1)));

  public static final Supplier<Item> CLONE_HAT = deferredRegister.register("clone_hat",
      () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
          .setHeadshotReductionPercentage(0.2F)
          .stacksTo(1)));

  public static final Supplier<Item> COMBAT_BDU_HELMET =
      deferredRegister.register("combat_bdu_helmet",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
              .setHeadshotReductionPercentage(0.3F)
              .stacksTo(1)));

  public static final Supplier<Item> COOKIE_MASK = deferredRegister.register("cookie_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> COW_MASK = deferredRegister.register("cow_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> CREEPER_MASK = deferredRegister.register("creeper_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> DEADPOOL_MASK =
      deferredRegister.register("deadpool_mask",
          () -> new HatItem((HatItem.Properties) new HatItem.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> DOCTOR_MASK = deferredRegister.register("doctor_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> FIREMAN_HAT = deferredRegister.register("fireman_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> GAS_MASK = deferredRegister.register("gas_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .setImmuneToFlashes(true)
          .setImmuneToGas(true)
          .stacksTo(1)));

  public static final Supplier<Item> GHILLIE_HAT = deferredRegister.register("ghillie_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> GREEN_ARMY_HELMET =
      deferredRegister.register("green_army_helmet",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
              .setHeadshotReductionPercentage(0.3F)
              .stacksTo(1)));

  public static final Supplier<Item> GREEN_BALLISTIC_HELMET =
      deferredRegister.register("green_ballistic_helmet",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
              .setHeadshotReductionPercentage(0.4F)
              .stacksTo(1)));

  public static final Supplier<Item> GREEN_HARD_HAT =
      deferredRegister.register("green_hard_hat",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 0.0F)
              .setHeadshotReductionPercentage(0.2F)
              .stacksTo(1)));

  public static final Supplier<Item> GREY_ARMY_HELMET =
      deferredRegister.register("grey_army_helmet",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
              .setHeadshotReductionPercentage(0.3F)
              .stacksTo(1)));

  public static final Supplier<Item> HACKER_MASK = deferredRegister.register("hacker_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> HAZMAT_HAT = deferredRegister.register("hazmat_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .setImmuneToFlashes(true)
          .stacksTo(1)));

  public static final Supplier<Item> JUGGERNAUT_HELMET =
      deferredRegister.register("juggernaut_helmet",
          () -> new HatItem((HatItem.Properties) new HatItem.Properties()
              .setHeadshotReductionPercentage(0.2F)
              .stacksTo(1)));

  public static final Supplier<Item> KNIGHT_HAT = deferredRegister.register("knight_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> MILITARY_HAZMAT_HAT =
      deferredRegister.register("military_hazmat_hat",
          () -> new HatItem((HatItem.Properties) new HatItem.Properties()
              .setImmuneToFlashes(true)
              .stacksTo(1)));

  public static final Supplier<Item> NINJA_HAT = deferredRegister.register("ninja_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> NV_GOGGLES_HAT =
      deferredRegister.register("nv_goggles_hat",
          () -> new HatItem((HatItem.Properties) new HatItem.Properties()
              .setNightVision(true)
              .stacksTo(1)));

  public static final Supplier<Item> ORANGE_HARD_HAT =
      deferredRegister.register("orange_hard_hat",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 0.0F)
              .setHeadshotReductionPercentage(0.2F)
              .stacksTo(1)));

  public static final Supplier<Item> PAYDAY_MASK = deferredRegister.register("payday_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> PAYDAY2_MASK = deferredRegister.register("payday2_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> PILOT_HELMET = deferredRegister.register("pilot_helmet",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> PUMPKIN_MASK = deferredRegister.register("pumpkin_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> RADAR_CAP = deferredRegister.register("radar_cap",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> RIOT_HAT = deferredRegister.register("riot_hat",
      () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
          .setHeadshotReductionPercentage(0.25F)
          .stacksTo(1)));

  public static final Supplier<Item> SANTA_HAT = deferredRegister.register("santa_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> SCUBA_MASK = deferredRegister.register("scuba_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .waterBreathing(true)
          .stacksTo(1)));

  public static final Supplier<Item> SHEEP_MASK = deferredRegister.register("sheep_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> SKI_MASK = deferredRegister.register("ski_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .setImmuneToFlashes(true)
          .stacksTo(1)));

  public static final Supplier<Item> SPETSNAZ_HELMET =
      deferredRegister.register("spetsnaz_helmet",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
              .setHeadshotReductionPercentage(0.4F)
              .stacksTo(1)));

  public static final Supplier<Item> TOP_HAT = deferredRegister.register("top_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> TRAPPER_HAT = deferredRegister.register("trapper_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> USHANKA_HAT = deferredRegister.register("ushanka_hat",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  public static final Supplier<Item> WINTER_MILITARY_HELMET =
      deferredRegister.register("winter_military_helmet",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 1.0F)
              .setHeadshotReductionPercentage(0.3F)
              .stacksTo(1)));

  public static final Supplier<Item> YELLOW_HARD_HAT =
      deferredRegister.register("yellow_hard_hat",
          () -> new HatItem((HatItem.Properties) hatArmor(2.0F, 0.0F)
              .setHeadshotReductionPercentage(0.2F)
              .stacksTo(1)));

  public static final Supplier<Item> ZOMBIE_MASK = deferredRegister.register("zombie_mask",
      () -> new HatItem((HatItem.Properties) new HatItem.Properties()
          .stacksTo(1)));

  // ================================================================================
  // Clothing
  // ================================================================================

  public static final Supplier<Item> ARMY_CLOTHING =
      deferredRegister.register("army_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> SAS_CLOTHING =
      deferredRegister.register("sas_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> SPETSNAZ_CLOTHING =
      deferredRegister.register("spetsnaz_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> POLICE_CLOTHING =
      deferredRegister.register("police_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> CAMO_CLOTHING =
      deferredRegister.register("camo_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> COMBAT_BDU_CLOTHING =
      deferredRegister.register("combat_bdu_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> WINTER_ARMY_CLOTHING =
      deferredRegister.register("winter_army_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> ARMY_DESERT_CLOTHING =
      deferredRegister.register("army_desert_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> PILOT_CLOTHING =
      deferredRegister.register("pilot_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> HAZMAT_CLOTHING =
      deferredRegister.register("hazmat_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .fireImmunity()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> TAC_GHILLIE_CLOTHING =
      deferredRegister.register("tac_ghillie_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> SWAT_CLOTHING =
      deferredRegister.register("swat_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> SPACE_SUIT_CLOTHING =
      deferredRegister.register("space_suit_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> SHERIFF_CLOTHING =
      deferredRegister.register("sheriff_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> JUGGERNAUT_CLOTHING =
      deferredRegister.register("juggernaut_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .fireImmunity()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.HEAVY));

  public static final Supplier<Item> FIREMAN_CLOTHING =
      deferredRegister.register("fireman_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .fireImmunity()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> DOCTOR_CLOTHING =
      deferredRegister.register("doctor_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> SMART_CLOTHING =
      deferredRegister.register("smart_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.CASUAL));

  public static final Supplier<Item> CASUAL_GREEN_CLOTHING =
      deferredRegister.register("casual_green_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.CASUAL));

  public static final Supplier<Item> BUILDER_CLOTHING =
      deferredRegister.register("builder_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> BUSINESS_CLOTHING =
      deferredRegister.register("business_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.CASUAL));

  public static final Supplier<Item> SEC_GUARD_CLOTHING =
      deferredRegister.register("sec_guard_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> MIL_HAZMAT_CLOTHING =
      deferredRegister.register("mil_hazmat_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .fireImmunity()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> FULL_GHILLIE_CLOTHING =
      deferredRegister.register("full_ghillie_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> RED_DUSK_CLOTHING =
      deferredRegister.register("red_dusk_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> CLONE_CLOTHING =
      deferredRegister.register("clone_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> COOKIE_CLOTHING =
      deferredRegister.register("cookie_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.CASUAL));

  public static final Supplier<Item> DEADPOOL_CLOTHING =
      deferredRegister.register("deadpool_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.CASUAL));

  public static final Supplier<Item> NINJA_CLOTHING =
      deferredRegister.register("ninja_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.CASUAL));

  public static final Supplier<Item> ARMY_MEDIC_CLOTHING =
      deferredRegister.register("army_medic_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> BLUE_DUSK_CLOTHING =
      deferredRegister.register("blue_dusk_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> PRESIDENT_CLOTHING =
      deferredRegister.register("president_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> YELLOW_DUSK_CLOTHING =
      deferredRegister.register("yellow_dusk_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> ORANGE_DUSK_CLOTHING =
      deferredRegister.register("orange_dusk_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> GREEN_DUSK_CLOTHING =
      deferredRegister.register("green_dusk_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> WHITE_DUSK_CLOTHING =
      deferredRegister.register("white_dusk_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> PURPLE_DUSK_CLOTHING =
      deferredRegister.register("purple_dusk_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> SCUBA_CLOTHING =
      deferredRegister.register("scuba_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .enhancesSwimming()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  public static final Supplier<Item> DDPAT_CLOTHING =
      deferredRegister.register("ddpat_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.MILITARY));

  public static final Supplier<Item> CONTRACTOR_CLOTHING =
      deferredRegister.register("contractor_clothing",
          () -> new ClothingItem((ClothingItem.Properties) new ClothingItem.Properties()
              .stacksTo(1),
              ActionTypes.SHRED_CLOTHING,
              ClothingType.UTILITY));

  // ================================================================================
  // Gun Parts
  // ================================================================================

  public static final Supplier<Item> SMALL_BARREL = deferredRegister.register("small_barrel",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> MEDIUM_BARREL =
      deferredRegister.register("medium_barrel",
          () -> new Item(new Item.Properties()));

  public static final Supplier<Item> HEAVY_BARREL = deferredRegister.register("heavy_barrel",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> SMALL_BODY = deferredRegister.register("small_body",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> MEDIUM_BODY = deferredRegister.register("medium_body",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> HEAVY_BODY = deferredRegister.register("heavy_body",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> SMALL_HANDLE = deferredRegister.register("small_handle",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> MEDIUM_HANDLE =
      deferredRegister.register("medium_handle",
          () -> new Item(new Item.Properties()));

  public static final Supplier<Item> HEAVY_HANDLE = deferredRegister.register("heavy_handle",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> SMALL_STOCK = deferredRegister.register("small_stock",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> MEDIUM_STOCK = deferredRegister.register("medium_stock",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> MEDIUM_BOLT = deferredRegister.register("medium_bolt",
      () -> new Item(new Item.Properties()));

  public static final Supplier<Item> HEAVY_BOLT = deferredRegister.register("heavy_bolt",
      () -> new Item(new Item.Properties()));

  // ================================================================================
  // Miscellaneous
  // ================================================================================

  public static final Supplier<Item> BINOCULARS =
      deferredRegister.register("binoculars",
          () -> new BinocularsItem(new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> PARACHUTE =
      deferredRegister.register("parachute",
          () -> new ParachuteItem(new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<HandcuffsItem> HANDCUFFS =
      deferredRegister.register("handcuffs",
          () -> new HandcuffsItem(ActionTypes.APPLY_HANDCUFFS, new Item.Properties()
              .durability(200)));

  public static final Supplier<Item> HANDCUFFS_KEY =
      deferredRegister.register("handcuffs_key",
          () -> new HandcuffsKeyItem(new Item.Properties()
              .stacksTo(1)));

  public static final Supplier<Item> ELECTRONIC_SIGNAL =
      deferredRegister.register("electronic_signal",
          () -> new Item(new Item.Properties()
              .stacksTo(1)));

    public static void contributeProtectionMappings(ProtectionConfig.Builder builder) {
        builder.helmet(BLUE_HARD_HAT, "hard_hat");
        builder.helmet(GREEN_HARD_HAT, "hard_hat");
        builder.helmet(ORANGE_HARD_HAT, "hard_hat");
        builder.helmet(YELLOW_HARD_HAT, "hard_hat");

        builder.helmet(ARMY_HELMET, "military_helmet");
        builder.helmet(GREEN_ARMY_HELMET, "military_helmet");
        builder.helmet(CAMO_HELMET, "military_helmet");
        builder.helmet(COMBAT_BDU_HELMET, "military_helmet");
        builder.helmet(GREY_ARMY_HELMET, "military_helmet");
        builder.helmet(WINTER_MILITARY_HELMET, "military_helmet");

        builder.helmet(SPETSNAZ_HELMET, "ballistic_helmet");
        builder.helmet(BLACK_BALLISTIC_HAT, "ballistic_helmet");
        builder.helmet(GREEN_BALLISTIC_HELMET, "ballistic_helmet");

        builder.helmet(RIOT_HAT, "riot_helmet");
        builder.helmet(JUGGERNAUT_HELMET, "juggernaut_helmet");

        builder.vest(BLACK_TACTICAL_VEST, "tactical_vest");
        builder.vest(GREEN_TACTICAL_VEST, "tactical_vest");
        builder.vest(GREY_TACTICAL_VEST, "tactical_vest");
        builder.vest(TAN_TACTICAL_VEST, "tactical_vest");
        builder.vest(GHILLIE_TACTICAL_VEST, "tactical_vest");
        builder.vest(RIOT_VEST, "riot_vest");

        builder.cosmeticHelmet(BEANIE_HAT);
        builder.cosmeticHelmet(GAS_MASK);
        builder.cosmeticHelmet(PILOT_HELMET);
        builder.cosmeticHelmet(PAYDAY_MASK);
        builder.cosmeticHelmet(PAYDAY2_MASK);
        builder.cosmeticHelmet(SANTA_HAT);
        builder.cosmeticHelmet(HAZMAT_HAT);
        builder.cosmeticHelmet(MILITARY_HAZMAT_HAT);
        builder.cosmeticHelmet(NV_GOGGLES_HAT);
    }

  // ================================================================================
  // Medical
  // ================================================================================

  public static final Supplier<ActionItem> FIRST_AID_KIT =
      deferredRegister.register("first_aid_kit",
          () -> new ActionItem(ActionTypes.USE_FIRST_AID_KIT, new Item.Properties()
              .stacksTo(3)));

  public static final Supplier<Item> DIRTY_RAG =
      deferredRegister.register("dirty_rag",
      () -> new ActionItem(ActionTypes.WASH_RAG, new Item.Properties()
          .stacksTo(3)));

  public static final Supplier<Item> BLOODY_RAG =
      deferredRegister.register("bloody_rag",
      () -> new ActionItem(ActionTypes.WASH_RAG, new Item.Properties()
          .stacksTo(3)));

  public static final Supplier<Item> CLEAN_RAG =
      deferredRegister.register("clean_rag",
      () -> new ActionItem(ActionTypes.USE_CLEAN_RAG, new Item.Properties()
          .stacksTo(3)));

  public static final Supplier<ActionItem> ADRENALINE_SYRINGE =
      deferredRegister.register("adrenaline_syringe",
          () -> new ActionItem(ActionTypes.USE_ADRENALINE_SYRINGE, new Item.Properties()
              .stacksTo(3)));

  public static final Supplier<ActionItem> SYRINGE =
      deferredRegister.register("syringe",
          () -> new ActionItem(ActionTypes.USE_SYRINGE, new Item.Properties()
              .stacksTo(3)));

  public static final Supplier<ActionItem> BLOOD_SYRINGE =
      deferredRegister.register("blood_syringe",
          () -> new ActionItem(ActionTypes.USE_BLOOD_SYRINGE, new Item.Properties()
              .stacksTo(3)));

  public static final Supplier<ActionItem> BANDAGE =
      deferredRegister.register("bandage",
          () -> new ActionItem(ActionTypes.USE_BANDAGE, new Item.Properties()
              .stacksTo(3)));

  // ================================================================================
  // Backpacks
  // ================================================================================

  public static final Supplier<Item> SMALL_RED_BACKPACK = deferredRegister
      .register("small_red_backpack", ModItems::smallBackpack);

  public static final Supplier<Item> SMALL_ORANGE_BACKPACK = deferredRegister
      .register("small_orange_backpack", ModItems::smallBackpack);

  public static final Supplier<Item> SMALL_YELLOW_BACKPACK = deferredRegister
      .register("small_yellow_backpack", ModItems::smallBackpack);

  public static final Supplier<Item> SMALL_GREEN_BACKPACK = deferredRegister
      .register("small_green_backpack", ModItems::smallBackpack);

  public static final Supplier<Item> SMALL_BLUE_BACKPACK = deferredRegister
      .register("small_blue_backpack", ModItems::smallBackpack);

  public static final Supplier<Item> SMALL_PURPLE_BACKPACK = deferredRegister
      .register("small_purple_backpack", ModItems::smallBackpack);

  public static final Supplier<Item> MEDIUM_RED_BACKPACK = deferredRegister
      .register("medium_red_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_ORANGE_BACKPACK = deferredRegister
      .register("medium_orange_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_YELLOW_BACKPACK = deferredRegister
      .register("medium_yellow_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_GREEN_BACKPACK = deferredRegister
      .register("medium_green_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_BLUE_BACKPACK = deferredRegister
      .register("medium_blue_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_PURPLE_BACKPACK = deferredRegister
      .register("medium_purple_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_GREY_BACKPACK = deferredRegister
      .register("medium_grey_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_BLACK_BACKPACK = deferredRegister
      .register("medium_black_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_GHILLIE_BACKPACK = deferredRegister
      .register("medium_ghillie_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> MEDIUM_WHITE_BACKPACK = deferredRegister
      .register("medium_white_backpack", ModItems::mediumBackpack);

  public static final Supplier<Item> LARGE_GREY_BACKPACK = deferredRegister
      .register("large_grey_backpack", ModItems::largeBackpack);

  public static final Supplier<Item> LARGE_GREEN_BACKPACK = deferredRegister
      .register("large_green_backpack", ModItems::largeBackpack);

  public static final Supplier<Item> LARGE_TAN_BACKPACK = deferredRegister
      .register("large_tan_backpack", ModItems::largeBackpack);

  public static final Supplier<Item> LARGE_BLACK_BACKPACK = deferredRegister
      .register("large_black_backpack", ModItems::largeBackpack);

  public static final Supplier<Item> LARGE_GHILLIE_BACKPACK = deferredRegister
      .register("large_ghillie_backpack", ModItems::largeBackpack);

  public static final Supplier<Item> TAN_GUN_BAG = deferredRegister
      .register("tan_gun_bag", ModItems::gunBag);

  public static final Supplier<Item> GREY_GUN_BAG = deferredRegister
      .register("grey_gun_bag", ModItems::gunBag);

  private static HatItem.Properties hatArmor(float armor, float armorToughness) {
    return new HatItem.Properties()
        .attributeModifier(Attributes.ARMOR,
            new AttributeModifier(ModAttributeModifierIds.HAT_ARMOR,
                armor, AttributeModifier.Operation.ADD_VALUE))
        .attributeModifier(Attributes.ARMOR_TOUGHNESS,
            new AttributeModifier(ModAttributeModifierIds.HAT_ARMOR_TOUGHNESS,
                armorToughness, AttributeModifier.Operation.ADD_VALUE));
  }

  private static StorageItem weakVest() {
    return vest(8.0F, 1.0F);
  }

  private static StorageItem strongVest() {
    return vest(12.0F, 2.0F);
  }

  private static StorageItem vest(float armor, float armorToughness) {
    return new StorageItem((StorageItem.Properties) new StorageItem.Properties()
        .attributeModifier(Attributes.ARMOR,
            new AttributeModifier(ModAttributeModifierIds.STORAGE_ARMOR,
                armor, AttributeModifier.Operation.ADD_VALUE))
        .attributeModifier(Attributes.ARMOR_TOUGHNESS,
            new AttributeModifier(ModAttributeModifierIds.STORAGE_ARMOR_TOUGHNESS,
                armorToughness, AttributeModifier.Operation.ADD_VALUE))
        .menuConstructor(GenericMenu::createVest)
        .slot(Equipment.Slot.VEST)
        .itemRows(2)
        .stacksTo(1));
  }

  private static StorageItem smallBackpack() {
    return new StorageItem((StorageItem.Properties) new StorageItem.Properties()
        .slot(Equipment.Slot.BACKPACK)
        .itemRows(2)
        .menuConstructor(GenericMenu::createSmallBackpack)
        .toolTip(Component.translatable("small_backpack.information"))
        .stacksTo(1));
  }

  private static StorageItem mediumBackpack() {
    return new StorageItem((StorageItem.Properties) new StorageItem.Properties()
        .slot(Equipment.Slot.BACKPACK)
        .itemRows(3)
        .menuConstructor(GenericMenu::createMediumBackpack)
        .toolTip(Component.translatable("medium_backpack.information"))
        .stacksTo(1));
  }

  private static StorageItem largeBackpack() {
    return new StorageItem((StorageItem.Properties) new StorageItem.Properties()
        .slot(Equipment.Slot.BACKPACK)
        .itemRows(4)
        .menuConstructor(GenericMenu::createLargeBackpack)
        .toolTip(Component.translatable("large_backpack.information"))
        .stacksTo(1));
  }

  private static StorageItem gunBag() {
    return new StorageItem((StorageItem.Properties) new StorageItem.Properties()
        .slot(Equipment.Slot.BACKPACK)
        .itemRows(2)
        .menuConstructor(GenericMenu::createGunBag)
        .toolTip(Component.translatable("gunbag.information"))
        .stacksTo(1));
  }

  static {
    ArbitraryTooltips.registerTooltip(SCUBA_MASK,
        Component.translatable("clothing_item.water_breathing")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(SCUBA_CLOTHING,
        Component.translatable("clothing_item.water_speed")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(HANDCUFFS,
        Component.translatable("handcuffs.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(FIRST_AID_KIT,
        Component.translatable("first_aid_kit.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(ADRENALINE_SYRINGE,
        Component.translatable("adrenaline_syringe.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(SYRINGE,
        Component.translatable("syringe.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(BLOOD_SYRINGE,
        Component.translatable("blood_syringe.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(BANDAGE,
        Component.translatable("bandage.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(DIRTY_RAG,
        Component.translatable("dirty_rag.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(BLOODY_RAG,
        Component.translatable("bloody_rag.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(CLEAN_RAG,
        Component.translatable("clean_rag.information")
            .withStyle(ChatFormatting.GRAY));
    ArbitraryTooltips.registerTooltip(PARACHUTE,
        Component.translatable("parachute.information")
            .withStyle(ChatFormatting.GRAY));
  }
}
