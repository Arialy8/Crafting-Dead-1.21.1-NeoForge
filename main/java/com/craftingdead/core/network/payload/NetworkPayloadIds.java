/*
 * Crafting Dead 1.21.1 port helper.
 */
package com.craftingdead.core.network.payload;

import com.craftingdead.core.CraftingDead;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Central CustomPacketPayload ids for the 1.21.1 networking migration.
 *
 * <p>The old Crafting Dead network layer still registers many packets through SimpleChannel while the
 * port is in progress. Giving each packet a vanilla payload id now lets individual messages be moved to
 * the 1.21 payload registrar one-by-one without changing call sites across gun, entity, GUI and
 * capability code.</p>
 */
public final class NetworkPayloadIds {

  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.OpenEquipmentMenuMessage> OPEN_EQUIPMENT_MENU = type("open_equipment_menu");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.OpenCraftingMenuMessage> OPEN_CRAFTING_MENU = type("open_crafting_menu");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.RightClickStateMessage> RIGHT_CLICK_STATE = type("right_click_state");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.EnableCombatModeMessage> ENABLE_COMBAT_MODE = type("enable_combat_mode");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.DamageHandcuffsMessage> DAMAGE_HANDCUFFS = type("damage_handcuffs");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.TankInputMessage> TANK_INPUT = type("tank_input");

  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.OpenStorageMessage> OPEN_STORAGE = type("open_storage");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SecondaryActionMessage> SECONDARY_ACTION = type("secondary_action");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.TriggerPressedMessage> TRIGGER_PRESSED = type("trigger_pressed");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SetFireModeMessage> SET_FIRE_MODE = type("set_fire_mode");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.CancelActionMessage> CANCEL_ACTION = type("cancel_action");

  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.CrouchMessage> CROUCH = type("crouch");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.ParachuteSyncMessage> PARACHUTE_SYNC = type("parachute_sync");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.NPCTriggerPressedMessage> NPC_TRIGGER_PRESSED = type("npc_trigger_pressed");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.BlockDestroyActionMessage> BLOCK_DESTROY_ACTION = type("block_destroy_action");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.BlockDestroyParticleMessage> BLOCK_DESTROY_PARTICLE = type("block_destroy_particle");

  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SyncLivingMessage> SYNC_LIVING = type("sync_living");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SyncGunContainerSlotMessage> SYNC_GUN_CONTAINER_SLOT = type("sync_gun_container_slot");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SyncGunEquipmentSlotMessage> SYNC_GUN_EQUIPMENT_SLOT = type("sync_gun_equipment_slot");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SyncProtectionConfigMessage> SYNC_PROTECTION_CONFIG = type("sync_protection_config");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SyncHydrationMessage> SYNC_HYDRATION = type("sync_hydration");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SyncSurvivalStatsMessage> SYNC_SURVIVAL_STATS = type("sync_survival_stats");


  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.TraumaPacket> TRAUMA = type("trauma");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.PerformActionMessage> PERFORM_ACTION = type("perform_action");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.HitMessage> HIT = type("hit");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.KillFeedMessage> KILL_FEED = type("kill_feed");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.ValidatePendingHitMessage> VALIDATE_PENDING_HIT = type("validate_pending_hit");

  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.C2SOpenFriendMenuMessage> FRIEND_OPEN_MENU = type("friend_open_menu");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.C2SFriendActionMessage> FRIEND_ACTION = type("friend_action");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.C2SFriendRequestActionMessage> FRIEND_REQUEST_ACTION = type("friend_request_action");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.S2CSyncFriendMenuMessage> SYNC_FRIEND_MENU = type("sync_friend_menu");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.S2CSyncFriendMessage> SYNC_FRIEND = type("sync_friend");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.C2SFriendTeamActionMessage> FRIEND_TEAM_ACTION = type("friend_team_action");
  public static final CustomPacketPayload.Type<com.craftingdead.core.network.message.play.SyncBloodMoonMessage> SYNC_BLOOD_MOON = type("sync_blood_moon");

  private NetworkPayloadIds() {}

  private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
    return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, path));
  }
}
