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

import com.craftingdead.core.world.effect.ModMobEffects;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ParachuteSyncMessage(int entityId, boolean hasParachute) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ParachuteSyncMessage> TYPE = NetworkPayloadIds.PARACHUTE_SYNC;

  public static final StreamCodec<FriendlyByteBuf, ParachuteSyncMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(ParachuteSyncMessage::encode, ParachuteSyncMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void encode(ParachuteSyncMessage packet, FriendlyByteBuf buf) {
    NetworkBufferUtil.writeInt(buf, packet.entityId(), -1, Integer.MAX_VALUE);
    NetworkBufferUtil.writeBoolean(buf, packet.hasParachute());
  }

  public static ParachuteSyncMessage decode(FriendlyByteBuf buf) {
    return new ParachuteSyncMessage(NetworkBufferUtil.readInt(buf, -1),
        NetworkBufferUtil.readBoolean(buf, false));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var level = context.player() == null ? null : context.player().level();
      if (level == null) {
        return;
      }
      Entity entity = level.getEntity(this.entityId);
      if (entity instanceof LivingEntity livingEntity) {
        if (this.hasParachute) {
          livingEntity.addEffect(new MobEffectInstance(ModMobEffects.holder(ModMobEffects.PARACHUTE)));
        } else {
          livingEntity.removeEffect(ModMobEffects.holder(ModMobEffects.PARACHUTE));
        }
      }
    });
    return true;
  }
}
