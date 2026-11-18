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

package com.craftingdead.core.network;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.network.message.play.BlockDestroyActionMessage;
import com.craftingdead.core.network.message.play.BlockDestroyParticleMessage;
import com.craftingdead.core.network.message.play.C2SFriendActionMessage;
import com.craftingdead.core.network.message.play.C2SFriendRequestActionMessage;
import com.craftingdead.core.network.message.play.C2SFriendTeamActionMessage;
import com.craftingdead.core.network.message.play.C2SOpenFriendMenuMessage;
import com.craftingdead.core.network.message.play.CancelActionMessage;
import com.craftingdead.core.network.message.play.CrouchMessage;
import com.craftingdead.core.network.message.play.DamageHandcuffsMessage;
import com.craftingdead.core.network.message.play.EnableCombatModeMessage;
import com.craftingdead.core.network.message.play.HitMessage;
import com.craftingdead.core.network.message.play.KillFeedMessage;
import com.craftingdead.core.network.message.play.NPCTriggerPressedMessage;
import com.craftingdead.core.network.message.play.OpenCraftingMenuMessage;
import com.craftingdead.core.network.message.play.OpenEquipmentMenuMessage;
import com.craftingdead.core.network.message.play.OpenStorageMessage;
import com.craftingdead.core.network.message.play.ParachuteSyncMessage;
import com.craftingdead.core.network.message.play.PerformActionMessage;
import com.craftingdead.core.network.message.play.RightClickStateMessage;
import com.craftingdead.core.network.message.play.SecondaryActionMessage;
import com.craftingdead.core.network.message.play.SetFireModeMessage;
import com.craftingdead.core.network.message.play.SyncGunContainerSlotMessage;
import com.craftingdead.core.network.message.play.SyncGunEquipmentSlotMessage;
import com.craftingdead.core.network.message.play.SyncBloodMoonMessage;
import com.craftingdead.core.network.message.play.SyncHydrationMessage;
import com.craftingdead.core.network.message.play.SyncSurvivalStatsMessage;
import com.craftingdead.core.network.message.play.SyncLivingMessage;
import com.craftingdead.core.network.message.play.SyncProtectionConfigMessage;
import com.craftingdead.core.network.message.play.S2CSyncFriendMenuMessage;
import com.craftingdead.core.network.message.play.S2CSyncFriendMessage;
import com.craftingdead.core.network.message.play.TraumaPacket;
import com.craftingdead.core.network.message.play.TankInputMessage;
import com.craftingdead.core.network.message.play.TriggerPressedMessage;
import com.craftingdead.core.network.message.play.ValidatePendingHitMessage;
import com.craftingdead.core.network.payload.NetworkPayloadAudit;
import com.craftingdead.core.network.payload.NetworkPayloadMigrationHooks;
import com.craftingdead.core.network.payload.NetworkPayloadRegistrations;
import com.craftingdead.core.network.payload.NetworkPayloadRegistrations.PayloadRegistration;
import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

public enum NetworkChannel {

  PLAY;

  private static final Logger logger = LogUtils.getLogger();
  private static final int NETWORK_VERSION = 1;
  private static boolean loaded;

  public static void loadChannels(RegisterPayloadHandlersEvent event) {
    if (loaded) return;
    try {
      NetworkPayloadAudit.validateOnce();
      int count = NetworkPayloadMigrationHooks.preparedPlayPayloadCount();
      logger.info("Crafting Dead network: registering {} play payloads", count);
      if (count <= 0) {
        throw new IllegalStateException("No Crafting Dead play payloads prepared");
      }

      var registrar = event.registrar(CraftingDead.ID).versioned(String.valueOf(NETWORK_VERSION));

      for (PayloadRegistration<?> registration : NetworkPayloadRegistrations.PLAY) {
        register(registrar, registration);
      }

      registrar.playToServer(com.craftingdead.core.network.message.play.RenameChalkMessage.TYPE,
          com.craftingdead.core.network.message.play.RenameChalkMessage.CODEC, (msg, context) -> msg.handle(context));
      loaded = true;
      logger.info("Crafting Dead network: registration complete");
    } catch (Throwable e) {
      logger.error("Crafting Dead network: registration failed - network features will not work", e);
    }
  }

