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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlockDestroyActionMessage(BlockPos pos) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<BlockDestroyActionMessage> TYPE = NetworkPayloadIds.BLOCK_DESTROY_ACTION;

  public static final StreamCodec<FriendlyByteBuf, BlockDestroyActionMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(BlockDestroyActionMessage::encode, BlockDestroyActionMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void encode(BlockDestroyActionMessage msg, FriendlyByteBuf buf) {
    NetworkBufferUtil.writeBlockPos(buf, msg.pos());
  }

  public static BlockDestroyActionMessage decode(FriendlyByteBuf buf) {
    return new BlockDestroyActionMessage(NetworkBufferUtil.readBlockPos(buf, BlockPos.ZERO));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var player = context.player();
      if (player instanceof ServerPlayer serverPlayer && this.pos != null) {
        var level = serverPlayer.serverLevel();
        var state = level.getBlockState(this.pos);
        if (!state.isAir()) {
          PacketDistributor.sendToPlayersTrackingChunk(level,
              new ChunkPos(this.pos),
              new BlockDestroyParticleMessage(this.pos, state));
          level.setBlock(this.pos, Blocks.AIR.defaultBlockState(), 3);
        }
      }
    });
    return true;
  }
}

