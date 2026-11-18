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

import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RightClickStateMessage(boolean isDown, int ticks) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<RightClickStateMessage> TYPE = NetworkPayloadIds.RIGHT_CLICK_STATE;

  public static final StreamCodec<FriendlyByteBuf, RightClickStateMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(RightClickStateMessage::encode, RightClickStateMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void encode(RightClickStateMessage msg, FriendlyByteBuf buf) {
    NetworkBufferUtil.writeBoolean(buf, msg.isDown());
    NetworkBufferUtil.writeInt(buf, msg.ticks(), 0, 20 * 60);
  }

  public static RightClickStateMessage decode(FriendlyByteBuf buf) {
    return new RightClickStateMessage(NetworkBufferUtil.readBoolean(buf, false),
        NetworkBufferUtil.readInt(buf, -1));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var player = context.player();
      if (player == null || !player.isAlive()) {
        return;
      }
      var extension = PlayerExtension.get(player);
      if (extension == null) {
        return;
      }
      if (this.isDown) {
        extension.setHoldingRightClick(true);
        int fixedTicks = this.ticks > 0 ? this.ticks : extension.getRightClickTicks();
        extension.setRightClickTicks(Math.max(0, fixedTicks));
      } else {
        extension.setHoldingRightClick(false);
        extension.setRightClickTicks(0);
      }
    });
    return true;
  }
}

