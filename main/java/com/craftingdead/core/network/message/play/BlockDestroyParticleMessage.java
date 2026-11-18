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
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlockDestroyParticleMessage(BlockPos pos, BlockState state) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<BlockDestroyParticleMessage> TYPE = NetworkPayloadIds.BLOCK_DESTROY_PARTICLE;

  public static final StreamCodec<FriendlyByteBuf, BlockDestroyParticleMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(BlockDestroyParticleMessage::encode, BlockDestroyParticleMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void encode(BlockDestroyParticleMessage msg, FriendlyByteBuf buf) {
    NetworkBufferUtil.writeBlockPos(buf, msg.pos());
    NetworkBufferUtil.writeVarInt(buf, Block.getId(msg.state()), 0, Integer.MAX_VALUE);
  }

  public static BlockDestroyParticleMessage decode(FriendlyByteBuf buf) {
    return new BlockDestroyParticleMessage(NetworkBufferUtil.readBlockPos(buf, BlockPos.ZERO),
        Block.stateById(NetworkBufferUtil.readVarInt(buf, 0)));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var level = context.player() == null ? null : context.player().level();
      if (level != null && this.pos != null) {
        var state = level.getBlockState(this.pos);
        level.addDestroyBlockEffect(this.pos, state.isAir() ? this.state : state);
      }
    });
    return true;
  }
}
