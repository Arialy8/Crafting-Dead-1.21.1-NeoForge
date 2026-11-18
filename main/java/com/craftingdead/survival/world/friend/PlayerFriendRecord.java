package com.craftingdead.survival.world.friend;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class PlayerFriendRecord {

  private static final String TAG_FRIENDS = "Friends";
  private static final String TAG_REQUESTS = "FriendRequests";
  private static final String TAG_NAME = "Name";

  private String name;
  private final List<UUID> friends = new ArrayList<>();
  private final List<UUID> friendRequests = new ArrayList<>();

  public PlayerFriendRecord(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<UUID> getFriends() {
    return this.friends;
  }

  public List<UUID> getFriendRequests() {
    return this.friendRequests;
  }

  public boolean isFriend(UUID uuid) {
    return this.friends.contains(uuid);
  }

  public boolean hasPendingRequest(UUID uuid) {
    return this.friendRequests.contains(uuid);
  }

  public void addFriend(UUID uuid) {
    if (!this.friends.contains(uuid)) {
      this.friends.add(uuid);
    }
  }

  public void removeFriend(UUID uuid) {
    this.friends.remove(uuid);
  }

  public void addRequest(UUID uuid) {
    if (!this.friendRequests.contains(uuid)) {
      this.friendRequests.add(uuid);
    }
  }

  public boolean removeRequest(UUID uuid) {
    return this.friendRequests.remove(uuid);
  }

  public CompoundTag save(CompoundTag tag) {
    tag.putString(TAG_NAME, this.name != null ? this.name : "Unknown");

    ListTag friendsTag = new ListTag();
    for (UUID friendUuid : this.friends) {
      CompoundTag entry = new CompoundTag();
      entry.putUUID("UUID", friendUuid);
      friendsTag.add(entry);
    }
    tag.put(TAG_FRIENDS, friendsTag);

    ListTag requestsTag = new ListTag();
    for (UUID requestUuid : this.friendRequests) {
      CompoundTag entry = new CompoundTag();
      entry.putUUID("UUID", requestUuid);
      requestsTag.add(entry);
    }
    tag.put(TAG_REQUESTS, requestsTag);

    return tag;
  }

  public static PlayerFriendRecord load(CompoundTag tag) {
    PlayerFriendRecord record = new PlayerFriendRecord(tag.getString(TAG_NAME));

    ListTag friendsTag = tag.getList(TAG_FRIENDS, Tag.TAG_COMPOUND);
    for (int index = 0; index < friendsTag.size(); index++) {
      CompoundTag entry = friendsTag.getCompound(index);
      record.friends.add(entry.getUUID("UUID"));
    }

    ListTag requestsTag = tag.getList(TAG_REQUESTS, Tag.TAG_COMPOUND);
    for (int index = 0; index < requestsTag.size(); index++) {
      CompoundTag entry = requestsTag.getCompound(index);
      record.friendRequests.add(entry.getUUID("UUID"));
    }

    return record;
  }
}
