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
import com.craftingdead.core.trauma.ProtectionConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncProtectionConfigMessage(String serializedConfig) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncProtectionConfigMessage> TYPE = NetworkPayloadIds.SYNC_PROTECTION_CONFIG;

  public static final StreamCodec<FriendlyByteBuf, SyncProtectionConfigMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(SyncProtectionConfigMessage::encode, SyncProtectionConfigMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }


  private static final int MAX_LENGTH = 1 << 15;

  public void encode(FriendlyByteBuf out) {
    String config = this.serializedConfig == null ? "" : this.serializedConfig;
    NetworkBufferUtil.writeUtf(out, config, MAX_LENGTH);
  }

  public static SyncProtectionConfigMessage decode(FriendlyByteBuf in) {
    try {
      return new SyncProtectionConfigMessage(NetworkBufferUtil.readUtf(in, MAX_LENGTH, "{}"));
    } catch (RuntimeException ex) {
      return new SyncProtectionConfigMessage("");
    }
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      if (this.serializedConfig != null && !this.serializedConfig.isBlank()) {
        ProtectionConfig.applySerializedConfig(this.serializedConfig);
      }
    });
    return true;
  }
}