  private static <T extends CustomPacketPayload> void register(
      PayloadRegistrar registrar, PayloadRegistration<T> registration) {
    switch (registration.direction()) {
      case PLAY_TO_CLIENT -> registrar.playToClient(
          registration.type(),
          registration.streamCodec(),
          (payload, context) -> handlePayload(payload, context));
      case PLAY_TO_SERVER -> registrar.playToServer(
          registration.type(),
          registration.streamCodec(),
          (payload, context) -> handlePayload(payload, context));
      case BIDIRECTIONAL -> registrar.playBidirectional(
          registration.type(),
          registration.streamCodec(),
          (payload, context) -> handlePayload(payload, context));
    }
  }

  private static <T extends CustomPacketPayload> void handlePayload(T payload, IPayloadContext context) {
    try {
      logger.debug("Crafting Dead network: handling payload {}", payload.getClass().getSimpleName());
      if (payload instanceof OpenEquipmentMenuMessage msg) { msg.handle(context); }
      else if (payload instanceof RightClickStateMessage msg) { msg.handle(context); }
      else if (payload instanceof PerformActionMessage msg) { msg.handle(context); }
      else if (payload instanceof CancelActionMessage msg) { msg.handle(context); }
      else if (payload instanceof TriggerPressedMessage msg) { msg.handle(context); }
      else if (payload instanceof NPCTriggerPressedMessage msg) { msg.handle(context); }
      else if (payload instanceof SecondaryActionMessage msg) { msg.handle(context); }
      else if (payload instanceof SetFireModeMessage msg) { msg.handle(context); }
      else if (payload instanceof ValidatePendingHitMessage msg) { msg.handle(context); }
      else if (payload instanceof CrouchMessage msg) { msg.handle(context); }
      else if (payload instanceof EnableCombatModeMessage msg) { msg.handle(context); }
      else if (payload instanceof DamageHandcuffsMessage msg) { msg.handle(context); }
      else if (payload instanceof OpenStorageMessage msg) { msg.handle(context); }
      else if (payload instanceof OpenCraftingMenuMessage msg) { msg.handle(context); }
      else if (payload instanceof BlockDestroyActionMessage msg) { msg.handle(context); }
      else if (payload instanceof BlockDestroyParticleMessage msg) { msg.handle(context); }
      else if (payload instanceof SyncLivingMessage msg) { msg.handle(context); }
      else if (payload instanceof SyncGunContainerSlotMessage msg) { msg.handle(context); }
      else if (payload instanceof SyncGunEquipmentSlotMessage msg) { msg.handle(context); }
      else if (payload instanceof SyncHydrationMessage msg) { msg.handle(context); }
      else if (payload instanceof SyncSurvivalStatsMessage msg) { msg.handle(context); }
      else if (payload instanceof HitMessage msg) { msg.handle(context); }
      else if (payload instanceof KillFeedMessage msg) { msg.handle(context); }
      else if (payload instanceof ParachuteSyncMessage msg) { msg.handle(context); }
      else if (payload instanceof TraumaPacket msg) { msg.handle(context); }
      else if (payload instanceof SyncProtectionConfigMessage msg) { msg.handle(context); }
      else if (payload instanceof C2SOpenFriendMenuMessage msg) { msg.handle(context); }
      else if (payload instanceof C2SFriendActionMessage msg) { msg.handle(context); }
      else if (payload instanceof C2SFriendRequestActionMessage msg) { msg.handle(context); }
      else if (payload instanceof S2CSyncFriendMenuMessage msg) { msg.handle(context); }
      else if (payload instanceof S2CSyncFriendMessage msg) { msg.handle(context); }
      else if (payload instanceof C2SFriendTeamActionMessage msg) { msg.handle(context); }
      else if (payload instanceof SyncBloodMoonMessage msg) { msg.handle(context); }
      else if (payload instanceof TankInputMessage msg) { msg.handle(context); }
      else {
        logger.warn("Crafting Dead network: unhandled payload type {}", payload.getClass().getName());
      }
    } catch (Throwable e) {
      logger.error("Crafting Dead network: error handling payload {}",
          payload.getClass().getSimpleName(), e);
    }
  }

  public void sendToServer(Object message) {
    try {
      NetworkPayloadAudit.validateOutgoing(message);
      if (message instanceof CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
      } else {
        logger.error("Crafting Dead network: invalid message type {}",
            message.getClass().getName());
      }
    } catch (Throwable e) {
      logger.error("Crafting Dead network: failed to send message to server", e);
    }
  }

  public void sendToPlayer(ServerPlayer player, Object message) {
    try {
      NetworkPayloadAudit.validateOutgoing(message);
      if (message instanceof CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
      } else {
        logger.error("Crafting Dead network: invalid message type {}",
            message.getClass().getName());
      }
    } catch (Throwable e) {
      logger.error("Crafting Dead network: failed to send message to player", e);
    }
  }
}
