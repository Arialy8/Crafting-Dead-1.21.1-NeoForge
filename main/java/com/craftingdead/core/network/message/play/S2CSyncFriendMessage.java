package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.survival.client.friend.ClientFriendCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CSyncFriendMessage(List<UUID> friends) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<S2CSyncFriendMessage> TYPE =
      NetworkPayloadIds.SYNC_FRIEND;

  public static final StreamCodec<FriendlyByteBuf, S2CSyncFriendMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(S2CSyncFriendMessage::encode, S2CSyncFriendMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    if (this.friends == null) {
      out.writeInt(0);
      return;
    }
    out.writeInt(this.friends.size());
    for (UUID uuid : this.friends) {
      out.writeUUID(uuid);
    }
  }

  public static S2CSyncFriendMessage decode(FriendlyByteBuf in) {
    int size = in.readInt();
    List<UUID> friends = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      friends.add(in.readUUID());
    }
    return new S2CSyncFriendMessage(friends);
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> ClientFriendCache.setFriends(this.friends));
    return true;
  }
}
