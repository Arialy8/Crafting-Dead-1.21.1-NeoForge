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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HitMessage(Vec3 hitPos, boolean dead) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<HitMessage> TYPE = NetworkPayloadIds.HIT;

  public static final StreamCodec<FriendlyByteBuf, HitMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(HitMessage::encode, HitMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    Vec3 safeHitPos = this.hitPos == null ? Vec3.ZERO : this.hitPos;
    NetworkBufferUtil.writeDouble(out, safeCoordinate(safeHitPos.x()), 0.0D);
    NetworkBufferUtil.writeDouble(out, safeCoordinate(safeHitPos.y()), 0.0D);
    NetworkBufferUtil.writeDouble(out, safeCoordinate(safeHitPos.z()), 0.0D);
    NetworkBufferUtil.writeBoolean(out, this.dead);
  }

  public static HitMessage decode(FriendlyByteBuf in) {
    double x = safeCoordinate(NetworkBufferUtil.readDouble(in, 0.0D));
    double y = safeCoordinate(NetworkBufferUtil.readDouble(in, 0.0D));
    double z = safeCoordinate(NetworkBufferUtil.readDouble(in, 0.0D));
    return new HitMessage(new Vec3(x, y, z), NetworkBufferUtil.readBoolean(in, false));
  }

  private static double safeCoordinate(double value) {
    return Double.isFinite(value) ? value : 0.0D;
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var clientDist = CraftingDead.getInstance().getClientDist();
      if (clientDist != null && this.hitPos != null) {
        clientDist.handleHit(this.hitPos, this.dead);
      }
    });
    return true;
  }
}
