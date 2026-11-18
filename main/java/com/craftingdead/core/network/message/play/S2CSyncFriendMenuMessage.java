package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.survival.client.friend.FriendClientData;
import com.craftingdead.survival.client.gui.screen.FriendScreen;
import com.craftingdead.survival.world.friend.FriendEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CSyncFriendMenuMessage(
    List<FriendEntry> onlinePlayers,
    List<FriendEntry> friends,
    List<FriendEntry> requests,
    List<FriendEntry> teamMembers,
    UUID ownerUuid,
    boolean isInSquad,
    boolean openScreen) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<S2CSyncFriendMenuMessage> TYPE =
      NetworkPayloadIds.SYNC_FRIEND_MENU;

  public static final StreamCodec<FriendlyByteBuf, S2CSyncFriendMenuMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(S2CSyncFriendMenuMessage::encode, S2CSyncFriendMenuMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    writeEntries(out, this.onlinePlayers);
    writeEntries(out, this.friends);
    writeEntries(out, this.requests);
    writeEntries(out, this.teamMembers);
    out.writeUUID(this.ownerUuid != null ? this.ownerUuid : new UUID(0, 0));
    out.writeBoolean(this.isInSquad);
    out.writeBoolean(this.openScreen);
  }

  public static S2CSyncFriendMenuMessage decode(FriendlyByteBuf in) {
    var online = readEntries(in);
    var friends = readEntries(in);
    var requests = readEntries(in);
    var team = readEntries(in);
    UUID owner = in.readUUID();
    if (owner.getMostSignificantBits() == 0 && owner.getLeastSignificantBits() == 0) {
      owner = null;
    }
    boolean inSquad = in.readBoolean();
    boolean open = in.readBoolean();
    return new S2CSyncFriendMenuMessage(online, friends, requests, team, owner, inSquad, open);
  }

  private static void writeEntries(FriendlyByteBuf out, List<FriendEntry> entries) {
    out.writeInt(entries.size());
    for (FriendEntry entry : entries) {
      out.writeUUID(entry.uuid());
      out.writeUtf(entry.name());
      out.writeBoolean(entry.online());
    }
  }

  private static List<FriendEntry> readEntries(FriendlyByteBuf in) {
    int size = in.readInt();
    List<FriendEntry> entries = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      entries.add(new FriendEntry(in.readUUID(), in.readUtf(Short.MAX_VALUE), in.readBoolean()));
    }
    return entries;
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      FriendClientData.set(this.onlinePlayers, this.friends, this.requests,
          this.teamMembers, this.ownerUuid, this.isInSquad);
      Minecraft minecraft = Minecraft.getInstance();
      if (this.openScreen) {
        minecraft.setScreen(new FriendScreen());
      } else if (minecraft.screen instanceof FriendScreen friendScreen) {
        friendScreen.refresh();
      }
    });
    return true;
  }
}
