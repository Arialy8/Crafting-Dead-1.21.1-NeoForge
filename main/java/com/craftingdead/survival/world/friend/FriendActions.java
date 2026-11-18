package com.craftingdead.survival.world.friend;

import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.S2CSyncFriendMenuMessage;
import com.craftingdead.core.network.message.play.S2CSyncFriendMessage;
import com.craftingdead.survival.world.SurvivalTeamSystem;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FriendActions {

  private FriendActions() {}

  public static void sendFriendRequest(ServerPlayer sender, UUID targetUuid) {
    MinecraftServer server = sender.getServer();
    if (server == null) {
      return;
    }
    ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
    if (target == null) {
      sender.sendSystemMessage(Component.literal("\u00a7c该玩家不在线。"));
      return;
    }
    if (sender.getUUID().equals(targetUuid)) {
      sender.sendSystemMessage(Component.literal("\u00a7c你不能添加自己为好友。"));
      return;
    }
    FriendData friendData = FriendData.get(server);
    PlayerFriendRecord senderRecord = friendData.getOrCreate(sender.getUUID());
    if (senderRecord.isFriend(targetUuid)) {
      sender.sendSystemMessage(Component.literal("\u00a7c该玩家已经是你的好友了。"));
      return;
    }
    PlayerFriendRecord targetRecord = friendData.getOrCreate(targetUuid);
    if (targetRecord.hasPendingRequest(sender.getUUID())) {
      sender.sendSystemMessage(Component.literal("\u00a7c你已经向该玩家发送过好友请求了。"));
      return;
    }
    targetRecord.addRequest(sender.getUUID());
    friendData.setDirty();
    String senderName = sender.getDisplayName().getString();
    String targetName = target.getDisplayName().getString();
    sender.sendSystemMessage(Component.literal("\u00a7a已向 \u00a7f" + targetName
        + " \u00a7a发送好友请求。"));
    target.sendSystemMessage(Component.literal("\u00a7f" + senderName
        + " \u00a7a向你发送了好友请求。"));
    syncFriendMenu(sender, false);
    syncFriendMenu(target, false);
  }

  public static void acceptFriendRequest(ServerPlayer receiver, UUID requesterUuid) {
    MinecraftServer server = receiver.getServer();
    if (server == null) {
      return;
    }
    FriendData friendData = FriendData.get(server);
    PlayerFriendRecord receiverRecord = friendData.getOrCreate(receiver.getUUID());
    if (!receiverRecord.hasPendingRequest(requesterUuid)) {
      receiver.sendSystemMessage(Component.literal("\u00a7c没有来自该玩家的好友请求。"));
      return;
    }
    receiverRecord.removeRequest(requesterUuid);
    receiverRecord.addFriend(requesterUuid);
    PlayerFriendRecord requesterRecord = friendData.getOrCreate(requesterUuid);
    requesterRecord.addFriend(receiver.getUUID());
    friendData.setDirty();
    String requesterName = resolvePlayerName(server, requesterUuid);
    receiver.sendSystemMessage(Component.literal(
        "\u00a7a你现在与 \u00a7f" + requesterName + " \u00a7a成为了好友。"));
    syncFriendMenu(receiver, false);
    syncFriendsToClient(receiver);
    ServerPlayer requester = server.getPlayerList().getPlayer(requesterUuid);
    if (requester != null) {
      String receiverName = receiver.getDisplayName().getString();
      requester.sendSystemMessage(Component.literal(
          "\u00a7f" + receiverName + " \u00a7a接受了你的好友请求。"));
      syncFriendMenu(requester, false);
      syncFriendsToClient(requester);
    }
  }

  public static void denyFriendRequest(ServerPlayer receiver, UUID requesterUuid) {
    MinecraftServer server = receiver.getServer();
    if (server == null) {
      return;
    }
    FriendData friendData = FriendData.get(server);
    PlayerFriendRecord receiverRecord = friendData.getOrCreate(receiver.getUUID());
    if (!receiverRecord.removeRequest(requesterUuid)) {
      receiver.sendSystemMessage(Component.literal("\u00a7c没有来自该玩家的好友请求。"));
      return;
    }
    friendData.setDirty();
    String requesterName = resolvePlayerName(server, requesterUuid);
    receiver.sendSystemMessage(Component.literal(
        "\u00a7e已拒绝来自 \u00a7f" + requesterName + " \u00a7e的好友请求。"));
    syncFriendMenu(receiver, false);
  }

  public static void removeFriend(ServerPlayer sender, UUID targetUuid) {
    MinecraftServer server = sender.getServer();
    if (server == null) {
      return;
    }
    if (sender.getUUID().equals(targetUuid)) {
      sender.sendSystemMessage(Component.literal("\u00a7c你不能移除自己。"));
      return;
    }
    FriendData friendData = FriendData.get(server);
    PlayerFriendRecord senderRecord = friendData.getOrCreate(sender.getUUID());
    if (!senderRecord.isFriend(targetUuid)) {
      sender.sendSystemMessage(Component.literal("\u00a7c该玩家不是你的好友。"));
      return;
    }
    String targetName = resolvePlayerName(server, targetUuid);
    senderRecord.removeFriend(targetUuid);
    PlayerFriendRecord targetRecord = friendData.getRecord(targetUuid);
    if (targetRecord != null) {
      targetRecord.removeFriend(sender.getUUID());
    }
    friendData.setDirty();
    sender.sendSystemMessage(Component.literal(
        "\u00a7f" + targetName + " \u00a77已从你的好友列表中移除。"));
    syncFriendMenu(sender, false);
    ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
    if (target != null) {
      syncFriendMenu(target, false);
    }
  }

  public static void syncFriendMenu(ServerPlayer player, boolean openScreen) {
    List<FriendEntry> onlinePlayers = getOnlinePlayers(player);
    List<FriendEntry> friends = getFriends(player);
    List<FriendEntry> requests = getRequests(player);
    List<FriendEntry> teamMembers = getTeamMembers(player);
    UUID ownerUuid = SurvivalTeamSystem.getSquadOwnerUuid(player);
    boolean isInSquad = SurvivalTeamSystem.isInSquad(player);
    NetworkChannel.PLAY.sendToPlayer(player,
        new S2CSyncFriendMenuMessage(onlinePlayers, friends, requests,
            teamMembers, ownerUuid, isInSquad, openScreen));
  }

  public static void syncTeamMenu(ServerPlayer player) {
    List<FriendEntry> teamMembers = getTeamMembers(player);
    UUID ownerUuid = SurvivalTeamSystem.getSquadOwnerUuid(player);
    boolean isInSquad = SurvivalTeamSystem.isInSquad(player);
    NetworkChannel.PLAY.sendToPlayer(player,
        new S2CSyncFriendMenuMessage(
            List.of(), List.of(), List.of(),
            teamMembers, ownerUuid, isInSquad, false));
  }

  public static void syncFriendsToClient(ServerPlayer player) {
    MinecraftServer server = player.getServer();
    if (server == null) {
      return;
    }
    FriendData friendData = FriendData.get(server);
    PlayerFriendRecord record = friendData.getRecord(player.getUUID());
    List<UUID> friendUuids = record != null ? List.copyOf(record.getFriends()) : List.of();
    NetworkChannel.PLAY.sendToPlayer(player, new S2CSyncFriendMessage(friendUuids));
  }

  private static List<FriendEntry> getOnlinePlayers(ServerPlayer viewer) {
    MinecraftServer server = viewer.getServer();
    List<FriendEntry> entries = new ArrayList<>();
    if (server == null) {
      return entries;
    }
    for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
      if (!onlinePlayer.getUUID().equals(viewer.getUUID())) {
        entries.add(new FriendEntry(
            onlinePlayer.getUUID(),
            onlinePlayer.getDisplayName().getString(),
            true));
      }
    }
    return entries;
  }

  private static List<FriendEntry> getFriends(ServerPlayer viewer) {
    MinecraftServer server = viewer.getServer();
    List<FriendEntry> entries = new ArrayList<>();
    if (server == null) {
      return entries;
    }
    FriendData friendData = FriendData.get(server);
    PlayerFriendRecord record = friendData.getRecord(viewer.getUUID());
    if (record == null) {
      return entries;
    }
    for (UUID friendUuid : record.getFriends()) {
      String name = resolvePlayerName(server, friendUuid);
      ServerPlayer onlineFriend = server.getPlayerList().getPlayer(friendUuid);
      entries.add(new FriendEntry(friendUuid, name, onlineFriend != null));
    }
    return entries;
  }

  private static List<FriendEntry> getRequests(ServerPlayer viewer) {
    MinecraftServer server = viewer.getServer();
    List<FriendEntry> entries = new ArrayList<>();
    if (server == null) {
      return entries;
    }
    FriendData friendData = FriendData.get(server);
    PlayerFriendRecord record = friendData.getRecord(viewer.getUUID());
    if (record == null) {
      return entries;
    }
    for (UUID requesterUuid : record.getFriendRequests()) {
      String name = resolvePlayerName(server, requesterUuid);
      ServerPlayer requester = server.getPlayerList().getPlayer(requesterUuid);
      entries.add(new FriendEntry(requesterUuid, name, requester != null));
    }
    return entries;
  }

  private static List<FriendEntry> getTeamMembers(ServerPlayer viewer) {
    MinecraftServer server = viewer.getServer();
    List<FriendEntry> entries = new ArrayList<>();
    if (server == null) {
      return entries;
    }
    for (UUID memberUuid : SurvivalTeamSystem.getTeamMembers(viewer)) {
      String name = resolvePlayerName(server, memberUuid);
      ServerPlayer onlineMember = server.getPlayerList().getPlayer(memberUuid);
      entries.add(new FriendEntry(memberUuid, name, onlineMember != null));
    }
    return entries;
  }

  private static String resolvePlayerName(MinecraftServer server, UUID playerUuid) {
    return server.getProfileCache().get(playerUuid)
        .map(GameProfile::getName)
        .orElse("Unknown");
  }
}
