package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SOpenFriendMenuMessage() implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<C2SOpenFriendMenuMessage> TYPE =
      NetworkPayloadIds.FRIEND_OPEN_MENU;

  public static final StreamCodec<FriendlyByteBuf, C2SOpenFriendMenuMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(C2SOpenFriendMenuMessage::encode, C2SOpenFriendMenuMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {}

  public static C2SOpenFriendMenuMessage decode(FriendlyByteBuf in) {
    return new C2SOpenFriendMenuMessage();
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      if (context.player() instanceof ServerPlayer serverPlayer) {
        com.craftingdead.survival.world.friend.FriendActions.syncFriendMenu(serverPlayer, true);
      }
    });
    return true;
  }
}
