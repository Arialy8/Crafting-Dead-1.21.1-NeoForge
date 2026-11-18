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
import com.craftingdead.core.world.entity.extension.LivingExtension;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncLivingMessage(int entityId, FriendlyByteBuf data) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncLivingMessage> TYPE = NetworkPayloadIds.SYNC_LIVING;

  public static final StreamCodec<FriendlyByteBuf, SyncLivingMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(SyncLivingMessage::encode, SyncLivingMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }


  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeVarInt(out, this.entityId, -1, Integer.MAX_VALUE);
    NetworkBufferUtil.writePayload(out, this.data);
  }

  public static SyncLivingMessage decode(FriendlyByteBuf in) {
    return new SyncLivingMessage(NetworkBufferUtil.readVarInt(in, -1),
        NetworkBufferUtil.readPayload(in));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> NetworkUtil.findEntity(context, this.entityId)
        .filter(entity -> this.data != null)
        .filter(entity -> entity instanceof LivingEntity)
        .flatMap(entity -> Optional.ofNullable(LivingExtension.get((LivingEntity) entity)))
        .ifPresent(living -> {
          RegistryFriendlyByteBuf registryData = new RegistryFriendlyByteBuf(
              this.data.copy(), living.entity().registryAccess());
          living.decode(registryData);
          registryData.release();
        }));
    return true;
  }
}
