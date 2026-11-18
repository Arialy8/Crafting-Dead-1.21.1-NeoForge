package com.craftingdead.immerse.world.base;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class BlueprintRestrictionData extends SavedData {

  private static final String DATA_NAME = "craftingdeadimmerse_blueprint_restrictions";
  private static final String ZONES_TAG = "Zones";
  private static final SavedData.Factory<BlueprintRestrictionData> FACTORY =
      new SavedData.Factory<>(BlueprintRestrictionData::new,
          BlueprintRestrictionData::load, DataFixTypes.LEVEL);

  private final Map<String, RestrictedZone> zones = new LinkedHashMap<>();

  public record RestrictedZone(String name, BlockPos min, BlockPos max) {

    public static RestrictedZone create(String name, BlockPos first, BlockPos second) {
      BlockPos min = new BlockPos(
          Math.min(first.getX(), second.getX()),
          Math.min(first.getY(), second.getY()),
          Math.min(first.getZ(), second.getZ()));
      BlockPos max = new BlockPos(
          Math.max(first.getX(), second.getX()),
          Math.max(first.getY(), second.getY()),
          Math.max(first.getZ(), second.getZ()));
      return new RestrictedZone(name, min, max);
    }

    public boolean contains(BlockPos pos) {
      return pos.getX() >= this.min.getX() && pos.getX() <= this.max.getX()
          && pos.getY() >= this.min.getY() && pos.getY() <= this.max.getY()
          && pos.getZ() >= this.min.getZ() && pos.getZ() <= this.max.getZ();
    }
  }

  public static boolean isRestricted(ServerLevel level, BlockPos pos) {
    return level != null && pos != null && get(level).zones.values().stream()
        .anyMatch(zone -> zone.contains(pos));
  }

  public static boolean add(ServerLevel level, String name, BlockPos first, BlockPos second) {
    if (level == null || name == null || name.isBlank() || first == null || second == null) {
      return false;
    }
    BlueprintRestrictionData data = get(level);
    String key = normalize(name);
    if (data.zones.containsKey(key)) {
      return false;
    }
    data.zones.put(key, RestrictedZone.create(name, first, second));
    data.setDirty();
    return true;
  }

  public static boolean remove(ServerLevel level, String name) {
    if (level == null || name == null) {
      return false;
    }
    BlueprintRestrictionData data = get(level);
    if (data.zones.remove(normalize(name)) == null) {
      return false;
    }
    data.setDirty();
    return true;
  }

  public static Collection<RestrictedZone> list(ServerLevel level) {
    return level == null ? java.util.List.of() : java.util.List.copyOf(get(level).zones.values());
  }

  public static Optional<RestrictedZone> find(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return Optional.empty();
    }
    return get(level).zones.values().stream().filter(zone -> zone.contains(pos)).findFirst();
  }

  private static String normalize(String name) {
    return name.toLowerCase(java.util.Locale.ROOT);
  }

  private static BlueprintRestrictionData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  private static BlueprintRestrictionData load(CompoundTag tag,
      HolderLookup.Provider registries) {
    BlueprintRestrictionData data = new BlueprintRestrictionData();
    ListTag zonesTag = tag.getList(ZONES_TAG, Tag.TAG_COMPOUND);
    for (int i = 0; i < zonesTag.size(); i++) {
      CompoundTag zoneTag = zonesTag.getCompound(i);
      String name = zoneTag.getString("Name");
      if (name.isBlank()) {
        continue;
      }
      BlockPos min = new BlockPos(zoneTag.getInt("MinX"), zoneTag.getInt("MinY"),
          zoneTag.getInt("MinZ"));
      BlockPos max = new BlockPos(zoneTag.getInt("MaxX"), zoneTag.getInt("MaxY"),
          zoneTag.getInt("MaxZ"));
      data.zones.put(normalize(name), new RestrictedZone(name, min, max));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag zonesTag = new ListTag();
    for (RestrictedZone zone : this.zones.values()) {
      CompoundTag zoneTag = new CompoundTag();
      zoneTag.putString("Name", zone.name());
      zoneTag.putInt("MinX", zone.min().getX());
      zoneTag.putInt("MinY", zone.min().getY());
      zoneTag.putInt("MinZ", zone.min().getZ());
      zoneTag.putInt("MaxX", zone.max().getX());
      zoneTag.putInt("MaxY", zone.max().getY());
      zoneTag.putInt("MaxZ", zone.max().getZ());
      zonesTag.add(zoneTag);
    }
    tag.put(ZONES_TAG, zonesTag);
    return tag;
  }
}
