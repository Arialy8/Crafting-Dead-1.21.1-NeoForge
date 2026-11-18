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

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.trauma.TraumaSeverity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TraumaPacket(TraumaSeverity severity, int aimSwayTicks, float aimSwayStrength) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<TraumaPacket> TYPE = NetworkPayloadIds.TRAUMA;

  public static final StreamCodec<FriendlyByteBuf, TraumaPacket> STREAM_CODEC =
      LegacyPayloadCodec.codec(TraumaPacket::encode, TraumaPacket::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeVarInt(out, (this.severity == null ? TraumaSeverity.NONE : this.severity).ordinal(), 0, TraumaSeverity.values().length - 1);
    NetworkBufferUtil.writeVarInt(out, this.aimSwayTicks, 0, 20 * 60);
    NetworkBufferUtil.writeFloat(out, this.aimSwayStrength, 0.0F, 50.0F);
  }

  public static TraumaPacket decode(FriendlyByteBuf in) {
    var severity = TraumaSeverity.fromOrdinal(NetworkBufferUtil.readCappedVarInt(in, TraumaSeverity.values().length - 1));
    int aimSwayTicks = NetworkBufferUtil.readCappedVarInt(in, 0, 20 * 60, 0);
    float aimSwayStrength = Math.max(0.0F, Math.min(NetworkBufferUtil.readFloat(in, 0.0F), 50.0F));
    return new TraumaPacket(severity, aimSwayTicks, aimSwayStrength);
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var clientDist = CraftingDead.getInstance().getClientDist();
      if (clientDist != null) {
        clientDist.handleTrauma(this);
      }
    });
    return true;
  }
}
