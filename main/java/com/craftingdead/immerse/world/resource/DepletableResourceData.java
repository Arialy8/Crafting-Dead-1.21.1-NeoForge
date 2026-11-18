package com.craftingdead.immerse.world.resource;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent harvest counts for Crafting Dead's non-destructive resource gathering. */
public final class DepletableResourceData extends SavedData {

  private static final String DATA_NAME = "craftingdeadimmerse_depletable_resources";
  private static final String ENTRIES_TAG = "Entries";
  private static final SavedData.Factory<DepletableResourceData> FACTORY =
      new SavedData.Factory<>(DepletableResourceData::new,
          DepletableResourceData::load, DataFixTypes.LEVEL);

  private final Map<Long, ResourceEntry> entries = new HashMap<>();

  public record HarvestResult(int amount, int remaining, int capacity) {
    public boolean exhausted() {
      return this.remaining == 0;
    }
  }

  private record ResourceEntry(String kind, int remaining, int capacity) {}

  public static HarvestResult harvest(ServerLevel level, BlockPos pos, String kind,
      RandomSource random) {
    DepletableResourceData data = get(level);
    long key = pos.asLong();
    ResourceEntry entry = data.entries.get(key);
    if (entry == null || !entry.kind().equals(kind)) {
      int capacity = 5 + random.nextInt(6);
      entry = new ResourceEntry(kind, capacity, capacity);
    }

    if (entry.remaining() <= 0) {
      data.entries.put(key, entry);
      return new HarvestResult(0, 0, entry.capacity());
    }

    double fullness = (double) entry.remaining() / entry.capacity();
    int amount = 1;
    if (random.nextDouble() < 0.55D * fullness) {
      amount++;
    }
    if (random.nextDouble() < 0.30D * fullness * fullness) {
      amount++;
    }
    if (random.nextDouble() < 0.12D * fullness * fullness * fullness) {
      amount++;
    }

    int remaining = entry.remaining() - 1;
    data.entries.put(key, new ResourceEntry(kind, remaining, entry.capacity()));
    data.setDirty();
    return new HarvestResult(amount, remaining, entry.capacity());
  }

  public static void remove(ServerLevel level, BlockPos pos) {
    DepletableResourceData data = get(level);
    if (data.entries.remove(pos.asLong()) != null) {
      data.setDirty();
    }
  }

  private static DepletableResourceData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  private static DepletableResourceData load(CompoundTag tag,
      HolderLookup.Provider registries) {
    DepletableResourceData data = new DepletableResourceData();
    ListTag entriesTag = tag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
    for (int i = 0; i < entriesTag.size(); i++) {
      CompoundTag entryTag = entriesTag.getCompound(i);
      String kind = entryTag.getString("Kind");
      int remaining = entryTag.getInt("Remaining");
      int capacity = entryTag.getInt("Capacity");
      if (!kind.isBlank() && capacity >= 5 && capacity <= 10
          && remaining >= 0 && remaining <= capacity) {
        data.entries.put(entryTag.getLong("Pos"),
            new ResourceEntry(kind, remaining, capacity));
      }
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag entriesTag = new ListTag();
    for (var entry : this.entries.entrySet()) {
      CompoundTag entryTag = new CompoundTag();
      entryTag.putLong("Pos", entry.getKey());
      entryTag.putString("Kind", entry.getValue().kind());
      entryTag.putInt("Remaining", entry.getValue().remaining());
      entryTag.putInt("Capacity", entry.getValue().capacity());
      entriesTag.add(entryTag);
    }
    tag.put(ENTRIES_TAG, entriesTag);
    return tag;
  }
}
