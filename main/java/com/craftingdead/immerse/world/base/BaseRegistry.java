package com.craftingdead.immerse.world.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class BaseRegistry extends SavedData {

  private static final String DATA_NAME = "craftingdeadimmerse_bases";
  private static final String BASES_TAG = "Bases";
  private static final SavedData.Factory<BaseRegistry> FACTORY =
      new SavedData.Factory<>(BaseRegistry::new, BaseRegistry::load, DataFixTypes.LEVEL);

  private final Map<ResourceKey<Level>, Map<UUID, BlockPos>> basesByDimension = new HashMap<>();

  public static Optional<BlockPos> getBasePos(Level level, UUID owner) {
    if (!(level instanceof ServerLevel serverLevel) || owner == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(get(serverLevel).basesByDimension
        .getOrDefault(level.dimension(), Map.of()).get(owner));
  }

  public static Optional<BlockPos> findBasePos(Level level, BlockPos target, int radius) {
    if (!(level instanceof ServerLevel serverLevel) || target == null) {
      return Optional.empty();
    }
    return get(serverLevel).basesByDimension.getOrDefault(level.dimension(), Map.of()).values()
        .stream()
        .filter(pos -> Math.abs(pos.getX() - target.getX()) <= radius
            && Math.abs(pos.getY() - target.getY()) <= radius
            && Math.abs(pos.getZ() - target.getZ()) <= radius)
        .min(java.util.Comparator.comparingDouble(pos -> pos.distSqr(target)));
  }

  public static void register(Level level, UUID owner, BlockPos pos) {
    if (!(level instanceof ServerLevel serverLevel) || owner == null || pos == null) {
      return;
    }
    BaseRegistry registry = get(serverLevel);
    BlockPos previous = registry.basesByDimension
        .computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
        .put(owner, pos.immutable());
    if (!pos.equals(previous)) {
      registry.setDirty();
    }
  }

  public static void unregister(Level level, UUID owner, BlockPos pos) {
    if (!(level instanceof ServerLevel serverLevel) || owner == null || pos == null) {
      return;
    }
    BaseRegistry registry = get(serverLevel);
    Map<UUID, BlockPos> bases = registry.basesByDimension.get(level.dimension());
    if (bases != null && pos.equals(bases.get(owner))) {
      bases.remove(owner);
      if (bases.isEmpty()) {
        registry.basesByDimension.remove(level.dimension());
      }
      registry.setDirty();
    }
  }

  private static BaseRegistry get(ServerLevel level) {
    return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  private static BaseRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
    BaseRegistry registry = new BaseRegistry();
    ListTag basesTag = tag.getList(BASES_TAG, Tag.TAG_COMPOUND);
    for (int i = 0; i < basesTag.size(); i++) {
      CompoundTag baseTag = basesTag.getCompound(i);
      if (!baseTag.hasUUID("Owner") || !baseTag.contains("Dimension", Tag.TAG_STRING)) {
        continue;
      }
      ResourceLocation dimensionId = ResourceLocation.tryParse(baseTag.getString("Dimension"));
      if (dimensionId == null) {
        continue;
      }
      ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
      BlockPos pos = new BlockPos(baseTag.getInt("X"), baseTag.getInt("Y"), baseTag.getInt("Z"));
      registry.basesByDimension.computeIfAbsent(dimension, ignored -> new HashMap<>())
          .put(baseTag.getUUID("Owner"), pos);
    }
    return registry;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag basesTag = new ListTag();
    this.basesByDimension.forEach((dimension, bases) -> bases.forEach((owner, pos) -> {
      CompoundTag baseTag = new CompoundTag();
      baseTag.putString("Dimension", dimension.location().toString());
      baseTag.putUUID("Owner", owner);
      baseTag.putInt("X", pos.getX());
      baseTag.putInt("Y", pos.getY());
      baseTag.putInt("Z", pos.getZ());
      basesTag.add(baseTag);
    }));
    tag.put(BASES_TAG, basesTag);
    return tag;
  }
}
