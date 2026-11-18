package com.craftingdead.survival.world.friend;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class FriendData extends SavedData {

  private static final String DATA_NAME = "craftingdeadsurvival_friends";
  private static final String TAG_RECORDS = "Records";
  private static final String TAG_UUID = "UUID";
  private static final String TAG_DATA = "Data";

  private static final Factory<FriendData> FACTORY = new Factory<>(
      FriendData::new, FriendData::load, DataFixTypes.LEVEL);

  private final Map<UUID, PlayerFriendRecord> records = new HashMap<>();

  public static FriendData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  public static FriendData get(MinecraftServer server) {
    return get(server.overworld());
  }

  public PlayerFriendRecord getOrCreate(UUID playerUuid) {
    return this.records.computeIfAbsent(playerUuid, uuid -> new PlayerFriendRecord("Unknown"));
  }

  public PlayerFriendRecord getRecord(UUID playerUuid) {
    return this.records.get(playerUuid);
  }

  public void addFriend(UUID playerUuid, UUID friendUuid) {
    this.getOrCreate(playerUuid).addFriend(friendUuid);
    this.setDirty();
  }

  public void removeFriend(UUID playerUuid, UUID friendUuid) {
    PlayerFriendRecord record = this.records.get(playerUuid);
    if (record != null) {
      record.removeFriend(friendUuid);
      this.setDirty();
    }
  }

  public void addRequest(UUID playerUuid, UUID requesterUuid) {
    this.getOrCreate(playerUuid).addRequest(requesterUuid);
    this.setDirty();
  }

  public boolean removeRequest(UUID playerUuid, UUID requesterUuid) {
    PlayerFriendRecord record = this.records.get(playerUuid);
    if (record != null) {
      boolean removed = record.removeRequest(requesterUuid);
      if (removed) {
        this.setDirty();
      }
      return removed;
    }
    return false;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag recordsTag = new ListTag();
    for (Map.Entry<UUID, PlayerFriendRecord> entry : this.records.entrySet()) {
      CompoundTag recordTag = new CompoundTag();
      recordTag.putUUID(TAG_UUID, entry.getKey());
      recordTag.put(TAG_DATA, entry.getValue().save(new CompoundTag()));
      recordsTag.add(recordTag);
    }
    tag.put(TAG_RECORDS, recordsTag);
    return tag;
  }

  private static FriendData load(CompoundTag tag, HolderLookup.Provider registries) {
    FriendData data = new FriendData();
    ListTag recordsTag = tag.getList(TAG_RECORDS, Tag.TAG_COMPOUND);
    for (int index = 0; index < recordsTag.size(); index++) {
      CompoundTag recordTag = recordsTag.getCompound(index);
      UUID playerUuid = recordTag.getUUID(TAG_UUID);
      PlayerFriendRecord record = PlayerFriendRecord.load(recordTag.getCompound(TAG_DATA));
      data.records.put(playerUuid, record);
    }
    return data;
  }
}
