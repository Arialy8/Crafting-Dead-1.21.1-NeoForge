package com.craftingdead.survival.client.friend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ClientFriendCache {

  private static final List<UUID> FRIENDS =
      Collections.synchronizedList(new ArrayList<>());

  private ClientFriendCache() {}

  public static void setFriends(List<UUID> newFriends) {
    synchronized (FRIENDS) {
      FRIENDS.clear();
      FRIENDS.addAll(newFriends);
    }
  }

  public static List<UUID> getFriends() {
    return FRIENDS;
  }
}
