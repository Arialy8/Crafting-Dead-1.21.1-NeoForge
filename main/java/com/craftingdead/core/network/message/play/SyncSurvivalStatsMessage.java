package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.immerse.client.ImmerseSurvivalHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncSurvivalStatsMessage(int survivalTicks, int zombieKills, int playerKills,
    long rankScore)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncSurvivalStatsMessage> TYPE =
      NetworkPayloadIds.SYNC_SURVIVAL_STATS;

  public static final StreamCodec<FriendlyByteBuf, SyncSurvivalStatsMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(SyncSurvivalStatsMessage::encode,
          SyncSurvivalStatsMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeVarInt(out, this.survivalTicks, 0, Integer.MAX_VALUE);
    NetworkBufferUtil.writeVarInt(out, this.zombieKills, 0, Integer.MAX_VALUE);
    NetworkBufferUtil.writeVarInt(out, this.playerKills, 0, Integer.MAX_VALUE);
    out.writeLong(this.rankScore);
  }

  public static SyncSurvivalStatsMessage decode(FriendlyByteBuf in) {
    return new SyncSurvivalStatsMessage(
        NetworkBufferUtil.readCappedVarInt(in, 0, Integer.MAX_VALUE, 0),
        NetworkBufferUtil.readCappedVarInt(in, 0, Integer.MAX_VALUE, 0),
        NetworkBufferUtil.readCappedVarInt(in, 0, Integer.MAX_VALUE, 0), Math.max(0, in.readLong()));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> ImmerseSurvivalHud.setStats(
        this.survivalTicks, this.zombieKills, this.playerKills, this.rankScore));
    return true;
  }
}
