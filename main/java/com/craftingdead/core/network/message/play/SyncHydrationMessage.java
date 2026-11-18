package com.craftingdead.core.network.message.play;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.survival.world.hydration.HydrationData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncHydrationMessage(int water) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncHydrationMessage> TYPE =
      NetworkPayloadIds.SYNC_HYDRATION;

  public static final StreamCodec<FriendlyByteBuf, SyncHydrationMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(SyncHydrationMessage::encode, SyncHydrationMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeVarInt(out, this.water, 0, HydrationData.MAX_WATER);
  }

  public static SyncHydrationMessage decode(FriendlyByteBuf in) {
    return new SyncHydrationMessage(
        NetworkBufferUtil.readCappedVarInt(in, 0, HydrationData.MAX_WATER, HydrationData.MAX_WATER));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var clientDist = CraftingDead.getInstance().getClientDist();
      if (clientDist != null) {
        clientDist.handleHydrationSync(this.water);
      }
    });
    return true;
  }
}
