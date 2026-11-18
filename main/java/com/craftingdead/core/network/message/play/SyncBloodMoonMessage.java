package com.craftingdead.core.network.message.play;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 血月状态同步包（服务端 → 客户端）。
 *
 * @param bloodMoonDay      今天是否为血月天
 * @param omenDay           今天是否为预兆日（血月前天空变暗）
 * @param daysUntilBloodMoon 距离血月还有几天（-1 表示血月功能关闭）
 */
public record SyncBloodMoonMessage(
    boolean bloodMoonDay,
    boolean omenDay,
    int daysUntilBloodMoon) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncBloodMoonMessage> TYPE =
      NetworkPayloadIds.SYNC_BLOOD_MOON;

  public static final StreamCodec<FriendlyByteBuf, SyncBloodMoonMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(SyncBloodMoonMessage::encode, SyncBloodMoonMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeBoolean(out, this.bloodMoonDay);
    NetworkBufferUtil.writeBoolean(out, this.omenDay);
    NetworkBufferUtil.writeVarInt(out, this.daysUntilBloodMoon, -1, 30);
  }

  public static SyncBloodMoonMessage decode(FriendlyByteBuf in) {
    return new SyncBloodMoonMessage(
        NetworkBufferUtil.readBoolean(in, false),
        NetworkBufferUtil.readBoolean(in, false),
        NetworkBufferUtil.readCappedVarInt(in, -1, 30, -1));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var clientDist = CraftingDead.getInstance().getClientDist();
      if (clientDist != null) {
        clientDist.handleBloodMoonSync(this.bloodMoonDay, this.omenDay,
            this.daysUntilBloodMoon);
      }
    });
    return true;
  }
}