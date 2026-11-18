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

package com.craftingdead.core.network.message.play;

import java.util.Optional;
import com.craftingdead.core.network.NetworkUtil;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.util.RegistryNameUtil;
import com.craftingdead.core.world.action.ActionType;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PerformActionMessage(ActionType<?> actionType, int performerEntityId,
    FriendlyByteBuf buf) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<PerformActionMessage> TYPE = NetworkPayloadIds.PERFORM_ACTION;

  public static final StreamCodec<FriendlyByteBuf, PerformActionMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(PerformActionMessage::encode, PerformActionMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeResourceLocation(out, RegistryNameUtil.actionType(this.actionType), RegistryNameUtil.actionType(null));
    NetworkBufferUtil.writeVarInt(out, this.performerEntityId, -1, Integer.MAX_VALUE);
    NetworkBufferUtil.writePayload(out, this.buf);
  }

  public static PerformActionMessage decode(FriendlyByteBuf in) {
    ActionType<?> actionType = ActionTypes.REGISTRY.get(
        NetworkBufferUtil.readResourceLocation(in, ResourceLocation.fromNamespaceAndPath("craftingdead", "missing")));
    return new PerformActionMessage(actionType, NetworkBufferUtil.readVarInt(in, -1),
        NetworkBufferUtil.readPayload(in));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      try {
        if (this.actionType == null) {
          return;
        }
        final var performer = NetworkUtil.findEntityOrSender(context, this.performerEntityId,
            LivingEntity.class)
            .flatMap(entity -> Optional.ofNullable(LivingExtension.get(entity)))
            .orElse(null);
        if (performer == null || this.buf == null) {
          return;
        }
        final var serverSide = NetworkUtil.isServerReception(context);
        if (!serverSide || this.actionType.isTriggeredByClient()) {
          var action = this.actionType.decode(performer, this.buf);
          if (action != null) {
            performer.performAction(action, serverSide);
          }
        }
      } finally {
        if (this.buf != null) {
          this.buf.release();
        }
      }
    });
    return true;
  }
}
