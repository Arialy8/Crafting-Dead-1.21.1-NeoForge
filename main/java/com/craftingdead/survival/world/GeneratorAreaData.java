package com.craftingdead.survival.world;

import com.craftingdead.survival.world.item.GeneratorAreaSelectorItem;
import com.craftingdead.survival.world.level.block.SurvivalBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

public final class GeneratorAreaData extends SavedData {
  private static final String DATA_NAME = "craftingdeadsurvival_generator_areas";
  private static final SavedData.Factory<GeneratorAreaData> FACTORY =
      new SavedData.Factory<>(GeneratorAreaData::new, GeneratorAreaData::load, DataFixTypes.LEVEL);
  private static final String[] TYPES = {"civilian_loot_gen", "civilian_rare_loot_gen",
      "medic_loot_gen", "military_loot_gen", "police_loot_gen"};
  private static final String[] LOOT_TYPES = {"civilian_loot", "civilian_rare_loot",
      "medic_loot", "military_loot", "police_loot"};
  private static final Map<UUID, Draft> DRAFTS = new HashMap<>();
  private final Map<String, Area> areas = new HashMap<>();

  public static void setPosition(UUID player, int index, BlockPos pos) {
    Draft old = DRAFTS.getOrDefault(player, new Draft(null, null));
    DRAFTS.put(player, index == 1 ? new Draft(pos.immutable(), old.pos2())
        : new Draft(old.pos1(), pos.immutable()));
  }

  public static Draft getDraft(UUID player) {
    return DRAFTS.get(player);
  }

  public static GeneratorAreaData get(ServerLevel level) {
    return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  public int create(ServerLevel level, String name, Draft draft, ItemStack selector) {
    String key = name.toLowerCase(java.util.Locale.ROOT);
    if (draft == null || draft.pos1() == null || draft.pos2() == null || this.areas.containsKey(key)) {
      return -1;
    }
    int minX = Math.min(draft.pos1().getX(), draft.pos2().getX());
    int maxX = Math.max(draft.pos1().getX(), draft.pos2().getX());
    int minY = Math.min(draft.pos1().getY(), draft.pos2().getY());
    int maxY = Math.max(draft.pos1().getY(), draft.pos2().getY());
    int minZ = Math.min(draft.pos1().getZ(), draft.pos2().getZ());
    int maxZ = Math.max(draft.pos1().getZ(), draft.pos2().getZ());
    int requested = GeneratorAreaSelectorItem.getCount(selector);
    Set<BlockPos> placed = new HashSet<>();
    int attempts = Math.max(100, requested * 40);
    while (placed.size() < requested && attempts-- > 0) {
      int x = minX + level.random.nextInt(maxX - minX + 1);
      int z = minZ + level.random.nextInt(maxZ - minZ + 1);
      List<BlockPos> columnCandidates = new ArrayList<>();
      int scanMinY = Math.max(minY, level.getMinBuildHeight() + 1);
      int scanMaxY = Math.min(maxY, level.getMaxBuildHeight() - 1);
      for (int y = scanMinY; y <= scanMaxY; y++) {
        BlockPos candidate = new BlockPos(x, y, z);
        if (!placed.contains(candidate) && canPlace(level, candidate)) {
          columnCandidates.add(candidate);
        }
      }
      if (columnCandidates.isEmpty()) {
        continue;
      }
      BlockPos surface = columnCandidates.get(level.random.nextInt(columnCandidates.size()));
      int type = chooseType(level, selector);
      if (level.setBlock(surface, SurvivalBlocks.BLOCKS.get(TYPES[type]).get().defaultBlockState(), 3)) {
        placed.add(surface.immutable());
      }
    }
    this.areas.put(key, new Area(name, level.dimension().location().toString(),
        draft.pos1().immutable(), draft.pos2().immutable(), List.copyOf(placed)));
    this.setDirty();
    return placed.size();
  }

  private static boolean canPlace(ServerLevel level, BlockPos pos) {
    BlockState target = level.getBlockState(pos);
    BlockState ground = level.getBlockState(pos.below());
    return level.isInWorldBounds(pos) && (target.isAir() || target.canBeReplaced())
        && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty()
        && !ground.isAir() && !ground.is(BlockTags.LEAVES)
        && ground.isFaceSturdy(level, pos.below(), Direction.UP);
  }

  private static int chooseType(ServerLevel level, ItemStack selector) {
    int total = 0;
    for (int i = 0; i < TYPES.length; i++) {
      total += GeneratorAreaSelectorItem.getWeight(selector, i);
    }
    if (total <= 0) {
      return level.random.nextInt(TYPES.length);
    }
    int roll = level.random.nextInt(total);
    int sum = 0;
    for (int i = 0; i < TYPES.length; i++) {
      sum += GeneratorAreaSelectorItem.getWeight(selector, i);
      if (roll < sum) return i;
    }
    return TYPES.length - 1;
  }

  public int delete(ServerLevel level, String name) {
    String key = name.toLowerCase(java.util.Locale.ROOT);
    Area area = this.areas.get(key);
    if (area == null || !area.dimension().equals(level.dimension().location().toString())) return -1;
    this.areas.remove(key);
    int removed = 0;
    for (BlockPos pos : area.generators()) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
        BlockPos lootPos = pos.relative(direction);
        if (java.util.Arrays.stream(LOOT_TYPES).anyMatch(type ->
            level.getBlockState(lootPos).is(SurvivalBlocks.BLOCKS.get(type).get()))) {
          level.removeBlock(lootPos, false);
        }
      }
      if (java.util.Arrays.stream(TYPES).anyMatch(type ->
          level.getBlockState(pos).is(SurvivalBlocks.BLOCKS.get(type).get()))) {
        level.removeBlock(pos, false);
        removed++;
      }
    }
    this.setDirty();
    return removed;
  }

  public List<Area> list() { return this.areas.values().stream().toList(); }

  private static GeneratorAreaData load(CompoundTag tag, HolderLookup.Provider registries) {
    GeneratorAreaData data = new GeneratorAreaData();
    ListTag list = tag.getList("Areas", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag entry = list.getCompound(i);
      List<BlockPos> generators = new ArrayList<>();
      for (long packed : entry.getLongArray("Generators")) generators.add(BlockPos.of(packed));
      Area area = new Area(entry.getString("Name"), entry.getString("Dimension"),
          BlockPos.of(entry.getLong("Pos1")), BlockPos.of(entry.getLong("Pos2")), generators);
      data.areas.put(area.name().toLowerCase(java.util.Locale.ROOT), area);
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = new ListTag();
    for (Area area : this.areas.values()) {
      CompoundTag entry = new CompoundTag();
      entry.putString("Name", area.name()); entry.putString("Dimension", area.dimension());
      entry.putLong("Pos1", area.pos1().asLong()); entry.putLong("Pos2", area.pos2().asLong());
      entry.putLongArray("Generators", area.generators().stream().mapToLong(BlockPos::asLong).toArray());
      list.add(entry);
    }
    tag.put("Areas", list); return tag;
  }

  public record Draft(BlockPos pos1, BlockPos pos2) {}
  public record Area(String name, String dimension, BlockPos pos1, BlockPos pos2,
      List<BlockPos> generators) {}
}
