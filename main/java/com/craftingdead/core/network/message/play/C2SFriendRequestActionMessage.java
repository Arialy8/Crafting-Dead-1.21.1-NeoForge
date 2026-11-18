package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.survival.world.friend.FriendActions;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SFriendRequestActionMessage(UUID targetUuid, boolean accept)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<C2SFriendRequestActionMessage> TYPE =
      NetworkPayloadIds.FRIEND_REQUEST_ACTION;

  public static final StreamCodec<FriendlyByteBuf, C2SFriendRequestActionMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(C2SFriendRequestActionMessage::encode,
          C2SFriendRequestActionMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    out.writeUUID(this.targetUuid);
    out.writeBoolean(this.accept);
  }

  public static C2SFriendRequestActionMessage decode(FriendlyByteBuf in) {
    return new C2SFriendRequestActionMessage(in.readUUID(), in.readBoolean());
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer serverPlayer)) {
        return;
      }
      if (this.accept) {
        FriendActions.acceptFriendRequest(serverPlayer, this.targetUuid);
      } else {
        FriendActions.denyFriendRequest(serverPlayer, this.targetUuid);
      }
    });
    return true;
  }
}
