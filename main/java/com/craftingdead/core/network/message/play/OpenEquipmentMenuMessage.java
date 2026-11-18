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

import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.vehicle.T72B3Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenEquipmentMenuMessage() implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<OpenEquipmentMenuMessage> TYPE = NetworkPayloadIds.OPEN_EQUIPMENT_MENU;

  public static final StreamCodec<FriendlyByteBuf, OpenEquipmentMenuMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(OpenEquipmentMenuMessage::encode, OpenEquipmentMenuMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {}

  public static OpenEquipmentMenuMessage decode(FriendlyByteBuf in) {
    return new OpenEquipmentMenuMessage();
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(
        () -> {
          var sender = context.player();
          if (sender != null) {
            if (sender.getVehicle() instanceof T72B3Entity tank) {
              sender.openMenu(tank);
              return;
            }
            var extension = PlayerExtension.get(sender);
            if (extension != null) {
              extension.openEquipmentMenu();
            }
          }
        });
    return true;
  }
}
