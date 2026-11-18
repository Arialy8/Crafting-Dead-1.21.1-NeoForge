/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from
 * time to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.core.network.message.play;

import java.util.Optional;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.NetworkUtil;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.gun.AbstractGun;
import com.craftingdead.core.world.item.gun.Gun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public record TriggerPressedMessage(int entityId, boolean triggerPressed,
    ResourceLocation gunItemId, boolean rejectedStaleShot) implements CustomPacketPayload {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final ResourceLocation FALLBACK_GUN_ID = BuiltInRegistries.ITEM.getKey(Items.AIR);

  public TriggerPressedMessage(int entityId, boolean triggerPressed) {
    this(entityId, triggerPressed, FALLBACK_GUN_ID, false);
  }

  public TriggerPressedMessage(int entityId, boolean triggerPressed, ItemStack gunStack) {
    this(entityId, triggerPressed, gunStack == null || gunStack.isEmpty()
        ? FALLBACK_GUN_ID
        : BuiltInRegistries.ITEM.getKey(gunStack.getItem()), false);
  }

  public TriggerPressedMessage(int entityId, boolean triggerPressed, ResourceLocation gunItemId) {
    this(entityId, triggerPressed, gunItemId, false);
  }

  public TriggerPressedMessage {
    if (gunItemId == null) {
      gunItemId = FALLBACK_GUN_ID;
    }
  }

  public static final CustomPacketPayload.Type<TriggerPressedMessage> TYPE = NetworkPayloadIds.TRIGGER_PRESSED;

  public static final StreamCodec<FriendlyByteBuf, TriggerPressedMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(TriggerPressedMessage::encode, TriggerPressedMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeVarInt(out, this.entityId, -1, Integer.MAX_VALUE);
    NetworkBufferUtil.writeBoolean(out, this.triggerPressed);
    NetworkBufferUtil.writeResourceLocation(out, this.gunItemId, FALLBACK_GUN_ID);
    NetworkBufferUtil.writeBoolean(out, this.rejectedStaleShot);
  }

  public static TriggerPressedMessage decode(FriendlyByteBuf in) {
    return new TriggerPressedMessage(NetworkBufferUtil.readVarInt(in, -1),
        NetworkBufferUtil.readBoolean(in, false),
        NetworkBufferUtil.readResourceLocation(in, FALLBACK_GUN_ID),
        NetworkBufferUtil.readBoolean(in, false));
  }

  private static ResourceLocation getItemId(ItemStack stack) {
    return stack == null || stack.isEmpty()
        ? FALLBACK_GUN_ID
        : BuiltInRegistries.ITEM.getKey(stack.getItem());
  }

  private boolean expectsSpecificGun() {
    return !FALLBACK_GUN_ID.equals(this.gunItemId);
  }

  private boolean matchesExpectedGun(ItemStack stack) {
    return !this.expectsSpecificGun() || getItemId(stack).equals(this.gunItemId);
  }

  private void sendStopToSender(IPayloadContext context) {
    var sender = context.player();
    if (sender instanceof ServerPlayer serverPlayer) {
      NetworkChannel.PLAY.sendToPlayer(serverPlayer,
          new TriggerPressedMessage(this.entityId, false, this.gunItemId, true));
    }
  }

  private void sendMainHandSyncToSender(IPayloadContext context,
      LivingExtension<?, ?> extension, Gun gun) {
    var sender = context.player();
    if (sender instanceof ServerPlayer serverPlayer && gun != null) {
      gun.prepareForMainHandStateSync(extension);
      NetworkChannel.PLAY.sendToPlayer(serverPlayer,
          new SyncGunEquipmentSlotMessage(this.entityId, EquipmentSlot.MAINHAND, gun, true));
    }
  }

  private Optional<Gun> findMatchingMainHandGun(LivingExtension<?, ?> extension) {
    if (!this.matchesExpectedGun(extension.mainHandItem())) {
      return Optional.empty();
    }
    return extension.mainHandGun();
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      final boolean serverReception = NetworkUtil.isServerReception(context);

      NetworkUtil.findEntityOrSender(context, this.entityId, LivingEntity.class)
          .flatMap(entity -> Optional.ofNullable(LivingExtension.get(entity)))
          .ifPresent(extension -> {
            var matchingGun = this.findMatchingMainHandGun(extension);

            if (serverReception && this.triggerPressed && matchingGun.isEmpty()) {
              
              this.sendStopToSender(context);
              return;
            }

            if (!serverReception && !this.triggerPressed && this.rejectedStaleShot) {
              // The server rejected a client-only shot. Stop the matching old firing loop and, if
              // that same ghost gun is still visible locally, clear its client-only magazine so
              // pressing fire again cannot restart a fake 20/30/350-round loop.
              AbstractGun.stopActiveTrigger(extension, this.gunItemId, false);
              matchingGun.ifPresent(gun -> gun.handleClientShotRejected(extension));
              return;
            }

            if (!serverReception && !this.triggerPressed && matchingGun.isEmpty()) {
              // Legacy non-rejected stop packet for an old gun that may no longer be visible.
              AbstractGun.stopActiveTrigger(extension, this.gunItemId, false);
              return;
            }

            matchingGun.ifPresent(gun -> {
              if (serverReception && this.triggerPressed) {
                gun.handleClientShotRequest(extension);
              } else {
                gun.setTriggerPressed(extension, this.triggerPressed, serverReception);
                if (serverReception && !this.triggerPressed && this.expectsSpecificGun()) {
                  // A client can switch back to a previously loaded gun while its local hotbar
                  // ItemStack still lacks the server CUSTOM_DATA that stores the magazine. Treat a
                  // gun-specific release packet as a lightweight state request and return the
                  // authoritative main-hand gun state. This fixes the v17 symptom where the server
                  // still had oldStack=magazine, but the client saw providerStack=air until
                  // reloading again.
                  this.sendMainHandSyncToSender(context, extension, gun);
                }
              }
            });
          });
    });
    return true;
  }
}
