package com.craftingdead.survival.client.friend;

import com.craftingdead.survival.world.friend.FriendEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FriendClientData {

  private static List<FriendEntry> onlinePlayers = new ArrayList<>();
  private static List<FriendEntry> friends = new ArrayList<>();
  private static List<FriendEntry> requests = new ArrayList<>();
  private static List<FriendEntry> teamMembers = new ArrayList<>();
  private static UUID ownerUuid;
  private static boolean isInSquad;

  private FriendClientData() {}

  public static void set(List<FriendEntry> onlinePlayers,
      List<FriendEntry> friends, List<FriendEntry> requests,
      List<FriendEntry> teamMembers, UUID ownerUuid, boolean isInSquad) {
    FriendClientData.onlinePlayers = new ArrayList<>(onlinePlayers);
    FriendClientData.friends = new ArrayList<>(friends);
    FriendClientData.requests = new ArrayList<>(requests);
    FriendClientData.teamMembers = new ArrayList<>(teamMembers);
    FriendClientData.ownerUuid = ownerUuid;
    FriendClientData.isInSquad = isInSquad;
  }

  public static List<FriendEntry> getOnlinePlayers() {
    return onlinePlayers;
  }

  public static List<FriendEntry> getFriends() {
    return friends;
  }

  public static List<FriendEntry> getRequests() {
    return requests;
  }

  public static List<FriendEntry> getTeamMembers() {
    return teamMembers;
  }

  public static UUID getOwnerUuid() {
    return ownerUuid;
  }

  public static boolean isInSquad() {
    return isInSquad;
  }
}
