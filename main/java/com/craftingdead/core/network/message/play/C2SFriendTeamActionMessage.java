package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.survival.world.SurvivalTeamSystem;
import com.craftingdead.survival.world.friend.FriendActions;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SFriendTeamActionMessage(
    TeamActionType actionType, UUID targetUuid) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<C2SFriendTeamActionMessage> TYPE =
      NetworkPayloadIds.FRIEND_TEAM_ACTION;

  public static final StreamCodec<FriendlyByteBuf, C2SFriendTeamActionMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(C2SFriendTeamActionMessage::encode, C2SFriendTeamActionMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    out.writeEnum(this.actionType);
    out.writeUUID(this.targetUuid);
  }

  public static C2SFriendTeamActionMessage decode(FriendlyByteBuf in) {
    return new C2SFriendTeamActionMessage(in.readEnum(TeamActionType.class), in.readUUID());
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer serverPlayer)) {
        return;
      }
      switch (this.actionType) {
        case INVITE -> SurvivalTeamSystem.inviteByUuid(serverPlayer, this.targetUuid);
        case KICK -> SurvivalTeamSystem.kickByUuid(serverPlayer, this.targetUuid);
        case TRANSFER -> SurvivalTeamSystem.transferLeadership(serverPlayer, this.targetUuid);
      }
      FriendActions.syncTeamMenu(serverPlayer);
      var server = serverPlayer.getServer();
      if (server != null) {
        ServerPlayer target = server.getPlayerList().getPlayer(this.targetUuid);
        if (target != null) {
          FriendActions.syncTeamMenu(target);
        }
      }
    });
    return true;
  }

  public enum TeamActionType {
    INVITE,
    KICK,
    TRANSFER
  }
}
