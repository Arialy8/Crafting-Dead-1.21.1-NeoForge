package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.survival.world.friend.FriendActions;
import com.craftingdead.survival.world.friend.FriendTab;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SFriendActionMessage(FriendTab tab, UUID targetUuid) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<C2SFriendActionMessage> TYPE =
      NetworkPayloadIds.FRIEND_ACTION;

  public static final StreamCodec<FriendlyByteBuf, C2SFriendActionMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(C2SFriendActionMessage::encode, C2SFriendActionMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    out.writeEnum(this.tab);
    out.writeUUID(this.targetUuid);
  }

  public static C2SFriendActionMessage decode(FriendlyByteBuf in) {
    return new C2SFriendActionMessage(in.readEnum(FriendTab.class), in.readUUID());
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer serverPlayer)) {
        return;
      }
      switch (this.tab) {
        case ONLINE -> FriendActions.sendFriendRequest(serverPlayer, this.targetUuid);
        case FRIENDS -> FriendActions.removeFriend(serverPlayer, this.targetUuid);
        default -> {}
      }
    });
    return true;
  }
}
